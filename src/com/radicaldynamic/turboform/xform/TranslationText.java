package com.radicaldynamic.turboform.xform;

public class TranslationText
{
    private String id;
    private String value;
    
    public TranslationText(String id)
    {
        setId(id);
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
