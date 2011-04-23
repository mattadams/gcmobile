package com.radicaldynamic.groupinform.utilities;

import java.util.ArrayList;
import java.util.Comparator;

import com.radicaldynamic.groupinform.xform.Translation;

public class TranslationSortByLang implements Comparator<Translation> 
{
    private String [] languages;
    private ArrayList<String> abbreviations = new ArrayList<String>();    
    
    public TranslationSortByLang(String [] languages, ArrayList<String> abbreviations)
    {
        this.languages = languages;
        this.abbreviations = abbreviations;        
    }
    
    @Override
    public int compare(Translation t1, Translation t2) 
    {   
        String l1 = t1.getLang();
        String l2 = t2.getLang();
        
        if (abbreviations.contains(l1.toLowerCase())) {
            int i = abbreviations.indexOf(l1.toLowerCase());
            l1 = languages[i];
        }
        
        if (abbreviations.contains(l2.toLowerCase())) {
            int i = abbreviations.indexOf(l2.toLowerCase());
            l2 = languages[i];
        }

        return l1.compareToIgnoreCase(l2);
    }

}
