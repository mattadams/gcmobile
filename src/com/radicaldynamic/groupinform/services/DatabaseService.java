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

    private String mHost = "127.0.0.1";
    private int mPort = 5985;
           
    private HttpClient mHttpClient = null;
    private StdCouchDbInstance mDbInstance = null;
    private StdCouchDbConnector mDb = null;
    
    private boolean mInit = false;
    private boolean mConnected = false;
    
    // This is the object that receives interactions from clients.  
    // See RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
  
    private ConditionVariable mCondition;   
    private NotificationManager mNM;
    
    private Runnable mTask = new Runnable() {
        public void run() {            
            for (int i = 1; i > 0; ++i) {
                if (mInit == false)
                    connect();
                // Retry connection to CouchDB every 120 seconds
                if (mCondition.block(120 * 1000))
                    break;
            }
        }
    };
      
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        Thread persistentConnectionThread = new Thread(null, mTask, "DatabaseService");        
        mCondition = new ConditionVariable(false);
        persistentConnectionThread.start();
    }
    
    @Override
    public void onDestroy() {   
        mNM.cancel(R.string.tf_connection_status_notification);
        mCondition.open();
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public DatabaseService getService() {
            return DatabaseService.this;
        }
    }    

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Open the default device database
     * 
     * @return TFCouchDbAdapter
     */
    public DatabaseService open() {
        String database = Collect.getInstance().getInformOnlineState().getDefaultDatabase();        
        return open(database);
    }
    
    /**
     * Open a specific database
     * 
     * @param database  Name of database to open
     * @return
     */
    public DatabaseService open(String database) {
        try {
            mDb = new StdCouchDbConnector("db_" + database, mDbInstance);
            mDb.createDatabaseIfNotExists();
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while opening DB db_" + database + ": " + e.toString());
        }    

        return this;
    }
    
    public List<String> getAllDatabases() {
        List<String> dbs = new ArrayList<String>();
        
        try {
            dbs = mDbInstance.getAllDatabases();
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while fetching databases: " + e.toString());            
        }
        
        return dbs;
    }
    
    public StdCouchDbConnector getDb() {
        // Last ditch attempt
        if (!mConnected)
            connect();
        
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
        
    private void connect() {
        mInit = true;        

        Log.d(Collect.LOGTAG, t + "establishing connection to " + mHost + ":" + mPort);
        
        try {                        
            mHttpClient = new StdHttpClient.Builder().host(mHost).port(mPort).build();     
            mDbInstance = new StdCouchDbInstance(mHttpClient);            
            
            mDbInstance.getAllDatabases();
            
            if (mConnected == false) {
                mConnected = true;
            }
            
            Log.d(Collect.LOGTAG, t + "connection to " + mHost + " successful");
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "while connecting to server " + mHost + ": " + e.toString());
            mConnected = false;                        
        } finally { 
            mInit = false;
        }
    }
}