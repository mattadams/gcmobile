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

package com.radicaldynamic.turboform.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.Spinner;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.adapters.LocalFormListAdapter;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.repository.FormRepository;
import com.radicaldynamic.turboform.utilities.DocumentUtils;

/*
 * 
 */
public class LocalFormList extends ListActivity
{
    private static final String t = "LocalFormList: ";

    private ProgressDialog mDialog;

    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage(getText(R.string.tf_loading_please_wait));
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(true);
        mDialog.show();

        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main_browser);
        setTitle(getString(R.string.app_name) + " > "
                + getString(R.string.main_menu));
        //setProgressBarIndeterminateVisibility(false);

        // TODO: select "my forms" group/database

        Spinner s1 = (Spinner) findViewById(R.id.form_filter);
        s1.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (Collect.mDb != null)
            loadScreen();
    }

    /**
     * onStop Re-enable the splash screen.
     */
    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        // MenuInflater inflater = getMenuInflater();
        // inflater.inflate(R.menu.tf_main_menu_context, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        // MenuInflater inflater = getMenuInflater();
        // inflater.inflate(R.menu.tf_main_menu_options, menu);
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        // AdapterContextMenuInfo info = (AdapterContextMenuInfo)
        // item.getMenuInfo();
        switch (item.getItemId()) {
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position,
            long id)
    {
        FormDocument form = (FormDocument) getListAdapter().getItem(position);

        Log.d(Collect.LOGTAG, t + "selected form " + form.getId() + " from list");

        Intent i = new Intent(this, FormBuilderList.class);
        i.putExtra(FormEntryActivity.KEY_FORMID, form.getId());
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Intent i = null;
        //
        // switch (item.getItemId()) {
        // case R.id.tf_synchronize:
        // i = new Intent(this, SynchronizeTabs.class);
        // startActivity(i);
        // return true;
        // }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<FormDocument> documents = new ArrayList<FormDocument>();
        private HashMap<String, HashMap<String, String>> instanceTalliesByStatus = new HashMap<String, HashMap<String, String>>();

        @Override
        protected Void doInBackground(Void... nothing)
        {
            documents = (ArrayList<FormDocument>) new FormRepository(Collect.mDb.getDb()).getAll();
            DocumentUtils.sortByName(documents);                             

            instanceTalliesByStatus = new FormRepository(Collect.mDb.getDb()).getFormsWithInstanceCounts();
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            //setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            LocalFormListAdapter adapter;
            adapter = new LocalFormListAdapter(getApplicationContext(),
                    R.layout.main_browser_list_item, documents,
                    instanceTalliesByStatus, (Spinner) findViewById(R.id.form_filter));
            setListAdapter(adapter);

            //setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Load the various elements of the screen that must wait for other tasks to
     * complete
     */
    private void loadScreen()
    {
        mRefreshViewTask = new RefreshViewTask();
        mRefreshViewTask.execute();

        registerForContextMenu(getListView());

        mDialog.cancel();
    }
}
