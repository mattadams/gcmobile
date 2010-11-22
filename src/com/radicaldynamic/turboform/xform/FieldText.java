package com.radicaldynamic.turboform.xform;

import java.util.Iterator;
import java.util.regex.Pattern;

import android.util.Log;

import com.radicaldynamic.turboform.application.Collect;

/*
 * Used for labels and hints on fields and item labels
 * 
 * This is class is responsible for looking up itext translations at run time
 */
public class FieldText
{
    private final String t = "FieldText: ";
    
    private String value    = null;         // Regular string value associated with this text object
    private String ref      = null;         // A reference to an itext translation
    
    public FieldText(String valueOrRef) 
    {
        Log.v(Collect.LOGTAG, t + "instantiating with " + valueOrRef);
        
        if (Pattern.matches("^jr:.*", valueOrRef)) {
            String [] items = valueOrRef.split("'");    // jr:itext('GroupLabel')
            String id = items[1];                       // The string between the single quotes
            
            ref = id;
        } else 
            value = valueOrRef;
    }
        
    @Override
    public String toString()
    {
        Log.v(Collect.LOGTAG, t + "trying to turn toString() a FieldText with value " + value + " and ref " + ref);
        
        String result = "";
        
        if (value == null) {
            /*
             * If this FieldText has a reference to an itext translation then obtain
             * a single translation to represent this field on the form builder screen.
             * 
             * FIXME: We should select the most appropriate language (not necessarily English)
             *        before falling back to English.  The most appropriate language can be determined
             *        by checking the locale of the device. 
             *        
             * TODO: I am not sure that the translation will always be labelled as "English" as
             *       I have seen at least one other XForm where it was written "eng".  Our code
             *       needs to take this into account.
             */
            if (ref == null)
                Log.w(Collect.LOGTAG, t + "exists but has neither value nor reference to itext translation");
            else {
                String translation = getTranslation("English", ref);
                
                if (translation.length() == 0)
                    // FIXME: should be a string resource
                    result = "[Translation Not Available]";
                else
                    // FIXME: so should i18n
                    result = translation + " [i18n]";
            }                          
        } else
            result = getValue();

        return result;
    }
    
    public void setValue(String value)
    {
        this.value = encodeXMLEntities(value);
    }
    
    public String getValue()
    {
        return decodeXMLEntities(value);
    }
    
    public void setRef(String ref)
    {
        this.ref = ref;
    }
    
    public String getRef()
    {
        return ref;
    }    
    
    // See http://en.wikipedia.org/wiki/Character_encodings_in_HTML#XML_character_references
    static public String decodeXMLEntities(String str)
    {
        str = str.replaceAll("&amp;", "&");
        str = str.replaceAll("&apos;", "'");
        str = str.replaceAll("&gt;", ">");
        str = str.replaceAll("&lt;", "<");
        str = str.replaceAll("&quot;", "\"");
        
        return str;
    }
    
    static public String encodeXMLEntities(String str)
    {
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("\"", "&quot;");
        
        return str;
    }
    
    /*
     * Retrieve a translation for a specific ID from a specific language.
     */
    private String getTranslation(String language, String id)
    {
        Iterator<Translation> translations = Collect.getInstance().getFormBuilderTranslationState().iterator();
        
        while (translations.hasNext()) {
            Translation translation = translations.next();
            
            if (translation.getLang().equals(language)) {
                Iterator<TranslationText> texts = translation.texts.iterator();
                
                Log.v(Collect.LOGTAG, t + "looking up " + language + " translations");
                
                while (texts.hasNext()) {
                    TranslationText text = texts.next();
                    
                    Log.v(Collect.LOGTAG, t + "looking at " + text.getId() + " for translation of " + id);
                    
                    if (text.getId().equals(id)) {
                        text.setUsed(true);
                        return text.getValue();
                    }
                }
            }
        }
        
        return "";
    }
}
