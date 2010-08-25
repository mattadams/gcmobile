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

package org.odk.collect.android.tasks;

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
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.odk.collect.android.listeners.FormLoaderListener;
import org.odk.collect.android.logic.FileReferenceFactory;
import org.odk.collect.android.utilities.FileUtils;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Background task for loading a form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormLoaderTask extends AsyncTask<String, String, FormLoaderTask.FECWrapper> {
    private final static String t = "FormLoaderTask";
    /**
     * Classes needed to serialize objects
     */
    public final static String[] SERIALIABLE_CLASSES =
        {
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
                "org.javarosa.xpath.expr.XPathStringLiteral",
                "org.javarosa.xpath.expr.XPathUnionExpr",
                "org.javarosa.xpath.expr.XPathVariableReference"
        };

    FormLoaderListener mStateListener;

    protected class FECWrapper {
        FormEntryController controller;


        protected FECWrapper(FormEntryController controller) {
            this.controller = controller;
        }


        protected FormEntryController getController() {
            return controller;
        }


        protected void free() {
            controller = null;
        }
    }

    FECWrapper data;


    /**
     * Initialize {@link FormEntryController} with {@link FormDef} from binary or from XML. If given
     * an instance, it will be used to fill the {@link FormDef}.
     */
    @Override
    protected FECWrapper doInBackground(String... path) {
        FormEntryController fec = null;
        FormDef fd = null;
        FileInputStream fis = null;

        String formPath = path[0];
        String instancePath = path[1];

        File formXml = new File(formPath);
        File formBin = new File(FileUtils.CACHE_PATH + FileUtils.getMd5Hash(formXml) + ".formdef");

        if (formBin.exists()) {
            // if we have binary, deserialize binary
            fd = deserializeFormDef(formBin);
            if (fd == null) {
                return null;
            }
        } else {
            // no binary, read from xml
            try {
                fis = new FileInputStream(formXml);
                fd = XFormUtils.getFormFromInputStream(fis);
                if (fd == null) {
                    return null;
                }
                serializeFormDef(fd, formPath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                if (fd == null) {
                    return null;
                }
            }
        }

        // new evaluation context for function handlers
        EvaluationContext ec = new EvaluationContext();
        fd.setEvaluationContext(ec);

        // create FormEntryController from formdef
        FormEntryModel fem = new FormEntryModel(fd);
        fec = new FormEntryController(fem);

        // import existing data into formdef
        if (instancePath != null) {
            // This order is important.  Import data, then initialize.
            importData(instancePath, fec);
            fd.initialize(false);
        } else {
            fd.initialize(true);
        }

        // set paths to /sdcard/odk/forms/formfilename-media/
        // This is a singleton, how do we ensure that we're not doing this
        // multiple times?
        String mediaPath =
            formXml.getName().substring(0, formXml.getName().lastIndexOf("."));
        
        Log.e("Carl", "mediaPath = " + mediaPath);
       
        if (ReferenceManager._().getFactories().length == 0) {
            ReferenceManager._().addReferenceFactory(
                new FileReferenceFactory(Environment.getExternalStorageDirectory() + "/odk/forms/"
                        + mediaPath + "-media"));
            ReferenceManager._()
                    .addRootTranslator(new RootTranslator("jr://images/", "jr://file/"));
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://audio/", "jr://file/"));
        }

        // clean up vars
        fis = null;
        fd = null;
        formBin = null;
        formXml = null;
        formPath = null;
        instancePath = null;

        data = new FECWrapper(fec);
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
                fec.getModel().getForm().localeChanged(fec.getModel().getLanguage(),
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DeserializationException e) {
            e.printStackTrace();
        }

        return fd;
    }


    /**
     * Write the FormDef to the file system as a binary blog.
     * 
     * @param filepath path to the form file
     */
    public void serializeFormDef(FormDef fd, String filepath) {
        // if cache folder is missing, create it.
        if (FileUtils.createFolder(FileUtils.CACHE_PATH)) {

            // calculate unique md5 identifier
            String hash = FileUtils.getMd5Hash(new File(filepath));
            File formDef = new File(FileUtils.CACHE_PATH + hash + ".formdef");

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
    }


    @Override
    protected void onPostExecute(FECWrapper wrapper) {
        synchronized (this) {
            if (mStateListener != null)
                mStateListener.loadingComplete(wrapper.getController());
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
