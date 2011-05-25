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

import org.ektorp.AttachmentInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.odk.collect.android.listeners.FormSavedListener;
import org.odk.collect.android.logic.FormController;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormInstance;

/**
 * Background task for loading a form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SaveToDiskTask extends AsyncTask<Void, String, Integer> {
    private final static String t = "SaveToDiskTask";

    private FormSavedListener mSavedListener;
    private Context mContext;
    private Boolean mSave;
    private Boolean mMarkCompleted;
    private ContentResolver mContentResolver;
    private Uri mUri;

    public static final int SAVED = 500;
    public static final int SAVE_ERROR = 501;
    public static final int VALIDATE_ERROR = 502;
    public static final int VALIDATED = 503;
    public static final int SAVED_AND_EXIT = 504;

    public SaveToDiskTask(ContentResolver cr, Uri uri) {
        mContentResolver = cr;
        mUri = uri;
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

        if (mSave && exportData(mContext, mMarkCompleted)) {
            return SAVED_AND_EXIT;
        } else if (exportData(mContext, mMarkCompleted)) {
            return SAVED;
        }

        return SAVE_ERROR;

    }


    public boolean exportData(Context context, boolean markCompleted) {

        ByteArrayPayload payload;
        try {

            // assume no binary data inside the model.
            // BEGIN custom
//            FormInstance datamodel =
//                FormEntryActivity.mFormController.getInstance();
            org.javarosa.core.model.instance.FormInstance datamodel =
                FormEntryActivity.mFormController.getInstance();
            // END custom
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();
            payload = (ByteArrayPayload) serializer.createSerializedPayload(datamodel);

            // write out xml
            exportXmlFile(payload, FormEntryActivity.InstancePath);

        } catch (IOException e) {
            Log.e(t, "Error creating serialized payload");
            e.printStackTrace();
            return false;
        }

        // BEGIN custom
//        if (mContentResolver.getType(mUri) == InstanceColumns.CONTENT_ITEM_TYPE) { 
//            ContentValues values = new ContentValues();
//            if (!mMarkCompleted) {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
//                mContentResolver.update(mUri, values, null, null);
//            } else {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_COMPLETE);
//                mContentResolver.update(mUri, values, null, null);
//            }
//            
//        } else if (mContentResolver.getType(mUri) == FormsColumns.CONTENT_ITEM_TYPE) {
//            Cursor c =  mContentResolver.query(mUri, null, null, null, null);
//            c.moveToFirst();
//            String jrformid = c.getString(c.getColumnIndex(FormsColumns.JR_FORM_ID));
//            String formname = c.getString(c.getColumnIndex(FormsColumns.DISPLAY_NAME));
//            
//            ContentValues values = new ContentValues();
//
//            if (mMarkCompleted) {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_COMPLETE);
//            } else {
//                values.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
//            }
//            values.put(InstanceColumns.INSTANCE_FILE_PATH, FormEntryActivity.InstancePath);
//            values.put(InstanceColumns.INSTANCE_FILE_PATH, FormEntryActivity.InstancePath);
//            values.put(InstanceColumns.SUBMISSION_URI, "submission");
//            values.put(InstanceColumns.DISPLAY_NAME, formname);
//            values.put(InstanceColumns.JR_FORM_ID, jrformid );
//            mContentResolver.insert(InstanceColumns.CONTENT_URI, values);
//            
//        }
        
        String instancePath = FormEntryActivity.InstancePath;
        String instanceId = instancePath.substring(instancePath.lastIndexOf("/") + 1, instancePath.lastIndexOf("."));
        
        try {
            FormInstance fid = Collect.getInstance().getDbService().getDb().get(FormInstance.class, instanceId);

            fid.getOdk().setUploadUri("submission");

            if (mMarkCompleted) 
                fid.setStatus(FormInstance.Status.complete);
            else
                fid.setStatus(FormInstance.Status.draft);

            Collect.getInstance().getDbService().getDb().update(fid);

            File instanceDir = new File(instancePath).getParentFile();
            String [] attachmentFilenames = instanceDir.list();

            for (String attachmentFilename : attachmentFilenames) {
                Log.v(Collect.LOGTAG, t + "attaching " + attachmentFilename + " to " + instanceId);

                // Make sure that we have the most current revision number
                fid = Collect.getInstance().getDbService().getDb().get(FormInstance.class, instanceId);
                
                File f = new File(new File(instancePath).getParentFile(), attachmentFilename);
                FileInputStream fis = new FileInputStream(f);
                
                String extension = attachmentFilename.substring(attachmentFilename.lastIndexOf(".") + 1);
                String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                
                // XML instance file should simply be named "xml"
                if (attachmentFilename.equals(instanceId + ".xml"))
                    attachmentFilename = "xml";
                
                AttachmentInputStream a = new AttachmentInputStream(attachmentFilename, fis, contentType, f.length());
                
                Collect.getInstance().getDbService().getDb().createAttachment(instanceId, fid.getRevision(), a);
            }        
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "error while attaching files to instance document " + instanceId);
            e.printStackTrace();
            return false;
        }        
        // END custom        
        
        return true;

    }


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
            if (mSavedListener != null)
                mSavedListener.savingComplete(result);
        }
    }


    public void setFormSavedListener(FormSavedListener fsl) {
        synchronized (this) {
            mSavedListener = fsl;
        }
    }


    public void setExportVars(Context context, Boolean saveAndExit,
            Boolean markCompleted) {
        mContext = context;
        mSave = saveAndExit;
        mMarkCompleted = markCompleted;
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
        while ((event = FormEntryActivity.mFormController.stepToNextEvent(FormController.STEP_OVER_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
            if (event != FormEntryController.EVENT_QUESTION) {
                continue;
            } else {
                int saveStatus =
                    FormEntryActivity.mFormController.answerQuestion(FormEntryActivity.mFormController.getQuestionPrompt()
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
