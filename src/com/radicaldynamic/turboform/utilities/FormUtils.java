package com.radicaldynamic.turboform.utilities;

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
import com.radicaldynamic.turboform.xform.Control;
import com.radicaldynamic.turboform.xform.Translation;
import com.radicaldynamic.turboform.xform.TranslationText;

public class FormUtils
{
    private static final String t = "FormUtils: ";
    
    private XMLTag mForm;                           // The "form" as it was loaded by xmltool
    private String mInstanceRoot;                   // The name of the instance root element 
    private String mDefaultPrefix;                  // The name of the default Xform prefix (needed for navigation)    
    private ArrayList<String> mControlList = new ArrayList<String>();    
    
    // State of controls and other form elements    
    private ArrayList<Control> mControlState = new ArrayList<Control>();
    private ArrayList<Translation> mTranslationState = new ArrayList<Translation>();
    
    {
        // List of valid controls that we can handle
        Collections.addAll(mControlList, "group", "input", "item", "repeat", "select", "select1", "trigger", "upload");
    }
    
    public FormUtils(String title) {
        //XMLTag mForm = XMLDoc.from(, false);
        System.out.println(mForm.toString());
    }
    
    public FormUtils(InputStream is)
    {
        mForm = (XMLDoc.from(is, false));
        mDefaultPrefix = mForm.getPefix("http://www.w3.org/2002/xforms");
        mInstanceRoot = mForm.gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix).gotoChild().getCurrentTagName();
        
        Log.d(Collect.LOGTAG, t + "default prefix for form: " + mDefaultPrefix);
        Log.d(Collect.LOGTAG, t + "instance root element name: " + mInstanceRoot);
    }
    
    public ArrayList<Control> getControlState()
    {
        return mControlState;
    }
    
    public ArrayList<Translation> getTranslationState()
    {
        return mTranslationState;
    }

    public XMLTag getForm()
    {
        return mForm;
    }
    
    /*
     * Returns a Base64 encoded form
     */
    public String getEncodedForm()
    {
        return Base64.encodeToString(mForm.gotoRoot().toBytes(), Base64.DEFAULT);       
    }
    
//    /*
//     * 
//     */
//    public void removeNode(String nodeName)
//    {
//        String ns = "";                 // Stores the namespace of nodes in the actual instance document                    
//        String [] nodePathParts;        // Stores various pieces of an XPath to a node
//        String nodeNameArg = nodeName;  // The original nodeName as it was passed to this method
//        
//        // Obtain default name space for the actual instance document (within the <instance> element)
//        if (mForm.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix).gotoChild().hasAttribute("xmlns")) {
//            ns = mForm.getPefix(mForm.getAttribute("xmlns")); 
//        } else {
//            ns = mDefaultPrefix;
//        }
//        
//        if ((nodePathParts = nodeName.split("/")).length > 0) {
//            nodeName = "";
//            
//            for (String pathPart : nodePathParts) {
//                nodeName = nodeName + ns + ":" + pathPart + "/";                
//            }
//            
//            nodeName = nodeName.substring(0, nodeName.length() - 1);
//        } else {
//            nodeName = ns + ":" + nodeName;
//        }
//        
//        mForm.gotoTag(nodeName).delete();
//        
//        removeBind(nodeNameArg);
//        removeControl(nodeNameArg);
//    }
//    
//    public void removeBind(final String nodePath)
//    {
//        final String initialPattern = "/" + mInstanceRoot + "/" + nodePath;  
//        
//        mForm.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).forEachChild(new CallBack() {
//            @Override
//            public void execute(XMLTag tag)
//            {
//                if (tag.getCurrentTagName().equals("bind"))
//                    if (Pattern.matches(initialPattern + "(|/.*)", tag.getAttribute("nodeset")))
//                        tag.delete();
//            }
//        });
//    }
//
//    public void removeControl(final String nodeName)
//    {
//        mForm.gotoRoot().gotoTag("h:body").forEachChild(new CallBack() {
//            @Override
//            public void execute(XMLTag tag)
//            {
//                removeRecursiveControl(tag, nodeName);
//            }
//        });
//    }
//    
//    public void removeRecursiveControl(XMLTag tag, final String nodeName) 
//    {
//        if (tag.hasAttribute("ref") && tag.getAttribute("ref").equals(nodeName.substring(nodeName.lastIndexOf("/") + 1, nodeName.length()))) {
//            // Remove an independent control
//            tag.delete();
//        } else if (tag.hasAttribute("nodeset") && tag.getAttribute("nodeset").equals("/" + mInstanceRoot + "/" + nodeName)) {
//            // Remove a repeat element
//            tag.delete(); 
//        } else if (tag.getChildCount() > 0) {
//            // If this element has children then iterate recursively over them 
//            tag.forEachChild(new CallBack() {
//                @Override
//                public void execute(XMLTag arg0)
//                {
//                    removeRecursiveControl(arg0, nodeName);                                        
//                }
//            });
//            
//            if (tag.getCurrentTagName().equals("group") && tag.getChildCount() == 1) {
//                // Remove empty groups
//                tag.delete();
//            } else if (tag.getCurrentTagName().equals("repeat") && tag.getChildCount() == 0) {
//                // Remove empty repeated questions                
//                // TODO: this should really call removeNode() to also remove the instance and binds
//                tag.delete();
//            }
//        }
//    }
    
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

        Log.d(Collect.LOGTAG, t + "parsing form controls...");
        parseFormControls(mForm.gotoRoot().gotoTag("h:body"));      
    }
    
    /*
     * Recursively iterate over the form controls, creating objects to represent these controls
     */
    private void parseFormControls(XMLTag tag)
    {       
        Log.v(Collect.LOGTAG, t + "visiting <" + tag.getCurrentTagName() + ">");
        
        if (mControlList.contains(tag.getCurrentTagName())) {
            if (tag.getCurrentTagLocation().split("/").length == 2) {                
                mControlState.add(new Control(tag));
            } else {
                attachChildToParentControl(tag, null);
            }
        } else if (tag.getCurrentTagName().equals("label")) {
            applyProperty(tag);
        } else if (tag.getCurrentTagName().equals("hint")) {
            applyProperty(tag);
        } else if (tag.getCurrentTagName().equals("value")) {
            applyProperty(tag);
        }

        // If control element has children then listControls recursively from the standpoint of every child
        if (tag.getChildCount() > 0) {           
            tag.forEachChild(new CallBack() {
                @Override
                public void execute(XMLTag arg0)
                {
                    parseFormControls(arg0);
                }
            });
        }
    }

    /*
     * For controls that are nested within other controls (e.g., not top-level group or repeat controls) 
     * determine which control is the parent and attach the new control to it as a child.
     * 
     * This uses the same test for determining a parent as recursivelyApplyProperty().
     */
    private void attachChildToParentControl(XMLTag child, Control parent)
    {
        Iterator<Control> it = null;
        
        if (parent == null)
            it = mControlState.iterator();
        else
            it = parent.children.iterator();
        
        while (it.hasNext()) {
            Control possibleParent = it.next();
            
            if (child.getCurrentTagLocation().split("/").length - possibleParent.getLocation().split("/").length == 1 &&
                    possibleParent.getLocation().equals(child.getCurrentTagLocation().substring(0, possibleParent.getLocation().length())))
                possibleParent.children.add(new Control(child));                
            
            if (!possibleParent.children.isEmpty())
                attachChildToParentControl(child, possibleParent);
        }
    }
    
    /*
     * Iterate recursively through the list of controls stopping only when the correct control object 
     * has been found (the control object to which this property applies and should be set for)
     */
    private boolean applyProperty(XMLTag tag)
    {
        Iterator<Control> it = mControlState.iterator();
        
        while (it.hasNext()) {
            Control c = it.next();
            if (recursivelyApplyProperty(c, tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*
     * Recursive method for applyProperty().  Determines the proper object to apply the property to by 
     * comparing the property location and the control object location.  E.g.,
     * 
     * If the control exists at *[2]/*[2]/*[3] then the property located at  *[2]/*[2]/*[3]/*[1] or 
     * any other direct child of *[2]/*[2]/*[3] should be assigned to same. 
     */
    private boolean recursivelyApplyProperty(Control targetControl, XMLTag tag)
    {
        if (tag.getCurrentTagLocation().split("/").length - targetControl.getLocation().split("/").length == 1 &&
                targetControl.getLocation().equals(tag.getCurrentTagLocation().substring(0, targetControl.getLocation().length()))) {           
            String label = tag.getInnerText();
            
            /*
             * Obtain a single translation to represent this control on the form builder screen
             * 
             * TODO: We should select the most appropriate language (not necessarily English)
             *       before falling back to English.  The most appropriate language can be determined
             *       by checking the locale of the device. 
             */ 
            if (tag.hasAttribute("ref")) {
                String ref = tag.getAttribute("ref");
                String [] items = ref.split("'");           // jr:itext('GroupLabel')
                String id = items[1];                       // The string between the single quotes
                
                label = getTranslation("English", id);
                label = label + " [i18n]";
            }
            
            // Set the label
            if (tag.getCurrentTagName().contains("label")) {
                if (tag.hasAttribute("ref")) 
                    targetControl.setLabel(tag.getAttribute("ref"));
                    
                targetControl.setLabel(label);                
            }
            
            // Set the hint
            if (tag.getCurrentTagName().contains("hint")) {
                if (tag.hasAttribute("ref")) 
                    targetControl.setHint(tag.getAttribute("ref"));
                
                targetControl.setHint(label);                
            }
            
            if (tag.getCurrentTagName().contains("value")) {
                targetControl.setValue(tag.getInnerText());
            }
            
            return true;
        }
        
        Iterator<Control> children = targetControl.children.iterator();
        
        while (children.hasNext()) {
            Control child = children.next();
            
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
            mTranslationState.get(mTranslationState.size() -1).texts.add(new TranslationText(tag.getAttribute("id")));
        } else if (tag.getCurrentTagName().equals("value")) {
            Log.v(Collect.LOGTAG, t + "adding translation: " + tag.getInnerText());
            mTranslationState.get(mTranslationState.size() - 1)
                .texts.get(mTranslationState.get(mTranslationState.size() - 1).texts.size() - 1)
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
     * Retrieve a translation for a specific ID from a specific language  
     */
    private String getTranslation(String language, String id)
    {
        Iterator<Translation> translations = mTranslationState.iterator();
        
        while (translations.hasNext()) {
            Translation translation = translations.next();
            
            if (translation.getLang().equals(language)) {
                Iterator<TranslationText> texts = translation.texts.iterator();
                
                while (texts.hasNext()) {
                    TranslationText text = texts.next();
                    
                    if (text.getId().equals(id))
                        return text.getValue();
                }
            }
        }
        
        return "[Translation Not Available]";
    }
}
