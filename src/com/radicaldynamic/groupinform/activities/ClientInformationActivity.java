package com.radicaldynamic.groupinform.activities;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
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
public class ClientInformationActivity extends Activity
{
    private static final String t = "ClientInformationActivity: ";
    
    private static final int MENU_CHANGE_ACCOUNT = 0;
    private static final int MENU_ACCOUNT_MEMBERS = 1;
    private static final int MENU_RESET_INFORM = 2;    
    
    // Strings returned by the Group Inform Server specific to this activity
    private static final String STATE = "state";
    private static final String LOCKED = "locked";
    private static final String UNLOCKED = "unlocked";    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.about_inform);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_inform_info));
        
        new RetrieveTransferState().execute();
        
        TextView accountNumber = (TextView) findViewById(R.id.accountNumber);
        TextView accountKey = (TextView) findViewById(R.id.accountKey);
        TextView devicePin = (TextView) findViewById(R.id.devicePin);
        
        accountNumber.setText(Collect.getInstance().getInformOnline().getAccountNumber());
        accountKey.setText(Collect.getInstance().getInformOnline().getAccountKey());
        devicePin.setText(Collect.getInstance().getInformOnline().getDevicePin());
        
        // Set up transfer state button to lock/unlock when clicked
        final ToggleButton transferState = (ToggleButton) findViewById(R.id.deviceTransferStatus);
        
        transferState.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                transferState.setEnabled(false);
                new ToggleTransferState().execute();   
            }            
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        //menu.add(0, MENU_CHANGE_ACCOUNT, 0, "Switch Account").setIcon(R.drawable.ic_menu_account_list);
        menu.add(0, MENU_ACCOUNT_MEMBERS, 0, "Account Members").setIcon(R.drawable.ic_menu_allfriends);
        menu.add(0, MENU_RESET_INFORM, 0, getString(R.string.tf_reset_inform)).setIcon(R.drawable.ic_menu_close_clear_cancel);
        
        return true;
    }    

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_CHANGE_ACCOUNT:            
            return true;
        case MENU_RESET_INFORM:
            resetInformDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    public class RetrieveTransferState extends AsyncTask<Void, Void, String> 
    {
        @Override
        protected String doInBackground(Void... params)
        {
            String url = Collect.getInstance().getInformOnline().getServerUrl() + "/transfer/show";
            
            JSONObject result;
            String jsonResult = HttpUtils.getUrlData(url);
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);   
                result = (JSONObject) new JSONTokener(jsonResult).nextValue();
                
                if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK)) {
                    return result.getString(STATE);
                } else {
                    return null;
                }
            } catch (NullPointerException e) {
                /* 
                 * Null pointers occur to jsonResult when HttpUtils.getUrlData() fails
                 * either as a result of a communication error with the node.js server
                 * or something else.
                 */
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");               
                return null;
            } catch (JSONException e) {
                // Parse errors (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                
                return null;
            }
        }
        
        @Override
        protected void onPreExecute()
        {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(String state)
        {
            ToggleButton transferState = (ToggleButton) findViewById(R.id.deviceTransferStatus);
            
            if (state == null) {
                transferState.setText(getText(R.string.tf_unavailable));
                transferState.setEnabled(false);
            } else {
                transferState.setEnabled(true);
                
                if (state.equals(LOCKED)) {
                    transferState.setChecked(true);
                } else {
                    transferState.setChecked(false);
                }
            }
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    public class ToggleTransferState extends AsyncTask<Void, Void, String> 
    {
        @Override
        protected String doInBackground(Void... params)
        {
            String url = Collect.getInstance().getInformOnline().getServerUrl() + "/transfer/toggle";
            
            String jsonResult = HttpUtils.getUrlData(url);
            JSONObject result;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);   
                result = (JSONObject) new JSONTokener(jsonResult).nextValue();
                
                if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK)) {
                    return result.optString(STATE, null);
                } else {
                    return null;
                }
            } catch (NullPointerException e) {
                /* 
                 * Null pointers occur to jsonResult when HttpUtils.getUrlData() fails
                 * either as a result of a communication error with the node.js server
                 * or something else.
                 */
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");               
                return null;
            } catch (JSONException e) {
                // Parse errors (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                
                return null;
            }
        }    
        
        @Override
        protected void onPreExecute()
        {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(String state)
        {
            ToggleButton transferState = (ToggleButton) findViewById(R.id.deviceTransferStatus);
            
            if (state == null) {
                transferState.setText(getText(R.string.tf_unavailable));
                transferState.setEnabled(false);
            } else if (state.equals(LOCKED)) {
                transferState.setEnabled(true);
                transferState.setChecked(true);
            } else if (state.equals(UNLOCKED)) {
                transferState.setEnabled(true);
                transferState.setChecked(false);
            }
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    private void resetCompleteDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_info)
        .setMessage(R.string.tf_reset_complete)
        .setTitle(R.string.tf_reset_complete_prompt)
        
        .setPositiveButton(R.string.tf_exit_inform, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                setResult(RESULT_OK);
                finish();
            }
        }) 
        
        .show();
    }
    
    private void resetIncompleteDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_alert)
        .setMessage(R.string.tf_reset_incomplete)
        .setTitle(R.string.tf_reset_incomplete_prompt)
        
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }) 
        
        .show();
    }
    
    private void resetInformDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
       
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_alert)
        .setMessage(R.string.tf_reset_warning)
        .setTitle(R.string.tf_reset_warning_prompt)
        
        .setPositiveButton(R.string.tf_reset, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {            
                if (verifyReset()) {
                    Collect.getInstance().getInformOnline().resetPreferences();
                    resetCompleteDialog();
                } else {
                    resetIncompleteDialog();
                }
            }
        })
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do something?
            }
        })
        
        .show();       
    }
    
    /*
     * Verify a request to reset the device with Inform Online.  A successful result
     * is required from the service before we will reset this device.
     */
    private boolean verifyReset()
    {
        boolean reset = false;
        
        String url = Collect.getInstance().getInformOnline().getServerUrl() + "/reset";
        
        String jsonResult = HttpUtils.getUrlData(url);
        JSONObject result;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);   
            result = (JSONObject) new JSONTokener(jsonResult).nextValue();
            
            if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK))
                reset = true;
        } catch (NullPointerException e) {
            /* 
             * Null pointers occur to jsonResult when HttpUtils.getUrlData() fails
             * either as a result of a communication error with the node.js server
             * or something else.
             */
            Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse errors (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);
            e.printStackTrace();
        }
        
        return reset;
    }
}