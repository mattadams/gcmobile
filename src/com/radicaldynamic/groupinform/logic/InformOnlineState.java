package com.radicaldynamic.groupinform.logic;

import org.apache.http.client.CookieStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * Stores the state of this device as registered with Inform Online
 */
public class InformOnlineState
{
    private static final String t = "InformOnlineState: ";
    
    // Constants for strings commonly encountered when interacting with the Inform Online service
    public static final String OK = "ok";
    public static final String FAILURE = "failure";
    public static final String RESULT = "result";
    public static final String REASON = "reason";
    
    // Constants for account information stored in preferences
    public static final String ACCOUNT_ID  = "informonline_accountid";      // Invisible to users
    public static final String ACCOUNT_KEY = "informonline_accountkey";     // Accessible to users
    public static final String ACCOUNT_NUM = "informonline_accountnum";     // Accessible
    
    // Constants for device information stored in preferences
    public static final String DEVICE_ID   = "informonline_deviceid";       // Invisible 
    public static final String DEVICE_KEY  = "informonline_devicekey";      // Invisible 
    public static final String DEVICE_PIN  = "informonline_devicepin";      // Accessible
    
    private String serverUrl;
    private boolean serverAlive = false;
    
    private String accountId;
    private String accountNumber;           // The licence number
    private String accountKey;              // The licence key    
    private String deviceId;
    private String deviceKey;
    private String devicePin;
    
    private CookieStore session = null;
    private SharedPreferences prefs;
       
    public InformOnlineState()
    {
        
    }
    
    public InformOnlineState(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadPreferences();
        
        // Initialize server URL
        setServerUrl("http://" + context.getText(R.string.tf_default_nodejs_server) 
                + ":" + context.getText(R.string.tf_default_nodejs_port));
    }
    
    /*
     * TODO: error messages and error handling needs to be suitable 
     */
    public boolean isRegistered()
    {
        boolean registered = false;
        
        if (accountId == null) {
            registered = false;
            
            // Try to ping the service to see if it is "up"
            String pingUrl = serverUrl + "/ping";
            String jsonResult = HttpUtils.getUrlData(pingUrl);
            JSONObject ping;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);                
                ping = (JSONObject) new JSONTokener(jsonResult).nextValue();
                
                String result = ping.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
                
                if (result.equals(InformOnlineState.OK))
                    serverAlive = true;                    
                else
                    serverAlive = false;
            } catch (NullPointerException e) {
                /* 
                 * Null pointers occur to jsonResult when HttpUtils.getUrlData() fails
                 * either as a result of a communication error with the node.js server
                 * or something else.
                 */
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");
                e.printStackTrace();
                serverAlive = false;
            } catch (JSONException e) {
                // Parse errors (malformed result) but assume we are still registered
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);
                e.printStackTrace();                
                registered = true;
                serverAlive = false;
            }
        } else {
            String checkinUrl = serverUrl + "/checkin/" + deviceId + "/" + deviceKey;
            String jsonResult = HttpUtils.getUrlData(checkinUrl);            
            JSONObject checkin;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);                
                checkin = (JSONObject) new JSONTokener(jsonResult).nextValue();
                
                String result = checkin.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
                
                if (result.equals(InformOnlineState.OK)) {
                    Log.i(Collect.LOGTAG, t + "successful checkin");
                    registered = true;
                    serverAlive = true;
                } else if (result.equals(InformOnlineState.FAILURE)) {
                    Log.w(Collect.LOGTAG, t + "checkin unsuccessful");
                    registered = false;
                    serverAlive = true;
                } else {
                    // Something bad happened (but assume we are still registered)
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                    registered = true;
                    serverAlive = false;
                }                
            } catch (NullPointerException e) {
                /* 
                 * Null pointers occur to jsonResult when HttpUtils.getUrlData() fails
                 * either as a result of a communication error with the node.js server
                 * or something else.
                 * 
                 * Assume we are still registered
                 */
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");
                e.printStackTrace();
                registered = true;
                serverAlive = false;
            } catch (JSONException e) {
                // Parse errors (malformed result) but assume we are still registered
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);
                e.printStackTrace();                
                registered = true;
                serverAlive = false;
            }
        }
        
        Log.i(Collect.LOGTAG, t + "device registration state is " + registered + " according to server-alive state " + serverAlive);
        
        if (registered == false) {
            // Clear the session for subsequent requests
            session = null;
            
            // If this device is not registered ACCORDING TO AN ACTIVE SERVICE...
            if (serverAlive) {
                // Purge the account and device registration info from the device
                resetPreferences();
                
                // TODO: tell the user that this device is no longer registered ... explain the possible causes
            }
        }
        
        return registered;        
    }
    
    public boolean isServerAlive()
    {
        return serverAlive;
    }

    public void setAccountId(String accountId)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ACCOUNT_ID, accountId);
        editor.commit();
        
        this.accountId = accountId;
    }
    
    public String getAccountId()
    {
        return accountId;
    }
    
    public void setAccountKey(String accountKey)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ACCOUNT_KEY, accountKey);
        editor.commit();
                
        this.accountKey = accountKey;
    }
    
    public String getAccountKey()
    {
        return accountKey;
    }
    
    public void setAccountNumber(String accountNumber)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ACCOUNT_NUM, accountNumber);
        editor.commit();                
        
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setDeviceId(String deviceId)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_ID, deviceId);
        editor.commit(); 
        
        this.deviceId = deviceId;
    }

    public String getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceKey(String deviceKey)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_KEY, deviceKey);
        editor.commit();
        
        this.deviceKey = deviceKey;
    }

    public String getDeviceKey()
    {
        return deviceKey;
    }

    public void setDevicePin(String devicePin)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_PIN, devicePin);
        editor.commit(); 
        
        this.devicePin = devicePin;
    }

    public String getDevicePin()
    {
        return devicePin;
    }
    
    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setSession(CookieStore session)
    {
        this.session = session;
    }

    public CookieStore getSession()
    {
        return session;
    }

    private void loadPreferences()
    {
        setAccountId(prefs.getString(ACCOUNT_ID, null));
        setAccountKey(prefs.getString(ACCOUNT_KEY, null));
        setAccountNumber(prefs.getString(ACCOUNT_NUM, null));
        
        setDeviceId(prefs.getString(DEVICE_ID, null));
        setDeviceKey(prefs.getString(DEVICE_KEY, null));
        setDevicePin(prefs.getString(DEVICE_PIN, null));
    }
    
    public void resetPreferences()
    {
        setAccountId(null);
        setAccountKey(null);
        setAccountNumber(null);
        
        setDeviceId(null);
        setDeviceKey(null);
        setDevicePin(null);
    }
}
