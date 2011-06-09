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

import com.radicaldynamic.groupinform.R;
import org.odk.collect.android.preferences.PreferencesActivity;

import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.adapters.UploaderListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.repositories.FormDefinitionRepo;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link MainMenuActivity}.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

public class InstanceUploaderList extends ListActivity {

    private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
    private static final String BUNDLE_TOGGLED_KEY = "toggled";

    private static final int MENU_PREFERENCES = Menu.FIRST;
    private static final int INSTANCE_UPLOADER = 0;

    private Button mUploadButton;
    private Button mToggleButton;

    // BEGIN custom
//    private SimpleCursorAdapter mInstances;    
//    private ArrayList<Long> mSelected = new ArrayList<Long>();
    private boolean mRestored = false;
    private boolean mToggled = false;
    
    // Tallies
    private Map<String, List<String>> mInstances = new HashMap<String, List<String>>();
    private ArrayList<String> mSelected = new ArrayList<String>();
    // END custom


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_uploader_list);

        mUploadButton = (Button) findViewById(R.id.upload_button);
        mUploadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mSelected.size() > 0) {
                    // items selected
                    uploadSelectedFiles();
                    // BEGIN custom
                    refreshData();
                    // END custom
                    mToggled = false;
                } else {
                    // no items selected
                    Toast.makeText(getApplicationContext(), getString(R.string.noselect_error),
                        Toast.LENGTH_SHORT).show();
                }
            }

        });

        mToggleButton = (Button) findViewById(R.id.toggle_button);
        mToggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle selections of items to all or none
                ListView ls = getListView();
                mToggled = !mToggled;
                // remove all items from selected list
                mSelected.clear();
                for (int pos = 0; pos < ls.getCount(); pos++) {
                    ls.setItemChecked(pos, mToggled);
                    // add all items if mToggled sets to select all
                    // BEGIN custom
//                    if (mToggled)
//                        mSelected.add(ls.getItemIdAtPosition(pos));
                    if (mToggled) {
                        FormDefinition form = (FormDefinition) ls.getItemAtPosition(pos);
                        mSelected.add(form.getId());                         
                    }
                    // END custom
                }
                mUploadButton.setEnabled(!(mSelected.size() == 0));

            }
        });

//        // get all complete or failed submission instances
//        String selection = InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?";
//        String selectionArgs[] = {
//            InstanceProviderAPI.STATUS_COMPLETE, InstanceProviderAPI.STATUS_SUBMISSION_FAILED
//        };
//
//        Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);
//        startManagingCursor(c);
//
//        String[] data = new String[] {
//                InstanceColumns.DISPLAY_NAME, InstanceColumns.DISPLAY_SUBTEXT
//        };
//        int[] view = new int[] {
//                R.id.text1, R.id.text2
//        };
//
//        // render total instance view
//        mInstances =
//            new SimpleCursorAdapter(this, R.layout.two_item_multiple_choice, c, data, view);
//        setListAdapter(mInstances);
//        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
//        getListView().setItemsCanFocus(false);
//        mUploadButton.setEnabled(!(mSelected.size() == 0));

        // set title
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.send_data));

//        // if current activity is being reinitialized due to changing orientation restore all check
//        // marks for ones selected
//        if (mRestored) {
//            ListView ls = getListView();
//            for (long id : mSelected) {
//                for (int pos = 0; pos < ls.getCount(); pos++) {
//                    if (id == ls.getItemIdAtPosition(pos)) {
//                        ls.setItemChecked(pos, true);
//                        break;
//                    }
//                }
//
//            }
//            mRestored = false;
//        }
    }


    private void uploadSelectedFiles() {
        // BEGIN custom
//        // send list of _IDs. 
//        long[] instanceIDs = new long[mSelected.size()];
//        for (int i = 0; i < mSelected.size(); i++) {
//           instanceIDs[i] = mSelected.get(i);
//        }
//        
//        Intent i = new Intent(this, InstanceUploaderActivity.class);
//        i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIDs);
//        startActivityForResult(i, INSTANCE_UPLOADER);
        
        Bundle b = new Bundle();

        for (String formId : mSelected) {
            b.putStringArrayList(formId, new ArrayList<String>(mInstances.get(formId)));
        }

        Intent i = new Intent(this, InstanceUploaderActivity.class);
        i.putExtra(FormEntryActivity.KEY_INSTANCES, b);
        startActivityForResult(i, INSTANCE_UPLOADER);
        // END custom
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // BEGIN custom
//        // get row id from db
//        Cursor c = (Cursor) getListAdapter().getItem(position);
//        long k = c.getLong(c.getColumnIndex(InstanceColumns._ID));
//
//        // add/remove from selected list
//        if (mSelected.contains(k))
//            mSelected.remove(k);
//        else
//            mSelected.add(k);

        FormDefinition form = (FormDefinition) getListAdapter().getItem(position);

        // Add/remove from selected list
        if (mSelected.contains(form.getId()))
            mSelected.remove(form.getId());
        else
            mSelected.add(form.getId());
        // END custom

        mUploadButton.setEnabled(!(mSelected.size() == 0));
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // BEGIN custom
//        long[] selectedArray = savedInstanceState.getLongArray(BUNDLE_SELECTED_ITEMS_KEY);
//        for (int i = 0; i < selectedArray.length; i++)
//            mSelected.add(selectedArray[i]);
        mSelected = savedInstanceState.getStringArrayList(BUNDLE_SELECTED_ITEMS_KEY);
        // END custom
        mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
        mRestored = true;
        // BEGIN custom
//        mUploadButton.setEnabled(selectedArray.length > 0);
        mUploadButton.setEnabled(mSelected.size() > 0);
        // END custom
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // BEGIN custom
//        long[] selectedArray = new long[mSelected.size()];
//        for (int i = 0; i < mSelected.size(); i++)
//            selectedArray[i] = mSelected.get(i);
//        outState.putLongArray(BUNDLE_SELECTED_ITEMS_KEY, selectedArray);
        outState.putStringArrayList(BUNDLE_SELECTED_ITEMS_KEY, mSelected);
        // END custom
        outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            // returns with a form path, start entry
            case INSTANCE_UPLOADER:
                if (intent.getBooleanExtra(FormEntryActivity.KEY_SUCCESS, false)) {
                    mSelected.clear();
                    getListView().clearChoices();
                    if (mInstances.isEmpty()) {
                        finish();
                    }
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    // BEGIN custom
    @Override
    protected void onResume() {
        refreshData();
        super.onResume();
    }
    
    private void refreshData() {
        if (!mRestored)
            mSelected.clear();

        new RefreshViewTask().execute();
    }
    
    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<FormDefinition> documents = new ArrayList<FormDefinition>();
    
        @Override
        protected Void doInBackground(Void... nothing)
        {
            mInstances = new FormDefinitionRepo(Collect.getInstance().getDbService().getDb()).getByAggregateReadiness();
            
            if (!mInstances.isEmpty()) {
                try {
                    documents = (ArrayList<FormDefinition>) new FormDefinitionRepo(Collect.getInstance().getDbService().getDb()).getAllActiveByKeys(new ArrayList<Object>(mInstances.keySet()));            
                    DocumentUtils.sortByName(documents);
                } catch (ClassCastException e) {
                    // TODO: is there a better way to handle empty lists?
                    Log.w(Collect.LOGTAG, e.toString());
                }
            }
            
            return null;
        }
    
        @Override
        protected void onPreExecute()
        {
            setProgressBarIndeterminateVisibility(true);
        }
    
        @Override
        protected void onPostExecute(Void nothing)
        {
            UploaderListAdapter adapter = new UploaderListAdapter(getApplicationContext(), R.layout.two_item_multiple_choice, documents, mInstances);
            setListAdapter(adapter);
            
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            getListView().setItemsCanFocus(false);
    
            mUploadButton.setEnabled(!(mSelected.size() == 0));
    
            // If current activity is being reinitialised due to changing orientation restore selected checkmarks
            if (mRestored) {
                ListView ls = getListView();
                
                for (int pos = 0; pos < ls.getCount(); pos++) {
                    FormDefinition form = (FormDefinition) ls.getItemAtPosition(pos);
                    
                    if (mSelected.contains(form.getId()))                    
                        ls.setItemChecked(pos, true);
                }   
    
                mRestored = false;
            }
    
            setProgressBarIndeterminateVisibility(false);
        }
    }
    // END custom
}
