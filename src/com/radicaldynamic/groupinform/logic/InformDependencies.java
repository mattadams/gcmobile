package com.radicaldynamic.groupinform.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.radicaldynamic.groupinform.application.Collect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

/*
 * Logic class for dealing with dependancies we have on other packages
 */
public class InformDependencies
{
    private static final String t = "InformDependencies: ";
    
    // Package names
    public static final String BARCODE = "com.google.zxing.client.android";
    public static final String COUCHDB = "com.couchone.couchdb";
    
    // Preference 
    public static final String DEPENDENCY_REMINDERS = "dependency_remiders";
    
    private HashMap<String, Integer> dependencies = new HashMap<String, Integer>(); 
    
    private boolean reminderEnabled;
    private boolean initialized = false;

    private Context context;    
    
    // Used by Collect
    public InformDependencies()
    {
        
    }
    
    public InformDependencies(Context context)
    {
        getDependencies().put(BARCODE, 0);
//        getDependencies().put(COUCHDB, 0);
        
        setContext(context);
        scan();
        
        // Load reminder preference
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        setReminderEnabled(p.getBoolean(DEPENDENCY_REMINDERS, true));
        
        setInitialized(true);
    }
    
    // Returns true if all dependencies are installed and false if otherwise
    public boolean allSatisfied()
    {
        Set<String> deps = getDependencies().keySet();
        Iterator<String> it = deps.iterator();
        
        boolean allInstalled = true;
        
        while (it.hasNext()) {
            String depKey = it.next();
            
            if (getDependencies().get(depKey) == 0)
                allInstalled = false;
        }
        
        return allInstalled;
    }
    
    // Returns the next unavailable dependency
    public String getNextDependency()
    {
        Set<String> deps = getDependencies().keySet();
        Iterator<String> it = deps.iterator();
               
        while (it.hasNext()) {
            String depKey = it.next();
            
            if (getDependencies().get(depKey) == 0) {
//                Log.v(Collect.LOGTAG, t + "next dependency is " + depKey);
                return depKey;
            }
        }
        
        return null;
    }
    
    public boolean isInstalled(String packageName)
    {
        boolean installed = false;
        
        if (getDependencies().containsKey(packageName))
            if (getDependencies().get(packageName) == 1)
                installed = true;

        return installed;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public Context getContext()
    {
        return context;
    }

    public void setDependancies(HashMap<String, Integer> dependancies)
    {
        this.dependencies = dependancies;
    }

    public HashMap<String, Integer> getDependencies()
    {
        return dependencies;
    }

    public void setReminderEnabled(boolean reminderEnabled)
    {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = p.edit();
        editor.putBoolean(DEPENDENCY_REMINDERS, reminderEnabled);
        editor.commit();
        
        this.reminderEnabled = reminderEnabled;
    }

    public boolean isReminderEnabled()
    {
        return reminderEnabled;
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    private void scan()
    {
        // Iterate through all installed applications and detect the ones that are important to us
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = getContext().getPackageManager().queryIntentActivities(mainIntent, 0);
        final Iterator<ResolveInfo> pkgAppsListIterator = pkgAppsList.iterator();
        
        while (pkgAppsListIterator.hasNext()) {
            ResolveInfo ri = pkgAppsListIterator.next();
            
//            Log.v(Collect.LOGTAG, t 
//                    + "activity name: " + ri.activityInfo.name 
//                    + ", package name: " + ri.activityInfo.packageName);
            
            if (getDependencies().containsKey(ri.activityInfo.packageName)) {
                Log.d(Collect.LOGTAG, t + "detected dependancy " + ri.activityInfo.packageName);
                getDependencies().put(ri.activityInfo.packageName, 1);
            }
        }
    }
}
