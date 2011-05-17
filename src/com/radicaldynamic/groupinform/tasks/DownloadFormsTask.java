/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.groupinform.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ektorp.Attachment;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.webkit.MimeTypeMap;

import com.couchone.libcouch.Base64Coder;
import com.radicaldynamic.groupinform.activities.FormDownloadList;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;
import com.radicaldynamic.groupinform.listeners.FormDownloaderListener;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

/**
 * Background task for downloading forms from urls or a formlist from a url. We overload this task a
 * bit so that we don't have to keep track of two separate downloading tasks and it simplifies
 * interfaces. If LIST_URL is passed to doInBackground(), we fetch a form list. If a hashmap
 * containing form/url pairs is passed, we download those forms.
 * 
 * @author carlhartung
 */
public class DownloadFormsTask extends
        AsyncTask<HashMap<String, String>, String, HashMap<String, String>> {

    // used to store form name if one errors
    public static final String DL_FORM = "dlform";

    // used to store error message if one occurs
    public static final String DL_ERROR_MSG = "dlerrormessage";

    // used to indicate that we tried to download forms. If it's not included we tried to download a
    // form list.
    public static final String DL_FORMS = "dlforms";

    private static final int CONNECTION_TIMEOUT = 30000;

    private FormDownloaderListener mStateListener;
    private ContentResolver mContentResolver;


    public DownloadFormsTask(ContentResolver cr) {
        mContentResolver = cr;
    }


    @Override
    protected HashMap<String, String> doInBackground(HashMap<String, String>... values) {
        if (values != null && values[0].containsKey(FormDownloadList.LIST_URL)) {
            // This gets a list of available forms from the specified server.
            HashMap<String, String> formList = new HashMap<String, String>();
            URL u = null;
            try {
                u = new URL(values[0].get(FormDownloadList.LIST_URL));
            } catch (MalformedURLException e) {
                formList.put(DL_ERROR_MSG, e.getLocalizedMessage());
                e.printStackTrace();
            }

            try {
                // prevent deadlock when connection is invalid
                URLConnection c = u.openConnection();
                c.setConnectTimeout(CONNECTION_TIMEOUT);
                c.setReadTimeout(CONNECTION_TIMEOUT);

                // write connection to file
                InputStream is = c.getInputStream();

                Document doc = null;
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    doc = db.parse(is);
                } catch (Exception e) {
                    formList.put(DL_ERROR_MSG,
                        "DocumentBuilderFactory error: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                // populate HashMap with form names and urls
                int formCount = 0;
                if (doc != null) {
                    NodeList formElements = doc.getElementsByTagName("form");
                    formCount = formElements.getLength();
                    Node n;
                    NodeList childList;
                    NamedNodeMap attrMap;
                    for (int i = 0; i < formCount; i++) {
                        n = formElements.item(i);
                        childList = n.getChildNodes();
                        attrMap = n.getAttributes();
                        if (childList.getLength() > 0 && attrMap.getLength() > 0) {
                            formList.put(childList.item(0).getNodeValue() + ".xml", attrMap.item(0)
                                    .getNodeValue());
                        }

                    }
                }
            } catch (IOException e) {
                formList.put(DL_ERROR_MSG, e.getLocalizedMessage());
                e.printStackTrace();
            }
            return formList;

        } else if (values != null) {
            // This downloads the selected forms.
            HashMap<String, String> toDownload = values[0];
            HashMap<String, String> result = new HashMap<String, String>();
            result.put(DL_FORMS, DL_FORMS); // indicate that we're trying to download forms.
            ArrayList<String> formNames = new ArrayList<String>(toDownload.keySet());

            // boolean error = false;
            int total = formNames.size();
            int count = 1;

            for (int i = 0; i < total; i++) {
                String form = formNames.get(i);
                publishProgress(form, Integer.valueOf(count).toString(), Integer.valueOf(total)
                        .toString());
                try {
                    File dl = downloadFile(form, toDownload.get(form));
                    String hash = FileUtils.getMd5Hash(dl);
                    
                    // BEGIN custom
//                  String selection = FormsColumns.MD5_HASH + "=?";
//                  String[] selectionArgs = {hash};
//                  Cursor c = mContentResolver.query(FormsColumns.CONTENT_URI, null, selection, selectionArgs, null);
//                  if (c.getCount() > 0) {
//                      // we alredy have this, so ignore it.
//                      // actually, delete it.
//                      dl.delete();
//                  } else {
//                      // add it
//                      ContentValues newValues = new ContentValues();
//
//                      HashMap<String, String> fields = FileUtils.parseXML(dl);
//
//                      String title = fields.get(FileUtils.TITLE);
//                      String ui = fields.get(FileUtils.UI);
//                      String model = fields.get(FileUtils.MODEL);
//                      String formid = fields.get(FileUtils.FORMID);
//
//                      if (title != null) {
//                          newValues.put(FormsColumns.DISPLAY_NAME, title);
//                      } else {
//                          // TODO: Return some nasty error.
//                      }
//                      if (formid != null) {
//                          newValues.put(FormsColumns.JR_FORM_ID, formid);
//                      } else {
//                          // TODO: return some nasty error.
//                      }
//                      if (ui != null) {
//                          newValues.put(FormsColumns.UI_VERSION, ui);
//                      }
//                      if (model != null) {
//                          newValues.put(FormsColumns.MODEL_VERSION, model);
//                      }
//
//                      newValues.put(FormsColumns.FORM_FILE_PATH, dl.getAbsolutePath());
//
//                      Uri uri = mContentResolver.insert(FormsColumns.CONTENT_URI, newValues);
//                  }

                    if (dl != null) {
                        String fileExtension = dl.getName().substring(dl.getName().lastIndexOf(".") + 1);
                        String fileContentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                        
                        HashMap<String, String> fields = FileUtils.parseXML(dl);

                        FormDefinitionDocument fDoc = new FormDefinitionDocument();
                        fDoc.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(FileUtils.getFileAsBytes(dl.getAbsoluteFile()))).toString(), fileContentType));
                        fDoc.setJavaRosaId(fields.get(FileUtils.FORMID));
                        fDoc.setModelVersion(fields.get(FileUtils.MODEL));
                        fDoc.setName(fields.get(FileUtils.TITLE));
                        fDoc.setUiVersion(fields.get(FileUtils.UI));                                                

                        Collect.getInstance().getDbService().getDb().create(fDoc);

                        FileUtils.deleteFolder(dl.getParent());
                    }
                    // END custom                   
                } catch (SocketTimeoutException se) {
                    se.printStackTrace();
                    result.put(DL_FORM, form);
                    result.put(DL_ERROR_MSG, "Unknown timeout exception");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    result.put(DL_FORM, form);
                    result.put(DL_ERROR_MSG, e.getLocalizedMessage());
                    break;
                }
                count++;
            }

            return result;
        }

        return null;
    }


    private File downloadFile(String name, String url) throws IOException {
        // create url
        URL u = null;
        File f = null;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw e;
        }

        try {
            // prevent deadlock when connection is invalid
            URLConnection c = u.openConnection();
            c.setConnectTimeout(CONNECTION_TIMEOUT);
            c.setReadTimeout(CONNECTION_TIMEOUT);

            // write connection to file
            InputStream is = c.getInputStream();
            
            // BEGIN custom
//          String path = FileUtils.FORMS_PATH + name;
//          int i = 2;
//          int slash = path.lastIndexOf("/") + 1;
//          int period = path.lastIndexOf(".") + 1;
//          String base = path.substring(0, slash - 1);
//          String filename = path.substring(slash, period - 1);
//          String ext = path.substring(period);
//          f = new File(path);
//          while (f.exists()) {
//              f = new File(base + "/" + filename + "_" + i + "." + ext);
//              i++;
//          }

            String dlPath = FileUtilsExtended.ODK_IMPORT_PATH + File.separator + UUID.randomUUID();
            FileUtils.createFolder(dlPath);
            f = new File(dlPath + File.separator + name);
            // END custom

            OutputStream os = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            os.flush();
            os.close();
            is.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        return f;
    }


    @Override
    protected void onPostExecute(HashMap<String, String> value) {
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.formDownloadingComplete(value);
            }
        }
    }


    @Override
    protected void onProgressUpdate(String... values) {
        synchronized (this) {
            if (mStateListener != null) {
                // update progress and total
                mStateListener.progressUpdate(values[0], new Integer(values[1]).intValue(),
                    new Integer(values[2]).intValue());
            }
        }

    }


    public void setDownloaderListener(FormDownloaderListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}
