package com.radicaldynamic.turboform.repository;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

import com.radicaldynamic.turboform.documents.RecordDocument;

public class RecordRepository extends CouchDbRepositorySupport<RecordDocument>
{

    public RecordRepository(CouchDbConnector db) {
        super(RecordDocument.class, db);
    }

}
