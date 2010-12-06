package com.radicaldynamic.groupinform.xform;

import java.io.InputStream;
import java.util.Iterator;

import android.util.Log;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDocument;

public final class FormWriter
{
    private static final String t = "FormWriter: ";
    
    private static FormDocument mFormDoc;
    private static XMLTag mFormTag;
    private static String mDefaultPrefix;
    private static String mInstanceRoot;
    
    public static byte[] writeXml(String instanceRoot)
    {
        // Retrieve and load a template XForm file (this makes it easier than hardcoding a new one from scratch)
        InputStream xis = Collect.getInstance().getResources().openRawResource(R.raw.xform_template);        
        mFormTag = XMLDoc.from(xis, false);
        mDefaultPrefix = mFormTag.getPefix("http://www.w3.org/2002/xforms");
        mInstanceRoot = instanceRoot;
        
        // Insert the title of this form into the XML
        mFormDoc = Collect.getInstance().getFbForm();
        mFormTag.gotoRoot().gotoTag("h:head/h:title").setText(FieldText.encodeXMLEntities(mFormDoc.getName()));
        
        // Either write out translations or remove the unused itext tag
        if (Collect.getInstance().getFbTranslationState().size() > 0) {
            if (!mFormTag.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).hasTag("%1$s:itext", mDefaultPrefix))
                mFormTag.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).addTag(mDefaultPrefix + ":itext");
            writeTranslations(null);
        } else {
            if (mFormTag.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix).hasTag("%1$s:itext", mDefaultPrefix))
                mFormTag.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:itext", mDefaultPrefix).delete();
        }
        
        writeInstance(null);        
        writeBinds();        
        writeControls(null);
        
        // Return XML for consumption
        return mFormTag.toBytes();
    }

    private static void writeControls(Field incomingField)
    {
        Iterator<Field> it;
        
        if (incomingField == null) {
            it = Collect.getInstance().getFbFieldState().iterator();
            mFormTag.gotoRoot().gotoTag("h:body");
        } else
            it = incomingField.getChildren().iterator();
        
        while (it.hasNext()) {
            Field field = it.next();
            
            mFormTag.addTag(field.getType());

            // Support for repeat (nodeset) references as well as regular references
            if (field.getXPath() != null)
                if (field.getType().equals("repeat"))
                    mFormTag.getCurrentTag().setAttribute("nodeset", field.getXPath());
                else 
                    mFormTag.getCurrentTag().setAttribute("ref", field.getXPath());
            
            // Upload control fields only
            if (field.getAttributes().containsKey("mediatype"))
                mFormTag.getCurrentTag().setAttribute("mediatype", field.getAttributes().get("mediatype"));
            
            // If the label does not reference an itext translation then attempt to output a regular label
            if (field.getLabel().getRef() == null) {
                // We don't gotoParent() after adding a tag with a text value (see longer comment in writeInstance)
                if (field.getLabel().toString().length() > 0) {
                    /* 
                     * Special support for labels that take advantage of XForm output from instance fields.  E.g.,
                     * <label>review widget. is your email still <output value="/widgets/regex"/>?</label>
                     */ 
                    mFormTag.addTag(XMLDoc.from("<label>" + FieldText.encodeXMLEntities(field.getLabel().toString().replace("xmlns=\"http://www.w3.org/2002/xforms\" ", "")) + "</label>", false));                                       
                }
            } else
                mFormTag.addTag("label").addAttribute("ref", "jr:itext('" + field.getLabel().getRef() + "')").gotoParent();
            
            // Do the same for hints
            if (field.getHint().getRef() == null) {
                if (field.getHint().toString().length() > 0) {
                    mFormTag.addTag(XMLDoc.from("<hint>" + FieldText.encodeXMLEntities(field.getHint().toString().replace("xmlns=\"http://www.w3.org/2002/xforms\" ", "")) + "</hint>", false));
                }                    
            } else 
                mFormTag.addTag("hint").addAttribute("ref", "jr:itext('" + field.getHint().getRef() + "')").gotoParent();
            
            // Special support for item control fields
            if (field.getType().equals("item")) {
                if (field.getItemValue() == null)
                    mFormTag.addTag("value").setText("");
                else 
                    mFormTag.addTag("value").setText(field.getItemValue());
            }
            
            writeControls(field);
            
            mFormTag.gotoParent();
        }       
    }

    private static void writeBinds()
    {
        Iterator<Bind> it = Collect.getInstance().getFbBindState().iterator();
        
        mFormTag.gotoRoot().gotoTag("h:head/%1$s:model", mDefaultPrefix);
        
        while (it.hasNext()) {
            Bind bind = it.next();
            
            if (bind.hasUnhandledAttribute())
                Log.w(Collect.LOGTAG, t + "bind " + bind.getXPath() + " has unhandled attributes that will not be written; data will be lost!");
            
            mFormTag.addTag("bind").addAttribute("nodeset", bind.getXPath());
            
            // The following are conditional attributes
            if (bind.getType() != null) mFormTag.getCurrentTag().setAttribute("type", bind.getType());
            if (bind.isReadonly()) mFormTag.getCurrentTag().setAttribute("readonly", bind.getReadonly());
            if (bind.isRequired()) mFormTag.getCurrentTag().setAttribute("required", bind.getRequired());
            
            if (bind.getPreload() != null && bind.getPreloadParams() != null) {
                mFormTag.getCurrentTag().setAttribute("jr:preload", bind.getPreload());
                mFormTag.getCurrentTag().setAttribute("jr:preloadParams", bind.getPreloadParams());
            }
            
            if (bind.getConstraint()    != null) mFormTag.getCurrentTag().setAttribute("constraint", bind.getConstraint());            
            if (bind.getConstraintMsg() != null) mFormTag.getCurrentTag().setAttribute("jr:constraintMsg", bind.getConstraintMsg());            
            if (bind.getRelevant()      != null) mFormTag.getCurrentTag().setAttribute("relevant", bind.getRelevant());            
            if (bind.getCalculate()     != null) mFormTag.addAttribute("calculate", bind.getCalculate());
            
            // Make sure the next bind is inserted at the same level as this one
            mFormTag.gotoParent();
        }
    }

    private static void writeInstance(Instance incomingInstance)
    {
        Iterator<Instance> it;
        
        if (incomingInstance == null) {
            it = Collect.getInstance().getFbInstanceState().iterator();
            
            // Initialize the instance root (only done once)
            mFormTag.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:instance", mDefaultPrefix);
            mFormTag.addTag(mInstanceRoot);            
            
            // TODO: find another way to accomplish this, xmltool makes it possible but very difficult
            //mFormTag.addTag(XMLDoc.from("<" + mInstanceRoot + " xmlns=\"" + mInstanceRoot + "\"></" + mInstanceRoot + ">", false));
        } else
            it = incomingInstance.getChildren().iterator();
            
        while (it.hasNext()) {
            Instance instance = it.next();
            
            if (instance.getChildren().isEmpty()) {
                /*
                 * For some reason unknown to me we can only call gotoParent() when adding 
                 * an empty tag.  Calling it after adding an empty tag OR a tag with a text
                 * value causes the nesting to get screwed up.  This doesn't make any sense
                 * to me.  It might be a bug with xmltool.
                 */
                if (instance.getDefaultValue().length() == 0) { 
                    mFormTag.addTag(instance.getName());
                    mFormTag.gotoParent();
                } else 
                    mFormTag.addTag(instance.getName()).setText(instance.getDefaultValue());             
                    
            } else {
                // Likely a repeated data set
                mFormTag.addTag(instance.getName()).addAttribute("jr:template", "");      
                writeInstance(instance);
                mFormTag.gotoParent();
            }
        }
    }

    private static void writeTranslations(Translation i18n)
    {
        Iterator<Translation> it;
        
        if (i18n == null)       
            it = Collect.getInstance().getFbTranslationState().iterator();
        else
            it = i18n.getTexts().iterator();
        
        while (it.hasNext()) {
            Translation text = it.next();
            
            if (text.isSet()) {
                mFormTag.gotoRoot().gotoTag("h:head/%1$s:model/%1$s:itext", mDefaultPrefix);
                mFormTag.addTag("translation").addAttribute("lang", text.getLang());
                writeTranslations(text);
            } else {
                mFormTag
                    .addTag("text").addAttribute("id", text.getId())
                    .addTag("value").setText(text.getValue())
                    .gotoParent();
            }
        }
    }
}
