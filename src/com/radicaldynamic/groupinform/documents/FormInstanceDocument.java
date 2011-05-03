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
    public static enum Status {placeholder, draft, complete, submitted, updated, removed, nothing};
    
    private String formId;
    private Status status;
    private String dateAggregated;          // The date that this document was last uploaded to an ODK Aggregate server
    private boolean submissionEditable;     // Same
    private String submissionUri;           // Compatibility with ODK Collect 1.1.6 or r479+    
    
    public FormInstanceDocument() {
        super("instance");
        
        if (isNew())
            setStatus(Status.placeholder);
    }

    public void setDateAggregated(String dateAggregated)
    {
        this.dateAggregated = dateAggregated;
    }

    public String getDateAggregated()
    {
        return dateAggregated;
    }
    
    @JsonIgnore
    public Calendar getDateAggregatedAsCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat(GenericDocument.DATETIME);
        Calendar calendar = Calendar.getInstance();
        
        try {
            calendar.setTime(sdf.parse(dateAggregated));
        } catch (ParseException e1) {
            Log.e(Collect.LOGTAG, t + "unable to parse dateAggregated, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
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

    public void setSubmissionEditable(boolean submissionEditable) {
        this.submissionEditable = submissionEditable;
    }

    public boolean isSubmissionEditable() {
        return submissionEditable;
    }

    public void setSubmissionUri(String submissionUri) {
        this.submissionUri = submissionUri;
    }

    public String getSubmissionUri() {
        return submissionUri;
    }
}
