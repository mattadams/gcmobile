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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.UploaderListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDocument;
import com.radicaldynamic.groupinform.preferences.ServerPreferences;
import com.radicaldynamic.groupinform.repository.FormRepository;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link MainMenuActivity}.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

// TODO long click form for submission log
public class InstanceUploaderList extends ListActivity {
    //private static final String t = "InstanceUploaderList: ";

    private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
    private static final String BUNDLE_TOGGLED_KEY = "toggled";

    private static final int MENU_PREFERENCES = Menu.FIRST;
    private static final int INSTANCE_UPLOADER = 0;

    private Button mActionButton;
    private Button mToggleButton;

    private Map<String, List<String>> mInstanceTallies = new HashMap<String, List<String>>();
    private ArrayList<String> mSelected = new ArrayList<String>();
    
    private boolean mRestored = false;
    private boolean mToggled = false;

    private RefreshViewTask mRefreshViewTask;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_uploader_list);

        mActionButton = (Button) findViewById(R.id.upload_button);
        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mSelected.size() > 0) {
                    // items selected
                    uploadSelectedFiles();
                    refreshData();
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
                // Toggle selections of items to all or none
                ListView ls = getListView();
                mToggled = !mToggled;

                // Remove all items from selected list
                mSelected.clear();

                for (int pos = 0; pos < ls.getCount(); pos++) {
                    ls.setItemChecked(pos, mToggled);

                    // Add all items if mToggled sets to select all
                    if (mToggled) {
                        FormDocument form = (FormDocument) ls.getItemAtPosition(pos);
                        mSelected.add(form.getId());                         
                    }
                }

                mActionButton.setEnabled(!(mSelected.size() == 0));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_PREFERENCES, 0, getString(R.string.server_preferences)).setIcon(R.drawable.ic_menu_preferences);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        switch (requestCode) {
        // Returns with a form path, start entry
        case INSTANCE_UPLOADER:
            if (intent.getBooleanExtra(FormEntryActivity.KEY_SUCCESS, false)) {
                refreshData();
                
                if (mInstanceTallies.isEmpty())
                    finish();
            }
            
            break;
            
        default:
            break;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);        
        
        FormDocument form = (FormDocument) getListAdapter().getItem(position);

        // Add/remove from selected list
        if (mSelected.contains(form.getId()))
            mSelected.remove(form.getId());
        else
            mSelected.add(form.getId());

        mActionButton.setEnabled(!(mSelected.size() == 0));
    }

    @Override
    protected void onResume() {
        refreshData();
        super.onResume();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSelected = savedInstanceState.getStringArrayList(BUNDLE_SELECTED_ITEMS_KEY);
        mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
        mRestored = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(BUNDLE_SELECTED_ITEMS_KEY, mSelected);
        outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
    }

    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<FormDocument> documents = new ArrayList<FormDocument>();
    
        @Override
        protected Void doInBackground(Void... nothing)
        {
            mInstanceTallies = new FormRepository(Collect.mDb.getDb()).getFormsByAggregateReadiness();
            
            if (!mInstanceTallies.isEmpty()) {
                documents = (ArrayList<FormDocument>) new FormRepository(Collect.mDb.getDb()).
                    getAllByKeys(new ArrayList<Object>(mInstanceTallies.keySet()));
            
                DocumentUtils.sortByName(documents);
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
            UploaderListAdapter adapter = new UploaderListAdapter(getApplicationContext(), 
                    R.layout.two_item_multiple_choice, documents, mInstanceTallies);
            setListAdapter(adapter);
            
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            getListView().setItemsCanFocus(false);
    
            mActionButton.setEnabled(!(mSelected.size() == 0));
    
            // If current activity is being reinitialised due to changing orientation restore selected checkmarks
            if (mRestored) {
                ListView ls = getListView();
                
                for (int pos = 0; pos < ls.getCount(); pos++) {
                    FormDocument form = (FormDocument) ls.getItemAtPosition(pos);
                    
                    if (mSelected.contains(form.getId()))                    
                        ls.setItemChecked(pos, true);
                }   
    
                mRestored = false;
            }
    
            setProgressBarIndeterminateVisibility(false);
        }
    }

    private void createPreferencesMenu() {
        Intent i = new Intent(this, ServerPreferences.class);
        startActivity(i);
    }

    private void refreshData() {
        if (!mRestored)
            mSelected.clear();

        mRefreshViewTask = new RefreshViewTask();
        mRefreshViewTask.execute();
    }

    private void uploadSelectedFiles() {
        ArrayList<String> selectedInstances = new ArrayList<String>();
        
        for (String formId : mSelected) {
            selectedInstances.addAll(mInstanceTallies.get(formId));            
        }
        
        Intent i = new Intent(this, InstanceUploaderActivity.class);
        i.putExtra(FormEntryActivity.KEY_INSTANCES, selectedInstances);
        startActivityForResult(i, INSTANCE_UPLOADER);
    }
}
