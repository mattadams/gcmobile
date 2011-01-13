package com.radicaldynamic.groupinform.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * Stores the state of this device as registered with Inform Online
 */
public class InformOnlineState
{
    private static final String t = "InformOnlineState: ";
    
    // Constants for strings commonly encountered when interacting with the Inform Online service
    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String FAILURE = "failure";
    public static final String RESULT = "result";
    public static final String REASON = "reason";
   
    // Constants for account information stored in preferences
    public static final String ACCOUNT_KEY = "informonline_accountkey";     // Accessible
    public static final String ACCOUNT_NUM = "informonline_accountnum";     // Accessible
    private static final String ACCOUNT_OWNER = "informonline_accountown";  // Invisible
    
    // Constants for device information stored in preferences
    public static final String DEVICE_ID   = "informonline_deviceid";       // Invisible 
    public static final String DEVICE_KEY  = "informonline_devicekey";      // Invisible 
    public static final String DEVICE_PIN  = "informonline_devicepin";      // Accessible
    public static final String DEVICE_EMAIL = "informonline_deviceemail";   // Accessible
    
    // Constants for session information stored in preferences
    public static final String SESSION     = "informonline_session";        // Invisible
    
    private String serverUrl;
    
    private String accountNumber;           // The licence number
    private String accountKey;              // The licence key
    private boolean accountOwner;
    private String deviceId;
    private String deviceKey;
    private String devicePin;
    
    private String deviceFingerprint;
    
    private CookieStore session = null;
    private SharedPreferences prefs;
    
    private boolean ready = false;
       
    // Used by Collect
    public InformOnlineState()
    {        
    }
    
    // Used by MainBrowserActivity.onCreate()
    public InformOnlineState(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadPreferences();
        
        // Initialize server URL
        setServerUrl("http://" + context.getText(R.string.tf_default_nodejs_server) + ":" + context.getText(R.string.tf_default_nodejs_port));
        
        // Set the device finger print
        setDeviceFingerprint(context);
    }
    
    public boolean checkin()
    {
        // Assume we are registered unless told otherwise
        boolean registered = true;
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("deviceId", deviceId));
        params.add(new BasicNameValuePair("deviceKey", deviceKey));
        params.add(new BasicNameValuePair("fingerprint", getDeviceFingerprint()));
        
        String postResult = HttpUtils.postUrlData(getServerUrl() + "/checkin", params);            
        JSONObject checkin;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing postResult " + postResult);                
            checkin = (JSONObject) new JSONTokener(postResult).nextValue();
            
            String result = checkin.optString(RESULT, FAILURE);
            
            if (result.equals(OK)) {
                Log.i(Collect.LOGTAG, t + "successful checkin");
            } else if (result.equals(FAILURE)) {
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
        
        Log.i(Collect.LOGTAG, t + "device registration state is " + registered);

        // Clear the session for subsequent requests and reset stored state
        if (registered == false)          
            resetDevice();
        
        return registered;
    }
    
    /*
     * Try and say "goodbye" to Inform Online so that we know that 
     * this client's session is no longer needed.
     * 
     * This is a "best effort" method and it is possible that the device may
     * have already been checked out by another process (such as resetting the device).
     */
    public boolean checkout()
    {
        boolean saidGoodbye = false;
        
        String getResult = HttpUtils.getUrlData(getServerUrl() + "/checkout");
        JSONObject checkout;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
            checkout = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = checkout.optString(RESULT, ERROR);
            
            if (result.equals(OK)) {
                Log.i(Collect.LOGTAG, t + "said goodbye to Inform Online");
                saidGoodbye = true;
            } else 
                Log.i(Collect.LOGTAG, t + "device checkout unnecessary");
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
        
        ready = false;
        session = null;
        
        return saidGoodbye;
    }
    
    public boolean ping()
    {
        boolean alive = false;
        
        // Try to ping the service to see if it is "up"
        String getResult = HttpUtils.getUrlData(getServerUrl() + "/ping");
        JSONObject ping;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
            ping = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = ping.optString(RESULT, ERROR);
            
            if (result.equals(OK) || result.equals(FAILURE))
                alive = true;
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
        
        return alive;
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

    public void setAccountOwner(boolean accountOwner)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ACCOUNT_OWNER, accountOwner);
        editor.commit();  
        
        this.accountOwner = accountOwner;
    }

    // This is a convenience method for making UI decisions ONLY.  NOTHING secure should be based upon this.
    public boolean isAccountOwner()
    {
        return accountOwner;
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

    public void setReady(boolean ready)
    {
        this.ready = ready;
    }

    public boolean isReady()
    {
        return ready;
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

    /*
     * See http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
     */
    public void setDeviceFingerprint(Context context)
    {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        
        this.deviceFingerprint = deviceUuid.toString();
    }

    public String getDeviceFingerprint()
    {
        return deviceFingerprint;
    }

    // Whether this device appears to be registered
    public boolean hasRegistration()
    {
        if (deviceId == null)
            return false;
        else {
            return true;
        }
    }

    public void resetDevice()
    {
        setAccountKey(null);
        setAccountNumber(null);
        setAccountOwner(false);
        
        setDeviceId(null);
        setDeviceKey(null);
        setDevicePin(null);
        
        ready = false;        
        session = null;  
        
        // Remove cached files
        FileUtils.deleteFile(FileUtils.DEVICE_CACHE_FILE_PATH);
        FileUtils.deleteFile(FileUtils.GROUP_CACHE_FILE_PATH);
    }

    private void loadPreferences()
    {
        setAccountKey(prefs.getString(ACCOUNT_KEY, null));
        setAccountNumber(prefs.getString(ACCOUNT_NUM, null));
        setAccountOwner(prefs.getBoolean(ACCOUNT_OWNER, false));
        
        setDeviceId(prefs.getString(DEVICE_ID, null));
        setDeviceKey(prefs.getString(DEVICE_KEY, null));
        setDevicePin(prefs.getString(DEVICE_PIN, null));
    }
}