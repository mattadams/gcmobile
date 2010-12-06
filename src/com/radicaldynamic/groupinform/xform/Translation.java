package com.radicaldynamic.groupinform.xform;

import java.util.ArrayList;


public class Translation
{
    private ArrayList<Translation> texts = new ArrayList<Translation>();
    
    // Being lazy here and setting to empty strings so we don't have to worry about null values 
    private String id       = "";    
    private String lang     = "";
    private String value    = "";
    private boolean set     = false;            // Whether or not this object is a translation set (e.g., series of translations)
    
    public Translation(String lang)
    {
        setLang(lang);
        setSet(true);
    }
    
    public Translation(String id, String value) 
    {
        setId(id);
        setValue(value);
    }   

    public void setTexts(ArrayList<Translation> texts)
    {
        this.texts = texts;
    }

    public ArrayList<Translation> getTexts()
    {
        return texts;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setLang(String lang)
    {
        this.lang = lang;
    }

    public String getLang()
    {
        return lang;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public void setSet(boolean set)
    {
        this.set = set;
    }

    public boolean isSet()
    {
        return set;
    }
}
