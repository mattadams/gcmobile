package com.radicaldynamic.turboform.xform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.util.Base64;
import android.util.Log;

import com.mycila.xmltool.CallBack;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

public class FormReader
{
    private static final String t = "FormReader: ";
    
    private XMLTag mForm;                           // The "form" as it was loaded by xmltool
    private String mInstanceRoot;                   // The name of the instance root element 
    private String mDefaultPrefix;                  // The name of the default XForm prefix (needed for navigation)    
    private ArrayList<String> mFieldList = new ArrayList<String>();    
    
    // State of binds, fields, instances and translations    
    private ArrayList<Bind> mBindState = new ArrayList<Bind>();
    private ArrayList<Field> mFieldState = new ArrayList<Field>();
    private ArrayList<Instance> mInstanceState = new ArrayList<Instance>();
    private ArrayList<Translation> mTranslationState = new ArrayList<Translation>();    
    
    {
        // List of valid fields that we can handle
        Collections.addAll(mFieldList, "group", "input", "item", "repeat", "select", "select1", "trigger", "upload");
    }
    
    /*
     * Used to read in a form definition for manipulation by the Form Builder.
     * 
     * If newForm is true then FormReader will expect that the form template is bare and
     * requires initialization (add new instance root, etc).
     */
    public FormReader(InputStream is, boolean newForm)
    {
        mForm = XMLDoc.from(is, false);
        mDefaultPrefix = mForm.getPefix("http://www.w3.org/2002/xforms");
        
        // Initialize new forms
        if (newForm) {
            // This might now be rigorous enough for i18n input
            String formName = Collect.getInstance().getFbForm().getName(); 
            String instanceRoot = formName.replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]", "");
            
            Log.d(Collect.LOGTAG, t + "generated instance root name and name space word of " + instanceRoot);
            
            mForm.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix);
            mForm.addTag(XMLDoc.from("<" + instanceRoot + " xmlns=\"" + instanceRoot + "\"></" + instanceRoot + ">", false));
        }
        
        mInstanceRoot = mForm
            .gotoRoot()
            .gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix)
            .gotoChild()
            .getCurrentTagName();
        
        Log.d(Collect.LOGTAG, t + "default prefix for form: " + mDefaultPrefix);
        Log.d(Collect.LOGTAG, t + "instance root element name: " + mInstanceRoot);
    }
    
    public ArrayList<Bind> getBindState()
    {
        return mBindState;
    }
    
    public ArrayList<Field> getFieldState()
    {
        return mFieldState;
    }
    
    public ArrayList<Instance> getInstanceState()
    {
        return mInstanceState;
    }
    
    public ArrayList<Translation> getTranslationState()
    {
        return mTranslationState;
    }
    
    /*
     * If there is no instance root to return then we should generate one but perhaps 
     * this should be done elsewhere?
     */
    public String getInstanceRoot()
    {
        return mInstanceRoot;
    }
    
    /*
     * Returns a Base64 encoded form
     */
    public String getEncodedForm()
    {
        return Base64.encodeToString(mForm.gotoRoot().toBytes(), Base64.DEFAULT);       
    }
    
    /*
     * Trigger method for doing all of the actual work
     */
    public void parseForm()
    {
        if (mForm.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).hasTag("%1$s:itext", mDefaultPrefix)) {
            Log.d(Collect.LOGTAG, t + "parsing itext form translations...");
            parseFormTranslations(mForm.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:itext", mDefaultPrefix));
        } else
            Log.d(Collect.LOGTAG, t + "no form translations to parse");        
        
        Log.d(Collect.LOGTAG, t + "parsing form binds...");
        parseFormBinds(mForm.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix));

        Log.d(Collect.LOGTAG, t + "parsing form fields...");
        parseFormFields(mForm.gotoRoot().gotoTag("h:body"));
        
        Log.d(Collect.LOGTAG, t + "parsing form instance...");
        parseFormInstance(mForm.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix).gotoChild(), "/" + mInstanceRoot);
    }
    
    /*
     * Recursively iterate over the form fields, creating objects to represent these fields
     */
    private void parseFormFields(XMLTag tag)
    {       
        Log.v(Collect.LOGTAG, t + "visiting <" + tag.getCurrentTagName() + ">");
        
        if (mFieldList.contains(tag.getCurrentTagName())) {
            if (tag.getCurrentTagLocation().split("/").length == 2) {
                // Add a top level field
                mFieldState.add(new Field(tag, mBindState, mInstanceRoot, null));
            } else {
                // Field belongs elsewhere as a child of another field
                attachChildToParentField(tag, null);
            }
        } else if (tag.getCurrentTagName().equals("label")) {
            applyProperty(tag);
        } else if (tag.getCurrentTagName().equals("hint")) {
            applyProperty(tag);
        } else if (tag.getCurrentTagName().equals("value")) {
            applyProperty(tag);
        }

        // If field element has children then list fields recursively from the standpoint of every child
        if (tag.getChildCount() > 0) {           
            tag.forEachChild(new CallBack() {
                @Override
                public void execute(XMLTag arg0)
                {
                    parseFormFields(arg0);
                }
            });
        }
    }

    /*
     * For fields that are nested within other fields (e.g., not top-level group or repeat fields) 
     * determine which field is the parent and attach the new field to it as a child.
     * 
     * This uses the same test for determining a parent as recursivelyApplyProperty().
     */
    private void attachChildToParentField(XMLTag child, Field incomingParent)
    {
        Iterator<Field> it = null;
        
        if (incomingParent == null)
            it = mFieldState.iterator();
        else
            it = incomingParent.children.iterator();
        
        while (it.hasNext()) {
            Field parent = it.next();
            
            if (child.getCurrentTagLocation().split("/").length - parent.getLocation().split("/").length == 1 &&
                    parent.getLocation().equals(child.getCurrentTagLocation().substring(0, parent.getLocation().length())))
                parent.children.add(new Field(child, mBindState, mInstanceRoot, parent));
            
            if (!parent.children.isEmpty())
                attachChildToParentField(child, parent);
        }
    }
    
    /*
     * Iterate recursively through the list of fields stopping only when the correct field object 
     * has been found (the field object to which this property applies and should be set for)
     */
    private boolean applyProperty(XMLTag tag)
    {
        Iterator<Field> it = mFieldState.iterator();
        
        while (it.hasNext()) {
            Field c = it.next();
            if (recursivelyApplyProperty(c, tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*
     * Recursive method for applyProperty().  Determines the proper object to apply the property to by 
     * comparing the property location and the field object location.  E.g.,
     * 
     * If the field exists at *[2]/*[2]/*[3] then the property located at  *[2]/*[2]/*[3]/*[1] or 
     * any other direct child of *[2]/*[2]/*[3] should be assigned to same. 
     */
    private boolean recursivelyApplyProperty(Field targetField, XMLTag tag)
    {
        if (tag.getCurrentTagLocation().split("/").length - targetField.getLocation().split("/").length == 1 &&
                targetField.getLocation().equals(tag.getCurrentTagLocation().substring(0, targetField.getLocation().length()))) {       
            
            // Set the label
            if (tag.getCurrentTagName().contains("label")) {
                if (tag.hasAttribute("ref")) 
                    targetField.setLabel(tag.getAttribute("ref"));
                else
                    targetField.setLabel(tag.getInnerText());
            }
            
            // Set the hint
            if (tag.getCurrentTagName().contains("hint")) {
                if (tag.hasAttribute("ref")) 
                    targetField.setHint(tag.getAttribute("ref"));
                else
                    targetField.setHint(tag.getInnerText());                
            }
            
            // Set the value of this item (used for select and select1 item lists)
            if (tag.getCurrentTagName().contains("value")) {
                targetField.setItemValue(tag.getInnerText());
            }
            
            return true;
        }
        
        Iterator<Field> children = targetField.children.iterator();
        
        while (children.hasNext()) {
            Field child = children.next();
            
            if (recursivelyApplyProperty(child, tag))
                return true;
        }
        
        // This should never happen
        return false;
    }
    
    /*
     * Recursively parse the form translations, adding objects to mTranslationState to represent them
     */
    private void parseFormTranslations(XMLTag tag)
    {
        if (tag.getCurrentTagName().equals("translation")) {
            Log.v(Collect.LOGTAG, t + "adding translations for " + tag.getAttribute("lang"));
            mTranslationState.add(new Translation(tag.getAttribute("lang")));
        } else if (tag.getCurrentTagName().equals("text")) {
            Log.v(Collect.LOGTAG, t + "adding translation ID " + tag.getAttribute("id"));
            mTranslationState.get(mTranslationState.size() -1).getTexts().add(new Translation(tag.getAttribute("id"), null));
        } else if (tag.getCurrentTagName().equals("value")) {
            Log.v(Collect.LOGTAG, t + "adding translation: " + tag.getInnerText());
            mTranslationState.get(mTranslationState.size() - 1)
                .getTexts().get(mTranslationState.get(mTranslationState.size() - 1).getTexts().size() - 1)
                .setValue(tag.getInnerText());
        }
 
        if (tag.getChildCount() > 0) {           
            tag.forEachChild(new CallBack() {
                @Override
                public void execute(XMLTag arg0)
                {
                    parseFormTranslations(arg0);
                }
            });
        }
    }
    
    /*
     * Go through the list of binds and build objects to represent them.
     * Binds are associated with field objects when the field is instantiated.
     */
    private void parseFormBinds(XMLTag tag)
    {
        tag.forEachChild(new CallBack() {
            @Override
            public void execute(XMLTag arg0)
            {
                if (arg0.getCurrentTagName().equals("bind"))
                    mBindState.add(new Bind(arg0, mInstanceRoot));               
            }
        });
    }
    
    /*
     * Recursively parse and use the information supplied in the form instance
     * to supplement the field objects in mFieldState 
     */
    private void parseFormInstance(XMLTag tag, final String instancePath)
    {
        /*
         * If the instancePath does not currently point to the instance root
         * (this only happens the first time this method runs) 
         */
        if (instancePath.equals("/" + mInstanceRoot) == false) {
            Instance newInstance = new Instance(instancePath, tag.getInnerText(), tag.getCurrentTagLocation(), mBindState);
            
            // Attempt to apply this instance to a pre-existing field -- if this fails then the instance is "hidden"
            newInstance.setHidden(!applyInstanceToField(null, newInstance));
            
            if (tag.getCurrentTagLocation().split("/").length == 5) {
                // Add a top level instance
                mInstanceState.add(newInstance);
            } else {
                attachChildToParentInstance(newInstance, null);
            }
        }
        
        tag.forEachChild(new CallBack() {
            @Override
            public void execute(XMLTag arg0)
            {
                parseFormInstance(arg0, instancePath + "/" + arg0.getCurrentTagName());
            }
        });
    }
    
    /*
     * Attempts to apply an instance to an existing field
     * 
     * Returns true to indicate that application was successful and false
     * to indicate that it was not (e.g., the instance is hidden)
     */
    private boolean applyInstanceToField(Field field, final Instance instance)
    {
        Iterator<Field> it;
        
        if (field == null) {
            it = mFieldState.iterator();
        } else { 
            if (field.getRef() != null && field.getRef().equals(instance.getXpath())) {
                Log.v(Collect.LOGTAG, t + "instance matched with field object via " + instance.getXpath());
                field.setInstance(instance);
                field.getInstance().setField(field);
                return true;
            }
            
            it = field.children.iterator();
        }
        
        while (it.hasNext()) {
            Field c = it.next();
            
            if (applyInstanceToField(c, instance)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*
     * For instances that are nested within other instances
     * (e.g., those that are probably nested in a repeated group somewhere)
     */
    private boolean attachChildToParentInstance(Instance child, Instance incomingParent)
    {
        Iterator<Instance> it = null;
        
        if (incomingParent == null)
            it = mInstanceState.iterator();
        else
            it = incomingParent.getChildren().iterator();
        
        while (it.hasNext()) {
            Instance parent = it.next();
            
            if (child.getLocation().split("/").length - parent.getLocation().split("/").length == 1 &&
                    parent.getLocation().equals(child.getLocation().substring(0, parent.getLocation().length()))) {
                child.setParent(parent);
                parent.getChildren().add(child);
                return true;
            }
                
            
            if (!parent.getChildren().isEmpty())
                attachChildToParentInstance(child, parent);
        }      
        
        return false;
    }
}