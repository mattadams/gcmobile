package com.radicaldynamic.turboform.repository;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.GenerateView;

import com.radicaldynamic.turboform.documents.InstanceDocument;

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
}
