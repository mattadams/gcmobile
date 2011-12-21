package com.radicaldynamic.groupinform.logic;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;



/*
 * This class represents a form folder result as returned from Inform Online
 */
public class AccountFolder
{
    private String id;
    private String rev;
    private String ownerId;
    private String name;
    private String description;
    private String visibility;
    private boolean replicated;

    public AccountFolder(
            String id, 
            String rev, 
            String ownerId, 
            String name, 
            String description, 
            String visibility, 
            boolean replicated)
    {
        this.setId(id);
        this.setRev(rev);
        this.setOwnerId(ownerId);
        this.setName(name);
        this.setDescription(description);
        this.setVisibility(visibility);
        this.setReplicated(replicated);
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }    

    public void setRev(String rev) { this.rev = rev; }
    public String getRev() { return rev; }
    
    public String getOwnerAlias() 
    { 
        AccountDevice device = Collect.getInstance().getDeviceState().getAccountDevices().get(ownerId);
        
        if (device == null)
            return Collect.getInstance().getString(R.string.tf_unavailable).toString();
        else
            return device.getDisplayName();
    }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerId() { return ownerId; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getVisibility() { return visibility; }

    public void setReplicated(boolean replicated) { this.replicated = replicated; }
    public boolean isReplicated() { return replicated; }
    
    public String toString() { return getName(); }
}