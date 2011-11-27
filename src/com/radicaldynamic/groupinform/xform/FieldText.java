package com.radicaldynamic.groupinform.xform;

import java.util.Iterator;
import java.util.regex.Pattern;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;

/*
 * Used for labels and hints on fields and item labels
 * 
 * This is class is responsible for looking up itext translations at run time
 */
public class FieldText
{
    private final String t = "FieldText: ";
    
    // If the value is null then ref should be non-null and refer to a translation ID
    private String value    = null;         // Regular string value associated with this text object
    private String ref      = null;         // A reference to an itext translation ID
    
    /*
     * Used by Field class to supply a default empty FieldText (which will hopefully be replaced upon form parsing)
     */
    public FieldText()
    {
        value = "";
    }
    
    /*
     * This constructor is intended to be used by FormReader when it 
     * parses and initialises labels & hints from recursivelyApplyProperty()
     */
    public FieldText(String valueOrRef) 
    {
        if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "instantiating with " + valueOrRef);
        
        if (Pattern.matches("^jr:.*", valueOrRef)) {
            String [] items = valueOrRef.split("'");    // jr:itext('GroupLabel')
            String id = items[1];                       // The string between the single quotes
            
            ref = id;
            value = null;
        } else {
            // Store these values as-is (they should be pre-encoded in the XML)
            value = valueOrRef;
            ref = null;
        }
    }
        
    // Takes a human readable string and encodes it for XMLs
    public void setValue(String value)
    {
        if (value == null) 
            this.value = value;
        else
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
    
    public boolean isTranslated()
    {
        return ref != null && ref.length() > 0 ? true : false;
    }
    
    @Override
    public String toString()
    {
        String result = "[Default Translation Missing]";
        
        if (value == null) {
            /*
             * If this FieldText has a reference to an itext translation then obtain
             * a single translation to represent this field on the form builder screen.
             * 
             * FIXME: We should select the most appropriate language (not necessarily English)
             *        before falling back to English.  The most appropriate language can be determined
             *        by checking the locale of the device.  Right now we just pick the first available language. 
             */
            if (ref == null) {
                if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "field has neither value nor reference to itext translation");
            } else {
                String translation = getDefaultTranslation(ref);

                if (translation instanceof String && translation.length() > 0)
                    result = translation;
            }                
        } else {
            result = getValue();
        }
    
        return result; 
    }

    /*
     * Retrieve a default translation for a specific ID
     */
    private String getDefaultTranslation(String id)
    {
        Iterator<Translation> translations = Collect.getInstance().getFormBuilderState().getTranslations().iterator();
        
        while (translations.hasNext()) {
            Translation t = translations.next();
            
            if (t.isFallback()) {
                Iterator<Translation> x = t.getTexts().iterator();
                
                while (x.hasNext()) {
                    Translation text = x.next();
                    
                    if (text.getId().equals(id))
                        return text.getValue();
                }
            }
        }
        
        return "";
    }
}
