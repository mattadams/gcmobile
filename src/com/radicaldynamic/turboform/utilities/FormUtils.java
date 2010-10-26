package com.radicaldynamic.turboform.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.util.Base64;

import com.mycila.xmltool.CallBack;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

public class FormUtils
{
    private XMLTag mForm;
    private String mInstanceRoot;
    private String mDefaultPrefix;

    public enum mControls {
        INPUT, SELECT, SELECT1, UPLOAD, TRIGGER, GROUP, REPEAT
    };

    public enum mBindAttributes {
        TYPE, REQUIRED, JR_PRELOAD, JR_PRELOAD_PARAMS, CONSTRAINT, CONSTRAINT_MSG, CALCULATE, RELEVANT, READONLY
    };

    public FormUtils(String title) {
        //XMLTag mForm = XMLDoc.from(, false);
        System.out.println(mForm.toString());
    }

    public FormUtils(File formFile) {
        mForm = (XMLDoc.from(formFile, false));
        mDefaultPrefix = mForm.getPefix("http://www.w3.org/2002/xforms");
        mInstanceRoot = mForm.gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix).gotoChild().getCurrentTagName();
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

    public void addNode(String nodeName)
    {

    }

    /*
     * 
     */
    public void removeNode(String nodeName)
    {
        String ns = "";                 // Stores the namespace of nodes in the actual instance document                    
        String [] nodePathParts;        // Stores various pieces of an XPath to a node
        String nodeNameArg = nodeName;  // The original nodeName as it was passed to this method
        
        // Obtain default name space for the actual instance document (within the <instance> element)
        if (mForm.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix).gotoChild().hasAttribute("xmlns")) {
            ns = mForm.getPefix(mForm.getAttribute("xmlns")); 
        } else {
            ns = mDefaultPrefix;
        }
        
        if ((nodePathParts = nodeName.split("/")).length > 0) {
            nodeName = "";
            
            for (String pathPart : nodePathParts) {
                nodeName = nodeName + ns + ":" + pathPart + "/";                
            }
            
            nodeName = nodeName.substring(0, nodeName.length() - 1);
        } else {
            nodeName = ns + ":" + nodeName;
        }
        
        mForm.gotoTag(nodeName).delete();
        
        removeBind(nodeNameArg);
        removeControl(nodeNameArg);
    }

    public void addBind(String nodeName, HashMap<String, String> attributes)
    {

    }

    public void removeBind(final String nodePath)
    {
        final String initialPattern = "/" + mInstanceRoot + "/" + nodePath;  
        
        mForm.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).forEachChild(new CallBack() {
            @Override
            public void execute(XMLTag tag)
            {
                if (tag.getCurrentTagName().equals("bind"))
                    if (Pattern.matches(initialPattern + "(|/.*)", tag.getAttribute("nodeset")))
                        tag.delete();
            }
        });
    }

    private String evalBindAttribute(mBindAttributes attribute, String value)
    {
        return value;

    }

    public void addControl(mControls control, String nodeName, String label,
            String hint)
    {

    }

    public void removeControl(final String nodeName)
    {
        mForm.gotoRoot().gotoTag("h:body").forEachChild(new CallBack() {
            @Override
            public void execute(XMLTag tag)
            {
                removeRecursiveControl(tag, nodeName);
            }
        });
    }
    
    public void removeRecursiveControl(XMLTag tag, final String nodeName) 
    {
        if (tag.hasAttribute("ref") && tag.getAttribute("ref").equals(nodeName.substring(nodeName.lastIndexOf("/") + 1, nodeName.length()))) {
            // Remove an independent control
            tag.delete();
        } else if (tag.hasAttribute("nodeset") && tag.getAttribute("nodeset").equals("/" + mInstanceRoot + "/" + nodeName)) {
            // Remove a repeat element
            tag.delete(); 
        } else if (tag.getChildCount() > 0) {
            // If this element has children then iterate recursively over them 
            tag.forEachChild(new CallBack() {
                @Override
                public void execute(XMLTag arg0)
                {
                    removeRecursiveControl(arg0, nodeName);                                        
                }
            });
            
            if (tag.getCurrentTagName().equals("group") && tag.getChildCount() == 1) {
                // Remove empty groups
                tag.delete();
            } else if (tag.getCurrentTagName().equals("repeat") && tag.getChildCount() == 0) {
                // Remove empty repeated questions                
                // TODO: this should really call removeNode() to also remove the instance and binds
                tag.delete();
            }
        }
    }
}
