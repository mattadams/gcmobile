package com.radicaldynamic.groupinform.database;

import org.ektorp.CouchDbInstance;
import org.ektorp.impl.StdCouchDbConnector;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.Generic;

/*
 * Custom CouchDB connector extended to manage metadata upon create, delete and update.
 * Additional attributes that are not common to most document classes should be managed
 * through class-specific Ektorp repositories. 
 */
public class InformCouchDbConnector extends StdCouchDbConnector 
{
    public InformCouchDbConnector(String databaseName, CouchDbInstance dbInstance) 
    {
        super(databaseName, dbInstance);
    }
    
    @Override
    public void create(Object o) {
        try {
            ((Generic) o).setCreatedBy(Collect.getInstance().getInformOnlineState().getDeviceId());
            ((Generic) o).setDateCreated(Generic.generateTimestamp());            
        } catch (Exception e) {
            // Is it worth logging this?
        }
        super.create(o);
    }
    
    @Override
    public String delete(Object o)
    {
        try {
            ((Generic) o).setUpdatedBy(Collect.getInstance().getInformOnlineState().getDeviceId());
            ((Generic) o).setDateUpdated(Generic.generateTimestamp());            
        } catch (Exception e) {
            // Is it worth logging this?
        }
        return super.delete(o);
    }

    @Override
    public void update(Object o)
    {
        try {
            ((Generic) o).setUpdatedBy(Collect.getInstance().getInformOnlineState().getDeviceId());
            ((Generic) o).setDateUpdated(Generic.generateTimestamp());            
        } catch (Exception e) {
            // Is it worth logging this?
        }
       
        super.update(o);
    }
}
