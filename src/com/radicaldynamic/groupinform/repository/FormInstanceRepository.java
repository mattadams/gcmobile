package com.radicaldynamic.groupinform.repository;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.groupinform.documents.FormInstanceDocument;

public class FormInstanceRepository extends CouchDbRepositorySupport<FormInstanceDocument>
{
    @SuppressWarnings("unused")
    private final static String t = "FormInstanceRepository: ";
    
    public FormInstanceRepository(CouchDbConnector db) {
        super(FormInstanceDocument.class, db);
        initStandardDesignDocument();
    }
    
    public List<FormInstanceDocument> findByFormId(String formId) {
        return queryView("by_formId", formId);
    }

    /*
     * Given a formId and an InstanceDocument status, return a list of 
     * instance IDs belonging to the form in question and having the 
     * desired status.
     */
    public ArrayList<String> findByFormAndStatus(String formId, FormInstanceDocument.Status status) {
        List<FormInstanceDocument> instancesByForm = findByFormId(formId);
        ArrayList<String> instanceIds = new ArrayList<String>();
        String stat = status.toString();
        
        for(FormInstanceDocument doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(stat)) {            
                instanceIds.add(doc.getId());
            }
        }
        
        return instanceIds;
    }
}
