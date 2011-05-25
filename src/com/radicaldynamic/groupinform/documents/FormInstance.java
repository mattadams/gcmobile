package com.radicaldynamic.groupinform.documents;

import com.radicaldynamic.groupinform.logic.ODKInstanceAttributes;

public class FormInstance extends Generic
{    
    private static final String t = "FormInstanceDoc: ";
    private static final long serialVersionUID = -2924171490521236262L;

    /*
     * Placeholder: Created when a new instance is created for entry (may be deleted if instance entry is cancelled before being saved)
     * Draft:       A form instance that is saved but not marked complete (same as ODK "incomplete" status)
     * Complete:    A form instance that is saved and marked complete
     * Updated:     Not an actual status (represents forms that have been updated/created by others)
     * Removed:     A form instance marked for delayed deletion
     * Nothing:     Not an actual status (represents queries for forms without regard for instance status)
     */
    public static enum Status { placeholder, draft, complete, updated, removed, nothing };

    private String formId;
    private ODKInstanceAttributes odk;
    private Status status;
    
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
}
