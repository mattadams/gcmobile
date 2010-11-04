package com.radicaldynamic.turboform.xform;

import java.util.regex.Pattern;

/*
 * Used for labels and hints on controls and item labels
 */
public class ControlText
{
    private String value;       // Regular string value associated with this text object
    private String ref;         // A reference to an itext translation
    
    public ControlText(String valueOrRef) 
    {
        if (Pattern.matches("^jr:", valueOrRef)) {
            String [] items = valueOrRef.split("'");    // jr:itext('GroupLabel')
            String id = items[1];                       // The string between the single quotes
            
            setRef(id);
        } else 
            setValue(valueOrRef);
    }
        
    @Override
    public String toString()
    {
        String result = "";
        
        if (value == null)
            result = getValue();
        else 
            result = getValue();
        
        return result;
    }
    
    public void setValue(String value)
    {
        this.value = value;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public void setRef(String ref)
    {
        this.ref = ref;
    }
    
    public String getRef()
    {
        return ref;
    }
}
