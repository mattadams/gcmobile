package com.radicaldynamic.gcmobile.android.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.activities.BrowserActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountDevice;

public class FilterByAssignmentDialog extends Builder 
{
    private List<AccountDevice> deviceList = new ArrayList<AccountDevice>();
        
    private CharSequence [] items;
    boolean [] checkedItems;

    public FilterByAssignmentDialog(final Context context, final Bundle searchFilter)
    {
        super(context);
        
        deviceList = new ArrayList<AccountDevice>(Collect.getInstance().getDeviceState().getDeviceList().values());
        
        // Remove "removed" device profiles from from the list
        Iterator<AccountDevice> deviceListIterator = deviceList.iterator();
        
        while (deviceListIterator.hasNext()) {
            AccountDevice d = deviceListIterator.next();
            
            if (d.getStatus().equals(AccountDevice.STATUS_REMOVED)) {
                deviceListIterator.remove();
            }
        }
        
        items = new CharSequence[deviceList.size()];
        checkedItems = new boolean[deviceList.size()];
        
        int i = 0;
        
        for (AccountDevice d : deviceList) {
            if (searchFilter.getStringArrayList(BrowserActivity.KEY_SEARCH_BY_ASSIGNMENT_IDS) != null
                    && searchFilter.getStringArrayList(BrowserActivity.KEY_SEARCH_BY_ASSIGNMENT_IDS).contains(d.getId())) 
            { 
                checkedItems[i] = true;
            } else {
                checkedItems[i] = false;    
            }
            
            items[i++] = d.getDisplayName();
        }
        
        setTitle(context.getString(R.string.tf_select_devices));
        
        setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedItems[which] = isChecked;
            }
        });
        
        setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                
                ArrayList<String> selectedDevices = new ArrayList<String>();

                int i = 0;
                
                for (AccountDevice d : deviceList) {
                    if (checkedItems[i++] == true)
                        selectedDevices.add(d.getId());
                }
                
                searchFilter.putStringArrayList(BrowserActivity.KEY_SEARCH_BY_ASSIGNMENT_IDS, selectedDevices);

                dialog.dismiss();               
            }
        });
    }
}
