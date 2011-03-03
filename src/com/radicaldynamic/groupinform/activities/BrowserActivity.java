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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ektorp.DbAccessException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.couchone.couchdb.CouchInstaller;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.BrowserListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDocument;
import com.radicaldynamic.groupinform.documents.InstanceDocument;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.repository.FormRepository;
import com.radicaldynamic.groupinform.repository.InstanceRepository;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.utilities.CouchDbUtils;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class BrowserActivity extends ListActivity
{
    private static final String t = "BrowserActivity: ";
    
    // Dialog status codes
    private static final int DIALOG_FOLDER_UNAVAILABLE = 1;
    private static final int DIALOG_OFFLINE_MODE_UNAVAILABLE_DB = 2;
    private static final int DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS = 3;    
    private static final int DIALOG_ONLINE_STATE_CHANGING = 4;
    private static final int DIALOG_TOGGLE_ONLINE_STATE = 5;
    private static final int DIALOG_OFFLINE_ATTEMPT_FAILED = 6;
    private static final int DIALOG_ONLINE_ATTEMPT_FAILED = 7;
        
    // Request codes for returning data from specified intent 
    private static final int RESULT_ABOUT_INFORM = 1;
    
    // See s1...OnItemSelectedListener() where this is used in a horrid workaround
    private boolean mSpinnerInit = false;
    
    // Custom message consumed by onCreateDialog()
    private String mDialogMessage = null;
    
    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);       
        setContentView(R.layout.browser);                

        // Load our custom window title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.folder_selector_title);

        // Initiate and populate spinner to filter forms displayed by instances types
        ArrayAdapter<CharSequence> instanceStatus = ArrayAdapter
        .createFromResource(this, R.array.tf_main_menu_form_filters, android.R.layout.simple_spinner_item);        
        instanceStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner s1 = (Spinner) findViewById(R.id.form_filter);
        s1.setAdapter(instanceStatus);
        s1.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                /*
                 * Probably an implementation bug, the listener erroneously is called during layout.
                 * Since this listener in effect triggers an Ektorp repository and this repository
                 * in turn creates Couch views and having the repository initiated twice within the same
                 * thread will cause a segfault we had to implement this little workaround to ensure
                 * that loadScreen() is not called twice.
                 * 
                 * See https://groups.google.com/group/android-developers/browse_thread/thread/d93ce1ef583a2a29
                 * and http://stackoverflow.com/questions/2562248/android-how-to-keep-onitemselected-from-firing-off-on-a-newly-instantiated-spinn
                 * for more on this disgusting issue. 
                 */
                if (mSpinnerInit == false)
                    mSpinnerInit = true;
                else
                    loadScreen();
            }

            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Set up listener for Folder Selector button in title
        Button b1 = (Button) findViewById(R.id.folderTitleButton);
        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
            }
        });

        // Set up listener for Online Status button in title
        Button b2 = (Button) findViewById(R.id.onlineStatusTitleButton);
        b2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showDialog(DIALOG_TOGGLE_ONLINE_STATE);
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        // "Exit" if the user resets Inform
        case RESULT_ABOUT_INFORM:
            setResult(RESULT_OK);
            finish();
            break; 
        }        
    }
    
    public Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        
        switch (id) {
        // Couldn't connect to DB (for a specific reason)
        case DIALOG_FOLDER_UNAVAILABLE:
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_folder_unavailable)
                .setMessage(mDialogMessage);
            
            builder.setPositiveButton(getString(R.string.tf_form_folders), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
                    removeDialog(DIALOG_FOLDER_UNAVAILABLE);
                }
            });
            
            if (!Collect.getInstance().getIoService().isSignedIn()) {
                builder.setNeutralButton(getString(R.string.tf_go_online), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new ToggleOnlineStateTask().execute();
                        removeDialog(DIALOG_FOLDER_UNAVAILABLE);
                    }
                });
            }
            
            dialog = builder.create();
            break;            
            
        // We can't go offline (CouchDB not installed or not available locally)
        case DIALOG_OFFLINE_MODE_UNAVAILABLE_DB:
            builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_unable_to_go_offline_dialog)
                .setMessage(R.string.tf_unable_to_go_offline_dialog_msg_reason_db);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
                    dialog.cancel();
                }
            });
            
            dialog = builder.create();
            break;            
            
        // We can't go offline (user has not selected any databases to be replicated)
        case DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_go_offline_dialog)
            .setMessage(R.string.tf_unable_to_go_offline_dialog_msg_reason_folders);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });

            dialog = builder.create();
            
            break;

        // Simple progress dialog for online/offline
        case DIALOG_ONLINE_STATE_CHANGING:
            if (Collect.getInstance().getIoService().isSignedIn())
                dialog = ProgressDialog.show(this, "", getText(R.string.tf_inform_state_disconnecting));
            else
                dialog = ProgressDialog.show(this, "", getText(R.string.tf_inform_state_connecting));
            
            break;
        
        // Prompt user to connect/disconnect
        case DIALOG_TOGGLE_ONLINE_STATE:
            String buttonText;
            
            builder
                .setCancelable(false) 
                .setIcon(R.drawable.ic_dialog_info);
            
            if (Collect.getInstance().getIoService().isSignedIn()) {
                builder
                    .setTitle(getText(R.string.tf_go_offline) + "?")
                    .setMessage(R.string.tf_go_offline_dialog_msg);
                
                buttonText = getText(R.string.tf_go_offline).toString();
            } else {
                builder
                    .setTitle(getText(R.string.tf_go_online) + "?")
                    .setMessage(R.string.tf_go_online_dialog_msg);

                buttonText = getText(R.string.tf_go_online).toString();
            }

            builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new ToggleOnlineStateTask().execute();
                    removeDialog(DIALOG_TOGGLE_ONLINE_STATE);
                }
            });

            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_TOGGLE_ONLINE_STATE);
                }
            });
            
            dialog = builder.create();
            break;
            
        // Tried going offline but couldn't
        case DIALOG_OFFLINE_ATTEMPT_FAILED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_go_offline_dialog)
            .setMessage(R.string.tf_unable_to_go_offline_dialog_msg_reason_generic);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    loadScreen();
                    dialog.cancel();
                }
            });

            dialog = builder.create();
            break;            
            
        // Tried going online but couldn't
        case DIALOG_ONLINE_ATTEMPT_FAILED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_go_online_dialog)
            .setMessage(R.string.tf_unable_to_go_online_dialog_msg_reason_generic);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    loadScreen();
                    dialog.cancel();
                }
            });

            dialog = builder.create();
            break;
            
        default:
            Log.e(Collect.LOGTAG, t + "showDialog() unimplemented for " + id);
        }
        
        return dialog;        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_browser_options, menu);
        return true;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                setResult(RESULT_OK);
                finish();
        }

        return super.onKeyDown(keyCode, event);
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
            loadScreen();
            break;
        case R.id.tf_aggregate:
            startActivity(new Intent(this, InstanceUploaderList.class));
            return true;
        case R.id.tf_manage:
            startActivity(new Intent(this, ManageFormsTabs.class));
            return true;
        case R.id.tf_info:
            startActivityForResult(new Intent(this, ClientInformationActivity.class), RESULT_ABOUT_INFORM);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            try {
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
            } catch (DbAccessException e) {
                Log.w(Collect.LOGTAG, t + "database access refused: " + e.toString());
            } catch (Exception e) {
                Log.e(Collect.LOGTAG, t + "unhandled exception: " + e.toString());
                e.printStackTrace();
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
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
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
                    R.layout.browser_list_item, 
                    documents,
                    instanceTallies,
                    (Spinner) findViewById(R.id.form_filter));
            
            setListAdapter(adapter);

            if (status == InstanceDocument.Status.nothing) {
                // Provide hints to user
                if (documents.isEmpty()) {
                    TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                    nothingToDisplay.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_add_form_hint), Toast.LENGTH_LONG).show();
                    openOptionsMenu();
                } else {
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
     * 
     * TODO
     * 
     * Deal with case where user has asked to go offline/online but it could not be done co-operatively
     */
    private class ToggleOnlineStateTask extends AsyncTask<Void, Void, Void>
    {        
        Boolean hasReplicatedFolders = false;
        Boolean missingCouch = false;
        Boolean missingSynchronizedFolders = false;
        Boolean unableToGoOffline = false;
        Boolean unableToGoOnline = false;
        
        ProgressDialog progressDialog = null;        
        
        final Handler progressHandler = new Handler() {
            public void handleMessage(Message msg) {
                progressDialog.setMessage(getString(R.string.tf_synchronizing_folder_count_dialog_msg, msg.arg1, msg.arg2));
            }
        };
        
        @Override
        protected Void doInBackground(Void... nothing)
        {
            // TODO? Perform checkin on demand -- this gives us the most accurate state 
            // Or maybe just again when the app starts up/is shown
            
            if (Collect.getInstance().getIoService().isSignedIn()) {
                if (hasReplicatedFolders) {
                    // Only attempt to synchronize if we can reasonably do so
                    if (Collect.getInstance().getIoService().isSignedIn())
                        synchronize();                      
                    
                    if (!Collect.getInstance().getIoService().goOffline())
                        unableToGoOffline = true;
                } else {
                    missingSynchronizedFolders = true;
                }
            } else {
                if (Collect.getInstance().getIoService().goOnline()) {                
                    if (hasReplicatedFolders)
                        synchronize();
                } else
                    unableToGoOnline = true;
            }

            return null;
        }

        @Override
        protected void onPreExecute()
        {          
            if (CouchInstaller.checkInstalled() && CouchDbUtils.isEnvironmentInitialized()) {
                hasReplicatedFolders = Collect.getInstance().getInformOnlineState().hasReplicatedFolders();
            } else {
                missingCouch = true;
            }
            
            if (hasReplicatedFolders) {
                progressDialog = new ProgressDialog(BrowserActivity.this);
                progressDialog.setMessage(getString(R.string.tf_synchronizing_folders_dialog_msg));  
                progressDialog.show();
            } else
                showDialog(DIALOG_ONLINE_STATE_CHANGING);
            
            // Not available while toggling
            Button b1 = (Button) findViewById(R.id.onlineStatusTitleButton);
            b1.setEnabled(false);
            b1.setText(R.string.tf_inform_state_transition);
            
            Button b2 = (Button) findViewById(R.id.folderTitleButton);
            b2.setEnabled(false);
            b2.setText("...");
        }

        @Override
        protected void onPostExecute(Void nothing)
        {   
            if (progressDialog == null)
                removeDialog(DIALOG_ONLINE_STATE_CHANGING);
            else
                progressDialog.cancel();
            
            if (missingCouch) {
                showDialog(DIALOG_OFFLINE_MODE_UNAVAILABLE_DB);
                loadScreen();
            } else if (missingSynchronizedFolders) {
                showDialog(DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS);
                loadScreen();
            } else if (unableToGoOffline) {
                // Load screen after user acknowledges to avoid stacking of dialogs
                showDialog(DIALOG_OFFLINE_ATTEMPT_FAILED);
            } else if (unableToGoOnline) {
                // Load screen after user acknowledges to avoid stacking of dialogs
                showDialog(DIALOG_ONLINE_ATTEMPT_FAILED);
            } else 
                loadScreen();
        }
        
        private void synchronize()
        {
            Set<String> folderSet = Collect.getInstance().getInformOnlineState().getAccountFolders().keySet();
            Iterator<String> folderIds = folderSet.iterator();
            
            int progress = 0;
            int total = 0;
            
            // Figure out how many folders are marked for replication
            while (folderIds.hasNext()) {
                AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(folderIds.next());
                
                if (folder.isReplicated())
                    total++;
            }
            
            // Reset iterator
            folderIds = folderSet.iterator();    
                
            while (folderIds.hasNext()) {
                AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(folderIds.next());                
                
                if (folder.isReplicated()) {
                    Log.i(Collect.LOGTAG, t + "about to begin triggered replication of " + folder.getName());
                    
                    // Update progress dialog
                    Message msg = progressHandler.obtainMessage();
                    msg.arg1 = ++progress;
                    msg.arg2 = total;
                    progressHandler.sendMessage(msg);
                    
                    try {                        
                        Collect.getInstance().getDbService().replicate(folder.getId(), DatabaseService.REPLICATE_PUSH);
                        Collect.getInstance().getDbService().replicate(folder.getId(), DatabaseService.REPLICATE_PULL);
                    } catch (Exception e) {
                        Log.w(Collect.LOGTAG, t + "problem replicating " + folder.getId() + ": " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Load the various elements of the screen that must wait for other tasks to complete
     */
    private void loadScreen()
    {
        // Reflect the online/offline status (may be disabled thanks to toggling state)
        Button b1 = (Button) findViewById(R.id.onlineStatusTitleButton);
        b1.setEnabled(true);

        if (Collect.getInstance().getIoService().isSignedIn())
            b1.setText(getText(R.string.tf_inform_state_online));
        else
            b1.setText(getText(R.string.tf_inform_state_offline));
        
        // Re-enable (may be disabled thanks to toggling state)
        Button b2 = (Button) findViewById(R.id.folderTitleButton);
        b2.setEnabled(true);

        // Spinner must reflect results of refresh view below
        Spinner s1 = (Spinner) findViewById(R.id.form_filter);        
        triggerRefresh(s1.getSelectedItemPosition());
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

    private void triggerRefresh(int position)
    {
        // Hide "nothing to display" message
        TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
        nothingToDisplay.setVisibility(View.INVISIBLE);
        
        String folderName = "...";
        
        // Open selected database
        try {            
            // Reflect the currently selected folder
            Button b2 = (Button) findViewById(R.id.folderTitleButton);
            
            try {
                folderName = Collect
                        .getInstance()
                        .getInformOnlineState()
                        .getAccountFolders()
                        .get(Collect.getInstance().getInformOnlineState().getSelectedDatabase())
                        .getName();
                
                // Shorten names that are too long
                if (folderName.length() > 23) 
                    folderName = folderName.substring(0, 20) + "...";
            } catch (NullPointerException e) {
                // Database metadata is not available at this time
                Log.w(Collect.LOGTAG, t + "folder metadata not available at this time");
            } finally {
                b2.setText(folderName);
            }
            
            Collect.getInstance().getDbService().open(Collect.getInstance().getInformOnlineState().getSelectedDatabase());
        
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
        } catch (DatabaseService.DbUnavailableDueToMetadataException e) {            
            mDialogMessage = getString(R.string.tf_unable_to_open_folder_missing_metadata);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);
        } catch (DatabaseService.DbUnavailableWhileOfflineException e) {
            mDialogMessage = getString(R.string.tf_unable_to_open_folder_while_offline, folderName);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);
        } catch (DatabaseService.DbUnavailableException e) {
            mDialogMessage = getString(R.string.tf_unable_to_open_folder, folderName);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);
        }
    }
}