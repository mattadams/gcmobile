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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.ektorp.DbAccessException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.model.xform.XPathReference;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.couchone.libcouch.Base64Coder;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormInstanceDocument;
import com.radicaldynamic.groupinform.listeners.FormSavedListener;
import com.radicaldynamic.groupinform.utilities.FileUtils;

/**
 * Background task for saving a form instance.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SaveToDiskTask extends AsyncTask<Void, String, Integer> {
    private final static String t = "SaveToDiskTask: ";
    
    private FormSavedListener mSavedListener;
    private String mInstanceDirPath;    
    private String mDefaultUrl;    
    private Boolean mSave;
    private Boolean mMarkCompleted;    
    
    private FormInstanceDocument mFormInstanceDoc;    

    public static final int SAVED = 500;
    public static final int SAVE_ERROR = 501;
    public static final int VALIDATE_ERROR = 502;
    public static final int VALIDATED = 503;
    public static final int SAVED_AND_EXIT = 504;

    
    /**
     * Initialize {@link FormEntryController} with {@link FormDef} from binary or from XML. If given
     * an instance, it will be used to fill the {@link FormDef}.
     */
    @Override
    protected Integer doInBackground(Void... nothing) 
    {
        // Validation failed, pass specific failure
        int validateStatus = validateAnswers();
        
        if (validateStatus != VALIDATED) {
            return validateStatus;
        }

        Collect.getInstance().getFormEntryController().getModel().getForm().postProcessInstance();

        if (mSave && exportData()) {
            return SAVED_AND_EXIT;
        } else if (exportData()) {
            return SAVED;
        }

        return SAVE_ERROR;
    }

    @Override
    protected void onPostExecute(Integer result)
    {
        synchronized (this) {
            if (mSavedListener != null)
                mSavedListener.savingComplete(result);
        }
    }

    public boolean exportData()
    {
        final String tt = t + "exportData(): ";
        
        boolean result = false;
        ByteArrayPayload payload = null;
        XFormSerializingVisitor serializer = null;
        
        try {
            // Assume no binary data inside the model
            FormInstance datamodel = Collect.getInstance().getFormEntryController().getModel().getForm().getInstance();
            serializer = new XFormSerializingVisitor();
            payload = (ByteArrayPayload) serializer.createSerializedPayload(datamodel);
                        
            // Write out instance XML
            result = exportXmlFile(payload);

            {
                boolean canEditSubmission = true;
                String url = mDefaultUrl;
                
                // now try to construct submission file                    
                try {
                    // assume no binary data inside the model.
                    FormEntryModel dataModel = Collect.getInstance().getFormEntryController().getModel();
                    FormDef formDef = dataModel.getForm();            
                    FormInstance formInstance = formDef.getInstance();            
                    IDataReference submissionElement = new XPathReference("/");
                    
                    // Determine the information about the submission...
                    SubmissionProfile p = formDef.getSubmissionProfile();
                    
                    if (p != null) {                            
                        submissionElement = p.getRef();
                        String altUrl = p.getAction();
                        
                        if (submissionElement == null || altUrl == null || !altUrl.startsWith("http") || p.getMethod() == null || !p.getMethod().equals("form-data-post")) {
                            Log.e(t, "Submission element should specify attributes: ref, method=\"form-data-post\", and action=\"http...\"");
                            return false;
                        }
                        
                        url = altUrl;
                        TreeElement e = formInstance.resolveReference(new XPathReference("/"));
                        TreeElement ee = formInstance.resolveReference(submissionElement);
                        
                        // we can edit the submission if the published fragment is the whole tree.
                        canEditSubmission = e.equals(ee);
                    }

                    if (mMarkCompleted) {
                        serializer = new XFormSerializingVisitor();
                        payload = (ByteArrayPayload) serializer.createSerializedPayload(formInstance, submissionElement);

                        exportXmlFile(payload, canEditSubmission, url);
                    }
                } catch (IOException e) {
                    Log.e(t, "Error creating serialized payload");
                    e.printStackTrace();
                    return false;
                }
            }            
        } catch (IOException e) {
            Log.e(Collect.LOGTAG, tt + "error creating serialized payload");
            e.printStackTrace();
            result = false;
        }
        
        return result;
    }

    public void setFormSavedListener(FormSavedListener fsl)
    {
        synchronized (this) {
            mSavedListener = fsl;
        }
    }
    
    /*
     * Export XML instance file & associated media attachments.
     */
    private boolean exportXmlFile(ByteArrayPayload payload)
    {
        final String tt = t + "exportXmlFile(): ";
        
        boolean result = false;
        
        // Create data stream
        InputStream is = payload.getPayloadStream();
        int len = (int) payload.getLength();
    
        // Read from data stream
        byte[] data = new byte[len];
        
        try {
            int read = is.read(data, 0, len);
            
            if (read > 0) {
                if (mMarkCompleted) {
                    mFormInstanceDoc.setStatus(FormInstanceDocument.Status.complete);
                } else {                    
                    mFormInstanceDoc.setStatus(FormInstanceDocument.Status.draft);
                    mFormInstanceDoc.setOdkSubmissionEditable(true);
                    mFormInstanceDoc.setOdkSubmissionUri(null);
                }

                // Save form data
                mFormInstanceDoc.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(data)).toString(), "text/xml"));
                Collect.getInstance().getDbService().getDb().update(mFormInstanceDoc);

                // Save media attachments one by one
                File cacheDir = new File(FileUtils.EXTERNAL_CACHE);
                String[] fileNames = cacheDir.list();                           

                for (String file : fileNames) {
                    Log.v(Collect.LOGTAG, tt + mFormInstanceDoc.getId() + ": evaluating " + file + " for save to DB");

                    if (Pattern.matches("^" + mFormInstanceDoc.getId() + "[.].*", file)) {
                        Log.d(Collect.LOGTAG, tt + mFormInstanceDoc.getId() + ": attaching " + file);

                        // Make sure we have the most current revision number
                        FormInstanceDocument document = Collect.getInstance().getDbService().getDb().get(FormInstanceDocument.class, mFormInstanceDoc.getId());

                        FileInputStream fis = new FileInputStream(new File(FileUtils.EXTERNAL_CACHE, file));
                        String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.substring(file.lastIndexOf(".") + 1));

                        AttachmentInputStream a = new AttachmentInputStream(file, fis, contentType);

                        // Must use the revision number (why?) http://code.google.com/p/ektorp/issues/detail?id=28
                        Collect.getInstance().getDbService().getDb().createAttachment(document.getId(), document.getRevision(), a);

                        a.close();
                        fis.close();
                    }
                }

                if (mFormInstanceDoc.getId().length() > 0) {
                    result = true;
                }
            }
        } catch (DbAccessException e) {
            Log.e(Collect.LOGTAG, tt + "unable to access database: " + e.toString());
        } catch (IOException e) {
            Log.e(Collect.LOGTAG, tt + "error reading from payload data stream");
            e.printStackTrace();
        }
        
        return result;
    }

    /*
     * Export submission file, for ODK compatibility.  SaveToDiskTask has a different but effectively similiar implementation.
     */
    private boolean exportXmlFile(ByteArrayPayload payload, boolean submissionEditable, String submissionUri) 
    {
        final String tt = t + "exportXmlSubmissionFile(): ";
        
        boolean result = false;
        
        // Create data stream
        InputStream is = payload.getPayloadStream();
        int len = (int) payload.getLength();
    
        // Read from data stream
        byte[] data = new byte[len];
        
        try {
            int read = is.read(data, 0, len);
            
            if (read > 0) {
                // SaveToDiskTask.java as of 1.1.6/r484 also sets
                // values.put(SubmissionsStorage.KEY_DISPLAY_SUB_SUBTEXT, app.getString(R.string.will_be_sent_to) + url);
                
                mFormInstanceDoc = Collect.getInstance().getDbService().getDb().get(FormInstanceDocument.class, mFormInstanceDoc.getId());
                mFormInstanceDoc.addInlineAttachment(new Attachment("xml.submit", new String(Base64Coder.encode(data)).toString(), "text/xml"));                
                mFormInstanceDoc.setOdkSubmissionEditable(submissionEditable);      
                mFormInstanceDoc.setOdkSubmissionUri(submissionUri);
                Collect.getInstance().getDbService().getDb().update(mFormInstanceDoc);
                
                if (mFormInstanceDoc.getId().length() > 0) {
                    Log.d(Collect.LOGTAG, tt + "successfully exported submission file");
                    result = true;
                }
            }
        } catch (DbAccessException e) {
            Log.e(Collect.LOGTAG, tt + "unable to access database: " + e.toString());
        } catch (IOException e) {
            Log.e(Collect.LOGTAG, tt + "error reading from payload data stream");
            e.printStackTrace();
        }
        
        return result;
    }

    public void setExportVars(FormInstanceDocument formInstanceDoc, String defaultUrl, Boolean saveAndExit, Boolean markCompleted) 
    {
        mFormInstanceDoc = formInstanceDoc;
        mDefaultUrl = defaultUrl;                
        mSave = saveAndExit;
        mMarkCompleted = markCompleted;
    }

    /**
     * Goes through the entire form to make sure all entered answers comply with their constraints.
     * Constraints are ignored on 'jump to', so answers can be outside of constraints. We don't
     * allow saving to disk, though, until all answers conform to their constraints/requirements.
     * 
     * @return validatedStatus
     */
    private int validateAnswers()
    {
    	FormEntryController fec = Collect.getInstance().getFormEntryController();
        FormEntryModel fem = fec.getModel();
        FormIndex i = fem.getFormIndex();
    
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
    
        int event;        
        while ((event = fec.stepToNextEvent()) != FormEntryController.EVENT_END_OF_FORM) {
            if (event != FormEntryController.EVENT_QUESTION) {
                continue;
            } else {
                int saveStatus = fec.answerQuestion(fem.getQuestionPrompt().getAnswerValue());                
                if (mMarkCompleted && saveStatus != FormEntryController.ANSWER_OK) { 
                    this.publishProgress(fem.getQuestionPrompt().getConstraintText(), Integer
                            .toString(saveStatus));
        			return saveStatus;
                }
            }
        }
    
        fec.jumpToIndex(i);        
        return VALIDATED;
    }
    

    @Override
    protected void onProgressUpdate(String... values) {
        Collect.getInstance().createConstraintToast(values[0], 
                Integer.valueOf(values[1]).intValue());
    }
    
}