package com.radicaldynamic.groupinform.logic;



/*
 * This class represents a form folder result as returned from Inform Online
 */
public class AccountFolder
{
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String visibility;
    private boolean replicated;

    public AccountFolder(String id, String ownerId, String name, String description, String visibility, boolean replicated)
    {
        this.setId(id);
        this.setOwnerId(ownerId);
        this.setName(name);
        this.setDescription(description);
        this.setVisibility(visibility);
        this.setReplicated(replicated);
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

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