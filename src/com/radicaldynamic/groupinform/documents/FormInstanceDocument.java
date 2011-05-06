package com.radicaldynamic.groupinform.documents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.codehaus.jackson.annotate.JsonIgnore;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;

public class FormInstanceDocument extends GenericDocument
{    
    private static final String t = "FormInstanceDocument: ";
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
    
    // See ODK provider/SubmissionsStorage.java
    public static enum OdkSubmissionStatus { complete, partial, failed }; 
    
    private String formId;
    private Status status;
    
    // Entirely for compatibility with ODK Aggregate
    private String odkSubmissionDate;          // Last submission attempted
    private boolean odkSubmissionEditable;     // Compatibility with ODK Collect 1.1.6 or r479+
    private String odkSubmissionResultMsg;
    private OdkSubmissionStatus odkSubmissionStatus;
    private String odkSubmissionUri;           // Compatibility with ODK Collect 1.1.6 or r479+    
    
    public FormInstanceDocument() 
    {
        super("instance");
        
        if (isNew())
            setStatus(Status.placeholder);
    }

    public void setFormId(String form)
    {
        this.formId = form;
    }

    public String getFormId()
    {
        return formId;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setOdkSubmissionDate(String odkSubmissionDate) 
    {
        this.odkSubmissionDate = odkSubmissionDate;
    }

    public String getOdkSubmissionDate() 
    {
        return odkSubmissionDate;
    }

    @JsonIgnore
    public Calendar getOdkSubmissionDateAsCalendar() 
    {
        SimpleDateFormat sdf = new SimpleDateFormat(GenericDocument.DATETIME);
        Calendar calendar = Calendar.getInstance();
        
        try {
            calendar.setTime(sdf.parse(odkSubmissionDate));
        } catch (ParseException e1) {
            Log.e(Collect.LOGTAG, t + "unable to parse dateAggregated, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
    }

    public void setOdkSubmissionEditable(boolean odkSubmissionEditable) 
    {
        this.odkSubmissionEditable = odkSubmissionEditable;
    }

    public boolean isOdkSubmissionEditable() 
    {
        return odkSubmissionEditable;
    }

    public void setOdkSubmissionResultMsg(String odkSubmissionResultMsg) 
    {
        this.odkSubmissionResultMsg = odkSubmissionResultMsg;
    }

    public String getOdkSubmissionResultMsg() 
    {
        return odkSubmissionResultMsg;
    }

    public void setOdkSubmissionStatus(OdkSubmissionStatus odkSubmissionStatus) 
    {
        this.odkSubmissionStatus = odkSubmissionStatus;
    }

    public OdkSubmissionStatus getOdkSubmissionStatus()
    {
        return odkSubmissionStatus;
    }

    public void setOdkSubmissionUri(String odkSubmissionUri) 
    {
        this.odkSubmissionUri = odkSubmissionUri;
    }

    public String getOdkSubmissionUri() 
    {
        return odkSubmissionUri;
    }
}
