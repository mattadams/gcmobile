package com.radicaldynamic.gcmobile.android.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.activities.FileDialog;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.SelectFieldImportListener;
import com.radicaldynamic.groupinform.tasks.SelectFieldImportTask;
import com.radicaldynamic.groupinform.xform.Field;

public class SelectFieldImportActivity extends Activity implements SelectFieldImportListener
{
    private static final String t = "SelectFieldImportActivity: ";
    
    private static final int RESULT_FILE_SELECTED = 0;
    
    private static final int DIALOG_IMPORT_COMPLETE = 0;
    private static final int DIALOG_IMPORT_EMPTY = 1;
    private static final int DIALOG_IMPORT_FAILED = 2;
    private static final int DIALOG_IMPORTING = 3;
    
    private static final String KEY_DIALOG_MSG = "dialog_msg";
    private static final String KEY_SELECTED_FILE = "selected_file";
    
    private SelectFieldImportTask mSelectFieldImportTask;
    
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private ViewFlipper mViewFlipper;
    private Button mNextStep;
    private Button mPreviousStep;
    private RadioButton mReplaceList;
    private CheckBox mSkipFirstRow;

    private String mDialogMsg = null;
    private String mSelectedFile = null;
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        case RESULT_FILE_SELECTED:
            mSelectedFile = intent.getStringExtra(FileDialog.RESULT_PATH);
            ((TextView) findViewById(R.id.stepSelectedFileName)).setText(new File(mSelectedFile).getName());
            break;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fb_select_import);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_list_import_wizard));
                
        mViewFlipper = (ViewFlipper) findViewById(R.id.wizardSteps);
        
        mNextStep = (Button) findViewById(R.id.stepNext);
        mNextStep.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (mViewFlipper.getCurrentView().getId() == R.id.wizardStep3) {
                    // Perform import
                    processCsvFile(false);
                } else {
                    // Proceed to next step
                    if (mSelectedFile == null) {
                        // User may not proceed if a file is not selected for import
                        Toast.makeText(getApplicationContext(), "Select a CSV file before continuing", Toast.LENGTH_LONG).show();
                    } else {
                        mViewFlipper.showNext();
                        updateWizardNavigation();                    
                    }                    
                }
            }   
        });
        
        mPreviousStep = (Button) findViewById(R.id.stepPrevious);
        mPreviousStep.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) 
            {
                mViewFlipper.showPrevious();
                updateWizardNavigation();
            }            
        });
        
        mReplaceList = (RadioButton) findViewById(R.id.importReplace);
        
        mSkipFirstRow = (CheckBox) findViewById(R.id.importSkipFirstRow);
        mSkipFirstRow.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
            {
                processCsvFile(true);
            }
        });
        
        Button selectFile = (Button) findViewById(R.id.importFileSelection);
        selectFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0)
            {
                Intent intent = new Intent(SelectFieldImportActivity.this, FileDialog.class);
                intent.putExtra(FileDialog.SELECTION_MODE, FileDialog.MODE_OPEN);
                intent.putExtra(FileDialog.START_PATH, "/sdcard");
                intent.putExtra(FileDialog.WINDOW_TITLE, "Select CSV File To Import");
                startActivityForResult(intent, RESULT_FILE_SELECTED);
            }
        });
        
        if (savedInstanceState == null) {
            
        } else {            
            if (savedInstanceState.containsKey(KEY_DIALOG_MSG))
                mDialogMsg = savedInstanceState.getString(KEY_DIALOG_MSG);

            if (savedInstanceState.containsKey(KEY_SELECTED_FILE))
                mSelectedFile = savedInstanceState.getString(KEY_SELECTED_FILE);
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mAlertDialog = null; 
        
        switch (id) {
        case DIALOG_IMPORT_COMPLETE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle("Import Successful")
            .setMessage(mDialogMsg);
            
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
            
            mAlertDialog = builder.create();
            break;
        
        case DIALOG_IMPORT_EMPTY:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle("Nothing To Import")
            .setMessage("The file that you selected does not contain any records to import.\n\nPlease select another file and try again.");
            
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                    mViewFlipper.setDisplayedChild(0);
                }
            });
            
            mAlertDialog = builder.create();
            break;
            
        case DIALOG_IMPORT_FAILED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle("Import Failed")
            .setMessage("The file that you selected cannot be imported. Either it is not a CSV file or the format of the CSV file is incorrect.\n\nPlease see the Form Editor Guide for further instructions.\n\nhttp://groupcomplete.com/help");
            
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                    mViewFlipper.setDisplayedChild(0);
                    updateWizardNavigation();
                }
            });
            
            mAlertDialog = builder.create();
            break;
            
        case DIALOG_IMPORTING:
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(mDialogMsg);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            return mProgressDialog;
        }
        
        return mAlertDialog;
    }
    
    @Override
    protected void onDestroy() 
    {
        // Destroy logic goes above this line
        super.onDestroy();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Handle resume of definition import task
        if (mSelectFieldImportTask != null) {
            mSelectFieldImportTask.setListener(this);
            
            if (mSelectFieldImportTask != null && mSelectFieldImportTask.getStatus() == AsyncTask.Status.FINISHED) {
                dismissDialog(DIALOG_IMPORTING);
            }
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance()
    {
        if (mSelectFieldImportTask != null && mSelectFieldImportTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSelectFieldImportTask;
        
        return null;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_DIALOG_MSG, mDialogMsg);
        outState.putString(KEY_SELECTED_FILE, mSelectedFile);
    }

    @Override
    public void importTaskFinished(Bundle data, ArrayList<List<String>> importedRecords) 
    {
        dismissDialog(DIALOG_IMPORTING);
        
        Iterator<List<String>> records = importedRecords.iterator();
        
        if (data.getBoolean(SelectFieldImportListener.SUCCESSFUL, false)) {
            // If import is empty, show message and abort
            if (importedRecords.isEmpty()) {
                showDialog(DIALOG_IMPORT_EMPTY);
                return;
            }
            
            switch (data.getInt(SelectFieldImportListener.MODE)) {
            case SelectFieldImportListener.MODE_IMPORT:
                if (data.getBoolean(SelectFieldImportListener.CLEAR_LIST, false)) {
                    if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "reset select list");
                    Collect.getInstance().getFormBuilderState().getField().getChildren().clear();
                }
                
                while (records.hasNext()) {
                    List<String> entry = records.next();
                    Field item = new Field();
                    
                    item.setEmpty(false);
                    item.setType("item");
                    item.setLabel(entry.get(0).trim());
                    item.setItemValue(entry.get(1).trim().replaceAll("[^a-zA-Z0-9_-]", ""));
                    
                    // Fall back in case value cannot be derived
                    if (item.getItemValue().length() == 0) {
                        item.setItemValue(UUID.randomUUID().toString());
                    }
                    
                    Collect.getInstance().getFormBuilderState().getField().getChildren().add(item);
                }
                
                mDialogMsg = importedRecords.size() + " items were added to the list.";
                showDialog(DIALOG_IMPORT_COMPLETE);
                break;
            
            case SelectFieldImportListener.MODE_PREVIEW:
                TableLayout table = (TableLayout) findViewById(R.id.importPreviewTable);
                table.removeAllViews();                
                
                while (records.hasNext()) {
                    List<String> cells = records.next();
                    TableRow row;
                    TextView c;
                    
                    if (table.getChildCount() == 0) {
                        row = new TableRow(this); 
                        
                        c = new TextView(this);
                        c.setText("COLUMN #1: LABEL");
                        c.setPadding(3, 3, 3, 3);
                        row.addView(c);                    

                        c = new TextView(this);
                        c.setText("COLUMN #2: VALUE");
                        c.setPadding(3, 3, 3, 3);
                        row.addView(c);
                        
                        table.addView(row, new TableLayout.LayoutParams());
                    }
                    
                    row = new TableRow(this);
                    
                    for (int i = 0; i < cells.size(); i++) {
                        c = new TextView(this);
                        c.setText(cells.get(i));
                        c.setPadding(5, 5, 5, 5);
                        c.setBackgroundDrawable(getResources().getDrawable(R.drawable.table_cell));
                        c.setTextColor(R.color.solid_white);
                        row.addView(c);
                    }
                    
                    table.addView(row, new TableLayout.LayoutParams());
                }
                
                break;
                
            }
        } else {
            showDialog(DIALOG_IMPORT_FAILED);
        }
    }

    private void updateWizardNavigation()
    {        
        switch (mViewFlipper.getCurrentView().getId()) {
        case R.id.wizardStep1:
            mPreviousStep.setEnabled(false);
            mNextStep.setText("Next  ");
            break;
        case R.id.wizardStep2:
            mPreviousStep.setEnabled(true);
            mNextStep.setText("Next  ");
            break;
        case R.id.wizardStep3:
            processCsvFile(true);
            mPreviousStep.setEnabled(true);
            mNextStep.setText("Import  ");
            break;
        }
    }
    
    private void processCsvFile(boolean preview)
    {
        mSelectFieldImportTask = new SelectFieldImportTask();
        mSelectFieldImportTask.setListener(this);
        mSelectFieldImportTask.setImportClearList(mReplaceList.isChecked());
        mSelectFieldImportTask.setImportFilePath(mSelectedFile);
        mSelectFieldImportTask.setImportSkipFirstLine(mSkipFirstRow.isChecked());
        
        if (preview) {
            // Preview
            mDialogMsg = "Importing preview...";
            mSelectFieldImportTask.setImportMode(SelectFieldImportListener.MODE_PREVIEW);
        } else {
            mDialogMsg = "Importing records...";
            mSelectFieldImportTask.setImportMode(SelectFieldImportListener.MODE_IMPORT);
        }        

        showDialog(DIALOG_IMPORTING);
        mSelectFieldImportTask.execute();
    }
}
