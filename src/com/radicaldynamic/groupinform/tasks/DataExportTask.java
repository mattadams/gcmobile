package com.radicaldynamic.groupinform.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.odk.collect.android.utilities.FileUtils;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mycila.xmltool.CallBack;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.gcmobile.android.activities.DataExportActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.listeners.DataExportListener;
import com.radicaldynamic.groupinform.repositories.FormInstanceRepo;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;
import com.radicaldynamic.groupinform.xform.FormReader;
import com.radicaldynamic.groupinform.xform.Instance;

//public class DataExportTask extends AsyncTask<Params, Progress, Result>
public class DataExportTask extends AsyncTask<Object, String, Void> 
{
    private final static String t = "DataExportTask: ";
    
    public final static int COMPLETE = 0;
    public final static int ERROR = 1;
    public final static int PROGRESS = 2;
    
    private static final String DATETIME = "yyyy_MM_dd-HH_mm_ss";
    
    private String mAttachmentPath;
    private String mCompleteMsg;    
    private String mErrorMsg;
    
    private Bundle mExportOptions;
    private Handler mHandler;
    private DataExportListener mStateListener;
    
    private FormDefinition mFormDefinition;
    private FormReader mFormReader;
    
    private LinkedHashMap<String, String> mExportHeaders = new LinkedHashMap<String, String>();
    private LinkedList<HashMap<String, ? super Object>> mExportData = new LinkedList<HashMap<String, ? super Object>>();
    private List<FormInstance> mExportList = new ArrayList<FormInstance>();
    
    @Override
    protected Void doInBackground(Object... params) 
    {
        final String tt = t + "doInBackground(): ";
        
        mHandler = (Handler) params[0];
        mFormDefinition = (FormDefinition) params[1];
        mExportOptions = (Bundle) params[2];
        
        int i;
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
                
        try {
            publishProgress("Retreiving form template...");
            AttachmentInputStream ais = Collect.getInstance().getDbService().getDb().getAttachment(mFormDefinition.getId(), "xml");
            
            publishProgress("Parsing template...");
            mFormReader = new FormReader(ais, false);
            ais.close();
            
            publishProgress("Generating headers...");

            // Include metadata headers?
            if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_RECORD_METADATA, false)) {
                mExportHeaders.put("formDefinitionName", "Template Name");
                mExportHeaders.put("formDefinitionUuid", "Template ID");
            }

            mExportHeaders.put("rowId", "Row Number");
            mExportHeaders.put("recordUuid", "Record ID");
                
            if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_RECORD_METADATA, false)) {
                mExportHeaders.put("dateCreated", "Record Date Created");
                mExportHeaders.put("createdBy", "Record Created By");
                mExportHeaders.put("dateUpdated", "Record Date Updated");
                mExportHeaders.put("updatedBy", "Record Updated By");
                mExportHeaders.put("recordStatus", "Record Status");
            }

            generateExportHeaders(mFormReader.getInstance());
            
            // Exit early if error
            if (mErrorMsg != null)
                return null;

            publishProgress("Retrieving records...");
            List<FormInstance> unfilteredList = ((FormInstanceRepo) new FormInstanceRepo(Collect.getInstance().getDbService().getDb())).findByFormId(mFormDefinition.getId());
            
            // Filter list
            for (i = 0; i < unfilteredList.size(); i++) {
                if (unfilteredList.get(i).getStatus().equals(FormInstance.Status.complete) 
                        && mExportOptions.getBoolean(DataExportActivity.KEY_EXPORT_COMPLETED, false)) {
                    mExportList.add(unfilteredList.get(i));
                }
                
                if (unfilteredList.get(i).getStatus().equals(FormInstance.Status.draft) 
                        && mExportOptions.getBoolean(DataExportActivity.KEY_EXPORT_DRAFT, false)) {
                    mExportList.add(unfilteredList.get(i));
                }
            }
            
            if (mExportList.size() == 0) {
                mErrorMsg = "No records found to export!";
                return null;
            }

            // Directory to place data files
            String prefix = "export_" + getExportTimestamp();
            String exportPath = Environment.getExternalStorageDirectory() + File.separator + prefix + File.separator; 
            FileUtils.createFolder(exportPath);

            // Compile export data for each instance
            for (i = 0; i < mExportList.size(); i++) {
                int idx = i + 1;                
                publishProgress("Exporting record " + idx + "/" + mExportList.size());
                
                FormInstance instance = mExportList.get(i);
                String instancePath = exportPath + File.separator + idx + "-" + instance.getId() + ".xml";
                
                if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "processing " + instance.getId() + " for export");

                HashMap<String, Attachment> attachments = (HashMap<String, Attachment>) instance.getAttachments();
                
                if (attachments == null) {
                    if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "skipping attachment download for " + instance.getId() + ": no attachments!");
                    continue;
                }

                // Download attachments (form instance XML & other media)
                for (Entry<String, Attachment> entry : attachments.entrySet()) {
                    String key = entry.getKey();
                    FileOutputStream file;
                    
                    if (key.equals("xml")) {
                        file = new FileOutputStream(instancePath);
                    } else {
                        // Determine if we should download the media file or skip it, as per the user
                        if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_MEDIA_FILES, false)) { 
                            file = new FileOutputStream(exportPath + File.separator + idx + "-" + instance.getId() + "-" + key);
                        } else {
                            continue;
                        }
                    }
                    
                    ais = Collect.getInstance().getDbService().getDb().getAttachment(instance.getId(), key);
                    
                    byte [] buffer = new byte[8192];
                    int bytesRead = 0;
                    
                    while ((bytesRead = ais.read(buffer)) != -1) {
                        file.write(buffer, 0, bytesRead);
                    }
                    
                    file.close(); 
                    ais.close();
                }
                
                // Prepare new record for export
                mExportData.add(new HashMap<String, Object>());

                // Add in per-record metadata?
                if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_RECORD_METADATA, false)) {
                    mExportData.getLast().put(mExportHeaders.get("formDefinitionUuid"), getStringValue(mFormDefinition.getId()));
                    mExportData.getLast().put(mExportHeaders.get("formDefinitionName"), getStringValue(mFormDefinition.getName()));
                }

                mExportData.getLast().put(mExportHeaders.get("rowId"), idx);
                mExportData.getLast().put(mExportHeaders.get("recordUuid"), instance.getId());
                    
                if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_RECORD_METADATA, false)) {
                    mExportData.getLast().put(mExportHeaders.get("recordStatus"), getStringValue(instance.getStatus().toString()));
                    mExportData.getLast().put(mExportHeaders.get("dateCreated"), getStringValue(instance.getDateCreated()));
                    mExportData.getLast().put(mExportHeaders.get("createdBy"), getStringValue(instance.getCreatedByAlias()));
                    mExportData.getLast().put(mExportHeaders.get("dateUpdated"), getStringValue(instance.getDateUpdated()));
                    mExportData.getLast().put(mExportHeaders.get("updatedBy"), getStringValue(instance.getUpdatedByAlias()));
                }
                
                // Parse instance data from XML file
                FileInputStream fis = new FileInputStream(new File(instancePath));
                XMLTag instanceXml = XMLDoc.from(fis, false);
                readData(instanceXml, "/" + instanceXml.getCurrentTagName());
                fis.close();
                
                // Remove XForm instance file unless the user has opted to keep it
                if (!mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_XFORM_FILES, false)) {
                    new File(instancePath).delete();
                }   
            }
            
            publishProgress("Writing CSV file...");            
            ICsvMapWriter writer = new CsvMapWriter(new FileWriter(exportPath + prefix + ".csv"), CsvPreference.EXCEL_PREFERENCE);
            
            Object[] headerObjects = mExportHeaders.values().toArray();
            String[] headers = Arrays.asList(headerObjects).toArray(new String[headerObjects.length]);
            writer.writeHeader(headers);            
            
            for (i = 0; i < mExportData.size(); i++) {                
                writer.write(mExportData.get(i), headers);
            }
            
            writer.close();
            
            // Total successfully exported vs. total in list to export
            String exportTally = mExportData.size() + "/" + mExportList.size();

            // Create a ZIP archive containing the requested file
            if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_ZIP, false)) {
                publishProgress("Compressing exported data...");
                
                String zip = Environment.getExternalStorageDirectory() + File.separator + prefix + ".zip";
                ZipArchiveOutputStream os = new ZipArchiveOutputStream(new File(zip));
                
                if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, tt + "creating zip file " + zip);
                
                for (File f : new File(exportPath).listFiles()) {
                    if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "adding " + f.getName() + " to zip file");
                    
                    os.putArchiveEntry(os.createArchiveEntry(f, f.getName()));
                    
                    FileInputStream is = new FileInputStream(f);                    
                    byte [] buffer = new byte[8192];
                    int bytesRead = 0;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }

                    is.close();                    
                    os.closeArchiveEntry();                    
                }
                
                os.close();
                
                // Remove export data directory
                FileUtilsExtended.deleteFolder(exportPath);
                
                mCompleteMsg = exportTally + " records have been successfully exported to the external storage on this device.\n\nPlease look for a ZIP file with the following name:\n\n" + prefix;
            } else {
                mCompleteMsg = exportTally + " records have been successfully exported to the external storage on this device.\n\nPlease look for a folder with the following name:\n\n" + prefix;
            }
            
            // If emailing, make a copy of the exported ZIP in the cache directory
            if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_SEND, false)) {
                mAttachmentPath = FileUtilsExtended.EXTERNAL_CACHE + File.separator + prefix + ".zip";
                
                org.apache.commons.io.FileUtils.copyFile(
                        new File(Environment.getExternalStorageDirectory() + File.separator + prefix + ".zip"), 
                        new File(mAttachmentPath));

                if (mExportOptions.getBoolean(DataExportActivity.KEY_OUTPUT_EXTERNAL, false)) {
                    mCompleteMsg = mCompleteMsg + "\n\nSelect \"Send\" to compose an email with the exported data attached in a ZIP file or to transfer the file via Bluetooth.";
                } else {
                    // Remove from /sdcard if user didn't ask for it to be there
                    new File(Environment.getExternalStorageDirectory() + File.separator + prefix + ".zip").delete();
                    
                    // Modify complete message
                    mCompleteMsg = exportTally + " records have been successfully exported.\n\nSelect \"Send\" to compose an email with the exported data attached in a ZIP file or to transfer the file via Bluetooth.";
                }
            }
        } catch (Exception e) {
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "problem exporting data: " + e.toString());
            e.printStackTrace();
            
            mErrorMsg = "An error occured while exporting your data. Please contact our support team at support@groupcomplete.com with this error message:\n\n"
                + e.toString();                
        }
        
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values)
    {
        Message update = Message.obtain();
        update.what = PROGRESS;
        
        Bundle data = new Bundle();
        data.putString(DataExportActivity.KEY_PROGRESS_MSG, values[0]);
        update.setData(data);
        
        mHandler.sendMessage(update);
    }

    @Override
    protected void onPostExecute(Void nothing) 
    {
        synchronized (this) {
            if (mStateListener != null) {
                Message done = Message.obtain();
                done.what = COMPLETE;
                mHandler.sendMessage(done);
                
                if (mErrorMsg == null) {
                    Bundle b = new Bundle();
                    b.putString(DataExportListener.KEY_MESSAGE, mCompleteMsg);
                    b.putString(DataExportListener.KEY_EMAIL_ATTACHMENT, mAttachmentPath);
                    mStateListener.exportComplete(b);
                } else {
                    mStateListener.exportError(mErrorMsg);
                }
            }
        }
    }
    
    public void setDataExportListener(DataExportListener sl) 
    {
        synchronized (this) {
            mStateListener = sl;
        }
    }
    
    public void setHandler(Handler h)
    {
        mHandler = h;
    }

    private void generateExportHeaders(ArrayList<Instance> instances)
    {
        final String tt = t + "buildExportHeaders(): ";
        
        Iterator<Instance> t = instances.iterator();
        
        while (t.hasNext()) {
            Instance i = t.next();
            
            if (i.getChildren().isEmpty()) {
                String[] xpathElements = i.getXPath().split("/");
                String xpathPrefix = "";
                
                // Ensure a unique column header by prepending an XPath prefix for nested tags 
                if (xpathElements.length > 3) {
                    for (int e = 3; e < xpathElements.length; e++) {
                        xpathPrefix = xpathPrefix + xpathElements[e] + " "; 
                    }
                }
                
                if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "add export column index " + i.getXPath() + " with column header " + xpathPrefix + i.getName());
                mExportHeaders.put(i.getXPath(), xpathPrefix + i.getName());
            } else {
                mErrorMsg = "This form template contains one or more repeated groups.\n\nExport of form data with repeated groups is not supported by this version of GC Mobile.\n\nSupport for exporting these types of forms will be added in a future release.";
                return;
            }
        }
    }
    
    private String getExportTimestamp()
    {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        
        formatter.setTimeZone(TimeZone.getDefault());
        formatter.applyPattern(DATETIME);
        
        return formatter.format(calendar.getTime());
    }
    
    // Don't ever return a null -- just give us an empty string, thanks
    private String getStringValue(String s)
    {
        if (s == null)
            return "";
        else
            return s.trim();
    }
    
    private void readData(XMLTag node, final String xpath)
    {   
        final String tt = t + "readData(): ";
        
        if (mExportHeaders.containsKey(xpath)) {
            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "insert data into record at using index " + mExportHeaders.get(xpath) + " (" + node.getText() + ")");
            mExportData.getLast().put(mExportHeaders.get(xpath), getStringValue(node.getText()));
        }
        
        node.forEachChild(new CallBack() {
            @Override
            public void execute(XMLTag arg0)
            {
                readData(arg0, xpath + "/" + arg0.getCurrentTagName());
            }
        });
    }
}
