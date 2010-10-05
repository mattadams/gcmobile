package com.radicaldynamic.turboform.repository;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.GenerateView;
import org.ektorp.support.View;

import com.radicaldynamic.turboform.documents.InstanceDocument;

@View(name = "all", map = "function(doc) { if (doc.type == 'instance') emit (doc._id, doc._id) }")
public class InstanceRepository extends CouchDbRepositorySupport<InstanceDocument>
{
    public InstanceRepository(CouchDbConnector db) {
        super(InstanceDocument.class, db);
        initStandardDesignDocument();
    }
    
    @GenerateView
    public List<InstanceDocument> findByForm(String formId) {
        return queryView("by_form", formId);
    }
    
    @GenerateView
    public List<InstanceDocument> findByStatus(InstanceDocument.Status status) {
        return queryView("by_status", status.toString());
    }
    
    public List<InstanceDocument> findByFormAndStatus(String formId, InstanceDocument.Status status) {
        List<InstanceDocument> instancesByForm = findByForm(formId);
        List<InstanceDocument> instancesToReturn = new ArrayList<InstanceDocument>();
        String stat = status.toString();
        
        for(InstanceDocument doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(stat)) {            
                instancesToReturn.add(doc);
            }
        }
        
        return instancesToReturn;
    }
}
