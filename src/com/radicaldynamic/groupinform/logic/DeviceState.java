package com.radicaldynamic.groupinform.logic;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.CookieStore;
import org.odk.collect.android.utilities.FileUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

/**
 * Stores the state of this device, registration and account information
 * (some of this information is dynamically populated and changes at runtime)
 */
public class DeviceState
{
    private static final String t = "DeviceState: ";
    
    // Constants for strings commonly encountered when interacting with the Inform Online service
    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String EXPIRED = "expired";
    public static final String FAILURE = "failure";
    public static final String RESULT = "result";
    public static final String REASON = "reason";
   
    // Constants for account information stored in preferences
    public static final String ACCOUNT_KEY = "informonline_accountkey";                         // Accessible
    public static final String ACCOUNT_NUM = "informonline_accountnum";                         // Accessible
    private static final String ACCOUNT_OWNER = "informonline_accountown";                      // Invisible
//    private static final String ACCOUNT_PLAN = "informonline_accountplan";                      // Accessible
    
//    private static final String ACCOUNT_LICENCED_SEATS = "informonline_accountlicencedseats";   // Accessible
//    private static final String ACCOUNT_ASSIGNED_SEATS = "informonline_accountassignedseats";   // Accessible
    
    // Constants for device information stored in preferences
    public static final String DEVICE_ID   = "informonline_deviceid";       // Invisible 
    public static final String DEVICE_KEY  = "informonline_devicekey";      // Invisible 
    public static final String DEVICE_PIN  = "informonline_devicepin";      // Accessible
    public static final String DEVICE_ROLE = "informonline_devicerole";     // Accessible 

    public static final String DEFAULT_DATABASE = "informonline_defaultdb"; // Invisible
    
    // The order associated to this device is expired - the account owner needs to fix it 
    // (either by renewing the order or associating the device profile with a new, unexpired order)
    public static final String EXPIRED_ORDER = "informonline_expired";
    
    // Dictates whether or not the app was put into offline mode manually
    public static final String OFFLINE_MODE = "informonline_offlinemode";   // Invisible
    
    // Constants for session information stored in preferences
    public static final String SESSION     = "informonline_session";        // Invisible
    
    // Lookup map for account devices, indexed by device ID
    private Map<String, AccountDevice> accountDevicesMap = new HashMap<String, AccountDevice>();
    private Map<String, AccountDevice> accountDevicesSyncMap = Collections.synchronizedMap(accountDevicesMap);
    
    // Lookup map for account folders, indexed by folder ID
    private Map<String, AccountFolder> accountFoldersMap = new HashMap<String, AccountFolder>();
    private Map<String, AccountFolder> accountFoldersSyncMap = Collections.synchronizedMap(accountFoldersMap);

    private String accountNumber;           // The licence number
    private String accountKey;              // The licence key
    private boolean accountOwner;           // Is the user also the account owner?
//    private String accountPlan;             // Plan type
    
//    private int accountLicencedSeats;       // The number of licenced seats for the account
//    private int accountAssignedSeats;       // The number of seats allocated and assigned (not necessarily active)
    
    private String deviceId;
    private String deviceKey;
    private String devicePin;    
    private String deviceFingerprint;
    private String deviceRole;
    
    private String defaultDatabase;
    private String selectedDatabase;        // The database the user has selected to work with at any given time
    
    private String serverUrl;
        
    private CookieStore session = null;
    
    private boolean offlineModeEnabled = false;
    private boolean expired = false;
    
    private Context mContext = null;    
    private SharedPreferences mPrefs = null;
    
    // Used by Collect
    public DeviceState()
    {
        
    }
    
    public DeviceState(Context context)
    {
        mContext = context;
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences();
        
        // Initialize server URL
        setServerUrl("https://" + mContext.getText(R.string.tf_default_ionline_server) + ":" + mContext.getText(R.string.tf_default_ionline_port));
        
        // Set the device finger print
        setDeviceFingerprint(mContext);
    }    
    
    public void setDeviceList(Map<String, AccountDevice> accountDevices) 
    { 
	if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setAccountDevices()");
        this.accountDevicesSyncMap = accountDevices; 
    }
    
    public Map<String, AccountDevice> getDeviceList() { 
        return accountDevicesSyncMap; 
    }
    
    public void setFolderList(Map<String, AccountFolder> accountFolders) 
    { 
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setAccountFolders()");
        this.accountFoldersSyncMap = accountFolders; 
    }
    
    public Map<String, AccountFolder> getFolderList() { 
        return accountFoldersSyncMap; 
    }

    public void setAccountKey(String accountKey)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setAccountKey() HIDDEN");
        
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(ACCOUNT_KEY, accountKey);
        editor.commit();
                
        this.accountKey = accountKey;
    }
    
    public String getAccountKey()
    {
        return accountKey;
    }
    
//    public void setAccountAssignedSeats(int accountAssignedSeats)
//    {
//        Log.d(Collect.LOGTAG, t + "setAccountAssignedSeats() " + accountAssignedSeats);
//        
//        SharedPreferences.Editor editor = mPrefs.edit();
//        editor.putInt(ACCOUNT_ASSIGNED_SEATS, accountAssignedSeats);
//        editor.commit();  
//        
//        this.accountAssignedSeats = accountAssignedSeats;
//    }
//
//    public int getAccountAssignedSeats()
//    {
//        return accountAssignedSeats;
//    }
//    
//    public void setAccountLicencedSeats(int accountLicencedSeats)
//    {
//        Log.d(Collect.LOGTAG, t + "setAccountLicencedSeats() " + accountLicencedSeats);
//        
//        SharedPreferences.Editor editor = mPrefs.edit();
//        editor.putInt(ACCOUNT_LICENCED_SEATS, accountLicencedSeats);
//        editor.commit();  
//        
//        this.accountLicencedSeats = accountLicencedSeats;
//    }
//
//    public int getAccountLicencedSeats()
//    {
//        return accountLicencedSeats;
//    }
    
    public void setAccountNumber(String accountNumber)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setAccountNumber() " + accountNumber);
        
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
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setAccountOwner() " + accountOwner);
        
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

//    public void setAccountPlan(String accountPlan)
//    {
//        Log.d(Collect.LOGTAG, t + "setAccountPlan() " + accountPlan);
//        
//        SharedPreferences.Editor editor = mPrefs.edit();
//        editor.putString(ACCOUNT_PLAN, accountPlan);
//        editor.commit();  
//        
//        this.accountPlan = accountPlan;
//    }
//
//    public String getAccountPlan()
//    {
//        return accountPlan;
//    }

    public void setDeviceId(String deviceId)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setDeviceId() " + deviceId);
        
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
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setDeviceKey() HIDDEN");
        
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
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setDevicePin() HIDDEN");
        
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(DEVICE_PIN, devicePin);
        editor.commit(); 
        
        this.devicePin = devicePin;
    }

    public String getDevicePin()
    {
        return devicePin;
    }

    public void setDeviceRole(String deviceRole) 
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(DEVICE_ROLE, deviceRole);
        editor.commit(); 
        
        this.deviceRole = deviceRole;
    }

    public String getDeviceRole() 
    {
        return deviceRole;
    }

    public void setExpired(boolean expired) 
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setExpired() " + expired);
        
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(EXPIRED_ORDER, expired);
        editor.commit();
        
        this.expired = expired;
    }

    public boolean isExpired() {
        return expired;
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
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setSession() called");
        
        this.session = session;
    }

    public CookieStore getSession()
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "getSession() called");
        
        return session;
    }

    public void setDefaultDatabase(String defaultDatabase)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setDefaultDatabase() " + defaultDatabase);
        
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(DEFAULT_DATABASE, defaultDatabase);
        editor.commit(); 
        
        this.defaultDatabase = defaultDatabase;
    }

    public String getDefaultDatabase()
    {
        return defaultDatabase;
    }

    public void setSelectedDatabase(String selectedDatabase)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setSelectedDatabase() " + selectedDatabase);
        
        this.selectedDatabase = selectedDatabase;
    }

    public String getSelectedDatabase()
    {
        return selectedDatabase;
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
        
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setDeviceFingerprint() " + deviceUuid.toString());
        
        this.deviceFingerprint = deviceUuid.toString();
    }

    public String getDeviceFingerprint()
    {
        return deviceFingerprint;
    }

    public void setOfflineModeEnabled(boolean offlineMode)
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "setOfflineModeEnabled() " + offlineMode);
        
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
    
    // Whether this device has folders that are marked for offline use (aka synchronized or replicated)
    public boolean hasReplicatedFolders()
    {
        // Does the user have at least one database selected for offline use?
        Iterator<String> folderIds = accountFoldersSyncMap.keySet().iterator();
        
        int replicatedFolders = 0;
        
        while (folderIds.hasNext()) {
            String id = folderIds.next();
            
            if (Collect.getInstance().getDeviceState().getFolderList().get(id).isReplicated())
                replicatedFolders++;                        
        }
        
        if (replicatedFolders == 0) 
            return false;
        else 
            return true;
    }
    
    public void resetDevice()
    {
        setAccountKey(null);
//        setAccountAssignedSeats(0);
//        setAccountLicencedSeats(0);
        setAccountNumber(null);
        setAccountOwner(false);
//        setAccountPlan(null);
        
        setDeviceId(null);
        setDeviceKey(null);
        setDevicePin(null);
        setDeviceRole(AccountDevice.ROLE_UNASSIGNED);
        
        setDefaultDatabase(null);
        setExpired(false);
        setOfflineModeEnabled(false);
        setSession(null);
                
        // Remove cache files
        new File(mContext.getCacheDir(), FileUtilsExtended.DEVICE_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtilsExtended.FOLDER_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtilsExtended.SESSION_CACHE_FILE).delete();
        
        // Remove external storage cache & files
        FileUtilsExtended.deleteDirectory(new File(FileUtilsExtended.EXTERNAL_CACHE));
        FileUtilsExtended.deleteDirectory(new File(FileUtilsExtended.EXTERNAL_FILES));

        // Shutdown CouchDB and remove databases & log files
//        if (Collect.getInstance().stopService(new Intent(Collect.getInstance().getApplicationContext(), CouchService.class)))
//            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "CouchService stopped");
        
        // Shutdown other services to ensure a full reset off all stateful information
        if (Collect.getInstance().stopService(new Intent(Collect.getInstance().getApplicationContext(), DatabaseService.class)))
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "DatabaseService stopped");
        
        if (Collect.getInstance().stopService(new Intent(Collect.getInstance().getApplicationContext(), InformOnlineService.class)))
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "InformOnlineService stopped");  

        // Remove DB files & log files
        if (FileUtilsExtended.deleteDirectory(new File(FileUtilsExtended.EXTERNAL_DB)))
            FileUtils.createFolder(FileUtilsExtended.EXTERNAL_DB);
    }

    private void loadPreferences()
    {
        setAccountKey(mPrefs.getString(ACCOUNT_KEY, null));
//        setAccountAssignedSeats(mPrefs.getInt(ACCOUNT_ASSIGNED_SEATS, 0));
//        setAccountLicencedSeats(mPrefs.getInt(ACCOUNT_LICENCED_SEATS, 0));
        setAccountNumber(mPrefs.getString(ACCOUNT_NUM, null));
        setAccountOwner(mPrefs.getBoolean(ACCOUNT_OWNER, false));
//        setAccountPlan(mPrefs.getString(ACCOUNT_PLAN, null));
        
        setDeviceId(mPrefs.getString(DEVICE_ID, null));
        setDeviceKey(mPrefs.getString(DEVICE_KEY, null));
        setDevicePin(mPrefs.getString(DEVICE_PIN, null));
        setDeviceRole(mPrefs.getString(DEVICE_ROLE, AccountDevice.ROLE_UNASSIGNED));
        
        setDefaultDatabase(mPrefs.getString(DEFAULT_DATABASE, null));
        setExpired(mPrefs.getBoolean(EXPIRED_ORDER, false));
        setSelectedDatabase(getDefaultDatabase());
        setOfflineModeEnabled(mPrefs.getBoolean(OFFLINE_MODE, false));
        
        // Further post-cleanup if the user "cleared data" via the Application Info screen
        if (getDeviceId() == null)
            resetDevice();
    }
}