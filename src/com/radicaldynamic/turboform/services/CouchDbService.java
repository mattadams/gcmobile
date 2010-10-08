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

package com.radicaldynamic.turboform.services;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.activities.MainBrowserActivity;
import com.radicaldynamic.turboform.application.Collect;

/**
 * Manages the files the application uses.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class CouchDbService extends Service {    
    private String mHost;
    private int mPort;
           
    private HttpClient mHttpClient = null;
    private StdCouchDbInstance mDbInstance = null;
    private StdCouchDbConnector mDb = null;
    
    private boolean mInit = false;
    private boolean mConnected = false;
    private boolean mFirstConnection = true;
    
    // This is the object that receives interactions from clients.  
    // See RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
  
    private ConditionVariable mCondition;   
    private NotificationManager mNM;
    
    private Runnable mTask = new Runnable() {
        public void run() {            
            for (int i = 1; i > 0; ++i) {
                if (mInit == false)
                    connect(true);
                // Retry every 120 seconds
                if (mCondition.block(120 * 1000))
                    break;
            }
        }
    };
      
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        Thread persistentConnectionThread = new Thread(null, mTask, "TFCouchDbService");        
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
        public CouchDbService getService() {
            return CouchDbService.this;
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
    public CouchDbService open() {
        TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);         
        String database = getString(R.string.tf_couchdb_prefix) + "device/" + mTelephonyMgr.getDeviceId();
        
        return open(database);
    }
    
    /**
     * Open a specific database
     * 
     * @param database  Name of database to open
     * @return
     */
    public CouchDbService open(String database) {
        connect(false);
        mDb = new StdCouchDbConnector(database, mDbInstance);        
        
        try {
            mDb.createDatabaseIfNotExists();
        } catch (Exception e) {          
            Log.e(Collect.LOGTAG, "While opening DB " + database + ": " + e.toString());
        }    

        return this;
    }
    
    public List<String> getAllDatabases() {
        List<String> dbs = new ArrayList<String>();
        
        try {
            dbs = mDbInstance.getAllDatabases();
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, "While fetching databases: " + e.toString());            
        }
        
        return dbs;
    }
    
    public StdCouchDbConnector getDb() {
        connect(false);
        
        for (int i = 1; i > 0; ++i) {
            if (mConnected == true) 
                break;              
            
            // This is to ensure that we do not run out of int space too soon
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return mDb;        
    }   
        
    private void connect(boolean persistent) {
        mInit = true;
        mHost = getString(R.string.tf_default_couchdb_server);
        mPort = 5984;
        
        if (persistent) {
            Log.d(Collect.LOGTAG, "Establishing persistent connection to " + mHost);
        } else {
            Log.d(Collect.LOGTAG, "Connecting to " + mHost);
        }            
        
        try {                        
            mHttpClient = new StdHttpClient.Builder().host(mHost).port(mPort).build();     
            mDbInstance = new StdCouchDbInstance(mHttpClient);            
            
            mDbInstance.getAllDatabases();
            
            if (mConnected == false) {
                mConnected = true;
                
                if (mFirstConnection == true) {
                    mFirstConnection = false;                    
                } else {
                    notifyOfConnectionAttempt(R.string.tf_connection_reestablished_status, R.string.tf_connection_reestablished_msg);    
                }                
            }
            
            Log.d(Collect.LOGTAG, "Connection to " + mHost + " successful");
        } catch (Exception e) {
            mConnected = false;
            Log.e(Collect.LOGTAG, "While connecting to server " + mHost + ": " + e.toString());          
            notifyOfConnectionAttempt(R.string.tf_connection_interrupted_status, R.string.tf_connection_interrupted_msg);                        
        } finally { 
            mInit = false;
        }
    } 
    
    private void notifyOfConnectionAttempt(int status, int message) {        
        Notification notification = new Notification(
                R.drawable.status_icon_check, 
                getText(status), 
                System.currentTimeMillis());
    
        notification.setLatestEventInfo(
                this, 
                getText(status), 
                getText(message), 
                PendingIntent.getActivity(this, 0, new Intent(this, MainBrowserActivity.class), 0));
    
        mNM.notify(R.string.tf_connection_status_notification, notification);
    }
}
