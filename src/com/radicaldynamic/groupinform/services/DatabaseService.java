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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
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
import com.radicaldynamic.groupinform.logic.AccountFolder;

/**
 * Database abstraction layer for CouchDB, based on Ektorp.
 * 
 * This does not control the stop/start of the actual DB.
 * See com.couchone.couchdb.CouchService for that.
 */
public class DatabaseService extends Service {
    private static final String t = "DatabaseService: ";

    // Replication modes
    public static final int REPLICATE_PUSH = 0;
    public static final int REPLICATE_PULL = 1;    
    
    private HttpClient mHttpClient = null;
    private CouchDbInstance mDbInstance = null;
    private CouchDbConnector mDbConnector = null;
    
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
                if (mInit == false) {
                    try {
                        mInit = true;
                        
                        connect(Collect
                                .getInstance()
                                .getInformOnlineState()
                                .getAccountFolders()
                                .get(Collect.getInstance().getInformOnlineState().getSelectedDatabase())
                                .isReplicated());
                        
                        replicateAll();
                    } catch (Exception e) {
                        Log.w(Collect.LOGTAG, t + "error automatically connecting to DB: " + e.toString()); 
                    } finally {
                        mInit = false;
                    }                    
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
    
    public CouchDbConnector getDb() 
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
        
        return mDbConnector;        
    }
    
    public boolean initLocalDb(String db)
    {
        try {
            ReplicationStatus status = replicate(db, REPLICATE_PULL);

            if (status == null)
                return false;
            else 
                return status.isOk();
        } catch (Exception e) {
            return false;
        }
    }
    
    /*
     * Does a database exist on the local CouchDB instance?
     */
    public boolean isDbLocal(String db)
    {
        HttpClient httpClient = new StdHttpClient.Builder().host("127.0.0.1").port(5985).build();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        
        if (dbInstance.getAllDatabases().indexOf("db_" + db) == -1)
            return false;
        else
            return true;
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
            if (mDbConnector instanceof StdCouchDbConnector && mDbConnector.getDatabaseName().equals("db_" + db)) {
                if ((dbToOpenIsReplicated && mConnectedToLocal) || (!dbToOpenIsReplicated && !mConnectedToLocal)) {
                    Log.d(Collect.LOGTAG, t + "database " + db + " already opened");
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
            mDbConnector = new StdCouchDbConnector("db_" + db, mDbInstance);
            
            // Only attempt to create the database if it is marked as being locally replicated
            if (mConnectedToLocal && dbToOpenIsReplicated)
                mDbConnector.createDatabaseIfNotExists();
            
            Collect.getInstance().getInformOnlineState().setSelectedDatabase(db);
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while opening DB db_" + db + ": " + e.toString());
            throw new DbUnavailableException();
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
            host = getString(R.string.tf_default_ionline_server);
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
    
    synchronized public ReplicationStatus replicate(String db, int mode)
    {
        // Will not replicate while offline
        if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled()) {
            Log.d(Collect.LOGTAG, t + "aborting replication of " + db + " (offline mode is enabled)");
            return null;
        }
        
        // Will not replicate unless signed in
        if (!Collect.getInstance().getIoService().isSignedIn()) {
            Log.w(Collect.LOGTAG, t + "aborting replication of " + db + " (not signed in)");
            return null;
        }        
        
        /* 
         * Lookup master cluster by IP.  Do this instead of relying on Erlang's internal resolver 
         * (and thus Google's public DNS).  Our builds of Erlang for Android do not yet use 
         * Android's native DNS resolver. 
         */
        String masterClusterIP = null;        
        
        try {
            InetAddress [] clusterInetAddresses = InetAddress.getAllByName(getString(R.string.tf_default_ionline_server));
            masterClusterIP = clusterInetAddresses[new Random().nextInt(clusterInetAddresses.length)].getHostAddress();
        } catch (UnknownHostException e) {
            Log.e(Collect.LOGTAG, t + "unable to lookup master cluster IP addresses: " + e.toString());
            e.printStackTrace();
        }
        
        // Create local instance of database
        boolean dbCreated = false;
        
        HttpClient httpClient = new StdHttpClient.Builder().host("127.0.0.1").port(5985).build();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        
        if (dbInstance.getAllDatabases().indexOf("db_" + db) == -1) {
            dbInstance.createDatabase("db_" + db);
            dbCreated = true;
        }
        
        // Configure replication direction
        String source = null;
        String target = null;

        switch (mode) {
        case REPLICATE_PUSH:
            source = "http://127.0.0.1:5985/db_" + db; 
            target = "http://" + masterClusterIP + ":5984/db_" + db;
            Log.d(Collect.LOGTAG, t + "about to replicate from " + source + " to " + target);
            break;

        case REPLICATE_PULL:
            source = "http://" + masterClusterIP + ":5984/db_" + db;
            target = "http://127.0.0.1:5985/db_" + db;
            Log.d(Collect.LOGTAG, t + "about to replicate from " + source + " to " + target);
            break;
        }
        
        ReplicationCommand cmd = new ReplicationCommand.Builder().source(source).target(target).build();
        ReplicationStatus status = null;
        
        try {            
            status = dbInstance.replicate(cmd);
        } catch (Exception e) {
            // Remove a recently created DB if the replication failed
            if (dbCreated) {
                dbInstance.deleteDatabase("db_" + db);
            }
        }
        
        return status;
    }
    
    private void replicateAll()
    {
        Set<String> folderSet = Collect.getInstance().getInformOnlineState().getAccountFolders().keySet();
        Iterator<String> folderIds = folderSet.iterator();
        
        while (folderIds.hasNext()) {
            AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(folderIds.next());            
            
            if (folder.isReplicated()) {
                Log.i(Collect.LOGTAG, t + "about to begin scheduled replication of " + folder.getName());
                
                try {
                    replicate(folder.getId(), REPLICATE_PUSH);
                    replicate(folder.getId(), REPLICATE_PULL);
                } catch (Exception e) {
                    Log.w(Collect.LOGTAG, t + "problem replicating " + folder.getId() + ": " + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
}