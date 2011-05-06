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

package com.radicaldynamic.groupinform.activities;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.couchdb.CouchInitializer;
import com.radicaldynamic.groupinform.couchdb.CouchInstaller;
import com.radicaldynamic.groupinform.couchdb.InformCouchClient;
import com.radicaldynamic.groupinform.couchdb.InformCouchService;
import com.radicaldynamic.groupinform.logic.InformDependencies;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.utilities.FileUtils;

/**
 * Application initialization: registration, login, database installation & init 
 */
public class LauncherActivity extends Activity
{
    private static final String t = "LauncherActivity: ";
    
    // Dialog constants
    private static final int DIALOG_DEPENDENCY_UNMET = 1;
    private static final int DIALOG_EXTERNAL_STORAGE_UNAVAILABLE = 2;
    private static final int DIALOG_UNABLE_TO_CONNECT_OFFLINE_DISABLED = 3;
    private static final int DIALOG_UNABLE_TO_CONNECT_OFFLINE_ENABLED = 4;
    private static final int DIALOG_UNABLE_TO_REGISTER = 5;    
    
    // Intent status codes
    private static final String KEY_REINIT_IOSERVICE = "key_reinit_ioservice";
    
    private static final int BROWSER_ACTIVITY = 1;
    
    private ProgressDialog mProgressDialog;
    private Toast mSplashToast;
    
    /*
     * Implement the callbacks that allow CouchDB to talk to this app
     * (not really necessary)
     */
    private InformCouchClient mCallback = new InformCouchClient.Stub() {
        @Override
        public void couchStarted(String host, int port) throws RemoteException {
        }

        @Override
        public void databaseCreated(String name, String user, String pass, String tag) throws RemoteException {
        }
    };
    
    // Service handling for the CouchDB process
    private ServiceConnection mCouchConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            try {
                Collect.getInstance().setCouchService(InformCouchService.Stub.asInterface(service));
                Collect.getInstance().getCouchService().initCouchDB(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Collect.getInstance().setCouchService(null);
        }
    };

    // Service handling for our connection to databases provided by Couch
    private ServiceConnection mDatabaseConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Collect.getInstance().setDbService(((DatabaseService.LocalBinder) service).getService());
        }

        public void onServiceDisconnected(ComponentName className)
        {
            Collect.getInstance().setDbService(null);
        }
    };    
    
    // Service handling for our connection to Inform Online
    private ServiceConnection mOnlineConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Collect.getInstance().setIoService(((InformOnlineService.LocalBinder) service).getService());
        }

        public void onServiceDisconnected(ComponentName className)
        {
            Collect.getInstance().setIoService(null);
        }
    };
    
    private Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case CouchInstaller.ERROR:
                AlertDialog.Builder builder = new AlertDialog.Builder(LauncherActivity.this);
                
                // TODO: write a better error message
                builder.setMessage(LauncherActivity.this.getString(R.string.couch_install_error))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { 
                        public void onClick(DialogInterface dialog, int id) { 
                            finish();                            
                        }
                    });
                
                AlertDialog alert = builder.create();
                alert.show();                
                break;

            case CouchInstaller.PROGRESS:
                if (msg.arg1 > 0)
                    mProgressDialog.setProgress(msg.arg1);                
                break;

            case CouchInstaller.COMPLETE:
                // We should only dismiss dialogs that are actually showing
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                
                if (msg.arg1 == 0)
                    restartActivity(true);
                
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // If SD card error, quit
        if (!FileUtils.storageReady())
            showDialog(DIALOG_EXTERNAL_STORAGE_UNAVAILABLE);

        Intent intent = getIntent();
        
        if (intent == null) {            
        } else {
            if (intent.getBooleanExtra(KEY_REINIT_IOSERVICE, false)) {
                if (Collect.getInstance().getIoService() instanceof InformOnlineService)
                    Collect.getInstance().getIoService().reinitializeService();
            }
        }
        
        displaySplash();

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);       
        setContentView(R.layout.launcher);
                
        if (Collect.getInstance().getIoService() instanceof InformOnlineService && Collect.getInstance().getIoService().isReady()) {            
            if (CouchInstaller.checkInstalled() && CouchInitializer.isEnvironmentInitialized()) {                
                if (Collect.getInstance().getInformDependencies().isInitialized()) {
                    if (Collect.getInstance().getInformDependencies().allSatisfied()) {                        
                        // Start the persistent online connection
                        startService(new Intent(LauncherActivity.this, InformOnlineService.class));
                        
                        // Start the persistent database connection
                        startService(new Intent(LauncherActivity.this, DatabaseService.class));                        
                        
                        startActivityForResult(new Intent(LauncherActivity.this, BrowserActivity.class), BROWSER_ACTIVITY);                        
                    } else {
                        showDialog(DIALOG_DEPENDENCY_UNMET);
                    }
                }                
            } else {                
                /*
                 * Install database engine
                 * 
                 * TODO: calculate required space before install begins 
                 */
                mProgressDialog = new ProgressDialog(LauncherActivity.this);
                mProgressDialog.setCancelable(false);                
                mProgressDialog.setTitle(R.string.tf_database_being_installed);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();

                new Thread() {
                    public void run() {
                        try {
                            CouchInstaller.doInstall(mProgressHandler);
                        } catch (Exception e) {
                            e.printStackTrace();
                            mProgressDialog.dismiss();
                            mProgressHandler.sendMessage(mProgressHandler.obtainMessage(CouchInstaller.ERROR));
                        }
                    }
                }.start();
            }
        } else {
            new InitializeApplicationTask().execute(getApplicationContext());
        }
        
        if (bindService(new Intent(LauncherActivity.this, InformOnlineService.class), mOnlineConnection, Context.BIND_AUTO_CREATE))
            Log.d(Collect.LOGTAG, t + "successfully bound to InformOnlineService");
        else 
            Log.e(Collect.LOGTAG, t + "unable to bind to InformOnlineService");
        
        if (bindService(new Intent(LauncherActivity.this, DatabaseService.class), mDatabaseConnection, Context.BIND_AUTO_CREATE))
            Log.d(Collect.LOGTAG, t + "successfully bound to DatabaseService");
        else 
            Log.e(Collect.LOGTAG, t + "unable to bind to DatabaseService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.ListActivity#onDestroy()
     * 
     * Recall:
     * Because onPause() is the first of the three [killable methods], it's the only one that's guaranteed to be called 
     * before the process is killed — onStop() and onDestroy() may not be. Therefore, you should use onPause() to write 
     * any persistent data (such as user edits) to storage. 
     */
    @Override
    protected void onDestroy()
    {
        // Close down our services (not all can be expected to be running)
        if (Collect.getInstance().getCouchService() instanceof InformCouchService) {
            try {
                Log.d(Collect.LOGTAG, t + "unbinding from CouchService");
                unbindService(mCouchConnection);                
            } catch (IllegalArgumentException e) {
                Log.w(Collect.LOGTAG, t + "CouchService not registered: " + e.toString());
            }
        }
        
        if (Collect.getInstance().getDbService() instanceof DatabaseService) {
            try {
                Log.d(Collect.LOGTAG, t + "unbinding from DatabaseService");
                unbindService(mDatabaseConnection);
            } catch (IllegalArgumentException e) { 
                Log.w(Collect.LOGTAG, t + "DatabaseService not registered: " + e.toString());    
            }
        }
        
        if (Collect.getInstance().getIoService() instanceof InformOnlineService) {
            try {
                Log.d(Collect.LOGTAG, t + "unbinding from InformOnlineService");
                unbindService(mOnlineConnection);
            } catch (IllegalArgumentException e) {
                Log.w(Collect.LOGTAG, t + "InformOnlineService not registered: " + e.toString());
            }
        }
        
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();        
        
        if (CouchInstaller.checkInstalled() && CouchInitializer.isEnvironmentInitialized())
            if (Collect.getInstance().getInformDependencies().isInitialized())
                if (!Collect.getInstance().getInformDependencies().allSatisfied())
                    showDialog(DIALOG_DEPENDENCY_UNMET);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        // "Exit" if the user returns from BrowserActivity
        case BROWSER_ACTIVITY:
            finish();
            break; 
        }        
    }
    
    public Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        
        switch (id) {
        case DIALOG_DEPENDENCY_UNMET:            
            /*
             * Since showDependencyDialog essentially enters a loop once unsatisfied dependencies
             * are found we need a way to get out once all dependencies have been satisfied or
             * after the user has skipped installation.  This is our out.  
             */        
            if (Collect.getInstance().getInformDependencies().getNextDependency() == null) {
                restartActivity(false);
                return builder.create();
            }
            
            String copy = getString(R.string.tf_unavailable);
            String dependency = Collect.getInstance().getInformDependencies().getNextDependency();
            
            if (dependency.equals(InformDependencies.BARCODE))
                copy = getString(R.string.com_google_zxing_client_android);
            else if (dependency.equals(InformDependencies.COUCHDB))
                copy = getString(R.string.com_couchone_couchdb);            
            
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_dependency_missing_dialog)
                .setMessage(getString(R.string.tf_dependency_missing_dialog_msg) + copy);
            
            builder.setPositiveButton(getText(R.string.tf_install), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // The install may not be successful but at this point we just pretend that it was
                    String dependency = Collect.getInstance().getInformDependencies().getNextDependency();                    
                    Collect.getInstance().getInformDependencies().getDependencies().put(dependency, 1);
                    
                    removeDialog(DIALOG_DEPENDENCY_UNMET);
                    showDialog(DIALOG_DEPENDENCY_UNMET);
                    
                    String uri = "market://details?id=" + dependency;
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(i);
                }
            });
            
            builder.setNeutralButton(getText(R.string.tf_remind_later), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {          
                    // This doesn't permanently mark the dependency as installed but it allows us to skip to the next one
                    Collect.getInstance().getInformDependencies().getDependencies().put(
                            Collect.getInstance().getInformDependencies().getNextDependency(), 1);

                    removeDialog(DIALOG_DEPENDENCY_UNMET);
                    showDialog(DIALOG_DEPENDENCY_UNMET);
                }
            });
            
            dialog = builder.create();            
            break;
        
        case DIALOG_EXTERNAL_STORAGE_UNAVAILABLE:
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.no_sd_error));
            
            builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });
            
            dialog = builder.create();
            break;
            
        // Registered    
        case DIALOG_UNABLE_TO_CONNECT_OFFLINE_DISABLED:
            String msg;            

            if (CouchInstaller.checkInstalled() 
                    && CouchInitializer.isEnvironmentInitialized()
                    && Collect.getInstance().getInformOnlineState().hasReplicatedFolders())
                msg = getString(R.string.tf_connection_error_registered_with_db_msg);
            else    
                msg = getString(R.string.tf_connection_error_registered_without_db_msg);
            
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)        
                .setTitle(R.string.tf_unable_to_connect)
                .setMessage(msg);

            builder.setPositiveButton(getText(R.string.tf_retry), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    restartActivity(true);
                }
            });
            
            if (CouchInstaller.checkInstalled() 
                    && CouchInitializer.isEnvironmentInitialized()
                    && Collect.getInstance().getInformOnlineState().hasReplicatedFolders()) {
                
                builder.setNeutralButton(getText(R.string.tf_go_offline), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        restartActivity(false);
                    }
                });    
            }

            builder.setNegativeButton(getText(R.string.tf_exit_inform), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Make sure that Couch shuts down (since it's a remote process)
                    if (Collect.getInstance().getCouchService() instanceof InformCouchService) {
                        // The user could not connect so there is no point of any of this continuing to run in the background
                        try {
                            Collect.getInstance().getCouchService().quitCouchDB();
                        } catch (RemoteException e) {
                            Log.w(Collect.LOGTAG, t + "unable to quit CouchDB: " + e.toString());
                            e.printStackTrace();
                        }
                        
                        Collect.getInstance().stopService(new Intent(InformCouchService.class.getName()));
                    }
                    
                    finish();
                }
            });

            dialog = builder.create();            
            break;
           
        // Registered
        case DIALOG_UNABLE_TO_CONNECT_OFFLINE_ENABLED:
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_info)        
                .setTitle(R.string.tf_offline_mode_enabled)
                .setMessage(getString(R.string.tf_offline_mode_enabled_msg));

            builder.setPositiveButton(getText(R.string.tf_continue), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    restartActivity(false);        
                }
            });

            dialog = builder.create();       
            break;
            
        case DIALOG_UNABLE_TO_REGISTER:
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)        
                .setTitle(R.string.tf_unable_to_connect)
                .setMessage(getString(R.string.tf_connection_error_unregistered_msg));

            builder.setPositiveButton(getText(R.string.tf_retry), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    restartActivity(true);
                }
            });

            builder.setNegativeButton(getText(R.string.tf_exit_inform), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });

            dialog = builder.create();            
            break;       
        }
        
        return dialog;
    }
    
    public class InitializeApplicationTask extends AsyncTask<Object, Void, Void> 
    {
        private boolean pinged = false;
        private boolean registered = false;
        
        @Override
        protected Void doInBackground(Object... args)
        {  
            // Timer
            int seconds = 0;
            
            // Create necessary directories
            FileUtils.createFolder(FileUtils.EXTERNAL_FILES);
            FileUtils.createFolder(FileUtils.EXTERNAL_CACHE);
            
            // The InformOnlineService will perform ping and check-in immediately (no need to duplicate here)
            while (true) {
                // Either break out if we were successful in connecting or we have waited too long
                if ((Collect.getInstance().getIoService() instanceof InformOnlineService && 
                        Collect.getInstance().getIoService().isInitialized()) || seconds > 30) 
                    break;
                
                    /*
                     * If we have waited longer than 10 seconds we may need to start forcing the issue.
                     * 
                     * This might happen because we have restarted to retry the connection but the 
                     * services have not restarted and we are waiting on them to complete their usual
                     * 10 minute delay.
                     * 
                     * FIXME: this should check every 10 seconds at the 10 seconds mark and after...
                     */
                if (Collect.getInstance().getIoService() instanceof InformOnlineService && seconds > 10)
                    Collect.getInstance().getIoService().goOnline();
                
                try {
                    Thread.sleep(1000);
                    seconds++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            pinged = Collect.getInstance().getIoService().isRespondingToPings();
            registered = Collect.getInstance().getIoService().isRegistered();
            
            seconds = 0;
            
            if (registered) {
                if (!Collect.getInstance().getInformDependencies().isInitialized())
                    Collect.getInstance().setInformDependencies(new InformDependencies(getApplicationContext()));               

                if (CouchInstaller.checkInstalled() && CouchInitializer.isEnvironmentInitialized() == false)
                    CouchInitializer.initializeEnvironment(mProgressHandler);

                if (CouchInstaller.checkInstalled() && CouchInitializer.isEnvironmentInitialized()) {
                    // Start the database process
                    startService(new Intent(InformCouchService.class.getName()));

                    if (bindService(new Intent(InformCouchService.class.getName()), mCouchConnection, Context.BIND_AUTO_CREATE))
                        Log.d(Collect.LOGTAG, t + "successfully bound to InformCouchService");
                    else 
                        Log.e(Collect.LOGTAG, t + "unable to bind to InformCouchService");                

                    // Wait a reasonable period of time for the DB to start up
                    while (Collect.getInstance().getCouchService() == null) {
                        if (seconds > 30)
                            break;

                        try {
                            Thread.sleep(1000);
                            seconds++;                        
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return null;
        }
    
        @Override
        protected void onPreExecute()
        {
            if (CouchInstaller.checkInstalled() && CouchInitializer.isEnvironmentInitialized() == false) {
                mProgressDialog = new ProgressDialog(LauncherActivity.this);
                mProgressDialog.setCancelable(false);                
                mProgressDialog.setTitle(R.string.tf_database_being_initialized);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
            }
        }
    
        @Override
        protected void onPostExecute(Void nothing) 
        {            
            if (pinged) {
                if (registered) {
                    restartActivity(false);
                } else {
                    startActivity(new Intent(getApplicationContext(), ClientRegistrationActivity.class));
                    finish();
                }
            } else {
                if (registered) {
                    if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled())
                        showDialog(DIALOG_UNABLE_TO_CONNECT_OFFLINE_ENABLED);
                    else
                        showDialog(DIALOG_UNABLE_TO_CONNECT_OFFLINE_DISABLED);
                } else {
                    showDialog(DIALOG_UNABLE_TO_REGISTER);
                }
            }
        }
    }
    
    // Restart this activity, optionally requesting a complete restart
    private void restartActivity(boolean fullRestart)
    {
        Intent i = new Intent(getApplicationContext(), LauncherActivity.class);
        
        // If the user wants a full restart then request reinitialization of the IO service
        if (fullRestart)
            i.putExtra(KEY_REINIT_IOSERVICE, true);
        
        startActivity(i);
        finish();
    }

    private void displaySplash()
    {
        // Don't show the splash screen if this app appears to be registered
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(InformOnlineState.DEVICE_ID, null) instanceof String) {
            return;
        }
        
        // Fetch the splash screen Drawable
        Drawable image = null;
    
        try {
            // Attempt to load the configured default splash screen
            // The following code only works in 1.6+
            // BitmapDrawable bitImage = new BitmapDrawable(getResources(), FileUtils.SPLASH_SCREEN_FILE_PATH);
            BitmapDrawable bitImage = new BitmapDrawable(FileUtils.EXTERNAL_FILES + File.separator + FileUtils.SPLASH_SCREEN_FILE);
    
            if (bitImage.getBitmap() != null
                    && bitImage.getIntrinsicHeight() > 0
                    && bitImage.getIntrinsicWidth() > 0) {
                image = bitImage;
            }
        } catch (Exception e) {
            // TODO: log exception for debugging?
        }
    
        // TODO: rework
        if (image == null) {
            // no splash provided...
//            if (FileUtils.storageReady() && !((new File(FileUtils.DEFAULT_CONFIG_PATH)).exists())) {
                // Show the built-in splash image if the config directory 
                // does not exist. Otherwise, suppress the icon.
                image = getResources().getDrawable(R.drawable.gc_color_preview);
//            }
            
            if (image == null) 
                return;
        }
    
        // Create ImageView to hold the Drawable...
        ImageView view = new ImageView(getApplicationContext());
    
        // Initialise it with Drawable and full-screen layout parameters
        view.setImageDrawable(image);
        
        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();
        
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, 0);
        
        view.setLayoutParams(lp);
        view.setScaleType(ScaleType.CENTER);
        view.setBackgroundColor(Color.WHITE);
    
        // And wrap the image view in a frame layout so that the full-screen layout parameters are honoured
        FrameLayout layout = new FrameLayout(getApplicationContext());
        layout.addView(view);        

        // Create the toast and set the view to be that of the FrameLayout
        mSplashToast = Toast.makeText(getApplicationContext(), "splash screen", Toast.LENGTH_LONG);
        mSplashToast.setView(layout);
        mSplashToast.setGravity(Gravity.CENTER, 0, 0);
        mSplashToast.show();
    }
}