package com.radicaldynamic.groupinform.repositories;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.groupinform.documents.FormInstance;

public class FormInstanceRepo extends CouchDbRepositorySupport<FormInstance>
{
    @SuppressWarnings("unused")
    private final static String t = "FormInstanceRepo: ";
    
    public FormInstanceRepo(CouchDbConnector db) 
    {
        super(FormInstance.class, db, "FormInstanceRepoR1");
        initStandardDesignDocument();
    }
    
    public List<FormInstance> findByFormId(String formId) 
    {
        return queryView("by_formId", formId);
    }

    /*
     * Given a formId and an InstanceDocument status, return a list of 
     * instance IDs belonging to the form in question and having the 
     * desired status.
     */
    public ArrayList<String> findByFormAndStatus(String formId, FormInstance.Status status) 
    {
        List<FormInstance> instancesByForm = findByFormId(formId);
        ArrayList<String> instanceIds = new ArrayList<String>();
        String stat = status.toString();
        
        for(FormInstance doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(stat)) {            
                instanceIds.add(doc.getId());
            }
        }
        
        return instanceIds;
    }
}
