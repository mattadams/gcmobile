package com.radicaldynamic.turboform.repository;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.turboform.documents.FormDocument;

public class FormRepository extends CouchDbRepositorySupport<FormDocument>
{

    public FormRepository(CouchDbConnector db) {
        super(FormDocument.class, db);
    }

}
