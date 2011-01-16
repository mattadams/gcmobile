package com.radicaldynamic.groupinform.logic;

import java.util.UUID;

import org.apache.http.client.CookieStore;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.utilities.FileUtils;

/*
 * Stores the state of this device as registered with Inform Online
 */
public class InformOnlineState
{
    @SuppressWarnings("unused")
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
    
    public static final String DEFAULT_DATABASE = "informonline_defaultdb"; // Invisible
    
    // Dictates whether or not the app was put into offline mode manually
    public static final String OFFLINE_MODE = "informonline_offlinemode";   // Invisible
    
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
    private String defaultDatabase;
    
    private boolean offlineModeEnabled = false;
    private CookieStore session = null;
    private SharedPreferences prefs;
       
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

    public void setDefaultDatabase(String defaultDatabase)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEFAULT_DATABASE, defaultDatabase);
        editor.commit(); 
        
        this.defaultDatabase = defaultDatabase;
    }

    public String getDefaultDatabase()
    {
        return defaultDatabase;
    }

    /*
     * See http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
     */
    private void setDeviceFingerprint(Context context)
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

    public void setOfflineModeEnabled(boolean offlineMode)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(OFFLINE_MODE, offlineMode);
        editor.commit();
        
        this.offlineModeEnabled = offlineMode;
    }

    public boolean isOfflineModeEnabled()
    {
        return offlineModeEnabled;
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
    
    private void removeFiles()
    {
        // Remove cached files
        FileUtils.deleteFile(FileUtils.DEVICE_CACHE_FILE_PATH);
        FileUtils.deleteFile(FileUtils.FOLDER_CACHE_FILE_PATH);
        FileUtils.deleteFile(FileUtils.SESSION_CACHE_FILE_PATH);
    }

    public void resetDevice()
    {
        setAccountKey(null);
        setAccountNumber(null);
        setAccountOwner(false);
        
        setDeviceId(null);
        setDeviceKey(null);
        setDevicePin(null);
        
        setDefaultDatabase(null);
        setOfflineModeEnabled(false);
        setSession(null); 
        
        removeFiles();
    }

    private void loadPreferences()
    {
        setAccountKey(prefs.getString(ACCOUNT_KEY, null));
        setAccountNumber(prefs.getString(ACCOUNT_NUM, null));
        setAccountOwner(prefs.getBoolean(ACCOUNT_OWNER, false));
        
        setDeviceId(prefs.getString(DEVICE_ID, null));
        setDeviceKey(prefs.getString(DEVICE_KEY, null));
        setDevicePin(prefs.getString(DEVICE_PIN, null));
        
        setDefaultDatabase(prefs.getString(DEFAULT_DATABASE, null));
        setOfflineModeEnabled(prefs.getBoolean(OFFLINE_MODE, false));
        
        // Further post-cleanup if the user "cleared data" via the Application Info screen
        if (getDeviceId() == null)
            resetDevice();
    }
}