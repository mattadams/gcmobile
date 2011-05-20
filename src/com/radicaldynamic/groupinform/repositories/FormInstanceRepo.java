package com.radicaldynamic.groupinform.repositories;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.groupinform.documents.FormInstanceDoc;

public class FormInstanceRepo extends CouchDbRepositorySupport<FormInstanceDoc>
{
    @SuppressWarnings("unused")
    private final static String t = "FormInstanceRepository: ";
    
    public FormInstanceRepo(CouchDbConnector db) 
    {
        super(FormInstanceDoc.class, db);
        initStandardDesignDocument();
    }
    
    public List<FormInstanceDoc> findByFormId(String formId) 
    {
        return queryView("by_formId", formId);
    }

    /*
     * Given a formId and an InstanceDocument status, return a list of 
     * instance IDs belonging to the form in question and having the 
     * desired status.
     */
    public ArrayList<String> findByFormAndStatus(String formId, FormInstanceDoc.Status status) 
    {
        List<FormInstanceDoc> instancesByForm = findByFormId(formId);
        ArrayList<String> instanceIds = new ArrayList<String>();
        String stat = status.toString();
        
        for(FormInstanceDoc doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(stat)) {            
                instanceIds.add(doc.getId());
            }
        }
        
        return instanceIds;
    }
}
