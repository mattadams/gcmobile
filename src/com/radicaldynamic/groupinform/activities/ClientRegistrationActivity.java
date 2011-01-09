package com.radicaldynamic.groupinform.activities;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

public class ClientRegistrationActivity extends Activity
{
    private static final String t = "ClientRegistrationActivity: ";
    
    private static final int DIALOG_BEGIN_REGISTRATION = 1;
    private static final int DIALOG_TRANSFER_DEVICE = 2;
    private static final int DIALOG_ACCOUNT_CREATED = 3;
    private static final int DIALOG_DEVICE_REGISTERED = 4;
    private static final int DIALOG_SYSTEM_ERROR = 10;
    
    // Constants used to match server responses
    private static final String REASON_INVALID_EMAIL = "invalid email address";             // New account failure
    private static final String REASON_EMAIL_ASSIGNED = "email address assigned";           // New account failure
    private static final String REASON_UNKNOWN = "unknown reason";                          // Unknown failure (default only)
    private static final String REASON_INVALID_PIN = "invalid pin";                         // Transfer failure
    private static final String REASON_DEVICE_LOCKED = "device locked";                     // Transfer failure
    private static final String REASON_TRANSFER_DELAYED = "transfer delayed";               // Transfer failure
    private static final String REASON_UNKNOWN_ACCOUNT_CONTACT = "unknown account contact"; // Remind failure
    
    private String mAccountNumber = "";      // Licence number
    private String mAccountKey = "";         // Licence key    
    private String mDevicePin = "";    
    private String mContactEmailAddress = "";
    
    private TextWatcher mAutoFormat = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s)
        {
            char dash = '-';

            try {
                if ((int) s.charAt(s.length() - 1) != (int) dash)
                    if (s.length() == 5 || s.length() == 10)
                        s.insert(s.length() - 1, "-");
            } catch (IndexOutOfBoundsException e) {
                // Do nothing
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_registration));
        setContentView(R.layout.client_registration);
        
        Button register = (Button) findViewById(R.id.register);
        register.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showDialog(DIALOG_BEGIN_REGISTRATION);                
            }            
        });
    }
    
    protected Dialog onCreateDialog(int id)
    {        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        switch (id) {
            case DIALOG_BEGIN_REGISTRATION:
                builder
                .setCancelable(false)
                .setTitle(R.string.tf_do_you_have_an_account)
                .setMessage(R.string.tf_do_you_have_an_account_msg)
                .setPositiveButton(R.string.tf_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        registerExistingAccountDialog();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        registerNewAccountDialog();
                    }
                });
                break;
                
            case DIALOG_TRANSFER_DEVICE:
                builder
                .setCancelable(false)
                .setTitle(R.string.tf_are_you_transferring_devices)
                .setMessage(R.string.tf_are_you_transferring_devices_msg)
                .setPositiveButton(R.string.tf_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        transferDeviceDialog();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        registerDeviceDialog();
                    }
                });
                break;
                
            case DIALOG_ACCOUNT_CREATED:
                builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_account_created)
                .setMessage(getString(R.string.tf_account_created_msg, mContactEmailAddress))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Return user to the main screen (application will be reinitialized with this information)
                        Intent i = new Intent(getApplicationContext(), MainBrowserActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
                break;       
                
            case DIALOG_DEVICE_REGISTERED:
                builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_device_registered)
                .setMessage(R.string.tf_device_registered_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Return user to the main screen (application will be reinitialized with this information)
                        Intent i = new Intent(getApplicationContext(), MainBrowserActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
                break;
                
            case DIALOG_SYSTEM_ERROR:
                builder
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_system_error_dialog)
                .setMessage(R.string.tf_system_error_dialog_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do something?
                    }
                });
                break;
        }
        
        return builder.create();
    }
    
    private void registerDeviceDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.register_device, null);        
        
        final EditText emailAddress = (EditText) view.findViewById(R.id.emailAddress);
        emailAddress.setText(mContactEmailAddress);
        
        builder
        .setCancelable(false)
        .setInverseBackgroundForced(true)
        .setView(view)
        
        .setPositiveButton(R.string.tf_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String email = emailAddress.getText().toString().trim();
                
                if (email.length() == 0) {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.tf_email_address_required),
                            Toast.LENGTH_LONG).show();

                    registerDeviceDialog();
                } else {
                    if (verifyDeviceRegistration(email)) {
                        showDialog(DIALOG_DEVICE_REGISTERED);
                    } else {
                        registerDeviceDialog();
                    }
                }
            }
        })
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                registerExistingAccountDialog();
            }
        })
        
        .show(); 
    }
    
    private void registerExistingAccountDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.register_existing_account, null);
        
        final EditText licenceNumber = (EditText) view.findViewById(R.id.accountNumber);
        final EditText licenceKey = (EditText) view.findViewById(R.id.accountKey);
        
        licenceNumber.addTextChangedListener(mAutoFormat);
        licenceKey.addTextChangedListener(mAutoFormat);
        
        licenceNumber.setText(mAccountNumber);
        licenceKey.setText(mAccountKey);
        
        builder
        .setCancelable(false)
        .setInverseBackgroundForced(true)
        .setTitle(getString(R.string.tf_supply_account_details))
        .setView(view)
        
        .setPositiveButton(R.string.tf_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String number = licenceNumber.getText().toString().trim();
                String key = licenceKey.getText().toString().trim();
                
                if (verifyAccountLicence(number, key)) {
                    showDialog(DIALOG_TRANSFER_DEVICE);
                } else {
                    registerExistingAccountDialog();
                }
            }
        })
        
        .setNeutralButton(R.string.tf_remind, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                requestAccountReminderDialog();
            }
        })   
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        })
        
        .show();                
    }
    
    private void registerNewAccountDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.register_new_account, null);        
        
        final EditText emailAddress = (EditText) view.findViewById(R.id.emailAddress);
        emailAddress.setText(mContactEmailAddress);
        
        builder
        .setCancelable(false)
        .setInverseBackgroundForced(true)
        .setTitle("New account")
        .setView(view)
        
        .setPositiveButton(R.string.tf_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String email = emailAddress.getText().toString().trim();
                
                if (email.length() == 0) {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.tf_email_address_required),
                            Toast.LENGTH_LONG).show();

                    registerNewAccountDialog();
                } else {
                    if (verifyNewAccount(email)) {
                        showDialog(DIALOG_ACCOUNT_CREATED);
                    } else {
                        registerNewAccountDialog();
                    }
                }
            }
        })
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        })
        
        .show(); 
    }
    
    private void requestAccountReminderDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.account_reminder, null);        
        
        final EditText emailAddress = (EditText) view.findViewById(R.id.emailAddress);
        emailAddress.setText(mContactEmailAddress);
        
        builder
        .setCancelable(false)
        .setInverseBackgroundForced(true)
        .setTitle(R.string.tf_request_account_reminder)
        .setView(view)
        
        .setPositiveButton(R.string.tf_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String email = emailAddress.getText().toString().trim();
                
                if (email.length() == 0) {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.tf_email_address_required),
                            Toast.LENGTH_LONG).show();

                    requestAccountReminderDialog();
                } else {
                    if (sendAccountReminder(email)) {
                        registerExistingAccountDialog();
                    } else {
                        requestAccountReminderDialog();
                    }
                }
            }
        })
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                registerExistingAccountDialog();
            }
        })
        
        .show(); 
    }
    
    /*
     * Request that Inform Online send an email to the account owner containing 
     * account reminder information (licence number and key).  Return a boolean 
     * indicating whether the request to send an email was successful.
     */
    private boolean sendAccountReminder(String email)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("email", email));
        
        String verifyUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/send/account/reminder";
        
        String jsonResult = HttpUtils.postUrlData(verifyUrl, params);
        JSONObject verify;
        
        try {            
            Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);            
            verify = (JSONObject) new JSONTokener(jsonResult).nextValue();
            
            String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_reminder_sent), Toast.LENGTH_LONG).show();
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = verify.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_EMAIL)) {
                    Log.w(Collect.LOGTAG, t + "invalid email address \"" + email + "\"");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_email), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_UNKNOWN_ACCOUNT_CONTACT)) {
                    Log.w(Collect.LOGTAG, t + "unknown account contact \"" + email + "\"");
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_unknown_contact_email), Toast.LENGTH_LONG).show();
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }        
    }
    
    /*
     * Set information about the associated account/device registration to the installation preferences
     */
    private void setRegistrationInformation(JSONObject container) throws JSONException
    {
        Collect.getInstance().getInformOnline().setAccountNumber(container.getString("accountNumber"));
        Collect.getInstance().getInformOnline().setAccountKey(container.getString("accountKey"));
        Collect.getInstance().getInformOnline().setDeviceId(container.getString("deviceId"));
        Collect.getInstance().getInformOnline().setDeviceKey(container.getString("deviceKey"));
        Collect.getInstance().getInformOnline().setDevicePin(container.getString("devicePin"));
        Collect.getInstance().getInformOnline().setDeviceEmail(container.getString("deviceEmail"));
    }

    private void transferDeviceDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.transfer_device, null);        
        
        final EditText devicePin = (EditText) view.findViewById(R.id.devicePin);
        devicePin.addTextChangedListener(mAutoFormat);
        devicePin.setText(mDevicePin);
        
        builder
        .setCancelable(false)
        .setInverseBackgroundForced(true)
        .setView(view)
        
        .setPositiveButton(R.string.tf_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pin = devicePin.getText().toString().trim(); 
                
                // Save for later
                mDevicePin = pin;
                
                if (pin.length() == 0 || !pin.matches("^[0-9]{4,4}-[0-9]{4,4}$")) {
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_device_pin_required), Toast.LENGTH_LONG).show();
                    transferDeviceDialog();
                } else {
                    if (verifyDeviceTransfer(pin)) {
                        showDialog(DIALOG_DEVICE_REGISTERED);
                    } else {
                        transferDeviceDialog();
                    }
                }
            }
        })
        
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                registerExistingAccountDialog();
            }
        })
        
        .show(); 
    }    

    private boolean verifyAccountLicence(String number, String key)
    {
        // Make sure abcd-ef12-3456 gets translated to ABCD-EF12-3456
        key = key.toUpperCase();
        
        // Store for reuse
        mAccountNumber = number;
        mAccountKey = key;
        
        // Both the licence number and key are required to proceed
        if (number.length() == 0 || key.length() == 0) {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_unable_to_proceed_without_licence_number_and_key), Toast.LENGTH_LONG).show();           
            return false;
        }
        
        Boolean error = false;

        // Ensure the licence number is formatted NNNN-NNNN-NNNN
        if (!number.matches("^[0-9]{4,4}-[0-9]{4,4}-[0-9]{4,4}$")) {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_improperly_formatted_licence_number), Toast.LENGTH_LONG).show();
            error = true;
        }

        // Ensure the licence key is formatted XXXX-XXXX-XXXX
        if (!key.matches("(?i)^[a-f0-9]{4,4}-[a-f0-9]{4,4}-[a-f0-9]{4,4}$")) {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_improperly_formatted_licence_key), Toast.LENGTH_LONG).show();
            error = true;
        }

        if (error) {            
            return false;
        } else {
            // Data to POST
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("licenceNumber", number));
            params.add(new BasicNameValuePair("licenceKey", key));
            
            String verifyUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/verify/licence";
            String jsonResult = HttpUtils.postUrlData(verifyUrl, params);
            JSONObject verify;
            
            try {
                Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);                
                verify = (JSONObject) new JSONTokener(jsonResult).nextValue();                
                
                String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
                
                // Match
                if (result.equals(InformOnlineState.OK)) {               
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_licence_validation_succeeded), Toast.LENGTH_SHORT).show();                    
                    return true;                   
                } else if (result.equals(InformOnlineState.FAILURE)) {
                    // No match                     
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_licence_validation_failed), Toast.LENGTH_LONG).show();                    
                    return false;
                } else {
                    // Something bad happened
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                   
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                    
                    return false;
                }
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");               
                Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return false;
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                e.printStackTrace();                
                return false;
            }
        }
    } // End verifyAccountLicence()
    
    private boolean verifyDeviceRegistration(String email)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("licenceNumber", mAccountNumber));
        params.add(new BasicNameValuePair("licenceKey", mAccountKey));
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnline().getDeviceFingerprint()));
        
        String transferUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/register/device";
        String jsonResult = HttpUtils.postUrlData(transferUrl, params);
        JSONObject verify;
        
        try {            
            Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);            
            verify = (JSONObject) new JSONTokener(jsonResult).nextValue();
            
            String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                // Store account information to preferences
                mContactEmailAddress = email;
                setRegistrationInformation(verify);
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = verify.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_EMAIL)) {
                    Log.w(Collect.LOGTAG, t + "invalid email address \"" + email + "\"");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_email), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_EMAIL_ASSIGNED)) {
                    Log.i(Collect.LOGTAG, t + "email address \"" + email + "\" already assigned to an account");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_registration_error_email_in_use), Toast.LENGTH_LONG).show();
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }      
    } // End verifyDeviceRegistration()
    
    private boolean verifyDeviceTransfer(String devicePin)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("licenceNumber", mAccountNumber));
        params.add(new BasicNameValuePair("licenceKey", mAccountKey));
        params.add(new BasicNameValuePair("devicePin", devicePin));
        
        String transferUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/transfer/do";
        String jsonResult = HttpUtils.postUrlData(transferUrl, params);
        JSONObject transfer;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);
            transfer = (JSONObject) new JSONTokener(jsonResult).nextValue();
            
            String result = transfer.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                setRegistrationInformation(transfer);
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = transfer.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_PIN)) {
                    Log.i(Collect.LOGTAG, t + "transfer failed (invalid PIN)");
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_pin), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_DEVICE_LOCKED)) {
                    Log.i(Collect.LOGTAG, t + "transfer failed (device locked)");
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_device_locked), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_TRANSFER_DELAYED)) {
                    Log.i(Collect.LOGTAG, t + "transfer delayed for " + transfer.getString("delay") + "ms");
                    
                    String approximation = " ";
                    String period = "";
                    String unit = "";

                    // The delay time is returned in milliseconds
                    Integer delayMilliseconds = transfer.getInt("delay");
                    
                    if (delayMilliseconds / 1000 / 60 > 0) {
                        approximation = " about ";
                        period = Integer.toString(delayMilliseconds / 1000 / 60);
                        unit = "minutes";
                    } else if (delayMilliseconds / 1000 > 0) {
                        period = Integer.toString(delayMilliseconds / 1000);
                        unit = "seconds";
                    } else {
                        period = "a few";
                        unit = "seconds";
                    }
                    
                    // Hack to turn "minutes" into "minute" or whatever
                    if (period.equals("1")) {
                        unit = unit.substring(0, unit.length() - 1);
                    }
                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_transfer_delayed_reason), Toast.LENGTH_LONG).show();                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_transfer_delayed_wait, approximation, period, unit), Toast.LENGTH_LONG).show();
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    } // End verifyDeviceTransfer()
    
    private boolean verifyNewAccount(String email)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnline().getDeviceFingerprint()));
        
        String verifyUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/register/account";
        
        String jsonResult = HttpUtils.postUrlData(verifyUrl, params);
        JSONObject verify;
        
        try {            
            Log.d(Collect.LOGTAG, t + "parsing jsonResult " + jsonResult);            
            verify = (JSONObject) new JSONTokener(jsonResult).nextValue();
            
            String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                // Used by DIALOG_ACCOUNT_CREATED
                mContactEmailAddress = email;
                
                // Store account information to preferences
                setRegistrationInformation(verify);
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = verify.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_EMAIL)) {
                    Log.w(Collect.LOGTAG, t + "invalid email address \"" + email + "\"");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_email), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_EMAIL_ASSIGNED)) {
                    Log.i(Collect.LOGTAG, t + "email address \"" + email + "\" already assigned to an account");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_registration_error_email_in_use), Toast.LENGTH_LONG).show();
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing jsonResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no jsonResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse jsonResult " + jsonResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }         
    } // End verifyNewAccount()
}