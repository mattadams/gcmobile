package com.radicaldynamic.groupinform.documents;

import java.util.List;

import com.radicaldynamic.groupinform.logic.ODKInstanceAttributes;

public class FormInstance extends Generic
{
    private static final long serialVersionUID = -2924171490521236262L;

    /*
     * Any:         Not an actual status (represents queries for forms without regard for instance status, e.g., give me everything)
     * Draft:       A form instance that is saved but not marked complete (same as ODK "incomplete" status)
     * Complete:    A form instance that is saved and marked complete
     * Placeholder: Created when a new instance is created for entry (may be deleted if instance entry is cancelled before being saved)
     * Removed:     A form instance marked for delayed deletion
     */
    public static enum Status { any, draft, complete, placeholder, removed };

    private String formId;
    private String name;
    private ODKInstanceAttributes odk;
    private Status status;
    private List<String> assignedTo;
    
    public FormInstance() 
    {
        super("instance");
    }

    public void setFormId(String form)
    {
        this.formId = form;
    }

    public String getFormId()
    {
        return formId;
    }    

    public void setName(String name) {
        if (name != null) 
            name = name.trim();        
        
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setOdk(ODKInstanceAttributes odk) {
        this.odk = odk;
    }

    public ODKInstanceAttributes getOdk() {
        if (odk == null) {
            odk = new ODKInstanceAttributes();
        }
            
        return odk;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setAssignedTo(List<String> assignedTo) {
        this.assignedTo = assignedTo;
    }

    public List<String> getAssignedTo() {
        return assignedTo;
    }
}
