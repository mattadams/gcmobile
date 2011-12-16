/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.groupinform.activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.ReplicationStatus;
import org.odk.collect.android.utilities.FileUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

import com.radicaldynamic.gcmobile.android.activities.DataExportActivity;
import com.radicaldynamic.gcmobile.android.activities.DataImportActivity;
import com.radicaldynamic.gcmobile.android.build.FieldList;
import com.radicaldynamic.gcmobile.android.dialogs.FilterByAssignmentDialog;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.BrowserLongListAdapter;
import com.radicaldynamic.groupinform.adapters.BrowserShortListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.listeners.DefinitionImportListener;
import com.radicaldynamic.groupinform.listeners.SynchronizeFoldersListener;
import com.radicaldynamic.groupinform.listeners.ToggleOnlineStateListener;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.repositories.FormDefinitionRepo;
import com.radicaldynamic.groupinform.repositories.FormInstanceRepo;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.tasks.DefinitionImportTask;
import com.radicaldynamic.groupinform.tasks.SynchronizeFoldersTask;
import com.radicaldynamic.groupinform.tasks.ToggleOnlineStateTask;
import com.radicaldynamic.groupinform.utilities.Base64Coder;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;
import com.radicaldynamic.groupinform.xform.FormReader;
import com.radicaldynamic.groupinform.xform.FormWriter;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class BrowserActivity extends ListActivity implements DefinitionImportListener, ToggleOnlineStateListener, SynchronizeFoldersListener
{
    private static final String t = "BrowserActivity: ";
    
    // Dialog status codes    
    private static final int DIALOG_COPY_TO_FOLDER = 0;
    private static final int DIALOG_CREATE_TEMPLATE = 1;
    private static final int DIALOG_FOLDER_OUTDATED = 2;
    private static final int DIALOG_FOLDER_UNAVAILABLE = 3;
    private static final int DIALOG_FORM_BUILDER_LAUNCH_ERROR = 4; 
    private static final int DIALOG_IMPORTING_TEMPLATE = 5;
    private static final int DIALOG_INSTANCES_UNAVAILABLE = 6;
    private static final int DIALOG_OFFLINE_ATTEMPT_FAILED = 7;
    private static final int DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS = 8;
    private static final int DIALOG_ONLINE_ATTEMPT_FAILED = 9;
    private static final int DIALOG_ONLINE_STATE_CHANGING = 10;
    private static final int DIALOG_REMOVE_FORM = 11;
    private static final int DIALOG_RENAME_TEMPLATE = 12;
    private static final int DIALOG_SEARCH_FILTER = 13;
    private static final int DIALOG_TOGGLE_ONLINE_STATE = 14;
    private static final int DIALOG_UNABLE_TO_COPY_DUPLICATE = 15;
    private static final int DIALOG_UNABLE_TO_IMPORT_TEMPLATE = 16;
    private static final int DIALOG_UNABLE_TO_RENAME_DUPLICATE = 17;
    private static final int DIALOG_UPDATING_FOLDER = 18;
    
    // Keys for option menu items
    private static final int MENU_OPTION_REFRESH    = 0;
    private static final int MENU_OPTION_SEARCH     = 1;
    private static final int MENU_OPTION_FOLDERS    = 2;
    private static final int MENU_OPTION_NEWFORM    = 3;
    private static final int MENU_OPTION_ODKTOOLS   = 4;
    private static final int MENU_OPTION_INFO       = 5;
    
    // Keys for context menu items
    private static final int MENU_CONTEXT_COPY      = 0;
    private static final int MENU_CONTEXT_EDIT      = 1;
    private static final int MENU_CONTEXT_EXPORT    = 2;
    private static final int MENU_CONTEXT_IMPORT    = 3;
    private static final int MENU_CONTEXT_REMOVE    = 4;
    private static final int MENU_CONTEXT_RENAME    = 5;
    
    // Keys for persistence between screen orientation changes
    private static final String KEY_COPY_TO_FOLDER_AS   = "copy_to_folder_as";
    private static final String KEY_COPY_TO_FOLDER_ID   = "copy_to_folder_id";
    private static final String KEY_COPY_TO_FOLDER_NAME = "copy_to_folder_name";
    private static final String KEY_DIALOG_MESSAGE      = "dialog_msg";
    private static final String KEY_FORM_DEFINITION     = "form_definition_doc";
    private static final String KEY_SELECTED_DB         = "selected_db";
    private static final String KEY_SEARCH_FILTER       = "search_filter";
    private static final String KEY_TASK_SELECTOR       = "task_selector";
    
    // Search filter keys
    private static final String KEY_SEARCH_BY_ASSIGNMENT        = "search_by_assignment";
    public static final String KEY_SEARCH_BY_ASSIGNMENT_IDS     = "search_by_assignment_ids";
    private static final String KEY_SEARCH_BY_STATUS            = "search_by_status";
        
    // Request codes for returning data from specified intent 
    private static final int RESULT_ABOUT   = 1;
    private static final int RESULT_COPY    = 2;    
    private static final int RESULT_IMPORT  = 3;

    private FormDefinition mFormDefinition;     // Stash for a selected form definition
    
    private Bundle mSearchFilter = null;
    
    private String mCopyToFolderId;             // Data passed back from user selection on AccountFolderList
    private String mCopyToFolderName;           // Same
    private String mCopyToFolderAs;             // Used to pass to DIALOG_UNABLE_TO_COPY_DUPLICATE
    private String mSelectedDatabase;           // To save & restore the currently selected database

    private CopyToFolderTask mCopyToFolderTask;
    private DefinitionImportTask mDefinitionImportTask;
    private RefreshViewTask mRefreshViewTask;
    private RemoveDefinitionTask mRemoveDefinitionTask;
    private RenameDefinitionTask mRenameDefinitionTask;
    private SynchronizeFoldersTask mSynchronizeFoldersTask;
    private ToggleOnlineStateTask mToggleOnlineStateTask;
    private UpdateFolderTask mUpdateFolderTask;
    
    private Dialog mDialog;
    private String mDialogMessage;              // Custom message consumed by onCreateDialog()
    private ProgressDialog mProgressDialog;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);       
        setContentView(R.layout.browser);                

        // Load our custom window title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_browser_activity);
        
        // Initiate and populate spinner to filter form browser on the basis of the currently selected task
        ArrayAdapter<String> taskSpinnerOptions = new ArrayAdapter(this, android.R.layout.simple_spinner_item);        
        String [] taskOptions = getResources().getStringArray(R.array.tf_task_selector_options);
        
        // Filter tasks for certain device roles
        for (String t : taskOptions) {
            if (Collect.getInstance().getInformOnlineState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY)) {
                if (!t.equals("Export Records") && !t.equals("Edit Form Templates")) {
                    taskSpinnerOptions.add(t);   
                }
            } else {
                taskSpinnerOptions.add(t);
            }
        }
        
        taskSpinnerOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Associate task spinner with options, set up listener for spinner
        Spinner s1 = (Spinner) findViewById(R.id.taskSpinner);   
        s1.setAdapter(taskSpinnerOptions);     
        
        if (savedInstanceState == null) {
            mDialogMessage = "";
            mSelectedDatabase = null;
        } else {
            // For "copy to folder" operation, restore destination folder
            if (savedInstanceState.containsKey(KEY_COPY_TO_FOLDER_AS))
                mCopyToFolderAs = savedInstanceState.getString(KEY_COPY_TO_FOLDER_AS);

            if (savedInstanceState.containsKey(KEY_COPY_TO_FOLDER_ID))
                mCopyToFolderId = savedInstanceState.getString(KEY_COPY_TO_FOLDER_ID);
            
            if (savedInstanceState.containsKey(KEY_COPY_TO_FOLDER_NAME))
                mCopyToFolderName = savedInstanceState.getString(KEY_COPY_TO_FOLDER_NAME);

            if (savedInstanceState.containsKey(KEY_DIALOG_MESSAGE))
                mDialogMessage = savedInstanceState.getString(KEY_DIALOG_MESSAGE);

            if (savedInstanceState.containsKey(KEY_SELECTED_DB))
                mSelectedDatabase = savedInstanceState.getString(KEY_SELECTED_DB);
            
            if (savedInstanceState.containsKey(KEY_TASK_SELECTOR))
                s1.setSelection(savedInstanceState.getInt(KEY_TASK_SELECTOR), true);
        }
        
        // Retrieve persistent data structures and processes
        Object data = getLastNonConfigurationInstance();
        
        if (data instanceof DefinitionImportTask) {
            mDefinitionImportTask = (DefinitionImportTask) data;
        } else if (data instanceof SynchronizeFoldersTask) {
            mSynchronizeFoldersTask = (SynchronizeFoldersTask) data;
        } else if (data instanceof ToggleOnlineStateTask) { 
            mToggleOnlineStateTask = (ToggleOnlineStateTask) data;
        } else if (data instanceof HashMap<?, ?>) {
            mFormDefinition = (FormDefinition) ((HashMap<String, Object>) data).get(KEY_FORM_DEFINITION);
            mSearchFilter = (Bundle) ((HashMap<String, Object>) data).get(KEY_SEARCH_FILTER);
        }
        
        s1.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (mRefreshViewTask == null || mRefreshViewTask.getStatus() == AsyncTask.Status.FINISHED) 
                    loadScreen();
            }

            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Set up listener for Folder title button
        ((Button) findViewById(R.id.folderTitleButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
            }
        });

        // Set up listener for Online/Offline title button        
        ((Button) findViewById(R.id.onlineStatusTitleButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (Collect.getInstance().getInformOnlineState().hasReplicatedFolders())
                    showDialog(DIALOG_TOGGLE_ONLINE_STATE);
                else 
                    showDialog(DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS);               
            }
        });
    }
    
    @Override
    protected void onDestroy() 
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        
        // Clean up definition import task
        if (mDefinitionImportTask != null) {
            mDefinitionImportTask.setListener(null);
            
            if (mDefinitionImportTask.getStatus() == AsyncTask.Status.FINISHED) {
                mDefinitionImportTask.cancel(true);
            }
        }
        
        // Clean up folder synchronization task
        if (mSynchronizeFoldersTask != null) {
            mSynchronizeFoldersTask.setListener(null);
            
            if (mSynchronizeFoldersTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSynchronizeFoldersTask.cancel(true);
            }
        }
        
        // Clean up online/offline toggle task
        if (mToggleOnlineStateTask != null) {
            mToggleOnlineStateTask.setListener(null);
            
            if (mToggleOnlineStateTask.getStatus() == AsyncTask.Status.FINISHED) {
                mToggleOnlineStateTask.cancel(true);
            }
        }

        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Handle resume of definition import task
        if (mDefinitionImportTask != null) {
            mDefinitionImportTask.setListener(this);
            
            if (mDefinitionImportTask != null && mDefinitionImportTask.getStatus() == AsyncTask.Status.FINISHED) {
                dismissDialog(DIALOG_IMPORTING_TEMPLATE);  
            }
        }        
        
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
        
        // Handle resume of toggle online state offline/online task
        if (mToggleOnlineStateTask != null) {
            mToggleOnlineStateTask.setListener(this);
            
            if (mToggleOnlineStateTask != null && mToggleOnlineStateTask.getStatus() == AsyncTask.Status.FINISHED) {
                removeDialog(DIALOG_ONLINE_STATE_CHANGING);  
            }
        }
        
        loadScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        case RESULT_ABOUT:
            // "Exit" if the user resets GC Mobile
            Intent i = new Intent();
            i.putExtra("exit_app", true);
            setResult(RESULT_OK, i);
            finish();
            break; 
            
        case RESULT_COPY:
            mCopyToFolderId   = intent.getStringExtra(AccountFolderList.KEY_FOLDER_ID);
            mCopyToFolderName = intent.getStringExtra(AccountFolderList.KEY_FOLDER_NAME);
            showDialog(DIALOG_COPY_TO_FOLDER);
            break;
            
        case RESULT_IMPORT:
            showDialog(DIALOG_IMPORTING_TEMPLATE);
            mDefinitionImportTask = new DefinitionImportTask();
            mDefinitionImportTask.setListener(this);
            mDefinitionImportTask.execute(intent.getStringExtra(FileDialog.RESULT_PATH));
            break;
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) 
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        FormDefinition fd = (FormDefinition) getListAdapter().getItem((int) info.id);
        Intent i;
        
        switch (item.getItemId()) {
        case MENU_CONTEXT_COPY:            
            mFormDefinition = fd;            
            i = new Intent(this, AccountFolderList.class);
            i.putExtra(AccountFolderList.KEY_COPY_TO_FOLDER, true);
            startActivityForResult(i, RESULT_COPY);
            return true;
            
        case MENU_CONTEXT_EDIT:
            FormBuilderLauncherTask fbl = new FormBuilderLauncherTask();
            fbl.execute(fd.getId());
            return true;
            
        case MENU_CONTEXT_EXPORT:
            i = new Intent(this, DataExportActivity.class);
            i.putExtra(FormEntryActivity.KEY_FORMPATH, fd.getId());
            startActivity(i);
            return true;
            
        case MENU_CONTEXT_IMPORT:
            i = new Intent(this, DataImportActivity.class);
            i.putExtra(FormEntryActivity.KEY_FORMPATH, fd.getId());
            startActivity(i);
            return true;
            
        case MENU_CONTEXT_REMOVE:
            mFormDefinition = fd;
            showDialog(DIALOG_REMOVE_FORM);
            return true;
            
        case MENU_CONTEXT_RENAME:
            mFormDefinition = fd;
            showDialog(DIALOG_RENAME_TEMPLATE);
            return true;    
            
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {        
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if (!Collect.getInstance().getInformOnlineState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY) 
                && ((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition() != 3) 
        {
            menu.add(0, MENU_CONTEXT_COPY, 0, getString(R.string.tf_copy_to_folder));
            menu.add(0, MENU_CONTEXT_EDIT, 0, getString(R.string.tf_edit_template));
            menu.add(0, MENU_CONTEXT_EXPORT, 0, getString(R.string.tf_export_records));
            menu.add(0, MENU_CONTEXT_IMPORT, 0, getString(R.string.tf_import_records));
            menu.add(0, MENU_CONTEXT_REMOVE, 0, getString(R.string.tf_remove_template));
            menu.add(0, MENU_CONTEXT_RENAME, 0, getString(R.string.tf_rename_template));
        }
    }
    
    public Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);        
        View view = null;
        mDialog = null;        
        
        if (isFinishing()) {
            return mDialog;
        }
        
        switch (id) {
        // User wishes to make a new form
        case DIALOG_CREATE_TEMPLATE:
            view = inflater.inflate(R.layout.dialog_create_or_rename_form, null);
            
            // Set an EditText view to get user input 
            final EditText newFormName = (EditText) view.findViewById(R.id.formName);
            
            builder.setView(view);
            builder.setInverseBackgroundForced(true);
            builder.setTitle(getText(R.string.tf_add_template));
            
            builder.setPositiveButton(getText(R.string.tf_create), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                
                    FormDefinition form = new FormDefinition();
                    form.setName(newFormName.getText().toString().trim());
                    form.setStatus(FormDefinition.Status.placeholder);
                    
                    if (form.getName().length() == 0) {
                        removeDialog(DIALOG_CREATE_TEMPLATE);
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_form_name_required), Toast.LENGTH_LONG).show();                        
                        showDialog(DIALOG_CREATE_TEMPLATE);
                    } else {
                        removeDialog(DIALOG_CREATE_TEMPLATE);
                        new CreateFormDefinitionTask().execute(form);
                    }
                }
            });
            
            builder.setNeutralButton(getString(R.string.tf_import), new DialogInterface.OnClickListener() {                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(BrowserActivity.this, FileDialog.class);
                    intent.putExtra(FileDialog.SELECTION_MODE, FileDialog.MODE_OPEN);
                    intent.putExtra(FileDialog.START_PATH, "/sdcard");
                    intent.putExtra(FileDialog.WINDOW_TITLE, "Select XForm File To Import");
                    startActivityForResult(intent, RESULT_IMPORT);
                }
            });
        
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_CREATE_TEMPLATE);
                }
            });
            
            mDialog = builder.create();
            break;
        
        case DIALOG_COPY_TO_FOLDER:
            view = inflater.inflate(R.layout.dialog_copy_to_folder, null);
            
            // Set an EditText view to get user input 
            final TextView copyDestination = (TextView) view.findViewById(R.id.copyDestination);
            final EditText copyName = (EditText) view.findViewById(R.id.copyName);
            
            copyDestination.setText(mCopyToFolderName);
            copyName.setText(mFormDefinition.getName());
            
            builder
            .setTitle(R.string.tf_copy_to_folder)
            .setView(view)
            .setInverseBackgroundForced(true);
            
            builder.setPositiveButton(getText(R.string.tf_copy), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String copyAsName = copyName.getText().toString().trim();
                    
                    if (copyAsName.length() > 0) {
                        mCopyToFolderAs = copyAsName;
                        mCopyToFolderTask = new CopyToFolderTask();
                        mCopyToFolderTask.execute(mFormDefinition, mCopyToFolderId, copyAsName);
                        removeDialog(DIALOG_COPY_TO_FOLDER);                        
                    } else {
                        removeDialog(DIALOG_COPY_TO_FOLDER);   
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_form_name_required), Toast.LENGTH_LONG).show();
                        showDialog(DIALOG_COPY_TO_FOLDER);
                    }
                }
            });
            
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_COPY_TO_FOLDER);
                }
            });
            
            mDialog = builder.create();
            break;
            
        // Local folder is most likely out-of-date
        case DIALOG_FOLDER_OUTDATED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_folder_outdated_dialog)
            .setMessage(getString(R.string.tf_folder_outdated_dialog_msg, getSelectedFolderName()));
            
            builder.setPositiveButton(getString(R.string.tf_update), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    removeDialog(DIALOG_FOLDER_OUTDATED);
                    mUpdateFolderTask = new UpdateFolderTask();
                    mUpdateFolderTask.execute();
                }
            });
            
            builder.setNeutralButton(getString(R.string.tf_form_folders), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
                    removeDialog(DIALOG_FOLDER_OUTDATED);
                }
            });
            
            mDialog = builder.create();
            break;
            
        // Couldn't connect to DB (for a specific reason)
        case DIALOG_FOLDER_UNAVAILABLE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_folder_unavailable)
            .setMessage(mDialogMessage);
            
            builder.setPositiveButton(getString(R.string.tf_form_folders), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    startActivity(new Intent(BrowserActivity.this, AccountFolderList.class));
                    removeDialog(DIALOG_FOLDER_UNAVAILABLE);
                }
            });
            
            if (!Collect.getInstance().getIoService().isSignedIn()) {
                builder.setNeutralButton(getString(R.string.tf_go_online), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeDialog(DIALOG_FOLDER_UNAVAILABLE);
                        
                        mToggleOnlineStateTask = new ToggleOnlineStateTask();
                        mToggleOnlineStateTask.setListener(BrowserActivity.this);
                        mToggleOnlineStateTask.execute();
                    }
                });
            }
            
            mDialog = builder.create();
            break;        
            
        // Unable to launch form builder (instances present) 
        case DIALOG_FORM_BUILDER_LAUNCH_ERROR:
            builder
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_launch_form_builder_dialog)
            .setMessage(R.string.tf_unable_to_launch_form_builder_dialog_msg);
                
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
            
        // Simple progress dialog to display while importing a definition to the current folder
        case DIALOG_IMPORTING_TEMPLATE:
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getText(R.string.tf_importing_template_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);            
            return mProgressDialog;
            
        // User requested forms (definitions or instances) to be loaded but none could be found 
        case DIALOG_INSTANCES_UNAVAILABLE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_load_instances_dialog)
            .setMessage(R.string.tf_unable_to_load_instances_dialog_msg);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    loadScreen();
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;

        // We can't go offline (user has not selected any databases to be replicated)
        case DIALOG_OFFLINE_MODE_UNAVAILABLE_FOLDERS:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_info)
            .setTitle(R.string.tf_unable_to_go_offline_dialog)
            .setMessage(R.string.tf_unable_to_go_offline_dialog_msg_reason_folders);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });

            mDialog = builder.create();
            
            break;

        // Simple progress dialog for online/offline
        case DIALOG_ONLINE_STATE_CHANGING:
            if (Collect.getInstance().getIoService().isSignedIn())
                mDialog = ProgressDialog.show(this, "", getText(R.string.tf_inform_state_disconnecting));
            else
                mDialog = ProgressDialog.show(this, "", getText(R.string.tf_inform_state_connecting));
            
            break;
            
        // Tried going offline but couldn't
        case DIALOG_OFFLINE_ATTEMPT_FAILED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_go_offline_dialog)
            .setMessage(R.string.tf_unable_to_go_offline_dialog_msg_reason_generic);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    loadScreen();
                    dialog.cancel();
                }
            });

            mDialog = builder.create();
            break;            
            
        // Tried going online but couldn't
        case DIALOG_ONLINE_ATTEMPT_FAILED:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_go_online_dialog)
            .setMessage(R.string.tf_unable_to_go_online_dialog_msg_reason_generic);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    loadScreen();
                    dialog.cancel();
                }
            });

            mDialog = builder.create();
            break;
            
        case DIALOG_REMOVE_FORM:
            String removeFormMessage = getString(R.string.tf_remove_form_without_instances_dialog_msg, mFormDefinition.getName());
            
            try {
                // Determine if draft or complete instances exist for this definition
                if (new FormInstanceRepo(Collect.getInstance().getDbService().getDb()).findByFormId(mFormDefinition.getId()).size() > 0) {
                    removeFormMessage = getString(R.string.tf_remove_form_with_instances_dialog_msg, mFormDefinition.getName());
                }
            } catch (Exception e) {
	        if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unexpected exception while processing DIALOG_REMOVE_FORM");
                e.printStackTrace();
            }
            
            builder
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_remove_form)
            .setMessage(removeFormMessage);

            builder.setPositiveButton(getString(R.string.tf_remove), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    removeDialog(DIALOG_REMOVE_FORM);
                    mRemoveDefinitionTask = new RemoveDefinitionTask();
                    mRemoveDefinitionTask.execute(mFormDefinition);
                }
            });
            
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_REMOVE_FORM);
                }
            });
            
            mDialog = builder.create();
            break;
            
        case DIALOG_RENAME_TEMPLATE:
            view = inflater.inflate(R.layout.dialog_create_or_rename_form, null);
            
            // Set an EditText view to get user input 
            final EditText renamedFormName = (EditText) view.findViewById(R.id.formName);
            TextView renamedFormNameHint = (TextView) view.findViewById(R.id.formNameHint);
            renamedFormNameHint.setText(getString(R.string.tf_rename_template_hint));
            
            builder
            .setView(view)
            .setInverseBackgroundForced(true)
            .setTitle(getText(R.string.tf_rename_template));
            
            renamedFormName.setText(mFormDefinition.getName());
            
            builder.setPositiveButton(getText(R.string.tf_rename), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) { 
                    String newName = renamedFormName.getText().toString().trim();
                    
                    if (newName.length() == 0) {
                        removeDialog(DIALOG_RENAME_TEMPLATE);
                        Toast.makeText(getApplicationContext(), getString(R.string.tf_form_name_required), Toast.LENGTH_LONG).show();                        
                        showDialog(DIALOG_RENAME_TEMPLATE);
                    } else {
                        if (newName.equals(mFormDefinition.getName())) {
                            // Do nothing
                        } else {
                            // Hijack this variable in case we need to display DIALOG_UNABLE_TO_RENAME_DUPLICATE
                            mCopyToFolderAs = newName;
                            
                            mRenameDefinitionTask = new RenameDefinitionTask();
                            mRenameDefinitionTask.execute(mFormDefinition, newName);
                        }
                    }
                }
            });
        
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_RENAME_TEMPLATE);
                }
            });
            
            mDialog = builder.create();
            break;
            
        case DIALOG_SEARCH_FILTER:
            view = inflater.inflate(R.layout.dialog_search_forms, null);
            
            if (mSearchFilter == null)
                mSearchFilter = new Bundle();
            
            // Set up adapter and spinner for assignment filter
            ArrayAdapter<String> filterByAssignmentOptions = 
                new ArrayAdapter<String>(
                        this, 
                        android.R.layout.simple_spinner_item, 
                        new ArrayList<String>(Arrays.asList("Any device", "This device", "Other devices")));
            
            filterByAssignmentOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            
            final Spinner filterByAssignment = (Spinner) view.findViewById(R.id.filterByAssignment);
            filterByAssignment.setAdapter(filterByAssignmentOptions);
            filterByAssignment.setSelection(mSearchFilter.getInt(KEY_SEARCH_BY_ASSIGNMENT, 0));
            filterByAssignment.setOnItemSelectedListener(new OnItemSelectedListener() {            
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
                {
                    // Pop up profile selection dialog
                    if (position == 2 && mSearchFilter.getInt(KEY_SEARCH_BY_ASSIGNMENT, 0) != 2) {
                        FilterByAssignmentDialog assignmentDialog = new FilterByAssignmentDialog(BrowserActivity.this, mSearchFilter);
                        assignmentDialog.show();
                    }
                    
                    mSearchFilter.putInt(KEY_SEARCH_BY_ASSIGNMENT, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) 
                {
                }            
            });
           
            // Set up adapter and spinner for status filter
            ArrayAdapter<String> filterByStatusOptions = 
                new ArrayAdapter<String>(
                        this, 
                        android.R.layout.simple_spinner_item, 
                        new ArrayList<String>(Arrays.asList("Any status", "Complete status", "Draft status")));
            
            filterByStatusOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            
            final Spinner filterByStatus = (Spinner) view.findViewById(R.id.filterByStatus);
            filterByStatus.setAdapter(filterByStatusOptions);
            filterByStatus.setSelection(mSearchFilter.getInt(KEY_SEARCH_BY_STATUS, 0));
            filterByStatus.setOnItemSelectedListener(new OnItemSelectedListener() {            
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
                {
                    mSearchFilter.putInt(KEY_SEARCH_BY_STATUS, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) 
                {
                }            
            });
            
            builder
                .setView(view)
                .setInverseBackgroundForced(true)
                .setTitle("Search Form List");
            
            builder.setPositiveButton(getString(R.string.tf_search), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_SEARCH_FILTER);                
                    
                    if (((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition() != 3)                    
                        ((Spinner) findViewById(R.id.taskSpinner)).setSelection(3, true);
                    else 
                        loadScreen();
                }
            });
            
            builder.setNegativeButton(getString(R.string.tf_clear_results), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_SEARCH_FILTER);    
                    mSearchFilter = null;
                    
                    if (((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition() != 3)                    
                        ((Spinner) findViewById(R.id.taskSpinner)).setSelection(3, true);
                    else 
                        loadScreen();
                }
            });
            
            mDialog = builder.create();
            break;

        case DIALOG_TOGGLE_ONLINE_STATE:
            view = inflater.inflate(R.layout.dialog_toggle_online_state, null);

            final CheckBox synchronizeFolders = (CheckBox) view.findViewById(R.id.synchronizeFolders);
            TextView synchronizeFoldersMessage = (TextView) view.findViewById(R.id.synchronizeFoldersMessage);

            String buttonText;

            builder
            .setView(view)
            .setInverseBackgroundForced(true)
            .setIcon(R.drawable.ic_dialog_info);

            if (Collect.getInstance().getIoService().isSignedIn()) {
                builder.setTitle(getText(R.string.tf_go_offline) + "?");
                synchronizeFoldersMessage.setText(getString(R.string.tf_go_offline_dialog_msg));
                buttonText = getText(R.string.tf_go_offline).toString();

            } else {
                builder.setTitle(getText(R.string.tf_go_online) + "?");
                synchronizeFoldersMessage.setText(getString(R.string.tf_go_online_dialog_msg));
                buttonText = getText(R.string.tf_go_online).toString();
            }

            builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_TOGGLE_ONLINE_STATE);

                    if (Collect.getInstance().getIoService().isSignedIn() && synchronizeFolders.isChecked()) {
                        mSynchronizeFoldersTask = new SynchronizeFoldersTask();
                        mSynchronizeFoldersTask.setListener(BrowserActivity.this);
                        mSynchronizeFoldersTask.setTransferMode(SynchronizeFoldersListener.MODE_SWAP);
                        mSynchronizeFoldersTask.setPostExecuteSwitch(true);
                        mSynchronizeFoldersTask.execute();
                    } else {
                        mToggleOnlineStateTask = new ToggleOnlineStateTask();
                        mToggleOnlineStateTask.setListener(BrowserActivity.this);

                        if (synchronizeFolders.isChecked()) {
                            mToggleOnlineStateTask.setPostExecuteSwitch(true);
                        }

                        mToggleOnlineStateTask.execute();
                    }
                }
            });

            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_TOGGLE_ONLINE_STATE);
                }
            });

            mDialog = builder.create();
            break;            

        case DIALOG_UNABLE_TO_COPY_DUPLICATE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_copy)
            .setMessage(getString(R.string.tf_unable_to_copy_duplicate_dialog_msg, mCopyToFolderName, mCopyToFolderAs));

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_UNABLE_TO_COPY_DUPLICATE);
                    showDialog(DIALOG_COPY_TO_FOLDER);
                }
            });

            mDialog = builder.create();
            break;
            
        case DIALOG_UNABLE_TO_IMPORT_TEMPLATE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_import_template)
            .setMessage(mDialogMessage);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_UNABLE_TO_IMPORT_TEMPLATE);
                }
            });

            mDialog = builder.create();
            break;

        case DIALOG_UNABLE_TO_RENAME_DUPLICATE:
            builder
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_unable_to_rename_dialog)
            .setMessage(getString(R.string.tf_unable_to_rename_dialog_msg, mCopyToFolderAs));

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_UNABLE_TO_RENAME_DUPLICATE);
                    showDialog(DIALOG_RENAME_TEMPLATE);
                }
            });

            mDialog = builder.create();
            break;    

        case DIALOG_UPDATING_FOLDER:
            mDialog = ProgressDialog.show(this, "", getString(R.string.tf_updating_with_param, getSelectedFolderName()));            
            break;
        }
        
        return mDialog;        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_OPTION_REFRESH, 0, getString(R.string.refresh))
            .setIcon(R.drawable.ic_menu_refresh);
        
        menu.add(0, MENU_OPTION_SEARCH, 0, getString(R.string.tf_search))
            .setIcon(R.drawable.ic_menu_search);
        
        menu.add(0, MENU_OPTION_FOLDERS, 0, getString(R.string.tf_form_folders))
            .setIcon(R.drawable.ic_menu_archive);
        
        if (!Collect.getInstance().getInformOnlineState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY)) {
            menu.add(0, MENU_OPTION_NEWFORM, 0, getString(R.string.tf_add_template))
                .setIcon(R.drawable.ic_menu_add);
        }
        
        menu.add(0, MENU_OPTION_ODKTOOLS, 0, getString(R.string.open_data_kit))
            .setIcon(R.drawable.ic_menu_upload);
        
        menu.add(0, MENU_OPTION_INFO, 0, getString(R.string.tf_inform_info))
            .setIcon(R.drawable.ic_menu_info_details);
        
        return true;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                setResult(RESULT_OK);
                finish();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        InstanceLoadPathTask ilp;
        Intent i;

        switch (((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition()) {
        case 0:
            // When showing all forms in folder... start a new form
            i = new Intent(this, FormEntryActivity.class);
            i.putStringArrayListExtra(FormEntryActivity.KEY_INSTANCES, new ArrayList<String>());
            i.putExtra(FormEntryActivity.KEY_FORMPATH, ((FormDefinition) getListAdapter().getItem(position)).getId());
            startActivity(i);
            break;

        case 1:
            // When showing all completed forms in folder... browse selected form instances
            ilp = new InstanceLoadPathTask();
            ilp.execute(((FormDefinition) getListAdapter().getItem(position)).getId(), FormInstance.Status.complete);
            break;

        case 2:
            // When showing all draft forms in folder... browse selected form instances
            ilp = new InstanceLoadPathTask();
            ilp.execute(((FormDefinition) getListAdapter().getItem(position)).getId(), FormInstance.Status.draft);
            break;
            
        case 3:
            // Load instance for editing
            String instanceId = ((FormInstance) getListAdapter().getItem(position)).getId();
            
            i = new Intent(this, FormEntryActivity.class);
            i.putStringArrayListExtra(FormEntryActivity.KEY_INSTANCES, new ArrayList<String>(Arrays.asList(instanceId)));
            i.putExtra(FormEntryActivity.KEY_INSTANCEPATH, instanceId);
            i.putExtra(FormEntryActivity.KEY_FORMPATH, ((FormInstance) getListAdapter().getItem(position)).getFormId());
            startActivity(i);
            break;

        case 4:
            // When showing all forms in folder... export records
            Intent dea = new Intent(this, DataExportActivity.class);
            dea.putExtra(FormEntryActivity.KEY_FORMPATH, ((FormDefinition) getListAdapter().getItem(position)).getId());
            startActivity(dea);
            break;

        case 5:           
            // When showing all forms in folder... edit a form
            FormBuilderLauncherTask fbl = new FormBuilderLauncherTask();
            fbl.execute(((FormDefinition) getListAdapter().getItem(position)).getId());
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_OPTION_REFRESH:
            loadScreen();
            break;
        case MENU_OPTION_SEARCH:
            showDialog(DIALOG_SEARCH_FILTER);
            break;
        case MENU_OPTION_FOLDERS:
            startActivity(new Intent(this, AccountFolderList.class));
            break;
        case MENU_OPTION_NEWFORM:
            showDialog(DIALOG_CREATE_TEMPLATE);
            break;
        case MENU_OPTION_ODKTOOLS:
            startActivity(new Intent(this, ODKActivityTab.class));
            break;          
        case MENU_OPTION_INFO:
            startActivityForResult(new Intent(this, ClientInformationActivity.class), RESULT_ABOUT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Pass references and other important information to the next thread
    @Override
    public Object onRetainNonConfigurationInstance()
    {
        if (mDefinitionImportTask != null && mDefinitionImportTask.getStatus() != AsyncTask.Status.FINISHED)
            return mDefinitionImportTask;
        
        if (mSynchronizeFoldersTask != null && mSynchronizeFoldersTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSynchronizeFoldersTask;
        
        if (mToggleOnlineStateTask != null && mToggleOnlineStateTask.getStatus() != AsyncTask.Status.FINISHED)
            return mToggleOnlineStateTask;
        
        // Avoid refetching documents from database by preserving them
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(KEY_FORM_DEFINITION, mFormDefinition);
        data.put(KEY_SEARCH_FILTER, mSearchFilter);
        
        return data;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_COPY_TO_FOLDER_AS, mCopyToFolderAs);
        outState.putString(KEY_COPY_TO_FOLDER_ID, mCopyToFolderId);
        outState.putString(KEY_COPY_TO_FOLDER_NAME, mCopyToFolderName);
        outState.putString(KEY_DIALOG_MESSAGE, mDialogMessage);
        outState.putString(KEY_SELECTED_DB, mSelectedDatabase);
        outState.putInt(KEY_TASK_SELECTOR, ((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition());
    }
    
    private class CopyToFolderTask extends AsyncTask<Object, Void, Void>
    {
        private static final String tt = t + "CopyToFolderTask: ";
        
        private static final String KEY_ITEM = "key_item";
        
        private boolean copied = false;
        private boolean duplicate = false;
        private String copyFormAsName;
        private String copyToFolderId;
        private FormDefinition formDefinition;

        ProgressDialog progressDialog = null;
        
        final Handler progressHandler = new Handler() {
            public void handleMessage(Message msg) {
                progressDialog.setMessage(getString(R.string.tf_copying_with_param, msg.getData().getString(KEY_ITEM)));
            }
        };
        
        @Override
        protected Void doInBackground(Object... params)
        {            
            formDefinition = (FormDefinition) params[0];
            copyToFolderId = (String) params[1];
            copyFormAsName = (String) params[2];
            
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, tt + "about to copy " + formDefinition.getId() + " to " + copyToFolderId);
            
            Message msg = progressHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(KEY_ITEM, formDefinition.getName());
            msg.setData(b);
            progressHandler.sendMessage(msg);
            
            AttachmentInputStream ais = null;;
            ByteArrayOutputStream output = null;
            byte [] xml = null;
            
            byte [] buffer = new byte[8192];
            int bytesRead;
            
            try {
                // Basic deduplication
                FormDefinitionRepo formDefinitionRepo = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb(copyToFolderId));
                List<FormDefinition> definitions = formDefinitionRepo.findByName(copyFormAsName);
                
                if (!definitions.isEmpty()) {
                    duplicate = true;
                    return null;
                }

                ais = Collect.getInstance().getDbService().getDb().getAttachment(formDefinition.getId(), "xml");
                
                FormDefinition copyOfFormDefinition = new FormDefinition();

                // If copying with the exact same name
                if (copyFormAsName.equals(formDefinition.getName())) {
                    output = new ByteArrayOutputStream();

                    while ((bytesRead = ais.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    
                    xml = output.toByteArray();
                    output.close();
                    
                    // No need to recompute this if it is an exact copy
                    copyOfFormDefinition.setXmlHash(formDefinition.getXmlHash());
                } else {
                    // Rename form definition
                    xml = renameFormDefinition(ais, copyFormAsName);
                    
                    // Save to file first so we can get md5 hash
                    File f = new File(FileUtilsExtended.EXTERNAL_CACHE + File.separator + UUID.randomUUID() + ".xml");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(xml);
                    fos.close();
                    
                    copyOfFormDefinition.setXmlHash(FileUtils.getMd5Hash(f));
                    
                    f.delete();
                }

                ais.close();
                
                copyOfFormDefinition.setName(copyFormAsName);
                copyOfFormDefinition.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(xml)).toString(), FormWriter.CONTENT_TYPE));                
                
                Collect.getInstance().getDbService().getDb(copyToFolderId).create(copyOfFormDefinition);

                // Copy all remaining attachments from the original form definition; preserve names
                if (formDefinition.getAttachments().size() > 1) {
                    String formCachePath = FileUtilsExtended.FORMS_PATH + File.separator + formDefinition.getId();
                    String formCacheMediaPath = formCachePath + File.separator + FileUtilsExtended.MEDIA_DIR;

                    FileUtils.createFolder(formCachePath);
                    FileUtils.createFolder(formCacheMediaPath);

                    // Download attachments
                    for (Entry<String, Attachment> entry : formDefinition.getAttachments().entrySet()) {
                        ais = Collect.getInstance().getDbService().getDb().getAttachment(formDefinition.getId(), entry.getKey());
                        FileOutputStream file;

                        if (!entry.getKey().equals("xml")) {
                            file = new FileOutputStream(formCacheMediaPath + File.separator + entry.getKey());

                            buffer = new byte[8192];
                            bytesRead = 0;

                            while ((bytesRead = ais.read(buffer)) != -1) {
                                file.write(buffer, 0, bytesRead);
                            }

                            file.close();
                        }

                        ais.close();
                    }

                    // Upload to new form definition document
                    String revision = copyOfFormDefinition.getRevision();

                    for (File f : new File(formCacheMediaPath).listFiles()) {
                        String fileName = f.getName();
                        String attachmentName = fileName;

                        if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + ": attaching " + fileName + " to " + copyOfFormDefinition.getId());

                        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                        String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

                        FileInputStream fis = new FileInputStream(f);
                        ais = new AttachmentInputStream(attachmentName, fis, contentType, f.length()); 
                        revision = Collect.getInstance().getDbService().getDb(copyToFolderId).createAttachment(copyOfFormDefinition.getId(), revision, ais);
                        fis.close();
                        ais.close();
                    }
                }

                copied = true;             
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, tt + "unexpected exception");
                e.printStackTrace();
            }
            
            return null;
        }
        
        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(BrowserActivity.this);
            progressDialog.setMessage(getString(R.string.tf_copying_please_wait));  
            progressDialog.show();
        }        

        @Override
        protected void onPostExecute(Void nothing)
        {   
            try {
                progressDialog.dismiss();
                progressDialog = null;
            } catch (Exception e) {
                // Do nothing if view is no longer attached
            }
            
            if (copied) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_was_successful, getString(R.string.tf_copy)), Toast.LENGTH_SHORT).show();
                
                if (copyToFolderId.equals(Collect.getInstance().getInformOnlineState().getSelectedDatabase()))
                    loadScreen();
            } else if (duplicate) {
                // Show duplicate explanation dialog
                showDialog(DIALOG_UNABLE_TO_COPY_DUPLICATE);
            } else {
                // Some other failure
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_failed, getString(R.string.tf_copy)), Toast.LENGTH_LONG).show();
            }
        }        
    }
    
    /*
     * Create a new form definition and launch the built-in form editor
     */
    private class CreateFormDefinitionTask extends AsyncTask<Object, Void, Void>
    {
        private boolean isDuplicate = false;
        private boolean isSuccessful = true;
        
        private FormDefinition f;
        
        private ProgressDialog progressDialog = null;
        
        @Override
        protected Void doInBackground(Object... params) 
        {
            f = (FormDefinition) params[0];
            
            try {
                // Basic deduplication
                FormDefinitionRepo repo = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb(Collect.getInstance().getInformOnlineState().getSelectedDatabase()));
                List<FormDefinition> definitions = repo.findByName(f.getName());

                if (!definitions.isEmpty()) {
                    isDuplicate = true;
                    return null;
                }

                // Create empty form from template
                InputStream is = getResources().openRawResource(R.raw.xform_template);

                // Set up variables to receive data
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                byte[] inputbuf = new byte[8192];            
                int inputlen;

                while ((inputlen = is.read(inputbuf)) > 0) {
                    data.write(inputbuf, 0, inputlen);
                }

                // Add initial XForm template
                f.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(data.toByteArray())).toString(), FormWriter.CONTENT_TYPE));
                
                // Create form definition
                Collect.getInstance().getDbService().getDb().create(f);
                
                is.close();
                data.close();
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to read XForm template file; create new form process will fail");
                e.printStackTrace();
                isSuccessful = false;
            }
            
            return null;
        }
        
        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(BrowserActivity.this);
            progressDialog.setMessage(getString(R.string.tf_creating_template_please_wait));  
            progressDialog.show();
        }
        
        @Override
        protected void onPostExecute(Void nothing)
        {
            try {
                progressDialog.dismiss();
                progressDialog = null;
            } catch (Exception e) {
                // Do nothing if view is no longer attached
            }
            
            if (isDuplicate) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_form_name_duplicate), Toast.LENGTH_LONG).show();
                showDialog(DIALOG_CREATE_TEMPLATE);
            } else {
                if (isSuccessful) {
                    Intent i = new Intent(BrowserActivity.this, FieldList.class);
                    i.putExtra(FormEntryActivity.KEY_FORMPATH, f.getId());
                    startActivity(i);                    
                } else {
                    mDialogMessage = getString(R.string.tf_unable_to_open_folder, getSelectedFolderName());
                    showDialog(DIALOG_FOLDER_UNAVAILABLE);
                }
            }
        }        
    }
    
    /*
     * Determine whether it is safe to launch the form browser.  For the time
     * being we need this so that we can allow/disallow access based on whether
     * instances exist for a given form.
     */
    private class FormBuilderLauncherTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... arg0)
        {
            String docId = arg0[0];
            List<FormInstance> instanceIds = new ArrayList<FormInstance>();
            String result = "";
            
            try {
                instanceIds = new FormInstanceRepo(Collect.getInstance().getDbService().getDb()).findByFormId(docId);
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unexpected exception " + e.toString());
                e.printStackTrace();
            } finally {                
                if (instanceIds.isEmpty())
                    result = docId;
            }
            
            return result;
        }
        
        @Override
        protected void onPreExecute()
        {
            setProgressVisibility(true);
        }
        
        @Override
        protected void onPostExecute(String docId)
        {
            if (docId.length() > 0) {
                // Success                
                Intent i = new Intent(BrowserActivity.this, FieldList.class);
                i.putExtra(FormEntryActivity.KEY_FORMPATH, docId);
                startActivity(i);
            } else {
                // Failure (instances present)
                showDialog(DIALOG_FORM_BUILDER_LAUNCH_ERROR);
            }
            
            setProgressVisibility(false);
        }        
    }

    /*
     * Retrieve all instances of a certain status for a specified definition,
     * populate the instance browse list and start FormEditActivity accordingly.
     */
    private class InstanceLoadPathTask extends AsyncTask<Object, Integer, Void>
    {
        String formId;
        ArrayList<String> instanceIds = new ArrayList<String>();
        boolean caughtExceptionInBackground = true;

        @Override
        protected Void doInBackground(Object... params)
        {
            try {
                formId = (String) params[0];
                FormInstance.Status status = (FormInstance.Status) params[1];
                instanceIds = new FormInstanceRepo(Collect.getInstance().getDbService().getDb()).findByFormAndStatus(formId, status);
                caughtExceptionInBackground = false;
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unhandled exception while processing InstanceLoadPathTask.doInBackground(): " + e.toString());
                e.printStackTrace();
            }
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            setProgressVisibility(true);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            if (caughtExceptionInBackground) {
                mDialogMessage = getString(R.string.tf_unable_to_open_folder, getSelectedFolderName());
                showDialog(DIALOG_FOLDER_UNAVAILABLE);
            } else {
                try {
                    Intent i = new Intent(BrowserActivity.this, FormEntryActivity.class);
                    i.putStringArrayListExtra(FormEntryActivity.KEY_INSTANCES, instanceIds);
                    i.putExtra(FormEntryActivity.KEY_INSTANCEPATH, instanceIds.get(0));
                    i.putExtra(FormEntryActivity.KEY_FORMPATH, formId);
                    startActivity(i);
                } catch (IndexOutOfBoundsException e) {
                    // There were no mInstanceIds returned (no DB error, per-se but something was missing)
                    showDialog(DIALOG_INSTANCES_UNAVAILABLE);
                }
            }

            setProgressVisibility(false);
        }
    }
    
    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Integer, Void>
    {
        private HashMap<String, HashMap<String, String>> tallies = new HashMap<String, HashMap<String, String>>();
                
        private ArrayList<FormDefinition> definitions = new ArrayList<FormDefinition>();
        private ArrayList<FormInstance> instances = new ArrayList<FormInstance>();
        
        private FormInstance.Status statusFilter = null;        
        private Bundle filterOptions = null;
        
        private boolean folderOutdated = false;
        private boolean folderUnavailable = false;    

        @Override
        protected Void doInBackground(Void... params)
        {
            try {    
                if (filterOptions == null && statusFilter == null) {
                    // Search results pulled but nothing to show
                } else if (filterOptions == null) {
                    // No search options, we must be using the simple status filter
                    
                    // TODO: move to AccountFolderList and activate when a user opens a folder?
                    Collect.getInstance().getDbService().performHousekeeping(Collect.getInstance().getInformOnlineState().getSelectedDatabase());

                    FormDefinitionRepo repo = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb());                
                    tallies = repo.getFormsByInstanceStatus(statusFilter);

                    if (statusFilter.equals(FormInstance.Status.any)) {
                        definitions = (ArrayList<FormDefinition>) repo.getAllActive();
                    } else {
                        definitions = (ArrayList<FormDefinition>) repo.getAllActiveByKeys(new ArrayList<Object>(tallies.keySet()));    
                    }

                    DocumentUtils.sortDefinitionsByName(definitions);
                } else {
                    // Use search filter options                    
                    FormDefinitionRepo definitionRepo = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb()); 
                    definitions = (ArrayList<FormDefinition>) definitionRepo.getAll();

                    List<String> assignmentParameter = new ArrayList<String>();
                    FormInstance.Status statusParameter = FormInstance.Status.any;
                    
                    switch (filterOptions.getInt(KEY_SEARCH_BY_ASSIGNMENT, 0)) {
                    case 0:
                        // Any device
                        break;
                    case 1:
                        // This device
                        assignmentParameter.add(Collect.getInstance().getInformOnlineState().getDeviceId());
                        break;
                    case 2:
                        // Specific devices
                        assignmentParameter = filterOptions.getStringArrayList(KEY_SEARCH_BY_ASSIGNMENT_IDS);
                        break;
                    }
                    
                    switch (filterOptions.getInt(KEY_SEARCH_BY_STATUS, 0)) {
                    case 0:
                        // Any status
                        break;
                    case 1:
                        // Complete only
                        statusParameter = FormInstance.Status.complete; 
                        break;
                    case 2:
                        // Draft only
                        statusParameter = FormInstance.Status.draft;
                        break;
                    }
                                       
                    FormInstanceRepo instanceRepo = new FormInstanceRepo(Collect.getInstance().getDbService().getDb());
                    instances = (ArrayList<FormInstance>) instanceRepo.findByFilterIndex(assignmentParameter, statusParameter);
                    
                    DocumentUtils.sortByDateCreated(instances);
                }
            } catch (ClassCastException e) {
                // TODO: is there a better way to handle empty lists?
            } catch (DocumentNotFoundException e) {
                /*
                 * This most likely cause of this exception is that a design document could not be found.  This will happen if we are
                 * running a version of Inform that expects a design document by a certain name but the local folder does not have
                 * the most recent design documents.
                 */
                if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + e.toString());
                folderOutdated = true;
                folderUnavailable = true;
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unexpected exception " + e.toString());
                e.printStackTrace();
                folderUnavailable = true;
            }

            return null;             
        }

        @Override
        protected void onPreExecute()
        {
            setProgressVisibility(true);
            ((Spinner) findViewById(R.id.taskSpinner)).setClickable(false);
            ((Spinner) findViewById(R.id.taskSpinner)).setEnabled(false);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {   
            boolean noResults = true;
            
            /*
             * Special hack to ensure that our application doesn't crash if we terminate it
             * before the AsyncTask has finished running.  This is stupid and I don't know
             * another way around it.
             * 
             * See http://dimitar.me/android-displaying-dialogs-from-background-threads/
             */
            if (isFinishing())
                return;
            
            // Stop progress
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
            // Re-enable task selector
            ((Spinner) findViewById(R.id.taskSpinner)).setClickable(true);
            ((Spinner) findViewById(R.id.taskSpinner)).setEnabled(true);

            if (folderUnavailable) {
                String db = Collect.getInstance().getInformOnlineState().getSelectedDatabase();
                boolean isReplicated = Collect.getInstance().getInformOnlineState().getAccountFolders().get(db).isReplicated();
                
                if (folderOutdated && isReplicated) {
                    showDialog(DIALOG_FOLDER_OUTDATED);
                } else {
                    mDialogMessage = getString(R.string.tf_unable_to_open_folder, getSelectedFolderName());
                    showDialog(DIALOG_FOLDER_UNAVAILABLE);
                }
            } else {
                if (filterOptions == null) {
                    noResults = definitions.isEmpty();
                    
                    BrowserShortListAdapter adapter = new BrowserShortListAdapter(BrowserActivity.this, R.layout.browser_list_item, definitions, tallies, (Spinner) findViewById(R.id.taskSpinner));
                    setListAdapter(adapter);
                } else {
                    noResults = instances.isEmpty();
                    
                    BrowserLongListAdapter adapter = new BrowserLongListAdapter(BrowserActivity.this, R.layout.browser_list_item, instances, definitions);
                    setListAdapter(adapter);
                }
                
                if (noResults) {
                    TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                    nothingToDisplay.setVisibility(View.VISIBLE);
                }
            }

            setProgressVisibility(false);
        }
        
        // Simplistic filtering
        public void setFilterByStatus(FormInstance.Status s)
        {
            statusFilter = s;
        }

        // Search filters for displaying forms via the long adapter
        public void setFilterOptions(Bundle b)
        {
            filterOptions = b;
        }
    }
    
    private class RemoveDefinitionTask extends AsyncTask<Object, Void, Void>
    {
        FormDefinition formDefinition;
        ProgressDialog progressDialog;
        boolean removed = false;   
        
        @Override
        protected Void doInBackground(Object... params)
        {
            formDefinition = (FormDefinition) params[0];
            formDefinition.setStatus(FormDefinition.Status.removed);
            
            try {
                Collect.getInstance().getDbService().getDb().update(formDefinition);
                removed = true;
            } catch (Exception e) {
                Log.e(Collect.LOGTAG, t + "unexpected exception");
                e.printStackTrace();
            }
            
            return null;
        }
    
        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(BrowserActivity.this);
            progressDialog.setMessage(getString(R.string.tf_removing_please_wait));  
            progressDialog.show();
        }
    
        @Override
        protected void onPostExecute(Void nothing)
        {   
            try {
                progressDialog.dismiss();
                progressDialog = null;
            } catch (Exception e) {
                // Do nothing if view is no longer attached
            }
            
            // TODO
            if (removed) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_was_successful, getString(R.string.tf_removal)), Toast.LENGTH_SHORT).show();
            } else {
                // Unspecified failure
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_failed, getString(R.string.tf_removal)), Toast.LENGTH_LONG).show();                
            }   
                        
            loadScreen();
        }
    }
    
    private class RenameDefinitionTask extends AsyncTask<Object, Void, Void>
    {
        private static final String tt = t + "RenameDefinitionTask: ";        
        private static final String KEY_ITEM = "key_item";
        
        private boolean renamed = false;
        private boolean duplicate = false;
        private String newName;
        private FormDefinition formDefinition;
        
        ProgressDialog progressDialog = null;
        
        final Handler progressHandler = new Handler() {
            public void handleMessage(Message msg) {
                progressDialog.setMessage(getString(R.string.tf_renaming_with_param, msg.getData().getString(KEY_ITEM)));
            }
        };
        
        @Override
        protected Void doInBackground(Object... params)
        {            
            formDefinition = (FormDefinition) params[0];
            newName = (String) params[1];
            
            Log.d(Collect.LOGTAG, tt + "about to rename " + formDefinition.getId() + " to " + newName);
            
            Message msg = progressHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(KEY_ITEM, formDefinition.getName());
            msg.setData(b);
            progressHandler.sendMessage(msg);
            
            AttachmentInputStream ais = null;
            byte [] xml = null;
            
            try {
                // Basic deduplication
                FormDefinitionRepo formDefinitionRepo = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb());
                List<FormDefinition> definitions = formDefinitionRepo.findByName(newName);
                
                if (!definitions.isEmpty()) {
                    // If there is more than one match OR the first (and only) match isn't the form that was selected
                    if (definitions.size() > 1 || definitions.get(0).getId() != formDefinition.getId()) {
                        duplicate = true;
                        return null;
                    }
                }
                
                ais = Collect.getInstance().getDbService().getDb().getAttachment(formDefinition.getId(), "xml");

                // Rename form definition
                xml = renameFormDefinition(ais, newName);

                // Save to file first so we can get md5 hash
                File f = new File(FileUtilsExtended.EXTERNAL_CACHE + File.separator + UUID.randomUUID() + ".xml");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(xml);
                fos.close();

                formDefinition.setXmlHash(FileUtils.getMd5Hash(f));

                f.delete();
                ais.close();
                
                formDefinition.setName(newName);
                formDefinition.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(xml)).toString(), FormWriter.CONTENT_TYPE));                
                
                Collect.getInstance().getDbService().getDb().update(formDefinition);
                
                renamed = true;           
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, tt + "unexpected exception");
                e.printStackTrace();
            }
            
            return null;
        }
        
        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(BrowserActivity.this);
            progressDialog.setMessage(getString(R.string.tf_renaming_please_wait));  
            progressDialog.show();
        }        

        @Override
        protected void onPostExecute(Void nothing)
        {   
            try {
                progressDialog.dismiss();
                progressDialog = null;
            } catch (Exception e) {
                // Do nothing if view is no longer attached
            }
            
            if (renamed) {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_was_successful, getString(R.string.tf_rename)), Toast.LENGTH_SHORT).show();
            } else if (duplicate) {
                // Show duplicate explanation dialog
                showDialog(DIALOG_UNABLE_TO_RENAME_DUPLICATE);
            } else { 
                // Some other failure
                Toast.makeText(getApplicationContext(), getString(R.string.tf_something_failed, getString(R.string.tf_rename)), Toast.LENGTH_LONG).show();
            }
            
            loadScreen();
        }        
    }    

    /*
     * Update (synchronize) a local database by pulling from the remote database.
     * Needed if the local database becomes outdated.
     */
    private class UpdateFolderTask extends AsyncTask<Void, Void, Void>
    {
        String db = Collect.getInstance().getInformOnlineState().getSelectedDatabase();
        AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(db);
        ReplicationStatus status = null;
        
        @Override
        protected Void doInBackground(Void... nothing)
        {
            try {
                status = Collect.getInstance().getDbService().replicate(folder.getId(), DatabaseService.REPLICATE_PULL);
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to replicate during UpdateFolderTask: " + e.toString());
                e.printStackTrace();
                status = null;
            }
            
            return null;
        }
    
        @Override
        protected void onPreExecute()
        {
            showDialog(DIALOG_UPDATING_FOLDER);
        }
    
        @Override
        protected void onPostExecute(Void nothing)
        {   
            removeDialog(DIALOG_UPDATING_FOLDER);
            
            // No changes is the same as "unable to update" because chances are it will lead to the same problem
            if (status == null || !status.isOk() || status.isNoChanges())
                Toast.makeText(getApplicationContext(), getString(R.string.tf_unable_to_update_folder, getSelectedFolderName()), Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(), getString(R.string.tf_folder_updated, getSelectedFolderName()), Toast.LENGTH_SHORT).show();
            
            loadScreen();
        }
    }

    // Attempt to return the current folder name (shortened to an appropriate length)
    public static String getSelectedFolderName()
    {
        String folderName = "...";
        
        try {
            folderName = Collect
                .getInstance()
                .getInformOnlineState()
                .getAccountFolders()
                .get(Collect.getInstance().getInformOnlineState().getSelectedDatabase())
                .getName();
            
            // Shorten names that are too long
            if (folderName.length() > 23) 
                folderName = folderName.substring(0, 20) + "...";
        } catch (NullPointerException e) {
            // Database metadata is not available at this time
            Log.w(Collect.LOGTAG, t + "folder metadata not available at this time");
            folderName = "?";
        }
        
        return folderName;
    }

    @Override
    public void importTaskFinished(Bundle data) 
    {
        dismissDialog(DIALOG_IMPORTING_TEMPLATE);        
        
        if (data.getBoolean(DefinitionImportListener.SUCCESSFUL, false)) {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_imported_file, data.getString(DefinitionImportListener.FILENAME)), Toast.LENGTH_SHORT).show();
            loadScreen();
        } else {
            mDialogMessage = data.getString(DefinitionImportListener.MESSAGE);
            showDialog(DIALOG_UNABLE_TO_IMPORT_TEMPLATE);
        }
        
        loadScreen();
    }

    @Override
    public void synchronizationHandler(Message msg) 
    {
        if (msg == null) {
            // Close any existing progress dialogs
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();

            // Start new dialog with suitable message
            mProgressDialog = new ProgressDialog(BrowserActivity.this);
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
     
        if (data.getBoolean(SynchronizeFoldersListener.POS, false)) {
            // Alternate post-synchronization workflow (go offline after synchronizing)
            mToggleOnlineStateTask = new ToggleOnlineStateTask();
            mToggleOnlineStateTask.setListener(BrowserActivity.this);
            mToggleOnlineStateTask.execute();
        } else {
            // Refresh after synchronizing to reflect any changes
            loadScreen();
        }
    }

    @Override
    public void toggleOnlineStateHandler() 
    {        
        showDialog(DIALOG_ONLINE_STATE_CHANGING);

        // Not available while toggling
        Button b1 = (Button) findViewById(R.id.onlineStatusTitleButton);
        b1.setEnabled(false);
        b1.setText(R.string.tf_inform_state_transition);

        Button b2 = (Button) findViewById(R.id.folderTitleButton);
        b2.setEnabled(false);
        b2.setText("...");
    }

    @Override
    public void toggleOnlineStateTaskFinished(Bundle data) 
    {
        removeDialog(DIALOG_ONLINE_STATE_CHANGING);

        switch (data.getInt(ToggleOnlineStateListener.OUTCOME)) {
        case ToggleOnlineStateListener.SUCCESSFUL:
            // If we are signed in after toggling then it makes sense that we'd want to synchronize (but only if requested)
            if (Collect.getInstance().getIoService().isSignedIn() && data.getBoolean(ToggleOnlineStateListener.POS, false)) {
                mSynchronizeFoldersTask = new SynchronizeFoldersTask();
                mSynchronizeFoldersTask.setListener(BrowserActivity.this);
                mSynchronizeFoldersTask.setTransferMode(SynchronizeFoldersListener.MODE_SWAP);
                mSynchronizeFoldersTask.execute();
            } else {
                loadScreen();
            }
            
            break;
        case ToggleOnlineStateListener.CANNOT_SIGNIN:
            // Load screen after user acknowledges to avoid stacking of dialogs
            showDialog(DIALOG_ONLINE_ATTEMPT_FAILED);
            break;
        case ToggleOnlineStateListener.CANNOT_SIGNOUT:
            // Load screen after user acknowledges to avoid stacking of dialogs
            showDialog(DIALOG_OFFLINE_ATTEMPT_FAILED);
            break;            
        }
    }

    /*
     * Load the various elements of the screen that must wait for other tasks to complete
     */
    private void loadScreen()
    {        
        String folderName = "?";
        
        try {
            // Reflect the online/offline status (may be disabled thanks to toggling state)
            Button b1 = (Button) findViewById(R.id.onlineStatusTitleButton);
            b1.setEnabled(true);            

            if (Collect.getInstance().getIoService().isSignedIn()) {
                b1.setText(getText(R.string.tf_inform_state_online));
            } else {
                if (Collect.getInstance().getInformOnlineState().isOfflineModeEnabled())
                    b1.setText(getText(R.string.tf_inform_state_offline));
                else 
                    b1.setText(getText(R.string.tf_inform_state_disconnected));
            }

            // Hide "nothing to display" message
            TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
            nothingToDisplay.setVisibility(View.INVISIBLE);
            
            // Restore selected database (but only once)
            if (mSelectedDatabase != null) {
                Log.v(Collect.LOGTAG, t + "restoring selected database " + mSelectedDatabase);
                Collect.getInstance().getInformOnlineState().setSelectedDatabase(mSelectedDatabase);
                mSelectedDatabase = null;
            }
            
            folderName = getSelectedFolderName();        
            
            // Re-enable and display currently selected folder (may be disabled thanks to toggling state) 
            Button b2 = (Button) findViewById(R.id.folderTitleButton);
            b2.setEnabled(true);
            b2.setText(folderName);

            // Open selected database
            Collect.getInstance().getDbService().open(Collect.getInstance().getInformOnlineState().getSelectedDatabase());
        
            mRefreshViewTask = new RefreshViewTask();

            // Spinner must reflect results of refresh view below            
            switch (((Spinner) findViewById(R.id.taskSpinner)).getSelectedItemPosition()) {
            case 0:
                Log.v("DEBUG", "refresh 0");
                // Show all templates to start new form
                mRefreshViewTask.setFilterByStatus(FormInstance.Status.any);
                break;
            case 1:
                Log.v("DEBUG", "refresh 1");
                // Show templates with complete forms
                mRefreshViewTask.setFilterByStatus(FormInstance.Status.complete);
                break;
            case 2:
                Log.v("DEBUG", "refresh 2");
                // Show templates with draft forms
                mRefreshViewTask.setFilterByStatus(FormInstance.Status.draft);
                break;
            case 3:
                Log.v("DEBUG", "refresh 3");
                // Show forms according to filter, search results
                mRefreshViewTask.setFilterOptions(mSearchFilter);
                mRefreshViewTask.setFilterByStatus(null);
                break;
            case 4:
                Log.v("DEBUG", "refresh 4");
                // Show all templates (for record export)
            case 5:
                Log.v("DEBUG", "refresh 5");
                // Show all templates (to edit template)
                mRefreshViewTask.setFilterByStatus(FormInstance.Status.any);
                break;
            }
            
            mRefreshViewTask.execute();
            
            registerForContextMenu(getListView());
        } catch (DatabaseService.DbUnavailableDueToMetadataException e) {            
            mDialogMessage = getString(R.string.tf_unable_to_open_folder_missing_metadata);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);
        } catch (DatabaseService.DbUnavailableWhileOfflineException e) {
            mDialogMessage = getString(R.string.tf_unable_to_open_folder_while_offline, folderName);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);
        } catch (DatabaseService.DbUnavailableException e) {
            mDialogMessage = getString(R.string.tf_unable_to_open_folder, folderName);
            showDialog(DIALOG_FOLDER_UNAVAILABLE);  
        } catch (NullPointerException e) {
            // Something failed to return -- restart app
            // FIXME: remove this workaround -- figure out why it happens in the first place
            Intent exit = new Intent();
            exit.putExtra("exit_app", true);
            setResult(RESULT_OK, exit);
            finish();            
            
            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }
    
    /*
     * Parse an attachment input stream (form definition XML file), affect h:title and instance 
     * root & id attribute and return the XML file as byte[] for consumption by the controlling task. 
     */
    private byte[] renameFormDefinition(AttachmentInputStream ais, String newName) throws Exception
    {
        FormReader fr = new FormReader(ais, false);

        // Populate global state (expected by FormWriter)
        Collect.getInstance().getFormBuilderState().setBinds(fr.getBinds());
        Collect.getInstance().getFormBuilderState().setFields(fr.getFields());
        Collect.getInstance().getFormBuilderState().setInstance(fr.getInstance());
        Collect.getInstance().getFormBuilderState().setTranslations(fr.getTranslations());
        
        return FormWriter.writeXml(newName, fr.getInstanceRoot(), fr.getInstanceRootId());
    }
    
    // Toggle progress spinner in custom title bar
    private void setProgressVisibility(boolean visible)
    {
        ProgressBar pb = (ProgressBar) getWindow().findViewById(R.id.titleProgressBar);
        
        if (pb != null) {
            if (visible) {
                pb.setVisibility(View.VISIBLE);
            } else {
                pb.setVisibility(View.GONE);
            }
        }
    }
}