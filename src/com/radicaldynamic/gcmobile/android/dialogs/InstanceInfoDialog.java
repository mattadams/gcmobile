package com.radicaldynamic.gcmobile.android.dialogs;

import java.util.Iterator;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.utilities.StringUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class InstanceInfoDialog extends AlertDialog 
{
    Context context;
    
    FormDefinition formDefinition;
    FormInstance formInstance;
    
    public InstanceInfoDialog(final Context context, final FormDefinition fd, final FormInstance fi) 
    {
        super(context);
        this.context = context;
        
        formDefinition = fd;
        formInstance = fi;
        
        setTitleAndMessage();
        
        setButton(context.getString(R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) 
            {
                dialog.cancel();
            }            
        });
        
        if (!Collect.getInstance().getDeviceState().getDeviceRole().equals(AccountDevice.ROLE_DATA_ENTRY)) {
            setButton2(context.getString(R.string.tf_assign_form), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) 
                {
                    new InstanceAssignDialog(context, fi).show();
                }            
            });
        }
    }

    private void setTitleAndMessage()
    {
        String assignedTo = "N/A";
        String createdBy = "N/A";
        String updatedBy = "N/A";        
        
        // Try to use the instance name and fallback to definition name
        if (formInstance.getName() == null || formInstance.getName().length() == 0)
            setTitle(formDefinition.getName());
        else 
            setTitle(formInstance.getName());
        
        // Get human readable list of people assigned to this instance, if applicable
        if (formInstance.getAssignedTo() != null && !formInstance.getAssignedTo().isEmpty()) {
            Iterator<String> i = formInstance.getAssignedTo().iterator();
            assignedTo = "";
            
            while (i.hasNext()) {
                String deviceId = i.next();
                
                if (Collect.getInstance().getDeviceState().getDeviceList().containsKey(deviceId)) {
                    AccountDevice device = Collect.getInstance().getDeviceState().getDeviceList().get(deviceId);
                    assignedTo = assignedTo + "\n- " + device.getDisplayName(); 
                }
            } 
        }
        
        if (formInstance.getDateCreated() != null) {
            createdBy = formInstance.getCreatedByAlias() + "\n" + formInstance.getDateCreated().replace(" +0000", "");
        }
        
        if (formInstance.getDateUpdated() != null) {
            updatedBy = formInstance.getUpdatedByAlias() + "\n" + formInstance.getDateUpdated().replace(" +0000", "");
        }

        // Display status, created by/date, updated by/date, assigned to
        setMessage(
                "Status: " + StringUtils.ucfirst(formInstance.getStatus().toString()) + "\n\n" +
                "Created By: " + createdBy + "\n\n" +
                "Updated By: " + updatedBy + "\n\n" +
                "Assigned To: " + assignedTo);
    }
}
