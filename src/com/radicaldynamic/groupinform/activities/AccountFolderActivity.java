package com.radicaldynamic.groupinform.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

public class AccountFolderActivity extends Activity
{
    private static final String t = "AccountFolderActivity: ";
    
    public static final String KEY_NEW_FOLDER = "new_folder";
    
    public static final String KEY_FOLDER_ID = "folder_id"; 
    public static final String KEY_FOLDER_REV = "folder_rev";
    public static final String KEY_FOLDER_OWNER = "folder_owner";
    public static final String KEY_FOLDER_NAME = "folder_name";
    public static final String KEY_FOLDER_DESC = "folder_description";
    public static final String KEY_FOLDER_VISIBILITY = "folder_visibility";
    
    private static final int MENU_REMOVE_FOLDER = 0;
    
    public static final int SAVING_DIALOG = 0;
    public static final int REMOVING_DIALOG = 1;
    public static final int CONFIRM_REMOVAL_DIALOG = 2;
    
    // Visibility status strings used by Inform Online
    public static final String PRIVATE_FOLDER = "private";
    public static final String SHARED_FOLDER = "shared";
        
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private AccountFolder mFolder;
    
    private EditText mFolderName;
    private EditText mFolderDescription;
    private RadioButton mFolderVisibilityShared;
    private RadioButton mFolderVisibilityPrivate;    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.folder);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_account_folder));
        
        if (savedInstanceState == null) {
            Intent i = getIntent();
            
            if (i == null) {
                 // New folder
                
            } else {
                if (i.getBooleanExtra(KEY_NEW_FOLDER, false))
                    mFolder = new AccountFolder(null, null, null, "", "", "private", false);
                else {
                    // Because we don't have a better way of shuffling these values around
                    mFolder = new AccountFolder(
                            i.getStringExtra(KEY_FOLDER_ID),
                            i.getStringExtra(KEY_FOLDER_REV),
                            i.getStringExtra(KEY_FOLDER_OWNER),
                            i.getStringExtra(KEY_FOLDER_NAME),
                            i.getStringExtra(KEY_FOLDER_DESC),
                            i.getStringExtra(KEY_FOLDER_VISIBILITY),
                            false);
                }
            }
        } else {
            // TODO            
        }        

        mFolderName = (EditText) findViewById(R.id.folderName);
        mFolderDescription = (EditText) findViewById(R.id.folderDescription);
        mFolderVisibilityPrivate = (RadioButton) findViewById(R.id.folderVisibilityPrivate);
        mFolderVisibilityShared = (RadioButton) findViewById(R.id.folderVisibilityShared);
        
        mFolderName.setText(mFolder.getName());
        mFolderDescription.setText(mFolder.getDescription());
        
        // Initialize fields based on whether the folder is private
        if (mFolder.getVisibility().equals(PRIVATE_FOLDER)) {
            mFolderVisibilityPrivate.setChecked(true);
        } else {
            mFolderVisibilityShared.setChecked(true);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
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
            
        case REMOVING_DIALOG:
            mProgressDialog = new ProgressDialog(this);   
            mProgressDialog.setMessage(getText(R.string.tf_removing_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);        
            
            return mProgressDialog;
            
        case CONFIRM_REMOVAL_DIALOG:
            mAlertDialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.tf_remove_folder) + "?")
                .setMessage(R.string.tf_remove_folder_dialog_msg)
                .setPositiveButton(R.string.tf_remove, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new RemoveFolderTask().execute();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
            
            mAlertDialog.show();
            
            break;
        }

        return null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        Boolean enabled = true;

        // Certain menu entries should not be available when creating a new folder
        if (mFolder.getId() == null)
            enabled = false;
        
        menu.add(0, MENU_REMOVE_FOLDER, 0, getString(R.string.tf_remove_folder)).setIcon(R.drawable.ic_menu_delete).setEnabled(enabled);
        
        return true;
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            showQuitDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {       
        switch (item.getItemId()) {              
        case MENU_REMOVE_FOLDER:
            showDialog(CONFIRM_REMOVAL_DIALOG);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private class CommitChangesTask extends AsyncTask<Void, Void, String>
    {        
        @Override
        protected String doInBackground(Void... nothing)
        {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            
            // New folder getId() values are null
            if (mFolder.getId() != null) {
                params.add(new BasicNameValuePair("folderId", mFolder.getId()));
                params.add(new BasicNameValuePair("folderRev", mFolder.getRev()));
            }
            
            params.add(new BasicNameValuePair("name", mFolderName.getText().toString().trim()));
            params.add(new BasicNameValuePair("description", mFolderDescription.getText().toString().trim()));
            
            if (mFolderVisibilityPrivate.isChecked())
                params.add(new BasicNameValuePair("visibility", PRIVATE_FOLDER));
            else 
                params.add(new BasicNameValuePair("visibility", SHARED_FOLDER));
            
            String processUrl;
            
            if (mFolder.getId() == null)
                processUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/folder/add";
            else 
                processUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/folder/update";
            
            return HttpUtils.postUrlData(processUrl, params);
        }

        @Override
        protected void onPreExecute()
        {
            showDialog(SAVING_DIALOG);
        }

        @Override
        protected void onPostExecute(String postResult)
        {
            JSONObject update;
            
            mProgressDialog.cancel();
            
            try {
                update = (JSONObject) new JSONTokener(postResult).nextValue();
                String result = update.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
                
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
    
    private class RemoveFolderTask extends AsyncTask<Void, Void, String>
    {        
        @Override
        protected String doInBackground(Void... nothing)
        {            
            String removeUrl = Collect.getInstance().getInformOnlineState().getServerUrl() 
                + "/folder/remove/" + mFolder.getId() + File.separator + mFolder.getRev(); 
            
            return HttpUtils.getUrlData(removeUrl);
        }
    
        @Override
        protected void onPreExecute()
        {
            showDialog(REMOVING_DIALOG);
        }
    
        @Override
        protected void onPostExecute(String getResult)
        {
            mProgressDialog.cancel();
            
            JSONObject update;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
                update = (JSONObject) new JSONTokener(getResult).nextValue();
                
                String result = update.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
                
                // Update successful
                if (result.equals(InformOnlineState.OK)) {  
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_removed_with_param, mFolder.getName()), Toast.LENGTH_SHORT).show();                    
                    
                    // Force the list to refresh (do not be destructive in case something bad happens later)
                    new File(getCacheDir(), FileUtils.FOLDER_CACHE_FILE).setLastModified(0);
                    
                    // Get out of here
                    finish();
                } else if (result.equals(InformOnlineState.FAILURE)) {
                    // There is only one possible failure right now (the user tried to remove their default DB)
                    Log.w(Collect.LOGTAG, t + "removal of default database denied");
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_unable_to_remove_defaultdb), Toast.LENGTH_LONG).show();
                } else {
                    // Something bad happened
                    Log.e(Collect.LOGTAG, t + "system error while processing getResult");                   
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }                
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");               
                Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    /*
     * Prompt shown to the user before they leave the field list 
     * (discard changes & quit, save changes & quit, return to form field list)
     */
    private void showQuitDialog()
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
                            if (mFolderName.getText().toString().trim().length() > 0)
                                new CommitChangesTask().execute();
                            else
                                Toast.makeText(
                                        getApplicationContext(), 
                                        getString(R.string.tf_folder_name_required), 
                                        Toast.LENGTH_LONG).show();
                            
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
