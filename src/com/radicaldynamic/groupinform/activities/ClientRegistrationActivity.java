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
    private static final int DIALOG_DEVICE_REGISTRATION_METHOD = 2;
    private static final int DIALOG_ACCOUNT_CREATED = 3;
    private static final int DIALOG_DEVICE_REGISTERED = 4;
    private static final int DIALOG_DEVICE_ACTIVE = 5;
    private static final int DIALOG_SYSTEM_ERROR = 10;
//    private static final int DIALOG_BETA_PREVIEW = 11;
    
    // verifyDeviceRegistration exit codes
    private static final int DEVICE_REGISTRATION_VERIFIED = 0;                              // Generic "registration ok"
    private static final int DEVICE_REGISTRATION_FAILED = 1;                                // Generic "registration failed"
    private static final int DEVICE_REGISTRATION_LIMITED = 2;                               // Licence seat limit
    
    // Constants used to match server responses
    public static final String REASON_INVALID_EMAIL = "invalid email address";              // New account/device failure
    public static final String REASON_EMAIL_ASSIGNED = "email address assigned";            // New account/device failure
    public static final String REASON_UNKNOWN = "unknown reason";                           // Unknown failure (default only)
    private static final String REASON_LICENCE_LIMIT = "seat licence limit reached";        // New device failure
    private static final String REASON_INVALID_PIN = "invalid pin";                         // Reactivation failure
    private static final String REASON_DEVICE_ACTIVE = "device active";                     // Reactivation failure
    private static final String REASON_REACTIVATION_DELAYED = "activation delayed";         // Reactivation failure
    private static final String REASON_UNKNOWN_ACCOUNT_CONTACT = "unknown account contact"; // Remind failure
    
    private String mAccountNumber = "";      // Licence number
    private String mAccountKey = "";         // Licence key    
    private String mDevicePin = "";    
    private String mContactEmailAddress = "";  
    
    // Alternate notification logic used when attempting to reactivate a device profile that is already active
    private boolean mOptionToNotifyDeviceUser = false;
    
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
//                showDialog(DIALOG_BETA_PREVIEW);
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
                
            case DIALOG_DEVICE_REGISTRATION_METHOD:
                builder
                .setCancelable(false)
                .setMessage(R.string.tf_device_registration_method_msg)
                .setPositiveButton(R.string.tf_device_registration_method_register_new, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        registerDeviceDialog();
                    }
                })
                .setNegativeButton(R.string.tf_device_registration_method_reactivate, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        reactivateDeviceDialog();                        
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
                        Intent i = new Intent(getApplicationContext(), LauncherActivity.class);
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
                        startActivity(new Intent(getApplicationContext(), LauncherActivity.class));
                        finish();
                    }
                });
                break;
                
            case DIALOG_DEVICE_ACTIVE:
                builder
                .setCancelable(false)
                .setTitle(R.string.tf_unable_to_reactivate_while_in_use_dialog)
                .setMessage(R.string.tf_unable_to_reactivate_while_in_use_dialog_msg)
                .setPositiveButton(R.string.tf_notify_device_owner, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendNotification();                        
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeDialog(DIALOG_DEVICE_ACTIVE);                        
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
                
//            case DIALOG_BETA_PREVIEW:
//                builder
//                .setCancelable(false)
//                .setIcon(R.drawable.splash_beta_blue)
//                .setTitle("Technology Preview")
//                .setMessage(R.string.tf_beta_release_msg)
//                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        removeDialog(DIALOG_BETA_PREVIEW);
//                        showDialog(DIALOG_BEGIN_REGISTRATION);
//                    }
//                });
//                break;                
        }
        
        return builder.create();
    }
    
    private void reactivateDeviceDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.reactivate_device, null);        
        
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
                    reactivateDeviceDialog();
                } else {
                    if (verifyDeviceReactivation(pin)) {
                        showDialog(DIALOG_DEVICE_REGISTERED);
                    } else {
                        if (mOptionToNotifyDeviceUser)
                            mOptionToNotifyDeviceUser = false;
                        else
                            reactivateDeviceDialog();
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
                    switch (verifyDeviceRegistration(email)) {
                    case DEVICE_REGISTRATION_VERIFIED:
                        showDialog(DIALOG_DEVICE_REGISTERED);
                        break;
                    case DEVICE_REGISTRATION_FAILED:
                        // Error message communicated via Toast
                        registerDeviceDialog();
                        break;
                    case DEVICE_REGISTRATION_LIMITED:
                        // New dialog displayed by verifyDeviceRegistration()
                        dialog.cancel();                    
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
                    showDialog(DIALOG_DEVICE_REGISTRATION_METHOD);
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
        .setTitle(R.string.tf_new_account)
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
        
        String verifyUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/send/reminder";
        
        String postResult = HttpUtils.postUrlData(verifyUrl, params);
        JSONObject verify;
        
        try {            
            verify = (JSONObject) new JSONTokener(postResult).nextValue();
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
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing postResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }        
    }    
    
    private boolean sendNotification()
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("licenceNumber", mAccountNumber));
        params.add(new BasicNameValuePair("licenceKey", mAccountKey));
        params.add(new BasicNameValuePair("devicePin", mDevicePin));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnlineState().getDeviceFingerprint()));        
        
        String verifyUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/send/notice";
        
        String postResult = HttpUtils.postUrlData(verifyUrl, params);
        JSONObject verify;
        
        try {            
            verify = (JSONObject) new JSONTokener(postResult).nextValue();
            
            String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_device_owner_notified_of_pin_use), Toast.LENGTH_LONG).show();
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_unable_to_notify_device_user), Toast.LENGTH_LONG).show();
                String reason = verify.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                Log.w(Collect.LOGTAG, t + "unable to notify device user: " + reason);
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing postResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                        
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
        Collect.getInstance().getInformOnlineState().setAccountKey(container.getString("accountKey"));
        Collect.getInstance().getInformOnlineState().setAccountNumber(container.getString("accountNumber"));
        Collect.getInstance().getInformOnlineState().setAccountOwner(container.getBoolean("accountOwner"));
        Collect.getInstance().getInformOnlineState().setDefaultDatabase(container.getString("defaultDb"));
        Collect.getInstance().getInformOnlineState().setDeviceId(container.getString("deviceId"));
        Collect.getInstance().getInformOnlineState().setDeviceKey(container.getString("deviceKey"));
        Collect.getInstance().getInformOnlineState().setDevicePin(container.getString("devicePin"));
    }
    
    // The special dialog that is shown when someone hits the seat licence limit for the account
    private void showLicenceLimitDialog(String seatLicenceLimit, String planType)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_info)
        .setTitle(R.string.tf_licence_limit_reached_dialog)
        .setMessage(getString(R.string.tf_licence_limit_reached_dialog_msg, seatLicenceLimit, planType))
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
            
            String verifyUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/verify/licence";
            String postResult = HttpUtils.postUrlData(verifyUrl, params);
            JSONObject verify;
            
            try {
                verify = (JSONObject) new JSONTokener(postResult).nextValue();                
                
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
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                   
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                    
                    return false;
                }
            } catch (NullPointerException e) {
                // Communication error
                Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");               
                Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return false;
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                e.printStackTrace();                
                return false;
            }
        }
    } // End verifyAccountLicence()
    
    private int verifyDeviceRegistration(String email)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("licenceNumber", mAccountNumber));
        params.add(new BasicNameValuePair("licenceKey", mAccountKey));
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnlineState().getDeviceFingerprint()));
        
        String registerDeviceUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/register/device";
        String postResult = HttpUtils.postUrlData(registerDeviceUrl, params);
        JSONObject verify;
        
        try {            
            verify = (JSONObject) new JSONTokener(postResult).nextValue();
            
            String result = verify.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                // Store account information to preferences
                mContactEmailAddress = email;
                setRegistrationInformation(verify);
                return DEVICE_REGISTRATION_VERIFIED;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = verify.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_EMAIL)) {
                    Log.w(Collect.LOGTAG, t + "invalid email address \"" + email + "\"");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_email), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_EMAIL_ASSIGNED)) {
                    Log.i(Collect.LOGTAG, t + "email address \"" + email + "\" already assigned to an account");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_registration_error_email_in_use), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_LICENCE_LIMIT)) {
                    Log.i(Collect.LOGTAG, t + "account seat licence limit reached");
                    showLicenceLimitDialog(verify.optString("licencedSeats", "?"), verify.optString("planType", "?"));
                    return DEVICE_REGISTRATION_LIMITED;
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
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
        
        // Generic "something went wrong" (announced via a toast)
        return DEVICE_REGISTRATION_FAILED;
    } // End verifyDeviceRegistration()
    
    private boolean verifyDeviceReactivation(String devicePin)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("licenceNumber", mAccountNumber));
        params.add(new BasicNameValuePair("licenceKey", mAccountKey));
        params.add(new BasicNameValuePair("devicePin", devicePin));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnlineState().getDeviceFingerprint()));
        
        String registerReactivateUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/register/reactivate";
        String postResult = HttpUtils.postUrlData(registerReactivateUrl, params);
        JSONObject reactivation;
        
        try {
            reactivation = (JSONObject) new JSONTokener(postResult).nextValue();
            
            String result = reactivation.optString(InformOnlineState.RESULT, InformOnlineState.FAILURE);
            
            if (result.equals(InformOnlineState.OK)) {
                setRegistrationInformation(reactivation);
                return true;
            } else if (result.equals(InformOnlineState.FAILURE)) {
                String reason = reactivation.optString(InformOnlineState.REASON, REASON_UNKNOWN);
                
                if (reason.equals(REASON_INVALID_PIN)) {
                    Log.i(Collect.LOGTAG, t + "reactivation failed (invalid PIN)");
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_invalid_pin), Toast.LENGTH_LONG).show();
                } else if (reason.equals(REASON_DEVICE_ACTIVE)) {
                    Log.i(Collect.LOGTAG, t + "reactivation failed (device active)");
                    mOptionToNotifyDeviceUser = true;
                    showDialog(DIALOG_DEVICE_ACTIVE);
                } else if (reason.equals(REASON_REACTIVATION_DELAYED)) {
                    Log.i(Collect.LOGTAG, t + "reactivation delayed for " + reactivation.getString("delay") + "ms");
                    
                    String approximation = " ";
                    String period = "";
                    String unit = "";

                    // The delay time is returned in milliseconds
                    Integer delayMilliseconds = reactivation.getInt("delay");
                    
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
                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_reactivation_delayed_reason), Toast.LENGTH_LONG).show();                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_reactivation_delayed_wait, approximation, period, unit), Toast.LENGTH_LONG).show();
                } else {
                    // Unhandled response
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing postResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    } // End verifyDeviceReactivation()
    
    private boolean verifyNewAccount(String email)
    {
        // Data to POST
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("fingerprint", Collect.getInstance().getInformOnlineState().getDeviceFingerprint()));
        
        String verifyUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/register/account";
        
        String postResult = HttpUtils.postUrlData(verifyUrl, params);
        JSONObject verify;
        
        try {            
            verify = (JSONObject) new JSONTokener(postResult).nextValue();
            
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
                    Log.e(Collect.LOGTAG, t + "system error while processing postResult");                    
                    Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
                }   
                
                return false;
            } else {
                // Something bad happened
                Log.e(Collect.LOGTAG, t + "system error while processing postResult");                
                Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();                
                return false;
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no postResult to parse.  Communication error with node.js server?");                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_communication_error_try_again), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse postResult " + postResult);                        
            Toast.makeText(getApplicationContext(), getString(R.string.tf_system_error_dialog_msg), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }         
    } // End verifyNewAccount()
}
