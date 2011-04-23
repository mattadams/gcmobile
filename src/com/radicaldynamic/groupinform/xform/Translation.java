package com.radicaldynamic.groupinform.xform;

import java.util.ArrayList;

public class Translation
{
    private ArrayList<Translation> texts = new ArrayList<Translation>();
    
    // Being lazy here and setting to empty strings so we don't have to worry about null values, at least initially
    private String id       = "";       // ID of this translation text
    private String lang     = "";       // Translation groups will have this set 
    private String value    = "";       // Translation text will have this set 
    
    private boolean group = false;     // Whether or not this object is a group of translations for a given language
    
    public Translation(String lang)
    {
        setLang(lang);
        setGroup(true);
    }
    
    public Translation(String id, String value) 
    {
        setId(id);
        setValue(value);
    }   
    
    public String toString()
    {
        String result = "";
        
        if (lang instanceof String && lang.length() > 0)
            result = lang;
        else if (value instanceof String && value.length() > 0)
            result = value;
        
        return result;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isGroup() {
        return group;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setTexts(ArrayList<Translation> texts) {
        this.texts = texts;
    }

    public ArrayList<Translation> getTexts() {
        return texts;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    // TODO: refactor this stuff (list of languages, abbreviations & lookups into a separate application-persistent singleton)
    public static String expandLangAbbreviation(String [] ls, ArrayList<String> a, String l) 
    {
        if (a.contains(l.toLowerCase())) {
            int i = a.indexOf(l.toLowerCase());
            l = ls[i];
        }
        
        return l;
    }
}
