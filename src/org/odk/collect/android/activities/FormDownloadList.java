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

package org.odk.collect.android.activities;

import com.radicaldynamic.groupinform.R;
import org.odk.collect.android.listeners.FormDownloaderListener;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.logic.FormDetails;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.tasks.DownloadFormListTask;
import com.radicaldynamic.groupinform.tasks.DownloadFormsTask;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.WebUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Responsible for displaying, adding and deleting all the valid forms in the forms directory.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormDownloadList extends ListActivity implements FormListDownloaderListener,
        FormDownloaderListener {
    private static final String t = "RemoveFileManageList";

    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;
    private static final int MENU_PREFERENCES = Menu.FIRST;

    private static final String BUNDLE_TOGGLED_KEY = "toggled";
    private static final String BUNDLE_SELECTED_COUNT = "selectedcount";
    private static final String BUNDLE_FORM_LIST = "formlist";
    private static final String DIALOG_TITLE = "dialogtitle";
    private static final String DIALOG_MSG = "dialogmsg";
    private static final String DIALOG_SHOWING = "dialogshowing";

    public static final String LIST_URL = "listurl";

    private String mAlertMsg;
    private boolean mAlertShowing = false;
    private boolean mSuccess = false;
    private String mAlertTitle;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private Button mActionButton;

    private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;
    private Button mToggleButton;
    private Button mRefreshButton;

    private HashMap<String, FormDetails> mFormNamesAndURLs;
    private ArrayAdapter<String> mFileAdapter;

    private boolean mToggled = false;
    private int mSelectedCount = 0;

    private int totalCount;


    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_file_manage_list);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.get_forms));
        mAlertMsg = getString(R.string.please_wait);

        // need white background before load
        getListView().setBackgroundColor(Color.WHITE);

        mActionButton = (Button) findViewById(R.id.add_button);
        mActionButton.setEnabled(false);
        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadSelectedFiles();
                mToggled = false;
            }
        });

        mToggleButton = (Button) findViewById(R.id.toggle_button);
        mToggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle selections of items to all or none
                ListView ls = getListView();
                mToggled = !mToggled;

                for (int pos = 0; pos < ls.getCount(); pos++)
                    ls.setItemChecked(pos, mToggled);

                mActionButton.setEnabled(!(selectedItemCount() == 0));
            }
        });

        mRefreshButton = (Button) findViewById(R.id.refresh_button);
        mRefreshButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mToggled = false;
                downloadFormList();
            }
        });

        if (savedInstanceState != null) {
            // If the screen has rotated, the hashmap with the form names and urls is passed here.
            if (savedInstanceState.containsKey(BUNDLE_FORM_LIST)) {
                mFormNamesAndURLs =
                    (HashMap<String, FormDetails>) savedInstanceState
                            .getSerializable(BUNDLE_FORM_LIST);
            }
            // indicating whether or not select-all is on or off.
            if (savedInstanceState.containsKey(BUNDLE_TOGGLED_KEY)) {
                mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
            }

            // how many items we've selected
            if (savedInstanceState.containsKey(BUNDLE_SELECTED_COUNT)) {
                mSelectedCount = savedInstanceState.getInt(BUNDLE_SELECTED_COUNT);
                mActionButton.setEnabled(!(mSelectedCount == 0));

            }

            // to restore alert dialog.
            if (savedInstanceState.containsKey(DIALOG_TITLE)) {
                mAlertTitle = savedInstanceState.getString(DIALOG_TITLE);
            }
            if (savedInstanceState.containsKey(DIALOG_MSG)) {
                mAlertMsg = savedInstanceState.getString(DIALOG_MSG);
            }
            if (savedInstanceState.containsKey(DIALOG_SHOWING)) {
                mAlertShowing = savedInstanceState.getBoolean(DIALOG_SHOWING);
            }
        }

        if (mAlertShowing) {
            createAlertDialog(mAlertTitle, mAlertMsg);
        }

        if (getLastNonConfigurationInstance() instanceof DownloadFormListTask) {
            mDownloadFormListTask = (DownloadFormListTask) getLastNonConfigurationInstance();
            if (mDownloadFormListTask == null) {
                Log.e("Carl", "downloading forms list...");
                downloadFormList();
            } else if (mDownloadFormListTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    dismissDialog(PROGRESS_DIALOG);
                } catch (IllegalArgumentException e) {
                    Log.w(t, "Attempting to close a dialog that was not previously opened");
                }

                buildView();
            }
        } else if (getLastNonConfigurationInstance() instanceof DownloadFormsTask) {
            mDownloadFormsTask = (DownloadFormsTask) getLastNonConfigurationInstance();

            if (mDownloadFormsTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    dismissDialog(PROGRESS_DIALOG);
                } catch (IllegalArgumentException e) {
                    Log.w(t, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
                buildView();
            }
        } else if (getLastNonConfigurationInstance() == null) {
            downloadFormList();
            buildView();
        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        mActionButton.setEnabled(!(selectedItemCount() == 0));
    }


    private void downloadFormList() {
        mFormNamesAndURLs = new HashMap<String, FormDetails>();
        if (mProgressDialog != null) {
            // This is needed because onPrepareDialog() is broken in 1.6.
            mProgressDialog.setMessage(getString(R.string.please_wait));
        }
        showDialog(PROGRESS_DIALOG);

        FileUtils.createFolder(FileUtils.CACHE_PATH);
        mDownloadFormListTask = new DownloadFormListTask(getContentResolver());
        mDownloadFormListTask.setDownloaderListener(this);
        mDownloadFormListTask.execute();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
        outState.putInt(BUNDLE_SELECTED_COUNT, selectedItemCount());
        outState.putSerializable(BUNDLE_FORM_LIST, mFormNamesAndURLs);
        outState.putString(DIALOG_TITLE, mAlertTitle);
        outState.putString(DIALOG_MSG, mAlertMsg);
        outState.putBoolean(DIALOG_SHOWING, mAlertShowing);
    }


    private int selectedItemCount() {
        int count = 0;
        SparseBooleanArray sba = getListView().getCheckedItemPositions();
        for (int i = 0; i < getListView().getCount(); i++) {
            if (sba.get(i, false)) {
                count++;
            }
        }
        return count;
    }


    private void buildView() {
        ArrayList<String> formNames = new ArrayList<String>(mFormNamesAndURLs.keySet());

        mFileAdapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice,
                    formNames);
        setListAdapter(mFileAdapter);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PREFERENCES, 0, getString(R.string.general_preferences)).setIcon(
            android.R.drawable.ic_menu_preferences);
        return true;
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PREFERENCES:
                createPreferencesMenu();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


    private void createPreferencesMenu() {
        Intent i = new Intent(this, PreferencesActivity.class);
        startActivity(i);
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // we use the same progress dialog for both
                            // so whatever isn't null is running
                            if (mDownloadFormListTask != null) {
                                mDownloadFormListTask.setDownloaderListener(null);
                            }
                            if (mDownloadFormsTask != null) {
                                mDownloadFormsTask.cancel(true);
                                mDownloadFormsTask.setDownloaderListener(null);
                            }
                        }
                    };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(mAlertMsg);
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
            case AUTH_DIALOG:
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle("Server Requires Authentication");
                b.setMessage("Please enter usernamd and password");

                LayoutInflater factory = LayoutInflater.from(this);
                final View dialogView = factory.inflate(R.layout.server_auth_dialog, null);

                b.setView(dialogView);
                b.setPositiveButton("ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                        EditText password = (EditText) dialogView.findViewById(R.id.password_edit);

                        SharedPreferences settings =
                            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        String server =
                            settings.getString(PreferencesActivity.KEY_SERVER_URL,
                                getString(R.string.default_server_url) + "formList");
                        Uri u = Uri.parse(server);

                        WebUtils.addCredentials(username.getText().toString(), password.getText()
                                .toString(), u.getHost());

                        downloadFormList();
                    }
                });
                b.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TOOD: cancel

                    }
                });

                b.setCancelable(false);
                mAlertShowing = false;
                return b.create();
        }
        return null;
    }


    /**
     * Adds the selected form
     */
    @SuppressWarnings("unchecked")
    private void downloadSelectedFiles() {
        totalCount = 0;
        ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();

        SparseBooleanArray sba = getListView().getCheckedItemPositions();
        for (int i = 0; i < getListView().getCount(); i++) {
            if (sba.get(i, false)) {
                String form = (String) getListAdapter().getItem(i);
                filesToDownload.add(mFormNamesAndURLs.get(form));
            }
        }
        totalCount = filesToDownload.size();

        if (totalCount > 0) {
            // show dialog box
            showDialog(PROGRESS_DIALOG);

            FileUtils.createFolder(FileUtils.FORMS_PATH);
            mDownloadFormsTask = new DownloadFormsTask();
            mDownloadFormsTask.setDownloaderListener(this);
            mDownloadFormsTask.execute(filesToDownload);
        } else {
            Toast.makeText(getApplicationContext(), R.string.noselect_error, Toast.LENGTH_SHORT)
                    .show();
        }
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mDownloadFormsTask != null) {
            return mDownloadFormsTask;
        } else {
            return mDownloadFormListTask;
        }
    }


    @Override
    protected void onDestroy() {
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(null);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(this);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(this);
        }
        super.onResume();
    }


    @Override
    protected void onPause() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        super.onPause();
    }


    public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
        dismissDialog(PROGRESS_DIALOG);
        String dialogMessage = null;
        String dialogTitle = null;

        if (result == null) {
            Log.e(t, "some bad error");
            return;
        }
        if (!result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            // Download succeeded
            mFormNamesAndURLs = result;
            mSuccess = true;
        } else {
            // Download failed
            dialogMessage =
                getString(R.string.list_failed_with_error,
                    result.get(DownloadFormListTask.DL_ERROR_MSG).errorStr);
            dialogTitle = getString(R.string.load_remote_form_error);
            createAlertDialog(dialogTitle, dialogMessage);

            mSuccess = false;
        }

        buildView();
    }


    private void createAlertDialog(String title, String message) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                        // just close the dialog
                        mAlertShowing = false;
                        // successful download, so quit
                        if (mSuccess) {
                            finish();
                        }

                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        if (mSuccess) {
            mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        } else {
            mAlertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        }
        mAlertShowing = true;
        mAlertMsg = message;
        mAlertTitle = title;
        mAlertDialog.show();
    }


    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        mAlertMsg = getString(R.string.fetching_file, currentFile, progress, total);
        mProgressDialog.setMessage(mAlertMsg);
    }


    @Override
    public void formListDownloadingError(int errorCode, String msg) {
        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }
        switch (errorCode) {
            case 401:
                String message = getString(R.string.list_failed_with_error, msg);
                showDialog(AUTH_DIALOG);
                break;
            default:
                String dialogMessage = getString(R.string.list_failed_with_error, msg);
                String dialogTitle = getString(R.string.load_remote_form_error);
                createAlertDialog(dialogTitle, dialogMessage);

        }

    }


    @SuppressWarnings("unchecked")
    @Override
    public void formsDownloadingComplete(String result) {

        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
            mDownloadFormsTask = null;
        }

        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }

        if (result.contentEquals("401")) {
            mDownloadFormsTask = new DownloadFormsTask();
            mDownloadFormsTask.setDownloaderListener(this);

            ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();

            SparseBooleanArray sba = getListView().getCheckedItemPositions();
            for (int i = 0; i < getListView().getCount(); i++) {
                if (sba.get(i, false)) {
                    String form = (String) getListAdapter().getItem(i);
                    filesToDownload.add(mFormNamesAndURLs.get(form));
                }
            }

            mDownloadFormsTask.execute(filesToDownload);
        }
    }

}
