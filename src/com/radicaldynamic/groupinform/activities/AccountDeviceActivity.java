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
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

public class AccountDeviceActivity extends Activity
{
    private static final String t = "AccountDeviceActivity: ";
    
    public static final String KEY_DEVICEID = "deviceid";
    
    public static final int SAVING_DIALOG = 0;
        
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private String mDeviceId;
    private AccountDevice mDevice;
    
    private EditText mDeviceAlias;
    private EditText mDeviceEmail;
    private TextView mDevicePin;
    private TextView mDeviceCheckin;
    private CheckBox mDeviceTransferStatus;
    private TextView mDeviceTransferStatusTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.device);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_account_device));
        
        if (savedInstanceState == null) {
            Intent i = getIntent();
            mDeviceId = i.getStringExtra(KEY_DEVICEID);
            mDevice = Collect.getInstance().getAccountDevices().get(mDeviceId);
        } else {
            // TODO            
        }
        
        mDeviceAlias = (EditText) findViewById(R.id.alias);
        mDeviceEmail = (EditText) findViewById(R.id.email);
        mDevicePin = (TextView) findViewById(R.id.pin);
        mDeviceCheckin = (TextView) findViewById(R.id.checkin);
        mDeviceTransferStatus = (CheckBox) findViewById(R.id.transferStatus);
        mDeviceTransferStatusTitle = (TextView) findViewById(R.id.transferStatusTitle);
        
        mDeviceAlias.setText(mDevice.getAlias());
        mDeviceEmail.setText(mDevice.getEmail());
        mDevicePin.setText(mDevice.getPin());
        mDeviceCheckin.setText(mDevice.getLastCheckin());
        
        // Initialize fields based on whether the device is locked
        if (mDevice.getTransferStatus().equals(ClientInformationActivity.LOCKED)) {
            mDeviceTransferStatus.setChecked(true);
            mDeviceTransferStatusTitle.setText(getString(R.string.tf_device_admin_transfer_status_locked));            
        } else {
            mDeviceTransferStatus.setChecked(false);
            mDeviceTransferStatusTitle.setText(getString(R.string.tf_device_admin_transfer_status_unlocked));
        }
        
        // Set up listener to detect changes to transfer status input element
        mDeviceTransferStatus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {                
                if (((CheckBox) v).isChecked())
                    mDeviceTransferStatusTitle.setText(getString(R.string.tf_device_admin_transfer_status_locked)); 
                else
                    mDeviceTransferStatusTitle.setText(getString(R.string.tf_device_admin_transfer_status_unlocked));                                   
            }
        });
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
        }

        return null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
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
        Intent i;
        
        switch (item.getItemId()) {
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private class UpdateDeviceInfo extends AsyncTask<Void, Void, String>
    {        
        @Override
        protected String doInBackground(Void... nothing)
        {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("deviceId", mDevice.getId()));
            params.add(new BasicNameValuePair("alias", mDeviceAlias.getText().toString().trim()));
            params.add(new BasicNameValuePair("email", mDeviceEmail.getText().toString().trim()));
            
            if (mDeviceTransferStatus.isChecked())
                params.add(new BasicNameValuePair("transfer", "locked"));
            else 
                params.add(new BasicNameValuePair("transfer", "unlocked"));
            
            String verifyUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/device/update";
            
            return HttpUtils.postUrlData(verifyUrl, params);
        }

        @Override
        protected void onPreExecute()
        {
            showDialog(SAVING_DIALOG);
        }

        @Override
        protected void onPostExecute(String jsonResult)
        {
            JSONObject update;
            
            mProgressDialog.cancel();
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);                
                update = (JSONObject) new JSONTokener(jsonResult).nextValue();
                
                String result = update.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
                
                // Update successful
                if (result.equals(InformOnlineState.OK)) {  
                    Toast.makeText(getApplicationContext(), getString(R.string.data_saved_ok), Toast.LENGTH_SHORT).show();                    
                    
                    // Force the list to refresh (do not be destructive in case something bad happens later)
                    new File(FileUtils.DEVICE_CACHE_FILE_PATH).setLastModified(0);
                    
                    // Commit changes to the cache-in-memory to avoid running InformOnlineService.loadDeviceHash()
                    Collect.getInstance().getAccountDevices().get(mDeviceId).setAlias(mDeviceAlias.getText().toString().trim());
                    Collect.getInstance().getAccountDevices().get(mDeviceId).setEmail(mDeviceEmail.getText().toString().trim());
                    
                    if (mDeviceTransferStatus.isChecked())
                        Collect.getInstance().getAccountDevices().get(mDeviceId).setTransferStatus("locked");
                    else 
                        Collect.getInstance().getAccountDevices().get(mDeviceId).setTransferStatus("unlocked");                    
                    
                    // Get out of here
                    finish();
                } else if (result.equals(InformOnlineState.FAILURE)) {
                    // Update failed because of something the user did                    
                    String reason = update.optString(InformOnlineState.REASON, ClientRegistrationActivity.REASON_UNKNOWN);
                    
                    if (reason.equals(ClientRegistrationActivity.REASON_INVALID_EMAIL)) {
                        Log.w(Collect.LOGTAG, t + "invalid email address \"" + mDeviceEmail.getText().toString().trim() + "\"");                    
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_email), Toast.LENGTH_LONG).show();
                    } else if (reason.equals(ClientRegistrationActivity.REASON_EMAIL_ASSIGNED)) {
                        Log.i(Collect.LOGTAG, t + "email address \"" + mDeviceEmail.getText().toString().trim() + "\" already assigned to an account");                    
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_registration_error_email_in_use), Toast.LENGTH_LONG).show();
                    } else {
                        // Unhandled response
                        Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                    }                    
                } else {
                    // Something bad happened
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                   
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }                
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");               
                Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                
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
                            new UpdateDeviceInfo().execute();
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