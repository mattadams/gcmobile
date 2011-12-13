package com.radicaldynamic.groupinform.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.Generic;

public class DocumentUtils
{

    // Sort a list of form definition documents alphabetically by name
    public static <T> void sortDefinitionsByName(ArrayList<T> documents) 
    {        
        Collections.sort(documents, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2)
            {
                FormDefinition f1 = (FormDefinition) o1;
                FormDefinition f2 = (FormDefinition) o2;
                
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });    
    }
    
    // Sort a list of documents by dateCreated
    public static <T> void sortByDateCreated(ArrayList<T> documents) 
    {
        Collections.sort(documents, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2)
            {
                Generic d1 = (Generic) o1;
                Generic d2 = (Generic) o2;
                
                return d1.getDateCreatedAsCalendar().compareTo(d2.getDateCreatedAsCalendar());
            }
        }); 
    }
    
    // Sort a list of documents by dateUpdated
    public static <T> void sortByDateUpdated(ArrayList<T> documents) 
    {
        Collections.sort(documents, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2)
            {
                Generic d1 = (Generic) o1;
                Generic d2 = (Generic) o2;
                
                return d1.getDateUpdatedAsCalendar().compareTo(d2.getDateUpdatedAsCalendar());
            }
        }); 
    }
}
