package com.radicaldynamic.turboform.documents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.codehaus.jackson.annotate.JsonIgnore;

import android.util.Log;

import com.radicaldynamic.turboform.application.Collect;

public class InstanceDocument extends GenericDocument
{    
    private static final long serialVersionUID = -2924171490521236262L;

    /*
     * Placeholder: Created when a new instance is created for entry (may be deleted if instance entry is cancelled before being saved)
     * Incomplete:  A form instance that is saved but not marked complete
     * Complete:    A form instance that is saved and marked complete
     * Updated:     Not an actual status (represents forms that have been updated/created by others)
     * Deleted:     A form instance marked for delayed deletion
     * Nothing:     Not an actual status (represents queries for forms without regard for instance status)
     */
    public static enum Status {placeholder, incomplete, complete, updated, deleted, nothing};
    
    private String form;
    private Status status;
    private String dateAggregated;          // The date that this document was last uploaded to an ODK Aggregate server
    
    public InstanceDocument() {
        super("instance");
        
        if (isNew())
            setStatus(Status.placeholder);
    }

    public void setForm(String form)
    {
        this.form = form;
    }

    public String getForm()
    {
        return form;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Status getStatus()
    {
        return status;
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
            Log.e(Collect.LOGTAG, "Unable to parse dateAggregated, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
    }
}
