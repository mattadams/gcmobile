/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.groupinform.services;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;

/**
 * Database abstraction layer for CouchDB, based on Ektorp.
 * 
 * This does not control the stop/start of the actual DB.
 * See com.couchone.couchdb.CouchService for that.
 */
public class DatabaseService extends Service {
    private static final String t = "DatabaseService: ";
           
    private HttpClient mHttpClient = null;
    private StdCouchDbInstance mDbInstance = null;
    private StdCouchDbConnector mDb = null;
    
    private boolean mInit = false;
    private boolean mConnected = false;
    private boolean mConnectedToLocal = false;
    
    // This is the object that receives interactions from clients.  
    // See RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
  
    private ConditionVariable mCondition;   
    private NotificationManager mNM;
    
    private Runnable mTask = new Runnable() 
    {
        public void run() {            
            for (int i = 1; i > 0; ++i) {
                if (mInit == false)
                    try {
                        connect(Collect
                                .getInstance()
                                .getInformOnlineState()
                                .getAccountFolders()
                                .get(Collect.getInstance().getInformOnlineState().getSelectedDatabase())
                                .isReplicated());
                    } catch (Exception e) {
                        Log.w(Collect.LOGTAG, t + "error automatically connecting to DB: " + e.toString()); 
                        mInit = false;
                    }
                
                // Retry connection to CouchDB every 5 minutes
                if (mCondition.block(300 * 1000))
                    break;
            }
        }
    };
    
    @SuppressWarnings("serial")
    public class DbUnavailableException extends Exception
    {
        DbUnavailableException()
        {
            super();
        }
    }
    
    @SuppressWarnings("serial")
    public class DbUnavailableWhileOfflineException extends DbUnavailableException
    {
        DbUnavailableWhileOfflineException()
        {
            super();
        }
    }
    
    @SuppressWarnings("serial")
    public class DbUnavailableDueToMetadataException extends DbUnavailableException
    {
        DbUnavailableDueToMetadataException(String db)
        {
            super();
            Log.w(Collect.LOGTAG, t + "metadata missing for DB " + db);
        }
    }
      
    @Override
    public void onCreate() 
    {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        Thread persistentConnectionThread = new Thread(null, mTask, "DatabaseService");        
        mCondition = new ConditionVariable(false);
        persistentConnectionThread.start();
    }
    
    @Override
    public void onDestroy() 
    {   
        mNM.cancel(R.string.tf_connection_status_notification);
        mCondition.open();
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
        return mBinder;
    }
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder 
    {
        public DatabaseService getService() 
        {
            return DatabaseService.this;
        }
    }

    public List<String> getAllDatabases() 
    {
        List<String> dbs = new ArrayList<String>();
        
        try {
            dbs = mDbInstance.getAllDatabases();
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while fetching databases: " + e.toString());            
        }
        
        return dbs;
    }
    
    public StdCouchDbConnector getDb() 
    {
        // Last ditch attempt
        if (!mConnected) {
            try {
                open(Collect.getInstance().getInformOnlineState().getSelectedDatabase());
            } catch (DbUnavailableException e) {
                Log.w(Collect.LOGTAG, t + "unable to connect to database server for getDb(): " + e.toString());
                e.printStackTrace();
            }
        }
        
        // Wait for a connection to a database server
        for (int i = 1; i > 0; ++i) {
            if (mConnected == true) 
                break;              
            
            // This is to ensure that we do not run out of int space too soon
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        return mDb;        
    }   
        
    /**
     * Open a specific database
     * 
     * @param database  Name of database to open
     * @return
     * @throws DbMetadataUnavailableException 
     * @throws DbUnavailableException 
     */
    public void open(String db) throws DbUnavailableException 
    {
        mInit = true;
        
        // If database metadata is not yet available then abort here
        if (db == null || Collect.getInstance().getInformOnlineState().getAccountFolders().get(db) == null) {
            throw new DbUnavailableDueToMetadataException(db);
        }
        
        if (!Collect.getInstance().getIoService().isSignedIn() && 
                !Collect.getInstance().getInformOnlineState().getAccountFolders().get(db).isReplicated())
            throw new DbUnavailableWhileOfflineException();
        
        boolean dbToOpenIsReplicated = Collect.getInstance().getInformOnlineState().getAccountFolders().get(db).isReplicated();        
        
        if (mConnected) {
            // Return quickly if the database is already opened and we are connected to the right host
            if (mDb instanceof StdCouchDbConnector && mDb.getDatabaseName().equals("db_" + db)) {
                if ((dbToOpenIsReplicated && mConnectedToLocal) || (!dbToOpenIsReplicated && !mConnectedToLocal)) {
                    Log.d(Collect.LOGTAG, t + "database " + db + " already opened");
                    mInit = false;
                    return;                
                }
            }            

            // Switch connections from local to remote and vice versa as required
            if (dbToOpenIsReplicated) {
                if (!mConnectedToLocal) {
                    Log.i(Collect.LOGTAG, t + "switching from remote to local database");
                    connect(true);
                }                
            } else {
                if (mConnectedToLocal) {
                    Log.i(Collect.LOGTAG, t + "switching from local to remote database");
                    connect(false);
                }
            }
        } else {
            // Last ditch attempt to connect
            connect(dbToOpenIsReplicated);
            
            // Wait for a connection to become available
            for (int i = 1; i > 0; ++i) {
                if (mConnected == true) 
                    break;              
                
                // Ensure that we do not run out of int space too soon
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            mDb = new StdCouchDbConnector("db_" + db, mDbInstance);
            
            // Only attempt to create the database if it is marked as being locally replicated
            if (mConnectedToLocal && dbToOpenIsReplicated)
                mDb.createDatabaseIfNotExists();
            
            Collect.getInstance().getInformOnlineState().setSelectedDatabase(db);
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while opening DB db_" + db + ": " + e.toString());
            throw new DbUnavailableException();
        } finally {
            mInit = false;
        }
    }

    synchronized private void connect(boolean local) throws DbUnavailableException 
    {        
        String host;
        int port;
        
        if (local) {
            host = "127.0.0.1";
            port = 5985;
        } else {
            host = "arthur.902northland.adams.home";
            port = 5984;
        }
        
        Log.d(Collect.LOGTAG, t + "establishing connection to " + host + ":" + port);
        
        try {                        
            mHttpClient = new StdHttpClient.Builder().host(host).port(port).build();     
            mDbInstance = new StdCouchDbInstance(mHttpClient);            
            
            mDbInstance.getAllDatabases();
            
            if (mConnected == false)
                mConnected = true;
            
            if (local)
                mConnectedToLocal = true;
            else
                mConnectedToLocal = false;                
            
            Log.d(Collect.LOGTAG, t + "connection to " + host + " successful");
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while connecting to server " + port + ": " + e.toString());            
            mConnected = false;            
            throw new DbUnavailableException();
        }
    }
}