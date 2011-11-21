package com.radicaldynamic.groupinform.activities;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.listeners.DataExportListener;
import com.radicaldynamic.groupinform.tasks.DataExportTask;

public class DataExportActivity extends Activity implements DataExportListener
{
    private static final String t = "DataExportActivity: ";
    
    // For use with mProgressHandler
    public static final String KEY_PROGRESS_MSG = "key_progress_msg";
    
    // Export options
    public static final String KEY_EXPORT_DRAFT = "export_draft";
    public static final String KEY_EXPORT_COMPLETED = "export_complete";
    public static final String KEY_OUTPUT_EXTERNAL = "output_external";
    public static final String KEY_OUTPUT_SEND = "output_send";
    public static final String KEY_OUTPUT_ZIP = "external_zip";
    public static final String KEY_OUTPUT_MEDIA_FILES = "output_media_files";
    public static final String KEY_OUTPUT_XFORM_FILES = "output_xform_files";
    public static final String KEY_OUTPUT_RECORD_METADATA = "output_record_metadata";
    
    // For handling activity restarts
    private static final String KEY_DATA_EXPORT_TASK = "data_export_task";
    private static final String KEY_FORM_DEFINITION  = "form_definition";
    
    private static final int DIALOG_DEFINITION_UNAVAILABLE = 1;
    private static final int DIALOG_EMAIL_DEPENDENCY = 2;
    private static final int DIALOG_OPTIONS_REQUIRED = 3;    
    
    private Dialog mDialog;
    private ProgressDialog mProgressDialog;
    
    private DataExportTask mDataExportTask;
    
    private CheckBox mExportDraft;
    private CheckBox mExportCompleted;
    private CheckBox mOutputExternal;
    private CheckBox mOutputSend;
    private CheckBox mOutputZip;
    private CheckBox mOutputMediaFiles;
    private CheckBox mOutputXFormFiles;
    private CheckBox mOutputRecordMetadata;
    
    private FormDefinition mFormDefinition;
    
    private Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) 
        {
            switch (msg.what) {     
            case DataExportTask.ERROR:
                AlertDialog.Builder builder = new AlertDialog.Builder(DataExportActivity.this);
                
                // TODO: write or pass a proper error message
                builder.setMessage(DataExportActivity.this.getString(R.string.tf_unknown_error))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { 
                        public void onClick(DialogInterface dialog, int id) { 
                            finish();                            
                        }
                    });
                
                AlertDialog alert = builder.create();
                alert.show();                
                break;

            case DataExportTask.PROGRESS:
                Bundle data = msg.getData();              
                mProgressDialog.setMessage(data.getString(KEY_PROGRESS_MSG));
                break;

            case DataExportTask.COMPLETE:
                mProgressDialog.dismiss();
                break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.data_export);

        mExportDraft        = (CheckBox) findViewById(R.id.exportDraft);
        mExportCompleted    = (CheckBox) findViewById(R.id.exportCompleted);
        mOutputExternal     = (CheckBox) findViewById(R.id.outputExternal);
        mOutputSend         = (CheckBox) findViewById(R.id.outputSend);
        mOutputZip          = (CheckBox) findViewById(R.id.outputZip);
        mOutputMediaFiles   = (CheckBox) findViewById(R.id.outputMediaFiles);
        mOutputXFormFiles   = (CheckBox) findViewById(R.id.outputXFormFiles);
        mOutputRecordMetadata = (CheckBox) findViewById(R.id.outputRecordMetadata);
        
        Button beginExport = (Button) findViewById(R.id.beginExport);
        
        beginExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (!verifyFilterOptions() || !verifyDestionationOptions()) {
                    showDialog(DIALOG_OPTIONS_REQUIRED);
                } else {
                    mProgressDialog = new ProgressDialog(DataExportActivity.this);
                    mProgressDialog.setCancelable(false);                
                    mProgressDialog.setMessage(getString(R.string.tf_preparing_to_export));
                    mProgressDialog.show();

                    new Thread() {
                        public void run() {
                            try {
                                mDataExportTask = new DataExportTask();
                                mDataExportTask.setDataExportListener(DataExportActivity.this);
                                mDataExportTask.execute(mProgressHandler, mFormDefinition, getExportOptions());
                            } catch (Exception e) {
                                e.printStackTrace();
                                mProgressDialog.dismiss();
                                mProgressHandler.sendMessage(mProgressHandler.obtainMessage(DataExportTask.ERROR));
                            }
                        }
                    }.start();
                }                
            }            
        });
        
        // Handle dependencies
        mOutputExternal.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mOutputSend.isChecked()) {
                    mOutputZip.setEnabled(isChecked);
                    mOutputMediaFiles.setEnabled(isChecked);
                    mOutputXFormFiles.setEnabled(isChecked);
                    mOutputRecordMetadata.setEnabled(isChecked);
                }

                if (!mOutputMediaFiles.isChecked() && isChecked)
                    mOutputMediaFiles.setChecked(true);

                if (!mOutputRecordMetadata.isChecked() && isChecked)
                    mOutputRecordMetadata.setChecked(true);
            }           
        });
        
        // Handle dependencies
        mOutputSend.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mOutputExternal.isChecked()) {
                    mOutputZip.setEnabled(isChecked);
                    mOutputMediaFiles.setEnabled(isChecked);
                    mOutputXFormFiles.setEnabled(isChecked);
                    mOutputRecordMetadata.setEnabled(isChecked);
                }
                
                if (!mOutputZip.isChecked() && isChecked)
                    mOutputZip.setChecked(true);

                if (!mOutputMediaFiles.isChecked() && isChecked)
                    mOutputMediaFiles.setChecked(true);

                if (!mOutputRecordMetadata.isChecked() && isChecked)
                    mOutputRecordMetadata.setChecked(true);
            }           
        });
        
        // Handle dependencies
        mOutputZip.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    if (mOutputSend.isChecked()) {
                        showDialog(DIALOG_EMAIL_DEPENDENCY);
                        mOutputZip.setChecked(true);
                    }
                }
            }
        });

        Object data = getLastNonConfigurationInstance();
        
        if (data instanceof HashMap<?, ?>) {
            // Continue data export
            if (((HashMap<?, ?>) data).containsKey(KEY_DATA_EXPORT_TASK)) {
                // Recreate progress handler for UI
                mProgressDialog = new ProgressDialog(DataExportActivity.this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMessage("Resuming export...");
                mProgressDialog.show();

                mDataExportTask = (DataExportTask) ((HashMap<?, ?>) data).get(KEY_DATA_EXPORT_TASK);
                mDataExportTask.setHandler(mProgressHandler);
            }
            
            // Save us refetching from the database
            if (((HashMap<?, ?>) data).containsKey(KEY_FORM_DEFINITION)) {
                mFormDefinition = (FormDefinition) ((HashMap<?, ?>) data).get(KEY_FORM_DEFINITION);
            }
        } else {
            Intent intent = getIntent();

            if (intent == null) {
                // Defaults?
            } else {
                String id = intent.getStringExtra(FormEntryActivity.KEY_FORMPATH);

                try {
                    mFormDefinition = Collect.getInstance().getDbService().getDb().get(FormDefinition.class, id);
                } catch (Exception e ){
                    Log.e(Collect.LOGTAG, t + "unexpected exception while retrieving form definition document: " + e.toString());
                    e.printStackTrace();

                    showDialog(DIALOG_DEFINITION_UNAVAILABLE);
                }
            }
        }

        if (mFormDefinition != null) {
            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_export_from) + " " + mFormDefinition.getName());
        }
    }
    
    public Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mDialog = null;
        
        switch (id) {
        case DIALOG_DEFINITION_UNAVAILABLE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_load_instances_dialog)
            .setMessage(R.string.tf_unable_to_load_instances_dialog_msg);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    dialog.cancel();
                    finish();
                }
            });
    
            mDialog = builder.create();
            break;
            
        case DIALOG_EMAIL_DEPENDENCY:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_export_send_dependency)
            .setMessage(R.string.tf_unable_to_export_send_dependency_msg);
            
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
        
        case DIALOG_OPTIONS_REQUIRED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_export_data_missing_options_dialog)
            .setMessage(R.string.tf_unable_to_export_data_missing_options_dialog_msg);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
        }

        return mDialog;
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.cancel();
    }

    @Override
    public Object onRetainNonConfigurationInstance() 
    {
        HashMap<String, Object> data = new HashMap<String, Object>();
        
        // If an export task is running, pass it to the next thread
        if (mDataExportTask != null && mDataExportTask.getStatus() != AsyncTask.Status.FINISHED)
            data.put(KEY_DATA_EXPORT_TASK, mDataExportTask);
        
        if (mFormDefinition != null)
            data.put(KEY_FORM_DEFINITION, mFormDefinition);
        
        return data;
    }
    
    @Override
    public void exportComplete(final Bundle data) 
    {
        final String tt = t + "exportComplete(): ";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_info)
        .setTitle("Export Successful")
        .setMessage(data.getString(DataExportListener.KEY_MESSAGE));
        
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        
        mOutputSend = (CheckBox) findViewById(R.id.outputSend);
        
        if (mOutputSend.isChecked()) {
            builder.setNeutralButton(getString(R.string.tf_send), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    String attachment = "file://" + data.getString(DataExportListener.KEY_EMAIL_ATTACHMENT);
                    Log.d(Collect.LOGTAG, tt + "Path to exported attachment is " + attachment);
                    
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    i.setType("application/zip");
                    i.putExtra(Intent.EXTRA_STREAM, Uri.parse(attachment));
                    startActivity(i);
                }
            });
        }
        
        mDialog = builder.create();
        mDialog.show();        
    }

    @Override
    public void exportError(String errorMsg) 
    {
        // Close progress automatically
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
        .setCancelable(false)
        .setIcon(R.drawable.ic_dialog_alert)
        .setTitle("Export Problem")
        .setMessage(errorMsg);
        
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        
        mDialog = builder.create();
        mDialog.show();
    }
    
    /*
     * Examine all output option controls and set a bundle that will represent options
     * selected.  Bundle will be passed to export task.
     */
    private Bundle getExportOptions()
    {
        Bundle data = new Bundle();
        
        if (mExportDraft.isChecked())
            data.putBoolean(KEY_EXPORT_DRAFT, true);
        
        if (mExportCompleted.isChecked())
            data.putBoolean(KEY_EXPORT_COMPLETED, true);
        
        if (mOutputExternal.isEnabled() && mOutputExternal.isChecked())
            data.putBoolean(KEY_OUTPUT_EXTERNAL, true);
        
        if (mOutputSend.isEnabled() && mOutputSend.isChecked()) {
            data.putBoolean(KEY_OUTPUT_SEND, true);
        }
        
        if (mOutputZip.isEnabled() && mOutputZip.isChecked())
            data.putBoolean(KEY_OUTPUT_ZIP, true);
        
        if (mOutputMediaFiles.isEnabled() && mOutputMediaFiles.isChecked())
            data.putBoolean(KEY_OUTPUT_MEDIA_FILES, true);
        
        if (mOutputXFormFiles.isEnabled() && mOutputXFormFiles.isChecked())
            data.putBoolean(KEY_OUTPUT_XFORM_FILES, true);

        if (mOutputRecordMetadata.isEnabled() && mOutputRecordMetadata.isChecked())
            data.putBoolean(KEY_OUTPUT_RECORD_METADATA, true);
        
        return data;        
    }    
    
    /*
     * Ensure that at least one output option is selected
     */
    private boolean verifyDestionationOptions()
    {
        if (mOutputExternal.isChecked() || mOutputSend.isChecked())
            return true;
        
        return false;  
    }

    /*
     * Ensure that at least one filter option is selected
     */
    private boolean verifyFilterOptions()
    {
        if (mExportDraft.isChecked() || mExportCompleted.isChecked())
            return true;

        return false;
    }
}
