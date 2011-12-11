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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.AccountFolderListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.SynchronizeFoldersListener;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.tasks.SynchronizeFoldersTask;
import com.radicaldynamic.groupinform.utilities.HttpUtils;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

/*
 * Interface used to switch between folders and select destination
 * folders to copy and/or move forms and/or data to.
 */
public class AccountFolderList extends ListActivity implements SynchronizeFoldersListener
{
    private static final String t = "AccountFolderList: ";

    private static final int MENU_ADD          = Menu.FIRST;
    private static final int MENU_SYNC_LIST    = Menu.FIRST + 1;
    
    private static final int CONTEXT_MENU_EDIT = Menu.FIRST;
    private static final int CONTEXT_MENU_INFO = Menu.FIRST + 1;
    
    public static final int DIALOG_DENIED_NOT_OWNER = 1;
    public static final int DIALOG_MORE_INFO        = 2;
    
    // Intent keys
    public static final String KEY_COPY_TO_FOLDER = "key_copytofolder";
    public static final String KEY_FOLDER_ID      = "key_folderid";
    public static final String KEY_FOLDER_NAME    = "key_foldername";    
    
    private RefreshViewTask mRefreshViewTask;
    private SynchronizeFoldersTask mSynchronizeFoldersTask;
    
    private AccountFolder mFolder;
    private String mSelectedFolderId;
    private String mSelectedFolderName;
    private boolean mCopyToFolder = false;
    
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.generic_list);
        
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            
            if (intent == null) {
                // Load defaults
            } else {
                mCopyToFolder = intent.getBooleanExtra(KEY_COPY_TO_FOLDER, false);
            }
        } else {
            if (savedInstanceState.containsKey(KEY_COPY_TO_FOLDER))
                mCopyToFolder = savedInstanceState.getBoolean(KEY_COPY_TO_FOLDER);
            
            if (savedInstanceState.containsKey(KEY_FOLDER_ID))
                mSelectedFolderId = savedInstanceState.getString(KEY_FOLDER_ID);
            
            if (savedInstanceState.containsKey(KEY_FOLDER_NAME))
                mSelectedFolderName = savedInstanceState.getString(KEY_FOLDER_NAME);
            
            Object data = getLastNonConfigurationInstance();
            
            if (data instanceof RefreshViewTask)
                mRefreshViewTask = (RefreshViewTask) data;
            else if (data instanceof SynchronizeFoldersTask)
                mSynchronizeFoldersTask = (SynchronizeFoldersTask) data;
        }
        
        if (mCopyToFolder)
            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_copy_to_folder));
        else 
            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_form_folders));
    }
    
    @Override
    protected void onDestroy() 
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        
        // Clean up folder synchronization task
        if (mSynchronizeFoldersTask != null) {
            mSynchronizeFoldersTask.setListener(null);
            
            if (mSynchronizeFoldersTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSynchronizeFoldersTask.cancel(true);
            }
        }
        
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();        
        
        // Handle resume of folder synchronization task
        if (mSynchronizeFoldersTask != null) {
            mSynchronizeFoldersTask.setListener(this);
            
            if (mSynchronizeFoldersTask != null) {
                if (mSynchronizeFoldersTask.getStatus() == AsyncTask.Status.RUNNING) {
                    synchronizationHandler(null);
                } else if (mSynchronizeFoldersTask.getStatus() == AsyncTask.Status.FINISHED) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                }
            }
        }
        
        loadScreen();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        boolean enabled = false;
        boolean visible = true;
        
        if (Collect.getInstance().getIoService().isSignedIn())
            enabled = true;
        
        if (mCopyToFolder)
            visible = false;
        
        if (!Collect.getInstance().getInformOnlineState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY)) {
            menu.add(0, CONTEXT_MENU_EDIT, 0, getString(R.string.tf_edit_folder)).setEnabled(enabled).setVisible(visible);
        }
        
        menu.add(0, CONTEXT_MENU_INFO, 0, getString(R.string.tf_more_info));
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        
        switch (id) {            
        case DIALOG_DENIED_NOT_OWNER:            
            builder
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_unable_to_edit_folder_not_owner_title)
                .setMessage(R.string.tf_unable_to_edit_folder_not_owner_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

            dialog = builder.create();
            break;
            
        case DIALOG_MORE_INFO:
            String message = "Owner:\n" + mFolder.getOwnerAlias() + "\n\n";
            message = message + "Description:\n" + mFolder.getDescription();
            
            builder
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(mFolder.getName())
            .setMessage(message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    removeDialog(DIALOG_MORE_INFO);
                }
            });
            
            dialog = builder.create();
            break;
        }

        return dialog;    
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        boolean enabled = false;
        boolean visible = true;
        
        if (Collect.getInstance().getIoService().isSignedIn())
            enabled = true;
        
        if (mCopyToFolder)
            visible = false;
        
        if (!Collect.getInstance().getInformOnlineState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY)) {
            menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_folder))
                .setIcon(R.drawable.ic_menu_add)
                .setEnabled(enabled)
                .setVisible(visible);
        }
        
        menu.add(0, MENU_SYNC_LIST, 0, getString(R.string.tf_replication_list))
            .setIcon(R.drawable.ic_menu_sync_list)
            .setEnabled(enabled)
            .setVisible(visible);
        
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        AccountFolder folder = (AccountFolder) getListView().getItemAtPosition(info.position);
        
        switch (item.getItemId()) {
        case CONTEXT_MENU_EDIT:
            if (Collect.getInstance().getInformOnlineState().getDeviceId().equals(folder.getOwnerId())) {
                Intent i = new Intent(this, AccountFolderActivity.class);
                i.putExtra(AccountFolderActivity.KEY_FOLDER_ID, folder.getId());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_REV, folder.getRev());            
                i.putExtra(AccountFolderActivity.KEY_FOLDER_OWNER, folder.getOwnerId());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_NAME, folder.getName());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_DESC, folder.getDescription());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_VISIBILITY, folder.getVisibility());
                startActivity(i);
            } else {
                showDialog(DIALOG_DENIED_NOT_OWNER);
            }           
            
            break;
                        
        case CONTEXT_MENU_INFO:
            mFolder = folder;
            showDialog(DIALOG_MORE_INFO);
            break;
            
        default:
            return super.onContextItemSelected(item);
        }
        
        return true;
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        mFolder = (AccountFolder) getListAdapter().getItem(position);
        
        // Store separately because mFolder may not be available after activity restart
        mSelectedFolderId = mFolder.getId();
        mSelectedFolderName = mFolder.getName();
        
        if (mFolder.isReplicated() && !Collect.getInstance().getDbService().isDbLocal(mFolder.getId())) {
            mSynchronizeFoldersTask = new SynchronizeFoldersTask();
            mSynchronizeFoldersTask.setListener(AccountFolderList.this);
            mSynchronizeFoldersTask.setTransferMode(SynchronizeFoldersListener.MODE_PULL);
            mSynchronizeFoldersTask.setFolders(new ArrayList<String>(Collections.singletonList(mFolder.getId())));
            mSynchronizeFoldersTask.execute();
        } else {
            openFolder();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            startActivity(new Intent(this, AccountFolderActivity.class).putExtra(AccountFolderActivity.KEY_NEW_FOLDER, true));
            break;
            
        case MENU_SYNC_LIST:
            startActivity(new Intent(this, AccountFolderReplicationList.class));
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object onRetainNonConfigurationInstance()
    {
        if (mRefreshViewTask != null && mRefreshViewTask.getStatus() != AsyncTask.Status.FINISHED)
            return mRefreshViewTask;
        
        if (mSynchronizeFoldersTask != null && mSynchronizeFoldersTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSynchronizeFoldersTask;
        
        return null;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_COPY_TO_FOLDER, mCopyToFolder);
        outState.putString(KEY_FOLDER_ID, mSelectedFolderId);
        outState.putString(KEY_FOLDER_NAME, mSelectedFolderName);
    }

    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();

        @Override
        protected Void doInBackground(Void... nothing)
        {
            if (FileUtilsExtended.isFileOlderThan(getCacheDir() + File.separator + FileUtilsExtended.FOLDER_CACHE_FILE, FileUtilsExtended.TIME_TWO_MINUTES))                    
                fetchFolderList(false);

            folders = loadFolderList();
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
            if (folders.isEmpty()) {
                TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                nothingToDisplay.setVisibility(View.VISIBLE);
            } else {
                AccountFolderListAdapter adapter;
                
                adapter = new AccountFolderListAdapter(
                        getApplicationContext(),
                        R.layout.folder_list_item,
                        folders);

                setListAdapter(adapter);
            }
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    /*
     * Fetch a new folder list from Inform Online and store it on disk
     */
    public static void fetchFolderList(boolean fetchAnyway)
    {
        if (Collect.getInstance().getIoService().isSignedIn() || fetchAnyway) {
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "fetching list of folders");
        } else {
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "not signed in, skipping folder list fetch");
            return;            
        }
        
        // Try to ping the service to see if it is "up"
        String folderListUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/folder/list";
        String getResult = HttpUtils.getUrlData(folderListUrl);
        JSONObject jsonFolderList;
        
        try {
            jsonFolderList = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = jsonFolderList.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
            
            if (result.equals(InformOnlineState.OK)) {
                // Write out list of jsonFolders for later retrieval by loadFoldersList()
                JSONArray jsonFolders = jsonFolderList.getJSONArray("folders");

                try {
                    // Write out a folder list cache file
                    FileOutputStream fos = new FileOutputStream(new File(Collect.getInstance().getCacheDir(), FileUtilsExtended.FOLDER_CACHE_FILE));
                    fos.write(jsonFolders.toString().getBytes());
                    fos.close();
                } catch (Exception e) {
                    if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to write folder cache: " + e.toString());
                    e.printStackTrace();
                }
            } else {
                // There was a problem.. handle it!
            }
        } catch (NullPointerException e) {
            // Communication error
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
    }

    public static ArrayList<AccountFolder> loadFolderList()
    {
        if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG , t + "loading folder cache");
        
        ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();
        
        if (!new File(Collect.getInstance().getCacheDir(), FileUtilsExtended.FOLDER_CACHE_FILE).exists()) {
            if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "folder cache file cannot be read: aborting loadFolderList()");
            return folders;
        }
        
        try {
            FileInputStream fis = new FileInputStream(new File(Collect.getInstance().getCacheDir(), FileUtilsExtended.FOLDER_CACHE_FILE));
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader buffer = new BufferedReader(reader, 8192);
            StringBuilder sb = new StringBuilder();
            
            String cur;

            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
            
            buffer.close();
            reader.close();
            fis.close();
            
            try {
                JSONArray jsonFolders = (JSONArray) new JSONTokener(sb.toString()).nextValue();
                
                for (int i = 0; i < jsonFolders.length(); i++) {
                    JSONObject jsonFolder = jsonFolders.getJSONObject(i);
                    
                    AccountFolder folder = new AccountFolder(
                            jsonFolder.getString("id"),
                            jsonFolder.getString("rev"),
                            jsonFolder.getString("owner"),
                            jsonFolder.getString("name"),
                            jsonFolder.getString("description"),
                            jsonFolder.getString("visibility"),
                            jsonFolder.getBoolean("replication"));
                    
                    folders.add(folder);
                    
                    // Also update the account folder hash since this will be needed by BrowserActivity, among other things
                    Collect.getInstance().getInformOnlineState().getAccountFolders().put(folder.getId(), folder);
                }
            } catch (JSONException e) {
                // Parse error (malformed result)
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "failed to parse JSON " + sb.toString());
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to read folder cache: " + e.toString());
            e.printStackTrace();
        }
     
        return folders;
    }

    @Override
    public void synchronizationHandler(Message msg) 
    {
        if (msg == null) {
            // Close any existing progress dialogs
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();

            // Start new dialog with suitable message
            mProgressDialog = new ProgressDialog(AccountFolderList.this);
            mProgressDialog.setMessage(getString(R.string.tf_synchronizing_folders_dialog_msg));  
            mProgressDialog.show();
        } else {
            // Update progress dialog
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.setMessage(getString(R.string.tf_synchronizing_folder_count_dialog_msg, msg.arg1, msg.arg2));
            }
        }
    }

    @Override
    public void synchronizationTaskFinished(Bundle data) 
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        
        if (data.getBoolean(SynchronizeFoldersListener.SUCCESSFUL)) {
            openFolder();
        } else {
            Toast.makeText(getApplicationContext(), "Unable to open " + mSelectedFolderName + ". Please try again in a few minutes.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load the various elements of the screen that must wait for other tasks to
     * complete
     */
    private void loadScreen()
    {
        mRefreshViewTask = new RefreshViewTask();
        mRefreshViewTask.execute();

        registerForContextMenu(getListView());
        
        if (mCopyToFolder) {
            Toast.makeText(getApplicationContext(), "Select destination folder", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openFolder()
    {
        setProgressBarIndeterminateVisibility(true);
        
        if (mCopyToFolder) {
            Intent i = new Intent();
            i.putExtra(KEY_FOLDER_NAME, mFolder.getName());
            i.putExtra(KEY_FOLDER_ID, mFolder.getId());
            setResult(RESULT_OK, i);
            finish();
        } else {
            Collect.getInstance().getInformOnlineState().setSelectedDatabase(mSelectedFolderId);
            finish();
        }
    }
}