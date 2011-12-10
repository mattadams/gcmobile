package com.radicaldynamic.groupinform.documents;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ektorp.Attachment;
import org.ektorp.support.CouchDbDocument;

import android.util.Log;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountDevice;

@SuppressWarnings("serial")
public class Generic extends CouchDbDocument
{
    private static final String t = "GenericDocument: ";

    public static final String DATETIME = "yyyy/MM/dd HH:mm:ss Z"; 
   
    private String createdBy;
    private String updatedBy;
    private String dateCreated;
    private String dateUpdated;
    
    private String type;
    
    /*
     * TODO: possibly remove?
     * 
     * This was originally added during conversion from FileDbAdapter to TFCouchDBService as 
     * a way to compare a serialised form definition with the original XML file.  It isn't being
     * used at the moment so we might want to remove it in the future.
     */
    private String xmlHash;
    
    Generic(String type) {
        setType(type);
    }
    
    @JsonIgnore
    public static String generateTimestamp() 
    {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        
        formatter.setTimeZone(TimeZone.getDefault());
        formatter.applyPattern(DATETIME);
        
        return formatter.format(calendar.getTime());       
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    
    @JsonIgnore
    public String getCreatedByAlias() 
    { 
        AccountDevice device = Collect.getInstance().getInformOnlineState().getAccountDevices().get(createdBy);
        
        if (device == null)
            return Collect.getInstance().getString(R.string.tf_unavailable).toString();
        else
            return device.getDisplayName();
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
    
    @JsonIgnore
    public String getUpdatedByAlias() 
    { 
        AccountDevice device = Collect.getInstance().getInformOnlineState().getAccountDevices().get(updatedBy);
        
        if (device == null)
            return Collect.getInstance().getString(R.string.tf_unavailable).toString();
        else
            return device.getDisplayName();
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    public String getDateCreated() {
        return dateCreated;
    }
    
    @JsonIgnore
    public Calendar getDateCreatedAsCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME);
        Calendar calendar = Calendar.getInstance();
        
        try {
            calendar.setTime(sdf.parse(dateCreated));
        } catch (ParseException e1) {
            if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to parse dateCreated, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
    }
    
    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
    
    public String getDateUpdated() {         
        return dateUpdated;
    }  

    @JsonIgnore
    public Calendar getDateUpdatedAsCalendar() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME);        
                
        try {
            calendar.setTime(sdf.parse(dateUpdated));
        } catch (ParseException e1) {
            Log.e(Collect.LOGTAG, t + "unable to parse dateUpdated, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void addInlineAttachment(Attachment a) {
        super.addInlineAttachment(a);
    }

    public void setXmlHash(String hash)
    {
        this.xmlHash = hash;
    }

    public String getXmlHash()
    {
        return xmlHash;
    }
}
