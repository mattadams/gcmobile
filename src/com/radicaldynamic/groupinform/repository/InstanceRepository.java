package com.radicaldynamic.groupinform.repository;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.groupinform.documents.InstanceDocument;

public class InstanceRepository extends CouchDbRepositorySupport<InstanceDocument>
{
    public InstanceRepository(CouchDbConnector db) {
        super(InstanceDocument.class, db);
        initStandardDesignDocument();
    }
    
    public List<InstanceDocument> findByFormId(String formId) {
        return queryView("by_formId", formId);
    }

    /*
     * Given a formId and an InstanceDocument status, return a list of 
     * instance IDs belonging to the form in question and having the 
     * desired status.
     */
    public ArrayList<String> findByFormAndStatus(String formId, InstanceDocument.Status status) {
        List<InstanceDocument> instancesByForm = findByFormId(formId);
        ArrayList<String> instanceIds = new ArrayList<String>();
        String stat = status.toString();
        
        for(InstanceDocument doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(stat)) {            
                instanceIds.add(doc.getId());
            }
        }
        
        return instanceIds;
    }
}
