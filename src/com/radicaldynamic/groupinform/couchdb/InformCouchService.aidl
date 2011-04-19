package com.radicaldynamic.groupinform.couchdb;

import com.radicaldynamic.groupinform.couchdb.InformCouchClient;

interface InformCouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void initCouchDB(InformCouchClient callback);
    
    /* The database may not be named as hinted here, this is to
     * prevent conflicts, cmdDb is not currently used
     */
    void initDatabase(InformCouchClient callback, String name, String pass, boolean cmdDb);

    /*
     * 
     */
    void quitCouchDB();
}
