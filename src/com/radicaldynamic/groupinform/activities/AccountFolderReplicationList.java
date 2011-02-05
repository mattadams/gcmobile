package com.radicaldynamic.groupinform.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

public class AccountFolderReplicationList extends ListActivity
{    
    private final String t = "AccountFolderReplicationList: ";
    
    public static final int SAVING_DIALOG = 0;
    
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private ListView mListView;
    
    private boolean mListChanged = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_synchronized_folders));
        
        mListView = getListView();

        mListView.setItemsCanFocus(false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                mListChanged = true;                
            }});
        
        new RefreshViewTask().execute();
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id) {
        case SAVING_DIALOG:
            mProgressDialog = new ProgressDialog(this);   
            mProgressDialog.setMessage(getText(R.string.tf_saving_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);            
            return mProgressDialog;
        }
        
        return null;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (mListChanged) {
                showSaveDialog();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
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
            folders = AccountFolderList.loadFolderList();
            
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
            if (folders.isEmpty()) {
                // TODO: What now?  This should never happen.
            } else {
                setListAdapter(new ArrayAdapter<AccountFolder>(
                        AccountFolderReplicationList.this, 
                        android.R.layout.simple_list_item_multiple_choice, 
                        folders));
                
                // Check off items
                for (int i = 0; i < mListView.getCount(); i++) {
                    boolean isReplicated = ((AccountFolder) mListView.getItemAtPosition(i)).isReplicated();
                    
                    Log.e(Collect.LOGTAG, "found item at position " + i + " replication state to be " + isReplicated);
                    
                    if (isReplicated)
                        mListView.setItemChecked(i, true);
                    else 
                        mListView.setItemChecked(i, false);
                }
            }
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    /*
     * Refresh the main form browser view as requested by the user
     */
    private class UpdateFolderReplicationList extends AsyncTask<Void, Void, String>
    {   
        @Override
        protected String doInBackground(Void... nothing)
        {
            // Get list of databases to replicate
            List<String> databasesToReplicateById = new ArrayList<String>();            
            SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
            
            for (int i = 0; i < checkedItemPositions.size(); i++) {
                if (checkedItemPositions.valueAt(i)) {
                    String databaseId = ((AccountFolder) mListView.getItemAtPosition(i)).getId();                    
                    Log.e(Collect.LOGTAG, t + "user selected database " + databaseId + " for replication");                    
                    databasesToReplicateById.add(databaseId);
                }
            }
            
            // Update device profile with list of databases to replicate
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            
            for (int i = 0; i < databasesToReplicateById.size(); i++)
                params.add(new BasicNameValuePair("databaseIds", databasesToReplicateById.get(i)));
            
            String updateUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/device/set/replications";
            
            return HttpUtils.postUrlData(updateUrl, params);
        }

        @Override
        protected void onPreExecute()
        {
            showDialog(SAVING_DIALOG);            
        }

        @Override
        protected void onPostExecute(String postResult)
        {
            mProgressDialog.cancel();
            
            JSONObject update;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing postResult " + postResult);                
                update = (JSONObject) new JSONTokener(postResult).nextValue();
                
                String result = update.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
                
                // Update successful
                if (result.equals(InformOnlineState.OK)) {  
                    Toast.makeText(getApplicationContext(), getString(R.string.data_saved_ok), Toast.LENGTH_SHORT).show();                    
                    
                    // Force the list to refresh (do not be destructive in case something bad happens later)
                    new File(getCacheDir(), FileUtils.FOLDER_CACHE_FILE).setLastModified(0);
                    
                    // Get out of here
                    finish();
                } else {
                    // Something bad happened
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                   
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }                
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");               
                Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }
    
    private void showSaveDialog()
    {
        String[] items = {
                getString(R.string.do_not_save),
                getString(R.string.tf_save_and_exit), 
                getString(R.string.tf_abort_exit)
        };
    
        mAlertDialog = new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.quit_application))
            .setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        case 0:
                            // Discard any changes and exit
                            finish();
                            break;
    
                        case 1:
                            // Save and exit                            
                            new UpdateFolderReplicationList().execute();
                            break;
    
                        case 2:
                            // Do nothing
                            break;    
                        }
                    }
                }).create();
    
        mAlertDialog.show();  
    }
}