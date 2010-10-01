package com.radicaldynamic.turboform.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.radicaldynamic.turboform.documents.FormDocument;

public class DocumentUtils
{
    public static <T> void sortByName(ArrayList<T> documents) {        
        Collections.sort(documents, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2)
            {
                FormDocument f1 = (FormDocument) o1;
                FormDocument f2 = (FormDocument) o2;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });    
    }
}
