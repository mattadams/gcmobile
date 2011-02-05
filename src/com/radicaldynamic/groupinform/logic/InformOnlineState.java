package com.radicaldynamic.groupinform.logic;

import java.io.File;
import java.util.UUID;

import org.apache.http.client.CookieStore;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
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
    public static final String ACCOUNT_KEY = "informonline_accountkey";                         // Accessible
    public static final String ACCOUNT_NUM = "informonline_accountnum";                         // Accessible
    private static final String ACCOUNT_OWNER = "informonline_accountown";                      // Invisible
    private static final String ACCOUNT_PLAN = "informonline_accountplan";                      // Accessible
    
    private static final String ACCOUNT_LICENCED_SEATS = "informonline_accountlicencedseats";   // Accessible
    private static final String ACCOUNT_ASSIGNED_SEATS = "informonline_accountassignedseats";   // Accessible
    
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
    private String accountPlan;             // Plan type
    
    private int accountLicencedSeats;       // The number of licenced seats for the account
    private int accountAssignedSeats;       // The number of seats allocated and assigned (not necessarily active)
    
    private String deviceId;
    private String deviceKey;
    private String devicePin;
    
    private String deviceFingerprint;
    private String defaultDatabase;
    
    private CookieStore session = null;
    
    private boolean offlineModeEnabled = false;
    
    private Context mContext = null;    
    private SharedPreferences mPrefs = null;
    
    // Used by Collect
    public InformOnlineState()
    {
        
    }
    
    public InformOnlineState(Context context)
    {
        mContext = context;
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences();
        
        // Initialize server URL
        setServerUrl("http://" + mContext.getText(R.string.tf_default_nodejs_server) + ":" + mContext.getText(R.string.tf_default_nodejs_port));
        
        // Set the device finger print
        setDeviceFingerprint(mContext);       
        
    }

    public void setAccountKey(String accountKey)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(ACCOUNT_KEY, accountKey);
        editor.commit();
                
        this.accountKey = accountKey;
    }
    
    public String getAccountKey()
    {
        return accountKey;
    }
    
    public void setAccountAssignedSeats(int accountAssignedSeats)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(ACCOUNT_ASSIGNED_SEATS, accountAssignedSeats);
        editor.commit();  
        
        this.accountAssignedSeats = accountAssignedSeats;
    }

    public int getAccountAssignedSeats()
    {
        return accountAssignedSeats;
    }
    
    public void setAccountLicencedSeats(int accountLicencedSeats)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(ACCOUNT_LICENCED_SEATS, accountLicencedSeats);
        editor.commit();  
        
        this.accountLicencedSeats = accountLicencedSeats;
    }

    public int getAccountLicencedSeats()
    {
        return accountLicencedSeats;
    }
    
    public void setAccountNumber(String accountNumber)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
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
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(ACCOUNT_OWNER, accountOwner);
        editor.commit();  
        
        this.accountOwner = accountOwner;
    }

    // This is a convenience method for making UI decisions ONLY.  NOTHING secure should be based upon this.
    public boolean isAccountOwner()
    {
        return accountOwner;
    }    

    public void setAccountPlan(String accountPlan)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(ACCOUNT_PLAN, accountPlan);
        editor.commit();  
        
        this.accountPlan = accountPlan;
    }

    public String getAccountPlan()
    {
        return accountPlan;
    }

    public void setDeviceId(String deviceId)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
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
        SharedPreferences.Editor editor = mPrefs.edit();
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
        SharedPreferences.Editor editor = mPrefs.edit();
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
        SharedPreferences.Editor editor = mPrefs.edit();
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
        SharedPreferences.Editor editor = mPrefs.edit();
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
        new File(mContext.getCacheDir(), FileUtils.DEVICE_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtils.FOLDER_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtils.SESSION_CACHE_FILE).delete();
    }

    public void resetDevice()
    {
        setAccountKey(null);
        setAccountAssignedSeats(0);
        setAccountLicencedSeats(0);
        setAccountNumber(null);
        setAccountOwner(false);
        setAccountPlan(null);
        
        setDeviceId(null);
        setDeviceKey(null);
        setDevicePin(null);
        
        setDefaultDatabase(null);
        setOfflineModeEnabled(false);
        setSession(null); 
        
        removeFiles();
        
        // Reset dependency preferences back to defaults
        if (Collect.getInstance().getInformDependencies().isInitialized())
            Collect.getInstance().getInformDependencies().setReminderEnabled(true);
    }

    private void loadPreferences()
    {
        setAccountKey(mPrefs.getString(ACCOUNT_KEY, null));
        setAccountAssignedSeats(mPrefs.getInt(ACCOUNT_ASSIGNED_SEATS, 0));
        setAccountLicencedSeats(mPrefs.getInt(ACCOUNT_LICENCED_SEATS, 0));
        setAccountNumber(mPrefs.getString(ACCOUNT_NUM, null));
        setAccountOwner(mPrefs.getBoolean(ACCOUNT_OWNER, false));
        setAccountPlan(mPrefs.getString(ACCOUNT_PLAN, null));
        
        setDeviceId(mPrefs.getString(DEVICE_ID, null));
        setDeviceKey(mPrefs.getString(DEVICE_KEY, null));
        setDevicePin(mPrefs.getString(DEVICE_PIN, null));
        
        setDefaultDatabase(mPrefs.getString(DEFAULT_DATABASE, null));
        setOfflineModeEnabled(mPrefs.getBoolean(OFFLINE_MODE, false));
        
        // Further post-cleanup if the user "cleared data" via the Application Info screen
        if (getDeviceId() == null)
            resetDevice();
    }
}