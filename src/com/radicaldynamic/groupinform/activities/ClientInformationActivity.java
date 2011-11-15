package com.radicaldynamic.groupinform.activities;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.SynchronizeFoldersListener;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.tasks.SynchronizeFoldersTask;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * This screen displays information about the current client installation, status and
 * other information pertaining to the device's registration with Inform Online.
 * 
 * Things that the user should be able to see on this screen are:
 * 
 *      a) Inform Client version
 *      b) Inform Online account number
 *      c) Inform Online account key? (perhaps only if they are the account owner)
 *      d) Inform Online device PIN
 *      e) Current operation mode: online (direct connect vs. using local databases + synchronisation) 
 *         OR disconnected (using local databases)
 *      f) Whether or not CouchDB is installed locally (and option to install if not installed and stop/start/restart the installed Couch)
 *      g) Local data storage usage      
 */
public class ClientInformationActivity extends Activity implements SynchronizeFoldersListener
{
    private static final String t = "ClientInformationActivity: ";
    
    private static final int MENU_ACCOUNT_MEMBERS = 0;
    private static final int MENU_RESET_INFORM = 1;
    
    // Dialog constants
    private static final int CONFIRM_RESET_DIALOG = 0;
    private static final int RESET_SUCCESSFUL_DIALOG = 1;
    private static final int RESET_FAILED_DIALOG = 2;
    private static final int RESET_PROGRESS_DIALOG = 3;
    
    private SynchronizeFoldersTask mSynchronizeFoldersTask;

    private Dialog mDialog;
    private ProgressDialog mProgressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.about_inform);        

        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_inform_info));

        TextView accountNumber = (TextView) findViewById(R.id.accountNumber);
        TextView accountKey = (TextView) findViewById(R.id.accountKey);

        accountNumber.setText(Collect.getInstance().getInformOnlineState().getAccountNumber());
        accountKey.setText(Collect.getInstance().getInformOnlineState().getAccountKey());

        TextView devicePin = (TextView) findViewById(R.id.devicePin);
        TextView deviceEmail = (TextView) findViewById(R.id.deviceEmail);                

        devicePin.setText(Collect.getInstance().getInformOnlineState().getDevicePin());

        try {
            deviceEmail.setText(
                    Collect
                    .getInstance()
                    .getInformOnlineState()
                    .getAccountDevices()
                    .get(Collect.getInstance().getInformOnlineState().getDeviceId())
                    .getEmail()
            );
        } catch (NullPointerException e) {
            // In the event that the device does not have access to this information yet
            deviceEmail.setText(getString(R.string.tf_unavailable));
        }
        
        // Retrieve persistent data structures and processes
        Object data = getLastNonConfigurationInstance();
        
        if (data instanceof SynchronizeFoldersTask) {
            mSynchronizeFoldersTask = (SynchronizeFoldersTask) data;
        }
    }
    
    protected Dialog onCreateDialog(int id)
    {   
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mDialog = null;
        
        switch (id) {
        case CONFIRM_RESET_DIALOG:            
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setMessage(R.string.tf_reset_warning_msg)
            .setTitle(getString(R.string.tf_reset_inform) + "?")         
            .setPositiveButton(R.string.tf_reset, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSynchronizeFoldersTask = new SynchronizeFoldersTask();
                    mSynchronizeFoldersTask.setListener(ClientInformationActivity.this);
                    mSynchronizeFoldersTask.setTransferMode(SynchronizeFoldersListener.MODE_PUSH);
                    mSynchronizeFoldersTask.execute();
                }
            })            
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();            
            break;
            
        case RESET_SUCCESSFUL_DIALOG:            
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setMessage(R.string.tf_reset_complete_msg)
            .setTitle(R.string.tf_reset_complete)            
            .setPositiveButton(R.string.tf_exit_inform, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    setResult(RESULT_OK);
                    finish();
                }
            });           
            
            mDialog = builder.create();          
            break;
            
        case RESET_FAILED_DIALOG:            
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setMessage(R.string.tf_reset_incomplete_msg)
            .setTitle(getString(R.string.tf_something_failed, R.string.tf_reset))            
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });            
            
            mDialog = builder.create();
            break;        
            
        case RESET_PROGRESS_DIALOG:
            mProgressDialog = new ProgressDialog(this);   
            mProgressDialog.setMessage(getText(R.string.tf_resetting_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);            
            return mProgressDialog;
        }
        
        return mDialog;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ACCOUNT_MEMBERS, 0, getString(R.string.tf_account_devices)).setIcon(R.drawable.ic_menu_allfriends);
        menu.add(0, MENU_RESET_INFORM, 0, getString(R.string.tf_reset_inform)).setIcon(R.drawable.ic_menu_close_clear_cancel).setEnabled(Collect.getInstance().getIoService().isSignedIn());
        return true;
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;
        
        switch (item.getItemId()) {
        case MENU_ACCOUNT_MEMBERS:
            i = new Intent(this, AccountDeviceList.class);
            startActivity(i);
            break;
        case MENU_RESET_INFORM:
            showDialog(CONFIRM_RESET_DIALOG);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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
    }
    
    @Override
    public Object onRetainNonConfigurationInstance()
    {
        if (mSynchronizeFoldersTask != null && mSynchronizeFoldersTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSynchronizeFoldersTask;
        
        return null;
    }

    public class ResetDeviceTask extends AsyncTask<Void, Void, String> 
    {
        @Override
        protected String doInBackground(Void... params)
        {            
            String url = Collect.getInstance().getInformOnlineState().getServerUrl() + "/device/reset";            
            return HttpUtils.getUrlData(url);
        }
        
        @Override
        protected void onPreExecute()
        {
            // Ignore orientation changes (hacky but keeps this task simple)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            showDialog(RESET_PROGRESS_DIALOG);
        }

        @Override
        protected void onPostExecute(String getResult)
        {
            dismissDialog(RESET_PROGRESS_DIALOG);
            
            JSONObject result;
            
            boolean success = false;
            
            try {   
                result = (JSONObject) new JSONTokener(getResult).nextValue();
                
                if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK))
                    success = true;
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
                e.printStackTrace();
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
                e.printStackTrace();
            }
            
            if (success) {
                Collect.getInstance().getInformOnlineState().resetDevice();
                showDialog(RESET_SUCCESSFUL_DIALOG);
            } else {
                showDialog(RESET_FAILED_DIALOG);
            }
        }
    }


    @Override
    public void synchronizationHandler(Message msg)
    {
        if (msg == null) {
            // Close any existing progress dialogs
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();

            // Start new dialog with suitable message
            mProgressDialog = new ProgressDialog(ClientInformationActivity.this);
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
            new ResetDeviceTask().execute();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
        }
    }
}