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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.ektorp.DocumentNotFoundException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.odk.collect.android.logic.FileReferenceFactory;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.utilities.FileUtils;

import android.os.AsyncTask;
import android.util.Log;

import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.listeners.FormLoaderListener;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

/**
 * Background task for loading a form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormLoaderTask extends AsyncTask<String, String, FormLoaderTask.FECWrapper> {
    private final static String t = "FormLoaderTask";
    /**
     * Classes needed to serialize objects. Need to put anything from JR in here.
     */
    public final static String[] SERIALIABLE_CLASSES = {
            "org.javarosa.core.model.FormDef", "org.javarosa.core.model.GroupDef",
            "org.javarosa.core.model.QuestionDef", "org.javarosa.core.model.data.DateData",
            "org.javarosa.core.model.data.DateTimeData",
            "org.javarosa.core.model.data.DecimalData",
            "org.javarosa.core.model.data.GeoPointData",
            "org.javarosa.core.model.data.helper.BasicDataPointer",
            "org.javarosa.core.model.data.IntegerData",
            "org.javarosa.core.model.data.MultiPointerAnswerData",
            "org.javarosa.core.model.data.PointerAnswerData",
            "org.javarosa.core.model.data.SelectMultiData",
            "org.javarosa.core.model.data.SelectOneData",
            "org.javarosa.core.model.data.StringData", "org.javarosa.core.model.data.TimeData",
            "org.javarosa.core.services.locale.TableLocaleSource",
            "org.javarosa.xpath.expr.XPathArithExpr", "org.javarosa.xpath.expr.XPathBoolExpr",
            "org.javarosa.xpath.expr.XPathCmpExpr", "org.javarosa.xpath.expr.XPathEqExpr",
            "org.javarosa.xpath.expr.XPathFilterExpr", "org.javarosa.xpath.expr.XPathFuncExpr",
            "org.javarosa.xpath.expr.XPathNumericLiteral",
            "org.javarosa.xpath.expr.XPathNumNegExpr", "org.javarosa.xpath.expr.XPathPathExpr",
            "org.javarosa.xpath.expr.XPathStringLiteral", "org.javarosa.xpath.expr.XPathUnionExpr",
            "org.javarosa.xpath.expr.XPathVariableReference"
    };

    private FormLoaderListener mStateListener;
    private String mErrorMsg;

    protected class FECWrapper {
        FormController controller;


        protected FECWrapper(FormController controller) {
            this.controller = controller;
        }


        protected FormController getController() {
            return controller;
        }


        protected void free() {
            controller = null;
        }
    } 

    FECWrapper data;
    
    // BEGIN custom
    FormDefinition mFormDefinition = null;
    FormInstance mFormInstance = null;
    // END custom


    /**
     * Initialize {@link FormEntryController} with {@link FormDef} from binary or from XML. If given
     * an instance, it will be used to fill the {@link FormDef}.
     */
    @Override
    protected FECWrapper doInBackground(String... path) {
        FormEntryController fec = null;
        FormDef fd = null;
        FileInputStream fis = null;
        mErrorMsg = null;
        
        // BEGIN custom 
        File formDefinitionFile = new File(path[0]);
        String formId = formDefinitionFile.getName().substring(0, formDefinitionFile.getName().lastIndexOf("."));
        
        // Retrieve form definition from database
        try {
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + ": retrieving form definition document " + formId);
            
            FileUtils.createFolder(formDefinitionFile.getParent());
            FileUtils.createFolder(formDefinitionFile.getParent() + File.separator + FileUtilsExtended.MEDIA_DIR); 
            
            mFormDefinition = Collect.getInstance().getDbService().getDb().get(FormDefinition.class, formId);
            HashMap<String, Attachment> attachments = (HashMap<String, Attachment>) mFormDefinition.getAttachments();
            
            // Download attachments (form definition XML & other media)
            for (Entry<String, Attachment> entry : attachments.entrySet()) {
                // TODO: Skip this attachment if a cache file exists and is newer than the document create date/update date                
                AttachmentInputStream ais = Collect.getInstance().getDbService().getDb().getAttachment(formId, entry.getKey());
                FileOutputStream file;
                
                if (entry.getKey().equals("xml")) {
                    file = new FileOutputStream(formDefinitionFile);
                } else {
                    file = new FileOutputStream(formDefinitionFile.getParent() + File.separator + FileUtilsExtended.MEDIA_DIR + File.separator + entry.getKey());
                }
                
                byte [] buffer = new byte[8192];
                int bytesRead = 0;
                
                while ((bytesRead = ais.read(buffer)) != -1) {
                    file.write(buffer, 0, bytesRead);
                }
                
                file.close();
                ais.close();
            }
        } catch (DocumentNotFoundException e) {
            if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + ": " + e.toString());
            mErrorMsg = "The form that you requested could not be found.  It may have been removed by one of your team members.\n\nSelect OK to refresh the screen and try again.";
            return null;
        } catch (Exception e) {
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + ": unexpected exception while retrieving form definition: " + e.toString());
            mErrorMsg = e.getMessage();
            e.printStackTrace();
            return null;
        }
        // END custom 

        String formPath = path[0];

        File formXml = new File(formPath);
        String formHash = FileUtils.getMd5Hash(formXml);        
        // BEGIN custom 
//      File formBin = new File(Collect.CACHE_PATH + "/" + formHash + ".formdef");
        File formBin = new File(formDefinitionFile.getParent() + File.separator + formHash + ".formdef");
        // END custom        

        if (formBin.exists()) {
            // if we have binary, deserialize binary
            Log.i(
                t,
                "Attempting to load " + formXml.getName() + " from cached file: "
                        + formBin.getAbsolutePath());
            fd = deserializeFormDef(formBin);
            if (fd == null) {
                // some error occured with deserialization. Remove the file, and make a new .formdef
                // from xml
                Log.w(t,
                    "Deserialization FAILED!  Deleting cache file: " + formBin.getAbsolutePath());
                formBin.delete();
            }
        }
        if (fd == null) {
            // no binary, read from xml
            try {
                Log.i(t, "Attempting to load from: " + formXml.getAbsolutePath());
                fis = new FileInputStream(formXml);
                fd = XFormUtils.getFormFromInputStream(fis);
                if (fd == null) {
                    mErrorMsg = "Error reading XForm file";
                } else {
                    serializeFormDef(fd, formPath);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mErrorMsg = e.getMessage();
            } catch (XFormParseException e) {
                mErrorMsg = e.getMessage();
                e.printStackTrace();
            } catch (Exception e) {
                mErrorMsg = e.getMessage();
                e.printStackTrace();
            } 
        }

        if (mErrorMsg != null) {
            return null;
        }

        // new evaluation context for function handlers
        EvaluationContext ec = new EvaluationContext();
        fd.setEvaluationContext(ec);

        // create FormEntryController from formdef
        FormEntryModel fem = new FormEntryModel(fd);
        fec = new FormEntryController(fem);

        try {
            // import existing data into formdef
            if (FormEntryActivity.mInstancePath != null) {
                // This order is important. Import data, then initialize.
                // BEGIN custom
                // importData(FormEntryActivity.mInstancePath, fec);
                try {
                    String instanceId = FormEntryActivity.mInstancePath.substring(FormEntryActivity.mInstancePath.lastIndexOf("/") + 1, FormEntryActivity.mInstancePath.lastIndexOf("."));

                    if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + ": retrieving form instance document " + instanceId);

                    String instanceFolder = FormEntryActivity.mInstancePath.substring(0, FormEntryActivity.mInstancePath.lastIndexOf("/")); 
                    FileUtils.createFolder(instanceFolder);

                    mFormInstance = Collect.getInstance().getDbService().getDb().get(FormInstance.class, instanceId);
                    HashMap<String, Attachment> attachments = (HashMap<String, Attachment>) mFormInstance.getAttachments();

                    // Download attachments (form instance XML & other media)
                    for (Entry<String, Attachment> entry : attachments.entrySet()) {
                        AttachmentInputStream ais = Collect.getInstance().getDbService().getDb().getAttachment(instanceId, entry.getKey());
                        FileOutputStream file;

                        if (entry.getKey().equals("xml")) {
                            file = new FileOutputStream(FormEntryActivity.mInstancePath);
                        } else {
                            file = new FileOutputStream(instanceFolder + File.separator + entry.getKey());
                        }

                        byte [] buffer = new byte[8192];
                        int bytesRead = 0;

                        while ((bytesRead = ais.read(buffer)) != -1) {
                            file.write(buffer, 0, bytesRead);
                        }

                        file.close();
                        ais.close();
                    }

                    importData(FormEntryActivity.mInstancePath, fec);
                } catch (Exception e) {
                    if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + ": unexpected exception while retrieving form instance: " + e.toString());
                    mErrorMsg = e.getMessage();
                    e.printStackTrace();            
                }
                // END custom
                fd.initialize(false);
            } else {
                fd.initialize(true);
            }
        } catch (RuntimeException e) {
            mErrorMsg = e.getMessage();
            return null;
        }

        // set paths to /sdcard/odk/forms/formfilename-media/
        // BEGIN custom
//        String formFileName = formXml.getName().substring(0, formXml.getName().lastIndexOf("."));
        // END custom

        // Remove previous forms
        ReferenceManager._().clearSession();

        // This should get moved to the Application Class
        if (ReferenceManager._().getFactories().length == 0) {
            // this is /sdcard/odk
            // BEGIN custom
//            ReferenceManager._().addReferenceFactory(
//                    new FileReferenceFactory(Environment.getExternalStorageDirectory() + "/odk"));
            ReferenceManager._().addReferenceFactory(
                new FileReferenceFactory(FileUtilsExtended.FORMS_PATH));
            // END custom
        }

        // Set jr://... to point to /sdcard/odk/forms/filename-media/
        // BEGIN custom
//        ReferenceManager._().addSessionRootTranslator(
//                new RootTranslator("jr://images/", "jr://file/forms/" + formFileName + "-media/"));
//            ReferenceManager._().addSessionRootTranslator(
//                new RootTranslator("jr://audio/", "jr://file/forms/" + formFileName + "-media/"));
//            ReferenceManager._().addSessionRootTranslator(
//                new RootTranslator("jr://video/", "jr://file/forms/" + formFileName + "-media/"));
        ReferenceManager._().addSessionRootTranslator(
            new RootTranslator("jr://images/", "jr://file/" + formId + "/media/"));
        ReferenceManager._().addSessionRootTranslator(
            new RootTranslator("jr://audio/", "jr://file/" + formId + "/media/"));
        ReferenceManager._().addSessionRootTranslator(
            new RootTranslator("jr://video/", "jr://file/" + formId + "/media/"));
        // END custom

        // clean up vars
        fis = null;
        fd = null;
        formBin = null;
        formXml = null;
        formPath = null;

        FormController fc = new FormController(fec);
        data = new FECWrapper(fc);
        return data;

    }


    public boolean importData(String filePath, FormEntryController fec) {
        // convert files into a byte array
        byte[] fileBytes = FileUtils.getFileAsBytes(new File(filePath));

        // get the root of the saved and template instances
        TreeElement savedRoot = XFormParser.restoreDataModel(fileBytes, null).getRoot();
        TreeElement templateRoot = fec.getModel().getForm().getInstance().getRoot().deepCopy(true);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) || savedRoot.getMult() != 0) {
            Log.e(t, "Saved form instance does not match template form definition");
            return false;
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);
            templateRoot.populate(savedRoot, fec.getModel().getForm());

            // populated model to current form
            fec.getModel().getForm().getInstance().setRoot(templateRoot);

            // fix any language issues
            // : http://bitbucket.org/javarosa/main/issue/5/itext-n-appearing-in-restored-instances
            if (fec.getModel().getLanguages() != null) {
                fec.getModel()
                        .getForm()
                        .localeChanged(fec.getModel().getLanguage(),
                            fec.getModel().getForm().getLocalizer());
            }

            return true;

        }
    }


    /**
     * Read serialized {@link FormDef} from file and recreate as object.
     * 
     * @param formDef serialized FormDef file
     * @return {@link FormDef} object
     */
    public FormDef deserializeFormDef(File formDef) {

        // TODO: any way to remove reliance on jrsp?

        // need a list of classes that formdef uses
        PrototypeManager.registerPrototypes(SERIALIABLE_CLASSES);
        FileInputStream fis = null;
        FormDef fd = null;
        try {
            // create new form def
            fd = new FormDef();
            fis = new FileInputStream(formDef);
            DataInputStream dis = new DataInputStream(fis);

            // read serialized formdef into new formdef
            fd.readExternal(dis, ExtUtil.defaultPrototypes());
            dis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fd = null;
        } catch (IOException e) {
            e.printStackTrace();
            fd = null;
        } catch (DeserializationException e) {
            e.printStackTrace();
            fd = null;
        } catch (Exception e) {
            e.printStackTrace();
            fd = null;
        }

        return fd;
    }


    /**
     * Write the FormDef to the file system as a binary blog.
     * 
     * @param filepath path to the form file
     */
    public void serializeFormDef(FormDef fd, String filepath) {
        // calculate unique md5 identifier
        String hash = FileUtils.getMd5Hash(new File(filepath));
        // BEGIN custom
//        File formDef = new File(Collect.CACHE_PATH + "/" + hash + ".formdef");
        String formId = filepath.substring(filepath.lastIndexOf("/") + 1, filepath.lastIndexOf("."));
        File formDef = new File(FileUtilsExtended.FORMS_PATH + File.separator + formId + File.separator + hash + ".formdef");
        // END custom

        // formdef does not exist, create one.
        if (!formDef.exists()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(formDef);
                DataOutputStream dos = new DataOutputStream(fos);
                fd.writeExternal(dos);
                dos.flush();
                dos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onPostExecute(FECWrapper wrapper) {
        synchronized (this) {
            if (mStateListener != null) {
                if (wrapper == null) {
                    mStateListener.loadingError(mErrorMsg);
                } else {
                    // BEGIN custom 
//                    mStateListener.loadingComplete(wrapper.getController());
                    mStateListener.loadingComplete(wrapper.getController(), mFormDefinition, mFormInstance);
                    // END custom 
                }
            }
        }
    }


    public void setFormLoaderListener(FormLoaderListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }


    public void destroy() {
        if (data != null) {
            data.free();
            data = null;
        }
    }

}
