package com.radicaldynamic.groupinform.utilities;

import java.util.Comparator;

import com.radicaldynamic.groupinform.xform.Translation;

public class TranslationSortByDefault implements Comparator<Translation> 
{    
    public TranslationSortByDefault() { }
    
    @Override
    public int compare(Translation t1, Translation t2) 
    {           
        if (t1.isFallback()) {
            return -1;
        }
        
        if (t2.isFallback()) {
            return 1;
        }
        
        return 0;
    }

}
