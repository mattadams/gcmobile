package com.radicaldynamic.groupinform.activities;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    
    private static final int MENU_ACCOUNT_MEMBERS = 0;
    private static final int MENU_THIS_DEVICE = 1;
    private static final int MENU_RESET_INFORM = 2;    
    
    // Strings returned by the Group Inform Server
    private static final String TRANSFER_STATE = "transfer";
    public static final String LOCKED = "locked";
    public static final String UNLOCKED = "unlocked";  
    
    // Intent constant for determining which "screen" to display
    private static final String SCREEN = "screen";
    private static final int SCREEN_DEFAULT = 0;
    private static final int SCREEN_DEVICE_INFO = 1;    
    
    private int mScreen;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.about_inform);        
        
        Intent intent = getIntent();

        mScreen = intent.getIntExtra(SCREEN, 0);
        
        TextView header = (TextView) findViewById(R.id.aboutInformHeader);

        switch (mScreen) {
        case SCREEN_DEFAULT:
            disableFormComponent(R.id.deviceInformation);

            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_inform_info));
            
            TextView accountNumber = (TextView) findViewById(R.id.accountNumber);
            TextView accountKey = (TextView) findViewById(R.id.accountKey);

            header.setText(getString(R.string.tf_about_inform_account_header));
            accountNumber.setText(Collect.getInstance().getInformOnlineState().getAccountNumber());
            accountKey.setText(Collect.getInstance().getInformOnlineState().getAccountKey());

            break;

        case SCREEN_DEVICE_INFO:
            disableFormComponent(R.id.accountInformation);

            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_device_info));

            new RetrieveTransferState().execute();

            header.setText(getString(R.string.tf_about_inform_device_header));
            TextView devicePin = (TextView) findViewById(R.id.devicePin);
            TextView deviceEmail = (TextView) findViewById(R.id.deviceEmail);                

            devicePin.setText(Collect.getInstance().getInformOnlineState().getDevicePin());
            deviceEmail.setText(
                    Collect.getInstance().getAccountDevices().get(
                            Collect.getInstance().getInformOnlineState().getDeviceId()
                            ).getEmail()        
            );

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

            break;
        }

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        // "Exit" if the user resets Inform
        case SCREEN_DEVICE_INFO:
            setResult(RESULT_OK);
            finish();
            break;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        switch (mScreen) {
        case SCREEN_DEFAULT:
            menu.add(0, MENU_ACCOUNT_MEMBERS, 0, getString(R.string.tf_account_devices)).setIcon(R.drawable.ic_menu_allfriends);
            menu.add(0, MENU_THIS_DEVICE, 0, getString(R.string.tf_this_device)).setIcon(R.drawable.ic_menu_myinfo);
            break;
        case SCREEN_DEVICE_INFO:
            menu.add(0, MENU_RESET_INFORM, 0, getString(R.string.tf_reset_inform)).setIcon(R.drawable.ic_menu_close_clear_cancel);
            break;
        }
        
        return true;
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
        case MENU_THIS_DEVICE:
            i = new Intent(this, ClientInformationActivity.class);
            i.putExtra(SCREEN, SCREEN_DEVICE_INFO);
            startActivityForResult(i, SCREEN_DEVICE_INFO);
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
            String url = Collect.getInstance().getInformOnlineState().getServerUrl() + "/device/transfer/show";
            
            JSONObject result;
            String getResult = HttpUtils.getUrlData(url);
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);   
                result = (JSONObject) new JSONTokener(getResult).nextValue();
                
                if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK)) {
                    return result.getString(TRANSFER_STATE);
                } else {
                    return null;
                }
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");  
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
                e.printStackTrace();
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
            String url = Collect.getInstance().getInformOnlineState().getServerUrl() + "/device/transfer/toggle";
            
            String getResult = HttpUtils.getUrlData(url);
            JSONObject result;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);   
                result = (JSONObject) new JSONTokener(getResult).nextValue();
                
                if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK)) {
                    return result.optString(TRANSFER_STATE, null);
                } else {
                    return null;
                }
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
                e.printStackTrace();
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
    
    private void disableFormComponent(int componentResource)
    {
        ViewGroup component = (ViewGroup) findViewById(componentResource);
        component.setVisibility(View.GONE);
    }
    
    private void resetCompleteDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
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
        }) 
        
        .show();
    }
    
    private void resetIncompleteDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_alert)
        .setMessage(R.string.tf_reset_incomplete_msg)
        .setTitle(R.string.tf_reset_incomplete)
        
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
        .setMessage(R.string.tf_reset_warning_msg)
        .setTitle(R.string.tf_reset_warning)
        
        .setPositiveButton(R.string.tf_reset, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {            
                if (verifyReset()) {
                    Collect.getInstance().getInformOnlineState().resetDevice();
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
        
        String url = Collect.getInstance().getInformOnlineState().getServerUrl() + "/device/reset";
        
        String getResult = HttpUtils.getUrlData(url);
        JSONObject result;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);   
            result = (JSONObject) new JSONTokener(getResult).nextValue();
            
            if (result.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE).equals(InformOnlineState.OK))
                reset = true;
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
        
        return reset;
    }
}