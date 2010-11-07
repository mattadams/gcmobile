package com.radicaldynamic.turboform.xform;

public class TranslationText
{
    private String id;
    private String value;
    private boolean used;                   // Store whether this ID is being used; this will help later on when
                                            // we go to write the form XML so we can figure out which IDs are needed
    
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

    public void setUsed(boolean used)
    {
        this.used = used;
    }

    public boolean isUsed()
    {
        return used;
    }
}
