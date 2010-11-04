package com.radicaldynamic.turboform.xform;

import java.util.ArrayList;


public class Translation
{
    private String lang;
    public ArrayList<TranslationText> texts = new ArrayList<TranslationText>();
    
    public Translation(String lang)
    {
        setLang(lang);
    }

    public void setLang(String lang)
    {
        this.lang = lang;
    }

    public String getLang()
    {
        return lang;
    }
}
