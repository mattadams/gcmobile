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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.AccountDeviceListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * 
 */
public class AccountDeviceList extends ListActivity
{
    private static final String t = "AccountDeviceList: ";

    private static final int MENU_ADD = Menu.FIRST;
    
    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.generic_list);        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_account_devices));
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
//        menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_group)).setIcon(R.drawable.ic_menu_add);
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
        AccountDevice device = (AccountDevice) getListAdapter().getItem(position);
        
        Log.d(Collect.LOGTAG, t + "selected device " + device.getId() + " from list");
        
        // Only account owners should proceed to the next screen
        if (Collect.getInstance().getInformOnline().isAccountOwner()) {
            Intent i = new Intent(this, AccountDeviceActivity.class);
            i.putExtra(AccountDeviceActivity.KEY_DEVICEID, device.getId());
            startActivity(i);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.tf_contact_account_owner), Toast.LENGTH_LONG).show();
        }
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
        private ArrayList<AccountDevice> devices = new ArrayList<AccountDevice>();

        @Override
        protected Void doInBackground(Void... nothing)
        {
            if (FileUtils.isFileOlderThan(FileUtils.DEVICE_CACHE_FILE_PATH, FileUtils.TIME_TWO_MINUTES))
                fetchDeviceList();

            devices = loadDeviceList();
            
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
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
            if (devices.isEmpty()) {
                TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                nothingToDisplay.setVisibility(View.VISIBLE);
            } else {
                AccountDeviceListAdapter adapter;
                
                adapter = new AccountDeviceListAdapter(
                        getApplicationContext(),
                        R.layout.folder_list_item,
                        devices);

                setListAdapter(adapter);
            }      
            
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    /*
     * Fetch a new device list from Inform Online and store it on disk 
     * (also store a hashed copy for later)
     */
    static public void fetchDeviceList()
    {
        Log.d(Collect.LOGTAG, t + "fetching new list of devices");
                
        // Try to ping the service to see if it is "up"
        String deviceListUrl = Collect.getInstance().getInformOnline().getServerUrl() + "/device/list";
        String getResult = HttpUtils.getUrlData(deviceListUrl);
        JSONObject jsonDeviceList;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
            jsonDeviceList = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = jsonDeviceList.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
            
            if (result.equals(InformOnlineState.OK)) {
                // Write out list of jsonDevices for later retrieval by loadDevicesList() and InformOnlineService.loadDevicesHash()                
                JSONArray jsonDevices = jsonDeviceList.getJSONArray("devices");
                
                try {
                    // Write out a device list cache file
                    FileOutputStream fos = new FileOutputStream(new File(FileUtils.DEVICE_CACHE_FILE_PATH));
                    fos.write(jsonDevices.toString().getBytes());
                    fos.close();
                } catch (Exception e) {                    
                    Log.e(Collect.LOGTAG, t + "unable to write device cache: " + e.toString());
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
    
    private ArrayList<AccountDevice> loadDeviceList()
    {
        Log.d(Collect.LOGTAG , t + "loading device cache");
        
        ArrayList<AccountDevice> devices = new ArrayList<AccountDevice>();
        
        try {
            FileInputStream fis = new FileInputStream(new File(FileUtils.DEVICE_CACHE_FILE_PATH));        
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader buffer = new BufferedReader(reader, 8192);
            StringBuilder sb = new StringBuilder();
            
            String cur;

            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
            
            buffer.close();
            reader.close();
            fis.close();
            
            try {
                JSONArray jsonDevices = (JSONArray) new JSONTokener(sb.toString()).nextValue();
                
                for (int i = 0; i < jsonDevices.length(); i++) {
                    JSONObject jsonDevice = jsonDevices.getJSONObject(i);

                    AccountDevice device = new AccountDevice(
                            jsonDevice.getString("id"),
                            jsonDevice.getString("alias"),
                            jsonDevice.getString("email"),
                            jsonDevice.getString("status"));

                    // Optional information that will only be present if the user is also an account owner
                    device.setLastCheckin(jsonDevice.optString("lastCheckin"));
                    device.setPin(jsonDevice.optString("pin"));
                    device.setTransferStatus(jsonDevice.optString("transferStatus"));
                    
                    // Show a device so long as it hasn't been marked as removed
                    if (!device.getStatus().equals("removed"))
                        devices.add(device);
                }
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse JSON " + sb.toString());
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "unable to read device cache: " + e.toString());
            e.printStackTrace();
        }
      
        return devices;
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