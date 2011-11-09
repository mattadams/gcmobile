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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.util.Log;

import com.radicaldynamic.groupinform.activities.AccountDeviceList;
import com.radicaldynamic.groupinform.activities.AccountFolderList;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.InformOnlineSession;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.HttpUtils;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

/**
 * 
 */
public class InformOnlineService extends Service {    
    private static final String t = "InformOnlineService: ";
    
    // Whether or not we are currently attempting to connect to the service
    private boolean mConnecting = false;
    
    // Assume that we are not initialized when this service starts
    private boolean mInitialized = false;
    
    // Assume that the "online service" is not answering when we start
    private boolean mServicePingSuccessful = false;
    
    // Assume that we are not signed in when this service starts
    private boolean mSignedIn = false;
    
    // This is the object that receives interactions from clients.  
    // See RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
  
    private ConditionVariable mCondition;
    
    private Runnable mTask = new Runnable() {
        public void run() {            
            for (int i = 1; i > 0; ++i) {
                if (mConnecting == false)
                    connect(false);
                // Retry connection to Inform Online service every 10 minutes
                if (mCondition.block(600 * 1000))
                    break;
            }
        }
    };
      
    @Override
    public void onCreate() {        
        // Do some basic initialization for this service
        Collect.getInstance().setInformOnlineState(new InformOnlineState(getApplicationContext()));
        restoreSession();        
        
        // connect() may not be run because of offline mode but metadata should still be loaded if available
        if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled()) {
            AccountDeviceList.loadDeviceList();
            AccountFolderList.loadFolderList();
        }
        
        Thread persistentConnectionThread = new Thread(null, mTask, "InformOnlineService");        
        mCondition = new ConditionVariable(false);
        persistentConnectionThread.start();
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     * 
     * Perform cleanup here.  Note that we need to effectively reset the state of this service here
     * because there is no guarantee that the object will be destroyed before the app resumes (and
     * then assumes that the state being reported is accurate).
     */
    @Override
    public void onDestroy() {
        if (isInitialized()) {
            serializeSession();
            reinitializeService();
        }
        
        mCondition.open();
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public InformOnlineService getService() {
            return InformOnlineService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(Collect.LOGTAG, t + "received start ID " + startId + ": " + intent);
        return START_STICKY;
    }    
    
    // Triggered by UI button when the user wants to manually switch to OFFLINE mode
    public boolean goOffline()
    {
        if (checkout()) {
            Log.i(Collect.LOGTAG, t + "went offline at users request");            
            Collect.getInstance().getInformOnlineState().setOfflineModeEnabled(true);
            return true;
        } else {
            Log.w(Collect.LOGTAG, t + "unable to go offline at users request");
            return false;
        }
    }
    
    // Triggered by UI button when the user wants to force online mode
    public boolean goOnline()
    {
        // Force online (but only if a connection attempt is not already underway)
        if (!mConnecting)
            connect(true);
        
        if (isSignedIn()) {
            Log.i(Collect.LOGTAG, t + "went online at users request");
            Collect.getInstance().getInformOnlineState().setOfflineModeEnabled(false);
            return true;
        } else {
            Log.w(Collect.LOGTAG, t + "unable to go online at users request");
            return false;
        }
    }
    
    // connect() has been run at least once
    public boolean isInitialized()
    {
        return mInitialized;
    }
     
    /*
     * Application is ready for regular operation & user interaction.
     * 
     * This does not mean that we were able to ping the service or 
     * that we are signed in.
     */
    public boolean isReady()
    {
        return isInitialized() && isRegistered();
    }
    
    // Registration info for this device is stored in the app's shared preferences
    public boolean isRegistered()
    {
        return Collect.getInstance().getInformOnlineState().hasRegistration();
    }
    
    // Is Inform Online available?
    public boolean isRespondingToPings()
    {
        return mServicePingSuccessful;
    }
    
    // Registered (implied) connected & signed in
    public boolean isSignedIn()
    {
        return mSignedIn;
    }    
    
    // Bring the service back to the defaults it would have had when originally started
    public void reinitializeService()
    {
        Log.d(Collect.LOGTAG, t + "service reinitialized");
        
        mInitialized
        = mServicePingSuccessful
        = mSignedIn
        = false;
    }

    /*
     * Sign in to the Inform Online service
     * Only called by connect()
     */
    private boolean checkin()
    {
        // Assume we are registered unless told otherwise
        boolean registered = true;
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("deviceId", Collect.getInstance().getInformOnlineState().getDeviceId()));
        params.add(new BasicNameValuePair("deviceKey", Collect.getInstance().getInformOnlineState().getDeviceKey()));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnlineState().getDeviceFingerprint()));
        
        try {
            params.add(new BasicNameValuePair("lastCheckinWith", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName));
        } catch (NameNotFoundException e1) {
            params.add(new BasicNameValuePair("lastCheckinWith", "unknown"));
        }
        
        String checkinUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/checkin";
        String postResult = HttpUtils.postUrlData(checkinUrl, params);            
        JSONObject checkin;
        
        try {
            checkin = (JSONObject) new JSONTokener(postResult).nextValue();
            String result = checkin.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                Log.i(Collect.LOGTAG, t + "successful checkin");
                Collect.getInstance().getInformOnlineState().setExpired(false);
            } else if (result.equals(InformOnlineState.EXPIRED)) {
                Log.i(Collect.LOGTAG, t + "associated order is expired; marking device as expired");
                Collect.getInstance().getInformOnlineState().setExpired(true);
            } else if (result.equals(InformOnlineState.FAILURE)) {
                Log.w(Collect.LOGTAG, t + "checkin unsuccessful");
                registered = false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing postResult");
            }                
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");
            e.printStackTrace();            
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);
            e.printStackTrace();
        }
        
        Log.d(Collect.LOGTAG, t + "device registration state is " + registered);

        // Clear the session for subsequent requests and reset stored state
        if (registered == false)          
            Collect.getInstance().getInformOnlineState().resetDevice();
        
        return registered;
    }
    
    /*
     * Try and say "goodbye" to Inform Online so that we know that this client's session is no longer needed.
     * 
     * Checkouts are the result of a manual process and as such they will knock us
     * offline until the user manually puts us back into the online state.
     */
    private boolean checkout()
    {
        boolean saidGoodbye = false;
        
        String checkoutUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/checkout";
        String getResult = HttpUtils.getUrlData(checkoutUrl);
        JSONObject checkout;
        
        try {
            checkout = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = checkout.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
            
            if (result.equals(InformOnlineState.OK)) {
                Log.i(Collect.LOGTAG, t + "said goodbye to Inform Online");
            } else { 
                Log.i(Collect.LOGTAG, t + "device checkout unnecessary");
            }
            
            saidGoodbye = true;
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        } finally {
            // Running a checkout ALWAYS "signs us out"
            mSignedIn = false;
        }
        
        Collect.getInstance().getInformOnlineState().setSession(null);
        
        return saidGoodbye;
    }
    
    /*
     * Connect to the Inform Online service and if registered, attempt to sign in
     */
    private void connect(boolean forceOnline)
    {
        mConnecting = true;
        
        // Make sure that the user has not specifically requested that we be offline
        if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled() && forceOnline == false) {
            Log.i(Collect.LOGTAG, t + "offline mode enabled; not auto-connecting");
            
            /* 
             * This is not a complete initialization (in the sense that we attempted connection) but we need 
             * to pretend that it is so that the UI can move forward to whatever state is most suitable.
             */
            mInitialized = true;
            mConnecting = false;
            
            return;
        }
        
        Log.d(Collect.LOGTAG, t + "pinging " + Collect.getInstance().getInformOnlineState().getServerUrl());
        
        // Try to ping the service to see if it is "up" (and determine whether we are registered)
        String pingUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/ping";
        String getResult = HttpUtils.getUrlData(pingUrl);
        JSONObject ping;
        
        try {
            ping = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = ping.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);

            // Online and registered (checked in)
            if (result.equals(InformOnlineState.OK)) {
                Log.i(Collect.LOGTAG, t + "ping successful (we are connected and checked in)");
                mServicePingSuccessful = mSignedIn = true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                Log.w(Collect.LOGTAG, t + "ping successful but not signed in (will attempt checkin)");
                mServicePingSuccessful = true;
                
                if (Collect.getInstance().getInformOnlineState().hasRegistration() && checkin()) {
                    Log.i(Collect.LOGTAG, t + "checkin successful (we are connected)");
                    mSignedIn = true;
                } else {
                    Log.w(Collect.LOGTAG, t + "checkin failed (registration invalid)");
                    mSignedIn = false;
                }
            } else {
                // Assume offline
                Log.w(Collect.LOGTAG, t + "ping failed (we are offline)");
                mSignedIn = false;
            }
        } catch (NullPointerException e) {
            // This usually indicates a communication error and will send us into an offline state
            Log.e(Collect.LOGTAG, t + "ping error while communicating with service (we are offline)");
            e.printStackTrace();
            mServicePingSuccessful = mSignedIn = false;            
        } catch (JSONException e) {
            // Parse errors (malformed result) send us into an offline state
            Log.e(Collect.LOGTAG, t + "ping error while parsing getResult " + getResult + " (we are offline)");
            e.printStackTrace();
            mServicePingSuccessful = mSignedIn = false;
        } finally {
            if (mSignedIn) {
                AccountDeviceList.fetchDeviceList();                
                AccountFolderList.fetchFolderList();                
            }   
            
            // Load regardless of whether we are signed in
            AccountDeviceList.loadDeviceList();
            AccountFolderList.loadFolderList();
            
            // Unblock
            mInitialized = true;
            mConnecting = false;            
        }
    }

    /*
     * Restore a serialized session from disk
     */
    private void restoreSession()
    {        
        // Restore any serialized session information
        File sessionCache = new File(getCacheDir(), FileUtilsExtended.SESSION_CACHE_FILE);
        
        if (sessionCache.exists()) {
            Log.d(Collect.LOGTAG, t + "restoring cached session");
            
            try {
                InformOnlineSession session = new InformOnlineSession();
                
                FileInputStream fis = new FileInputStream(sessionCache);
                ObjectInputStream ois = new ObjectInputStream(fis);
                session = (InformOnlineSession) ois.readObject();
                ois.close();
                fis.close();
                
                Collect.getInstance().getInformOnlineState().setSession(new BasicCookieStore());
                Iterator<InformOnlineSession> cookies = session.getCookies().iterator();
                
                if (cookies.hasNext()) {
                    InformOnlineSession ios = cookies.next();
                    
                    BasicClientCookie bcc = new BasicClientCookie(ios.getName(), ios.getValue());
                    bcc.setDomain(ios.getDomain());
                    bcc.setExpiryDate(ios.getExpiryDate());
                    bcc.setPath(ios.getPath());
                    bcc.setVersion(ios.getVersion());
                 
                    Collect.getInstance().getInformOnlineState().getSession().addCookie(bcc);
                }
            } catch (Exception e) {
                Log.w(Collect.LOGTAG, t + "problem restoring cached session " + e.toString());
                e.printStackTrace();
                
                // Don't leave a broken file hanging
                new File(getCacheDir(), FileUtilsExtended.SESSION_CACHE_FILE).delete();
                
                // Clear the session
                Collect.getInstance().getInformOnlineState().setSession(null);
            }
        } else {
            Log.d(Collect.LOGTAG, t + "no session to restore");
        }
    }
    
    /*
     * Serialize the current session to disk
     */
    private void serializeSession()
    {
        // Attempt to serialize the session for later use
        if (Collect.getInstance().getInformOnlineState().getSession() instanceof CookieStore) {
            Log.d(Collect.LOGTAG, t + "serializing session");
            
            try {
                InformOnlineSession session = new InformOnlineSession();
                
                Iterator<Cookie> cookies = Collect.getInstance().getInformOnlineState().getSession().getCookies().iterator();
                
                while (cookies.hasNext()) {
                    Cookie c = cookies.next();                    
                    session.getCookies().add(new InformOnlineSession(
                            c.getDomain(),
                            c.getExpiryDate(),
                            c.getName(),
                            c.getPath(),
                            c.getValue(),
                            c.getVersion()
                    ));
                }
                
                FileOutputStream fos = new FileOutputStream(new File(getCacheDir(), FileUtilsExtended.SESSION_CACHE_FILE));
                ObjectOutputStream out = new ObjectOutputStream(fos);
                out.writeObject(session);
                out.close();
                fos.close();
            } catch (Exception e) {
                Log.w(Collect.LOGTAG, t + "problem serializing session " + e.toString());
                e.printStackTrace();
                
                // Make sure that we don't leave a broken file hanging
                new File(getCacheDir(), FileUtilsExtended.SESSION_CACHE_FILE).delete();
            }
        } else {
            Log.d(Collect.LOGTAG, t + "no session to serialize");
        }
    }
}
