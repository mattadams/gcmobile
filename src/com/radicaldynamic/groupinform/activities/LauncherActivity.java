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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
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
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView.ScaleType;

import com.couchone.couchdb.CouchInstallActivity;
import com.couchone.couchdb.CouchInstaller;
import com.couchone.libcouch.ICouchClient;
import com.couchone.libcouch.ICouchService;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.BrowserListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDocument;
import com.radicaldynamic.groupinform.documents.InstanceDocument;
import com.radicaldynamic.groupinform.logic.InformDependencies;
import com.radicaldynamic.groupinform.repository.FormRepository;
import com.radicaldynamic.groupinform.repository.InstanceRepository;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.utilities.CouchDbUtils;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;
import com.radicaldynamic.groupinform.utilities.FileUtils;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class LauncherActivity extends ListActivity
{
    private static final String t = "LauncherActivity: ";
    
    // Request codes for returning data from specified intent 
    private static final int ABOUT_INFORM = 1;
    
    // Intent status codes
    private static final String KEY_REINIT_IOSERVICE = "key_reinit_ioservice";
    private static final String KEY_SIGNIN_RESTART = "key_signin_restart";

    private static boolean mShowSplash = true;
    private Toast mSplashToast;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private RefreshViewTask mRefreshViewTask;
    
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
            Collect.getInstance().getDbService().open();
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
        setContentView(R.layout.main_browser);
                
        if (Collect.getInstance().getIoService() instanceof InformOnlineService && Collect.getInstance().getIoService().isReady()) {
            if (CouchInstaller.checkInstalled()) {                
                // Load our custom window title
                getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.folder_selector_title);            

                // We don't use the on-screen progress indicator no longer needed
                RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
                onscreenProgress.setVisibility(View.GONE);        
                
                // Initiate and populate spinner to filter forms displayed by instances types
                ArrayAdapter<CharSequence> instanceStatus = ArrayAdapter
                    .createFromResource(this, R.array.tf_main_menu_form_filters, android.R.layout.simple_spinner_item);        
                instanceStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                
                s1.setVisibility(View.VISIBLE);
                s1.setAdapter(instanceStatus);
                s1.setOnItemSelectedListener(new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                    {
                        triggerRefresh(position);
                    }

                    public void onNothingSelected(AdapterView<?> parent) { }
                });
                
                // Set up listener for Folder Selector button in title
                Button b1 = (Button) findViewById(R.id.folderTitleButton);
                b1.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        startActivity(new Intent(LauncherActivity.this, AccountFolderList.class));
                    }
                });
                
                // Set up listener for Online Status button in title
                Button b2 = (Button) findViewById(R.id.onlineStatusTitleButton);
                b2.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        showToggleOnlineStateDialog();
                    }
                });      
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
        
        // Start the database connection
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
            Log.i(Collect.LOGTAG, t + "unbinding from DatabaseService");
            unbindService(mDatabaseConnection);
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
                
                // Load screen (but only if we're connected to Couch)
                if (Collect.getInstance().getCouchService() != null)
                    loadScreen();
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
        // "Exit" if the user resets Inform
        case ABOUT_INFORM:
            finish();
            break; 
        }        
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_browser_context, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_browser_options, menu);
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        // AdapterContextMenuInfo info = (AdapterContextMenuInfo)
        // item.getMenuInfo();
        switch (item.getItemId()) {
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        FormDocument form = (FormDocument) getListAdapter().getItem(position);
        InstanceLoadPathTask ilp;

        Log.d(Collect.LOGTAG, t + "selected form " + form.getId() + " from list");

        Spinner s1 = (Spinner) findViewById(R.id.form_filter);

        switch (s1.getSelectedItemPosition()) {
        // Show all forms (in folder)
        case 0:
            Intent i = new Intent("com.radicaldynamic.groupinform.action.FormEntry");
            i.putStringArrayListExtra(FormEntryActivity.KEY_INSTANCES, new ArrayList<String>());
            i.putExtra(FormEntryActivity.KEY_FORMID, form.getId());
            startActivity(i);
            break;
        // Show all draft forms
        case 1:
            ilp = new InstanceLoadPathTask();
            ilp.execute(form.getId(), InstanceDocument.Status.draft);
            break;
        // Show all completed forms
        case 2:
            ilp = new InstanceLoadPathTask();
            ilp.execute(form.getId(), InstanceDocument.Status.complete);
            break;
        // Show all unread forms (e.g., those added or updated by others)
        case 3:
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.tf_folders:
            startActivity(new Intent(this, AccountFolderList.class));
            break;
        case R.id.tf_refresh:
            Spinner s1 = (Spinner) findViewById(R.id.form_filter);        
            triggerRefresh(s1.getSelectedItemPosition());
            break;
        case R.id.tf_aggregate:
            startActivity(new Intent(this, InstanceUploaderList.class));
            return true;
        case R.id.tf_manage:
            startActivity(new Intent(this, ManageFormsTabs.class));
            return true;
        case R.id.tf_info:
            startActivityForResult(new Intent(this, ClientInformationActivity.class), ABOUT_INFORM);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
          
            setProgressVisibility(true);
        }
    
        @Override
        protected void onPostExecute(Void nothing) 
        {                        
            setProgressVisibility(false);
            
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

    /*
     * Determine how to load a form instance
     * 
     * If there is only one instance for the form in question then load that
     * instance directly. If there is more than one instance then load the
     * instance browser.
     */
    private class InstanceLoadPathTask extends AsyncTask<Object, Integer, Void>
    {
        String mFormId;
        ArrayList<String> mInstanceIds = new ArrayList<String>();

        @Override
        protected Void doInBackground(Object... params)
        {
            mFormId = (String) params[0];
            InstanceDocument.Status status = (InstanceDocument.Status) params[1];

            mInstanceIds = new InstanceRepository(Collect.getInstance().getDbService().getDb()).findByFormAndStatus(mFormId, status);
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            setProgressVisibility(true);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            Intent i = new Intent("com.radicaldynamic.groupinform.action.FormEntry");
            i.putStringArrayListExtra(FormEntryActivity.KEY_INSTANCES, mInstanceIds);
            i.putExtra(FormEntryActivity.KEY_INSTANCEID, mInstanceIds.get(0));
            i.putExtra(FormEntryActivity.KEY_FORMID, mFormId);            
            startActivity(i);

            setProgressVisibility(false);
        }
    }
    
    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<InstanceDocument.Status, Integer, InstanceDocument.Status>
    {
        private ArrayList<FormDocument> documents = new ArrayList<FormDocument>();
        private Map<String, String> instanceTallies = new HashMap<String, String>();

        @Override
        protected InstanceDocument.Status doInBackground(InstanceDocument.Status... status)
        {
            if (status[0] == InstanceDocument.Status.nothing) {
                try {
                    documents = (ArrayList<FormDocument>) new FormRepository(Collect.getInstance().getDbService().getDb()).getAll();
                    DocumentUtils.sortByName(documents);
                } catch (ClassCastException e) {
                    // TODO: is there a better way to handle empty lists?
                }
            } else {
                instanceTallies = new FormRepository(Collect.getInstance().getDbService().getDb()).getFormsByInstanceStatus(status[0]);
                
                if (!instanceTallies.isEmpty()) {
                    documents = (ArrayList<FormDocument>) new FormRepository(Collect.getInstance().getDbService().getDb()).getAllByKeys(new ArrayList<Object>(instanceTallies.keySet()));                    
                    DocumentUtils.sortByName(documents);
                }
            }

            return status[0];
        }

        @Override
        protected void onPreExecute()
        {
            setProgressVisibility(true);
        }

        @Override
        protected void onPostExecute(InstanceDocument.Status status)
        {
            /*
             * Special hack to ensure that our application doesn't crash if we terminate it
             * before the AsyncTask has finished running.  This is stupid and I don't know
             * another way around it.
             * 
             * See http://dimitar.me/android-displaying-dialogs-from-background-threads/
             */
            if (isFinishing())
                return;
            
            BrowserListAdapter adapter = new BrowserListAdapter(
                    getApplicationContext(),
                    R.layout.main_browser_list_item, 
                    documents,
                    instanceTallies, 
                    (Spinner) findViewById(R.id.form_filter));
            
            setListAdapter(adapter);

            if (status == InstanceDocument.Status.nothing) {
                // Provide hints to user
                if (documents.isEmpty()) {
                    TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                    nothingToDisplay.setVisibility(View.VISIBLE);
                    
                    // TODO: try and reintegrate this
//                  Toast.makeText(getApplicationContext(), getString(R.string.tf_add_form_hint), Toast.LENGTH_LONG).show();
//                  openOptionsMenu();
                } else {
                    if (mAlertDialog != null && !mAlertDialog.isShowing())
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_begin_instance_hint), Toast.LENGTH_SHORT).show();
                }
            } else {
                Spinner s1 = (Spinner) findViewById(R.id.form_filter);
                String descriptor = s1.getSelectedItem().toString().toLowerCase();

                // Provide hints to user
                if (documents.isEmpty()) {
                    TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                    nothingToDisplay.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_browse_instances_hint, descriptor), Toast.LENGTH_SHORT).show();
                }
            }

            setProgressVisibility(false);
        }
    }
    
    /*
     * TODO 
     * 
     * Implement progress dialog that will be updated to show the online/offline switch progress
     * (i.e., progress of folder synchronisations)
     */
    private class ToggleOnlineState extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... nothing)
        {
            if (Collect.getInstance().getIoService().isSignedIn()) {
                // TODO: deal with case where user has asked to go offline but it could not be done co-operatively
                // Inform user and prompt them to retry or to force offline
                Collect.getInstance().getIoService().goOffline();
            } else {
                Collect.getInstance().getIoService().goOnline();                    
            }

            return null;
        }

        @Override
        protected void onPreExecute()
        {

        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            loadScreen();
        }
    }

    /**
     * Load the various elements of the screen that must wait for other tasks to
     * complete
     */
    private void loadScreen()
    {
        // Reflect the online/offline status
        Button b1 = (Button) findViewById(R.id.onlineStatusTitleButton);

        if (Collect.getInstance().getIoService().isSignedIn())
            b1.setText(getText(R.string.tf_inform_state_online));
        else
            b1.setText(getText(R.string.tf_inform_state_offline));
        
        // Spinner must reflect results of refresh view below
        Spinner s1 = (Spinner) findViewById(R.id.form_filter);        
        triggerRefresh(s1.getSelectedItemPosition());
           
        registerForContextMenu(getListView());
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
    
    private void setProgressVisibility(boolean visible)
    {
        ProgressBar pb = (ProgressBar) getWindow().findViewById(R.id.titleProgressBar);
        
        if (pb != null) {
            if (visible) {
                pb.setVisibility(View.VISIBLE);
            } else {
                pb.setVisibility(View.GONE);
            }
        }
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
    
    private void showToggleOnlineStateDialog()
    {
        String buttonText;
        
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.setIcon(R.drawable.ic_dialog_info);
        
        if (Collect.getInstance().getIoService().isSignedIn()) {
            mAlertDialog.setTitle(getText(R.string.tf_go_offline) + "?");
            mAlertDialog.setMessage("You are currently online.  Group Inform will synchronize any folders that you have selected for offline use prior to going offline.");
            buttonText = getText(R.string.tf_go_offline).toString();
        } else {
            mAlertDialog.setTitle(getText(R.string.tf_go_online) + "?");
            mAlertDialog.setMessage("You are currently offline.");
            buttonText = getText(R.string.tf_go_online).toString();
        }

        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, buttonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new ToggleOnlineState().execute();
            }
        });

        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        mAlertDialog.show();
    }

    private void triggerRefresh(int position)
    {
        // Hide "nothing to display" message
        TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
        nothingToDisplay.setVisibility(View.INVISIBLE);

        mRefreshViewTask = new RefreshViewTask();

        switch (position) {
        // Show all forms (in folder)
        case 0:
            mRefreshViewTask.execute(InstanceDocument.Status.nothing);
            break;
        // Show all draft forms
        case 1:
            mRefreshViewTask.execute(InstanceDocument.Status.draft);
            break;
        // Show all completed forms
        case 2:
            mRefreshViewTask.execute(InstanceDocument.Status.complete);
            break;
        // Show all unread forms (e.g., those added or updated by others)
        case 3:
            mRefreshViewTask.execute(InstanceDocument.Status.updated);
            break;
        }   
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
