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
    
    /*
     * Used by Field class to supply a default empty FieldText (which will hopefully be replaced upon form parsing)
     */
    public FieldText()
    {
        this.value = "";
    }
    
    /*
     * This constructor is intended to be used by FormReader when it 
     * parses and initialises labels & hints from recursivelyApplyProperty()
     */
    public FieldText(String valueOrRef) 
    {
        Log.v(Collect.LOGTAG, t + "instantiating with " + valueOrRef);
        
        if (Pattern.matches("^jr:.*", valueOrRef)) {
            String [] items = valueOrRef.split("'");    // jr:itext('GroupLabel')
            String id = items[1];                       // The string between the single quotes
            
            ref = id;
        } else {
            // Store these values as-is (they should be pre-encoded in the XML)
            value = valueOrRef;
        }
    }
        
    @Override
    public String toString()
    {
        //Log.v(Collect.LOGTAG, t + "trying to turn toString() a FieldText with value " + value + " and ref " + ref);
        
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
    
    // Takes a human readable string and encodes it for XMLs
    public void setValue(String value)
    {
        this.value = encodeXMLEntities(value);
    }
    
    // Returns a decoded string ready for human consumption
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
    
    // Encoding with special support to ensure that <output value="/widgets/regex"/> is not encoded
    static public String encodeXMLEntities(String str)
    {        
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll("(?<!\"/)>", "&gt;");
        str = str.replaceAll("(?!<output)<", "&lt;");
        str = str.replaceAll("(?!value=\"|\"\\s*/)\"", "&quot;");
        
        return str;
    }
    
    /*
     * Retrieve a translation for a specific ID from a specific language.
     */
    private String getTranslation(String language, String id)
    {
        Iterator<Translation> translations = Collect.getInstance().getFbTranslationState().iterator();
        
        while (translations.hasNext()) {
            Translation translation = translations.next();
            
            if (translation.getLang().equals(language)) {
                Iterator<Translation> texts = translation.getTexts().iterator();
                
                Log.v(Collect.LOGTAG, t + "looking up " + language + " translations");
                
                while (texts.hasNext()) {
                    Translation text = texts.next();
                    
                    Log.v(Collect.LOGTAG, t + "looking at " + text.getId() + " for translation of " + id);
                    
                    if (text.getId().equals(id))
                        return text.getValue();
                }
            }
        }
        
        return "";
    }
}
