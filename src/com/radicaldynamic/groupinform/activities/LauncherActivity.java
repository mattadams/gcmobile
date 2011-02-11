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
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.couchone.couchdb.CouchInstallActivity;
import com.couchone.couchdb.CouchInstaller;
import com.couchone.libcouch.ICouchClient;
import com.couchone.libcouch.ICouchService;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.InformDependencies;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.utilities.CouchDbUtils;
import com.radicaldynamic.groupinform.utilities.FileUtils;

/**
 * Application initialization: registration, login, database installation & init 
 */
public class LauncherActivity extends Activity
{
    private static final String t = "LauncherActivity: ";
    
    // Intent status codes
    private static final String KEY_REINIT_IOSERVICE = "key_reinit_ioservice";
    private static final String KEY_SIGNIN_RESTART = "key_signin_restart";
    
    private static final int BROWSER_ACTIVITY = 1;

    private static boolean mShowSplash = true;
    private Toast mSplashToast;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    /*
     * Implement the callbacks that allow CouchDB to talk to this app
     * (not really necessary)
     */
    private ICouchClient mCallback = new ICouchClient.Stub() {
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
                Collect.getInstance().setCouchService(ICouchService.Stub.asInterface(service));
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
            
            if (Collect.getInstance().getInformOnlineState().getDefaultDatabase() instanceof String)
                Collect.getInstance().getDbService().open(Collect.getInstance().getInformOnlineState().getDefaultDatabase());
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
            
            case CouchInstallActivity.ERROR:
                AlertDialog.Builder builder = new AlertDialog.Builder(LauncherActivity.this);
                
                builder.setMessage(LauncherActivity.this.getString(R.string.couch_install_error))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { 
                        public void onClick(DialogInterface dialog, int id) { }
                    });
                
                AlertDialog alert = builder.create();
                alert.show();                
                break;

            case CouchInstallActivity.PROGRESS:
                if (msg.arg1 > 0)
                    mProgressDialog.setProgress(msg.arg1);                
                break;

            case CouchInstallActivity.COMPLETE:
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
        if (!FileUtils.storageReady()) {
            showErrorDialog(getString(R.string.no_sd_error), true);
        }

        Intent intent = getIntent();
        
        if (intent == null) {
            
        } else {
            if (intent.getBooleanExtra(KEY_REINIT_IOSERVICE, false)) {
                if (Collect.getInstance().getIoService() instanceof InformOnlineService)
                    Collect.getInstance().getIoService().reinitializeService();
                
                mShowSplash = false;
            }
            
            // Hide splash screen
            if (intent.getBooleanExtra(KEY_SIGNIN_RESTART, false))
                mShowSplash = false;
        }
        
        displaySplash();

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);       
        setContentView(R.layout.launcher);
                
        if (Collect.getInstance().getIoService() instanceof InformOnlineService && Collect.getInstance().getIoService().isReady()) {
            if (CouchInstaller.checkInstalled()) {
                startActivityForResult(new Intent(LauncherActivity.this, BrowserActivity.class), BROWSER_ACTIVITY);
            } else {                
                /*
                 * Install database engine
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
                            mProgressHandler.sendMessage(mProgressHandler.obtainMessage(CouchInstallActivity.ERROR));
                        }
                    }
                }.start();
            }
        } else {
            new InitializeApplicationTask().execute(getApplicationContext());
        }
        
        // Start the persistent online connection
        if (bindService(new Intent(LauncherActivity.this, InformOnlineService.class), mOnlineConnection, Context.BIND_AUTO_CREATE))
            Log.d(Collect.LOGTAG, t + "successfully bound to InformOnlineService");
        else 
            Log.e(Collect.LOGTAG, t + "unable to bind to InformOnlineService");
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
        // Close down our services
        if (Collect.getInstance().getCouchService() instanceof ICouchService) {
            try {
                Log.i(Collect.LOGTAG, t + "unbinding from CouchService");
                unbindService(mCouchConnection);                
                Log.d(Collect.LOGTAG, t + "unbound from CouchService");
            } catch (IllegalArgumentException e) {
                Log.w(Collect.LOGTAG, t + "CouchService not registered (was device reset?): " + e.toString());
            }
        }
        
        if (Collect.getInstance().getDbService() instanceof DatabaseService) {
            try {
                Log.i(Collect.LOGTAG, t + "unbinding from DatabaseService");
                unbindService(mDatabaseConnection);
                Log.d(Collect.LOGTAG, t + "unbound from DatabaseService");
            } catch (IllegalArgumentException e) { 
                Log.w(Collect.LOGTAG, t + "DatabaseService not registered: " + e.toString());    
            }
        }
        
        if (Collect.getInstance().getIoService() instanceof InformOnlineService) {
            Log.i(Collect.LOGTAG, t + "unbinding from to InformOnlineService");
            unbindService(mOnlineConnection);
        }
        
        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        // Dismiss any dialogs that might be showing
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (Collect.getInstance().getIoService() instanceof InformOnlineService && Collect.getInstance().getIoService().isReady()) {
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized()) {                 
                // Inform user about dependencies
                if (!Collect.getInstance().getInformDependencies().allSatisfied()) {
                    if (Collect.getInstance().getInformDependencies().isReminderEnabled()) {
                        showDependencyDialog();
                    }
                }
            }
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        
        // Re-enable the splash screen
        mShowSplash = true;
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
    
    public class InitializeApplicationTask extends AsyncTask<Object, Void, Void> 
    {
        private boolean mPinged = false;
        private boolean mRegistered = false;
        
        @Override
        protected Void doInBackground(Object... args)
        {  
            // Timer
            int seconds = 0;
            
            // Create necessary directories
            FileUtils.createFolder(FileUtils.EXTERNAL_FILES);
            FileUtils.createFolder(FileUtils.EXTERNAL_CACHE);
            
            // Initialize CouchDB & Erlang in internal storage if this has not already been done
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized() == false)
                CouchDbUtils.initializeEnvironment(mProgressHandler);
            
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized()) { 
                startService(new Intent(ICouchService.class.getName()));
                bindService(new Intent(ICouchService.class.getName()), mCouchConnection, Context.BIND_AUTO_CREATE);
                
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
            
            seconds = 0;
            
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
            
            // Start the database connection
            if (bindService(new Intent(LauncherActivity.this, DatabaseService.class), mDatabaseConnection, Context.BIND_AUTO_CREATE))
                Log.d(Collect.LOGTAG, t + "successfully bound to DatabaseService");
            else 
                Log.e(Collect.LOGTAG, t + "unable to bind to DatabaseService");
            
            mPinged = Collect.getInstance().getIoService().isRespondingToPings();
            mRegistered = Collect.getInstance().getIoService().isRegistered();
            
            // Initialize list of dependencies
            if (!Collect.getInstance().getInformDependencies().isInitialized())
                Collect.getInstance().setInformDependencies(new InformDependencies(getApplicationContext()));
               
            return null;
        }
    
        @Override
        protected void onPreExecute()
        {
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized() == false) {
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
            if (mPinged) {
                if (mRegistered) {
                    restartActivity(false);
                } else {
                    Intent i = new Intent(getApplicationContext(), ClientRegistrationActivity.class);
                    startActivity(i);
                    finish();
                }
            } else {
                if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled()) {
                    // TODO: Let the user know that offline mode is enabled and then proceed to (offline) interaction with the app
                    restartActivity(false);
                } else {
                    showConnectionErrorDialog(mRegistered);
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
        else 
            i.putExtra(KEY_SIGNIN_RESTART, true);
        
        startActivity(i);
        finish();
    }
    
    /*
     * An initial connection error should be handled differently depending on
     * a) whether this device has already been registered, and
     * b) whether this device has a local (and properly initialized) CouchDB installation
     */
    private void showConnectionErrorDialog(boolean registered)
    {
        mAlertDialog = new AlertDialog.Builder(this).create();

        mAlertDialog.setCancelable(false);
        mAlertDialog.setIcon(R.drawable.ic_dialog_alert);        
        mAlertDialog.setTitle(R.string.tf_connection_error);

        if (registered) 
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized())
                mAlertDialog.setMessage(getString(R.string.tf_connection_error_registered_with_db_msg));
            else    
                mAlertDialog.setMessage(getString(R.string.tf_connection_error_registered_without_db_msg));
        else 
            mAlertDialog.setMessage(getString(R.string.tf_connection_error_unregistered_msg));

        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.tf_retry), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                restartActivity(true);
            }
        });

        if (registered && CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized()) {
            mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getText(R.string.tf_go_offline), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    restartActivity(false);
                }
            });    
        }

        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.tf_exit_inform), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });

        mAlertDialog.show();
    }
    
    private void showDependencyDialog()
    {
        /*
         * Since showDependencyDialog essentially enters a loop once unsatisfied dependencies
         * are found we need a way to get out once all dependencies have been satisfied or
         * after the user has skipped installation.  This is our out.  
         */        
        if (Collect.getInstance().getInformDependencies().getNextDependency() == null)
            return;
        
        String copy = getString(R.string.tf_unavailable);
        
        if (Collect.getInstance().getInformDependencies().getNextDependency().equals(InformDependencies.BARCODE))
            copy = getString(R.string.com_google_zxing_client_android);
        else if (Collect.getInstance().getInformDependencies().getNextDependency().equals(InformDependencies.COUCHDB))
            copy = getString(R.string.org_couchdb_android);
        
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.setIcon(R.drawable.ic_dialog_info);
        mAlertDialog.setTitle(R.string.tf_dependency_missing_dialog_title);
        mAlertDialog.setMessage(getString(R.string.tf_dependency_missing_dialog_msg) + copy);
        
        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.tf_install), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // The install may not be successful but at this point we just pretend that it was
                Collect.getInstance().getInformDependencies().getDependencies().put(
                        Collect.getInstance().getInformDependencies().getNextDependency(), 1);
                
                String uri = "market://details?id=" + Collect.getInstance().getInformDependencies().getNextDependency();
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(i);
            }
        });
        
        mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getText(R.string.tf_remind_later), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {          
                // This doesn't permanently mark the dependency as installed but it allows us to skip to the next one
                Collect.getInstance().getInformDependencies().getDependencies().put(
                        Collect.getInstance().getInformDependencies().getNextDependency(), 1);
                
                showDependencyDialog();
                dialog.cancel();
            }
        });
        
        mAlertDialog.show();
    }

    private void showErrorDialog(String errorMsg, final boolean shouldExit)
    {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.setIcon(R.drawable.ic_dialog_alert);
        mAlertDialog.setMessage(errorMsg);

        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i)
            {
                switch (i) {
                case DialogInterface.BUTTON1:
                    if (shouldExit) {
                        finish();
                    }

                    break;
                }
            }
        };

        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }
    
    /**
     * displaySplash
     * 
     * Shows the splash screen if the mShowSplash member variable is true.
     * Otherwise a no-op.
     */
    void displaySplash()
    {
        if (!mShowSplash)
            return;
    
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