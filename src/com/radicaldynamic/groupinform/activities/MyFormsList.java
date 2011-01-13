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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.ektorp.Attachment;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.MyFormsListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDocument;
import com.radicaldynamic.groupinform.repository.FormRepository;
import com.radicaldynamic.groupinform.utilities.DocumentUtils;

/*
 * 
 */
public class MyFormsList extends ListActivity
{
    private static final String t = "MyFormsList: ";

    private static final int MENU_ADD = Menu.FIRST;
    
    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.main_menu));

        // TODO: select "my forms" group/database
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (Collect.mDb != null)
            loadScreen();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_form)).setIcon(R.drawable.ic_menu_add);
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
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        FormDocument form = (FormDocument) getListAdapter().getItem(position);

        Log.d(Collect.LOGTAG, t + "selected form " + form.getId() + " from list");

        Intent i = new Intent(this, FormBuilderFieldList.class);
        i.putExtra(FormEntryActivity.KEY_FORMID, form.getId());
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            promptForNewFormName();
            return true;
        }
        
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
            try {
                documents = (ArrayList<FormDocument>) new FormRepository(Collect.mDb.getDb()).getAll();
                DocumentUtils.sortByName(documents);                             

                instanceTalliesByStatus = new FormRepository(Collect.mDb.getDb()).getFormsWithInstanceCounts();                    
            } catch (ClassCastException e) {
                // TODO: is there a better way to handle empty lists?
            }
            
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
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
            if (documents.isEmpty()) {
                TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                nothingToDisplay.setVisibility(View.VISIBLE);
                
                openOptionsMenu();
            } else {
                MyFormsListAdapter adapter;
                
                adapter = new MyFormsListAdapter(
                        getApplicationContext(),
                        R.layout.main_browser_list_item, 
                        documents,
                        instanceTalliesByStatus, 
                        (Spinner) findViewById(R.id.form_filter));
                
                setListAdapter(adapter);
                
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.tf_edit_form_definition_hint),
                        Toast.LENGTH_LONG
                ).show();
            }            
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
    }    
    
    private void promptForNewFormName()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.create_form, null);        
        
        alert.setView(view);
        alert.setInverseBackgroundForced(true);
        alert.setTitle(getText(R.string.tf_create_form_dialog));

        // Set an EditText view to get user input 
        final EditText input = (EditText) view.findViewById(R.id.formName);
        
        alert.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {                
                FormDocument form = new FormDocument();
                form.setName(input.getText().toString());
                form.setStatus(FormDocument.Status.temporary);

                // Create a new form document and use an XForm template as the "xml" attachment
                try {
                    InputStream is = getResources().openRawResource(R.raw.xform_template);

                    // Set up variables to receive data
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    byte[] inputbuf = new byte[8192];            
                    int inputlen;

                    while ((inputlen = is.read(inputbuf)) > 0) {
                        data.write(inputbuf, 0, inputlen);
                    }
                    
                    form.addInlineAttachment(new Attachment("xml", Base64.encodeToString(data.toByteArray(), Base64.DEFAULT), "text/xml"));
                    Collect.mDb.getDb().create(form);
                    
                    is.close();
                    data.close();
                    
                    // Launch the form builder with the NEWFORM option set to true
                    Intent i = new Intent(MyFormsList.this, FormBuilderFieldList.class);
                    i.putExtra(FormEntryActivity.KEY_FORMID, form.getId());
                    i.putExtra(FormEntryActivity.NEWFORM, true);
                    startActivity(i);
                } catch (IOException e) {
                    Log.e(Collect.LOGTAG, t + "unable to read XForm template file; create new form process will fail");
                    e.printStackTrace();
                }
            }
        });

        alert.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        alert.show();
    }
}