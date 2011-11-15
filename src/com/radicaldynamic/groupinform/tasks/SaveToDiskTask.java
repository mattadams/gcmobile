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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.ektorp.AttachmentInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.utilities.FileUtils;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.listeners.FormSavedListener;

/**
 * Background task for loading a form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SaveToDiskTask extends AsyncTask<Void, String, Integer> {
    private final static String t = "SaveToDiskTask";

    private FormSavedListener mSavedListener;
    private Boolean mSave;
    private Boolean mMarkCompleted;
    @SuppressWarnings("unused")
    private Uri mUri;
    private String mInstanceName;

    public static final int SAVED = 500;
    public static final int SAVE_ERROR = 501;
    public static final int VALIDATE_ERROR = 502;
    public static final int VALIDATED = 503;
    public static final int SAVED_AND_EXIT = 504;
    
    // BEGIN custom
    FormInstance mFormInstance;
    // END custom


    // BEGIN custom
//    public SaveToDiskTask(Uri uri, Boolean saveAndExit, Boolean markCompleted, String updatedName) {
    public SaveToDiskTask(Uri uri, Boolean saveAndExit, Boolean markCompleted, String updatedName, FormInstance formInstance) {
    // END custom
        mUri = uri;
        mSave = saveAndExit;
        mMarkCompleted = markCompleted;
        mInstanceName = updatedName;
        // BEGIN custom
        mFormInstance = formInstance;
        // END custom
    }


    /**
     * Initialize {@link FormEntryController} with {@link FormDef} from binary or from XML. If given
     * an instance, it will be used to fill the {@link FormDef}.
     */
    @Override
    protected Integer doInBackground(Void... nothing) {

        // validation failed, pass specific failure
        int validateStatus = validateAnswers(mMarkCompleted);
        if (validateStatus != VALIDATED) {
            return validateStatus;
        }

        FormEntryActivity.mFormController.postProcessInstance();

        if (mSave && exportData(mMarkCompleted)) {
            return SAVED_AND_EXIT;
        } else if (exportData(mMarkCompleted)) {
            return SAVED;
        }

        return SAVE_ERROR;

    }


    /**
     * Write's the data to the sdcard, and updates the instances content provider.
     * In theory we don't have to write to disk, and this is where you'd add
     * other methods.
     * @param markCompleted
     * @return
     */
    public boolean exportData(boolean markCompleted) {
        ByteArrayPayload payload;
        try {

            // assume no binary data inside the model.
            // BEGIN custom
//            FormInstance datamodel = FormEntryActivity.mFormController.getInstance();
            org.javarosa.core.model.instance.FormInstance datamodel = FormEntryActivity.mFormController.getInstance();
            // END custom
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();
            payload = (ByteArrayPayload) serializer.createSerializedPayload(datamodel);

            // write out xml
            exportXmlFile(payload, FormEntryActivity.mInstancePath);

        } catch (IOException e) {
            Log.e(t, "Error creating serialized payload");
            e.printStackTrace();
            return false;
        }

        // BEGIN custom
//        // If FormEntryActivity was started with an Instance, just update that instance
//        if (Collect.getInstance().getContentResolver().getType(mUri) == InstanceColumns.CONTENT_ITEM_TYPE) {
//            ContentValues values = new ContentValues();
//            if (mInstanceName != null) {
//                values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
//            }
//            if (!mMarkCompleted) {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
//            } else {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_COMPLETE);
//            }
//            Collect.getInstance().getContentResolver().update(mUri, values, null, null);
//        } else if (Collect.getInstance().getContentResolver().getType(mUri) == FormsColumns.CONTENT_ITEM_TYPE) {
//            // If FormEntryActivity was started with a form, then it's likely the first time we're
//            // saving.
//            // However, it could be a not-first time saving if the user has been using the manual
//            // 'save data' option from the menu. So try to update first, then make a new one if that
//            // fails.
//            ContentValues values = new ContentValues();
//            if (mInstanceName != null) {
//                values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
//            }
//            if (mMarkCompleted) {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_COMPLETE);
//            } else {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
//            }
//
//            String where = InstanceColumns.INSTANCE_FILE_PATH + "=?";
//            String[] whereArgs = {
//                FormEntryActivity.mInstancePath
//            };
//            int updated =
//                Collect.getInstance().getContentResolver()
//                        .update(InstanceColumns.CONTENT_URI, values, where, whereArgs);
//            if (updated > 1) {
//                Log.w(t, "Updated more than one entry, that's not good");
//            } else if (updated == 1) {
//                Log.i(t, "Instance already exists, updating");
//                // already existed and updated just fine
//                return true;
//            } else {
//                Log.e(t, "No instance found, creating");
//                // Entry didn't exist, so create it.
//                Cursor c =
//                    Collect.getInstance().getContentResolver().query(mUri, null, null, null, null);
//                c.moveToFirst();
//                String jrformid = c.getString(c.getColumnIndex(FormsColumns.JR_FORM_ID));
//                String formname = c.getString(c.getColumnIndex(FormsColumns.DISPLAY_NAME));
//                String submissionUri = c.getString(c.getColumnIndex(FormsColumns.SUBMISSION_URI));
//
//                values.put(InstanceColumns.INSTANCE_FILE_PATH, FormEntryActivity.mInstancePath);
//                values.put(InstanceColumns.INSTANCE_FILE_PATH, FormEntryActivity.mInstancePath);
//                values.put(InstanceColumns.SUBMISSION_URI, submissionUri);
//                if (mInstanceName != null) {
//                    values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
//                } else {
//                    values.put(InstanceColumns.DISPLAY_NAME, formname);
//                }
//                values.put(InstanceColumns.JR_FORM_ID, jrformid);
//                Collect.getInstance().getContentResolver()
//                        .insert(InstanceColumns.CONTENT_URI, values);
//            }            
//        }
        
        String instancePath = FormEntryActivity.mInstancePath;
        String docId = instancePath.substring(instancePath.lastIndexOf("/") + 1, instancePath.lastIndexOf("."));
        boolean firstSave = mFormInstance.getStatus().equals(FormInstance.Status.placeholder);
        String revision = null;
        
        try {
            // Update document status, etc.
            if (mMarkCompleted) 
                mFormInstance.setStatus(FormInstance.Status.complete);
            else
                mFormInstance.setStatus(FormInstance.Status.draft);

            mFormInstance.setXmlHash(FileUtils.getMd5Hash(new File(instancePath)));
            mFormInstance.setName(mInstanceName);
            Collect.getInstance().getDbService().getDb().update(mFormInstance);
            revision = mFormInstance.getRevision();

            // Process attachments
            File instanceDir = new File(instancePath).getParentFile();
            String[] attachmentFilenames = instanceDir.list();

            /*
             * Get a list of attachments that already exist; we will use this to determine if 
             * we need to remove attachments after saving attachments that exist in our cache
             * directory.
             */
            Set<String> existingAttachments = new HashSet<String>();

            if (mFormInstance.getAttachments() != null) {
                existingAttachments = new HashSet<String>(mFormInstance.getAttachments().keySet());
            }

            for (String fileName : attachmentFilenames) {
                File f = new File(new File(instancePath).getParentFile(), fileName);
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
                boolean exists = false;
                
                // XML instance file should simply be named "xml"
                if (fileName.equals(docId + ".xml"))
                    fileName = "xml";
                
                // Remove this entry from the list of existing attachments (to be removed)
                if (existingAttachments.contains(fileName))
                    exists = existingAttachments.remove(fileName);

                // Skip identically named attachments with the same size
                if (exists && mFormInstance.getAttachments().get(fileName).getContentLength() == f.length()) {
                    Log.v(Collect.LOGTAG, t + ": " + fileName + " already attached to " + docId + " (" + f.length() + " bytes)");

                    // Don't skip the xml instance document!  Contents are likely to change but leave the size untouched.
                    if (!fileName.equals("xml"))
                        continue;
                } else {
                    Log.d(Collect.LOGTAG, t + ": attaching " + fileName + " to " + docId);
                }


                FileInputStream fis = new FileInputStream(f);
                String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                AttachmentInputStream a = new AttachmentInputStream(fileName, fis, contentType, f.length());
                
                revision = Collect.getInstance().getDbService().getDb().createAttachment(docId, revision, a);
            }
            
            // Remove attachments that no longer exist
            Iterator<String> attachmentsToRemove = existingAttachments.iterator();

            while (attachmentsToRemove.hasNext()) {
                String attachmentId = attachmentsToRemove.next();

                try {
                    Log.d(Collect.LOGTAG, t + ": removing unused attachment " + attachmentId);
                    revision = Collect.getInstance().getDbService().getDb().deleteAttachment(docId, revision, attachmentId);
                } catch (Exception e) {
                    Log.e(Collect.LOGTAG, t + ": unexpected exception while removing unused attachment " + attachmentId);
                    e.printStackTrace();
                }
            }

            // Make sure that we have the most current instance to return to FormEntryActivity
            mFormInstance = Collect.getInstance().getDbService().getDb().get(FormInstance.class, docId);
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + ": unexpected exception while attaching files to instance document " + docId);
            e.printStackTrace();

            /*
             * It is possible that the first update may succeed but that attachments changes may fail.
             * If this document is a new one, then remove it altogether so that we don't have stale 
             * and invalid documents laying about.  This is a last ditch effort.
             */
            try {
                if (firstSave) {
                    Log.d(Collect.LOGTAG, t + ": removing " + mFormInstance.getId() + " due to first save failure ");
                    Collect.getInstance().getDbService().getDb().delete(mFormInstance.getId(), mFormInstance.getRevision());
                    mFormInstance = Collect.getInstance().getDbService().getDb().get(FormInstance.class, docId);
                }
            } catch (Exception e1) {
                Log.e(Collect.LOGTAG, t + ": failed last ditch effort to tidy after save oops");
                e.printStackTrace();
            }

            return false;
        }        
        // END custom        
        
        return true;
    }


    /**
     * This method actually writes the xml to disk.
     * @param payload
     * @param path
     * @return
     */
    private boolean exportXmlFile(ByteArrayPayload payload, String path) {

        // create data stream
        InputStream is = payload.getPayloadStream();
        int len = (int) payload.getLength();

        // read from data stream
        byte[] data = new byte[len];
        try {
            int read = is.read(data, 0, len);
            if (read > 0) {
                // write xml file
                try {
                    // String filename = path + "/" +
                    // path.substring(path.lastIndexOf('/') + 1) + ".xml";
                    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
                    bw.write(new String(data, "UTF-8"));
                    bw.flush();
                    bw.close();
                    return true;

                } catch (IOException e) {
                    Log.e(t, "Error writing XML file");
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(t, "Error reading from payload data stream");
            e.printStackTrace();
            return false;
        }

        return false;
    }


    @Override
    protected void onPostExecute(Integer result) {
        synchronized (this) {
            // BEGIN custom
//            if (mSavedListener != null)
//                mSavedListener.savingComplete(result);
            if (mSavedListener != null)
                mSavedListener.savingComplete(result, mFormInstance);
            // END custom
        }
    }


    public void setFormSavedListener(FormSavedListener fsl) {
        synchronized (this) {
            mSavedListener = fsl;
        }
    }


    /**
     * Goes through the entire form to make sure all entered answers comply with their constraints.
     * Constraints are ignored on 'jump to', so answers can be outside of constraints. We don't
     * allow saving to disk, though, until all answers conform to their constraints/requirements.
     * 
     * @param markCompleted
     * @return validatedStatus
     */
    private int validateAnswers(Boolean markCompleted) {
        FormIndex i = FormEntryActivity.mFormController.getFormIndex();
        FormEntryActivity.mFormController.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        int event;
        while ((event =
            FormEntryActivity.mFormController.stepToNextEvent(FormController.STEP_OVER_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
            if (event != FormEntryController.EVENT_QUESTION) {
                continue;
            } else {
                int saveStatus =
                    FormEntryActivity.mFormController
                            .answerQuestion(FormEntryActivity.mFormController.getQuestionPrompt()
                                    .getAnswerValue());
                if (markCompleted && saveStatus != FormEntryController.ANSWER_OK) {
                    return saveStatus;
                }
            }
        }

        FormEntryActivity.mFormController.jumpToIndex(i);
        return VALIDATED;
    }

}
