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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.AccountFolderListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * 
 */
public class AccountFolderList extends ListActivity
{
    private static final String t = "AccountFolderList: ";

    private static final int MENU_ADD = Menu.FIRST;
    
    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_form_folders));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
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
        menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_folder)).setIcon(R.drawable.ic_menu_add);
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
//        FormDocument form = (FormDocument) getListAdapter().getItem(position);
//
//        Log.d(Collect.LOGTAG, t + "selected form " + form.getId() + " from list");
//
//        Intent i = new Intent(this, FormBuilderFieldList.class);
//        i.putExtra(FormEntryActivity.KEY_FORMID, form.getId());
//        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();

        @Override
        protected Void doInBackground(Void... nothing)
        {
            File folderCache = new File(FileUtils.FOLDER_CACHE_FILE_PATH);

            // If cache is older than 120 seconds
            if (folderCache.lastModified() < (Calendar.getInstance().getTimeInMillis() - 120 * 1000))                    
                fetchFolderList();

            folders = loadFolderList();
            
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
            
            if (folders.isEmpty()) {
                TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                nothingToDisplay.setVisibility(View.VISIBLE);
            } else {
                AccountFolderListAdapter adapter;
                
                adapter = new AccountFolderListAdapter(
                        getApplicationContext(),
                        R.layout.folder_list_item,
                        folders);

                setListAdapter(adapter);
            }            
        }
    }
    
    /*
     * Fetch a new folder list from Inform Online and store it on disk
     */
    private void fetchFolderList()
    {
        Log.d(Collect.LOGTAG, t + "fetching new list of folders");
        
        ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();
        
        // Try to ping the service to see if it is "up"
        String folderListUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/folder/list";
        String getResult = HttpUtils.getUrlData(folderListUrl);
        JSONObject jsonFolderList;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
            jsonFolderList = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = jsonFolderList.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
            
            if (result.equals(InformOnlineState.OK)) {
                JSONArray jsonFolders = jsonFolderList.getJSONArray("folders");
                
                for (int i = 0; i < jsonFolders.length(); i++) {
                    JSONObject jsonFolder = jsonFolders.getJSONObject(i);
                    
                    folders.add(new AccountFolder(
                            jsonFolder.getString("id"),
                            jsonFolder.getString("owner"),
                            jsonFolder.getString("name"),
                            jsonFolder.getString("description"),
                            jsonFolder.getString("visibility")));
                }
                
                try {
                    FileOutputStream fos = new FileOutputStream(new File(FileUtils.FOLDER_CACHE_FILE_PATH));
                    ObjectOutputStream out = new ObjectOutputStream(fos);
                    out.writeObject(folders);   
                    out.close();
                    fos.close();
                } catch (Exception e) {                    
                    Log.e(Collect.LOGTAG, t + "unable to write folder cache: " + e.toString());
                    e.printStackTrace();
                }
            } else {
                // There was a problem... handle it!
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private ArrayList<AccountFolder> loadFolderList()
    {
        Log.d(Collect.LOGTAG , t + "loading folder cache");
        
        ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();
        
        try {
            FileInputStream fis = new FileInputStream(new File(FileUtils.FOLDER_CACHE_FILE_PATH));
            ObjectInputStream in = new ObjectInputStream(fis);            
            folders = (ArrayList<AccountFolder>) in.readObject();
            in.close();
            fis.close();
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "unable to read form folder cache: " + e.toString());
            e.printStackTrace();
        }
     
        return folders;
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
}