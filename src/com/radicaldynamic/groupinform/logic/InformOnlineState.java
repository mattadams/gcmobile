package com.radicaldynamic.groupinform.logic;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.CookieStore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.couchone.libcouch.ICouchService;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.FileUtils;

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
    
    // Lookup map for account devices, indexed by device ID
    private Map<String, AccountDevice> accountDevicesMap = new HashMap<String, AccountDevice>();
    private Map<String, AccountDevice> accountDevicesSyncMap = Collections.synchronizedMap(accountDevicesMap);
    
    // Lookup map for account folders, indexed by folder ID
    private Map<String, AccountFolder> accountFoldersMap = new HashMap<String, AccountFolder>();
    private Map<String, AccountFolder> accountFoldersSyncMap = Collections.synchronizedMap(accountFoldersMap);

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
    private String selectedDatabase;        // The database the user has selected to work with at any given time
    
    private String serverUrl;
        
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
        setServerUrl("http://" + mContext.getText(R.string.tf_default_ionline_server) + ":" + mContext.getText(R.string.tf_default_ionline_port));
        
        // Set the device finger print
        setDeviceFingerprint(mContext);
    }    
    
    public void setAccountDevices(Map<String, AccountDevice> accountDevices) 
    { 
        Log.d(Collect.LOGTAG, t + "setAccountDevices()");
        
        this.accountDevicesSyncMap = accountDevices; 
    }
    
    public Map<String, AccountDevice> getAccountDevices() { 
        return accountDevicesSyncMap; 
    }
    
    public void setAccountFolders(Map<String, AccountFolder> accountFolders) 
    { 
        Log.d(Collect.LOGTAG, t + "setAccountFolders()");
        
        this.accountFoldersSyncMap = accountFolders; 
    }
    
    public Map<String, AccountFolder> getAccountFolders() { 
        return accountFoldersSyncMap; 
    }

    public void setAccountKey(String accountKey)
    {
        Log.d(Collect.LOGTAG, t + "setAccountKey() " + accountKey);
        
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
        Log.d(Collect.LOGTAG, t + "setAccountAssignedSeats() " + accountAssignedSeats);
        
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
        Log.d(Collect.LOGTAG, t + "setAccountLicencedSeats() " + accountLicencedSeats);
        
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
        Log.d(Collect.LOGTAG, t + "setAccountNumber() " + accountNumber);
        
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
        Log.d(Collect.LOGTAG, t + "setAccountOwner() " + accountOwner);
        
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
        Log.d(Collect.LOGTAG, t + "setAccountPlan() " + accountPlan);
        
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
        Log.d(Collect.LOGTAG, t + "setDeviceId() " + deviceId);
        
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
        Log.d(Collect.LOGTAG, t + "setDeviceKey() HIDDEN");
        
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
        Log.d(Collect.LOGTAG, t + "setDevicePin() HIDDEN");
        
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
        Log.d(Collect.LOGTAG, t + "setDefaultDatabase() " + defaultDatabase);
        
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
        Log.d(Collect.LOGTAG, t + "setSelectedDatabase() " + selectedDatabase);
        
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
        
        Log.d(Collect.LOGTAG, t + "setDeviceFingerprint() " + deviceUuid.toString());
        
        this.deviceFingerprint = deviceUuid.toString();
    }

    public String getDeviceFingerprint()
    {
        return deviceFingerprint;
    }

    public void setOfflineModeEnabled(boolean offlineMode)
    {
        Log.d(Collect.LOGTAG, t + "setOfflineModeEnabled() " + offlineMode);
        
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
            
            if (Collect.getInstance().getInformOnlineState().getAccountFolders().get(id).isReplicated())
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
        
        // Reset dependency preferences back to defaults
        if (Collect.getInstance().getInformDependencies().isInitialized())
            Collect.getInstance().getInformDependencies().setReminderEnabled(true);
                
        // Remove cache files
        new File(mContext.getCacheDir(), FileUtils.DEVICE_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtils.FOLDER_CACHE_FILE).delete();
        new File(mContext.getCacheDir(), FileUtils.SESSION_CACHE_FILE).delete();

        try {
            if (Collect.getInstance().getCouchService() instanceof ICouchService) {
                // Stop CouchDB
                Collect.getInstance().getCouchService().quitCouchDB();              
                Collect.getInstance().stopService(new Intent(ICouchService.class.getName()));

                // Remove DB files & log files
                if (FileUtils.deleteFolder(FileUtils.EXTERNAL_COUCH + "/var/lib/couchdb"))
                    FileUtils.createFolder(FileUtils.EXTERNAL_COUCH + "/var/lib/couchdb");    

                if (FileUtils.deleteFolder(FileUtils.EXTERNAL_COUCH + "/var/log/couchdb"))
                    FileUtils.createFolder(FileUtils.EXTERNAL_COUCH + "/var/log/couchdb");
            }
        } catch (RemoteException e) {
            Log.e(Collect.LOGTAG, t + "unable to quit CouchDB: " + e.toString());
            e.printStackTrace();
        }
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
        setSelectedDatabase(getDefaultDatabase());
        setOfflineModeEnabled(mPrefs.getBoolean(OFFLINE_MODE, false));
        
        // Further post-cleanup if the user "cleared data" via the Application Info screen
        if (getDeviceId() == null)
            resetDevice();
    }
}