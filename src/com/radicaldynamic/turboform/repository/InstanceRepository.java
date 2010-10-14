package com.radicaldynamic.turboform.repository;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.GenerateView;
import org.ektorp.support.View;

import com.radicaldynamic.turboform.documents.InstanceDocument;

@View(name = "all", map = "function(doc) { if (doc.type && doc.type == 'instance') emit (doc._id, doc._id) }")
public class InstanceRepository extends CouchDbRepositorySupport<InstanceDocument>
{
    public InstanceRepository(CouchDbConnector db) {
        super(InstanceDocument.class, db);
        initStandardDesignDocument();
    }
    
    @GenerateView
    public List<InstanceDocument> findByFormId(String formId) {
        return queryView("by_formId", formId);
    }
    
    @GenerateView
    public List<InstanceDocument> findByStatus(InstanceDocument.Status status) {
        return queryView("by_status", status.toString());
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
