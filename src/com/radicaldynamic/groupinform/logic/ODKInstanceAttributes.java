package com.radicaldynamic.groupinform.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.codehaus.jackson.annotate.JsonIgnore;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.Generic;

/*
 * Attributes associated with a FormInstance that serve no purpose but to provide interoperability with
 * Open Data Kit upstream functionality or compatibility with native ODK components (e.g., ODK Aggregate).
 */
public class ODKInstanceAttributes 
{   
    private static final String t = "ODKInstanceAttributes: ";
    
    // See ODK provider/SubmissionsStorage.java
    public static enum UploadStatus { complete, failed };     
    
    private String uploadDate;
    private UploadStatus uploadStatus;
    private String uploadUri;
    
    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public String getUploadDate() {
        return uploadDate;
    }
    
    @JsonIgnore
    public Calendar getUploadDateAsCalendar() 
    {
        SimpleDateFormat sdf = new SimpleDateFormat(Generic.DATETIME);
        Calendar calendar = Calendar.getInstance();
        
        try {
            calendar.setTime(sdf.parse(uploadDate));
        } catch (ParseException e1) {
            Log.e(Collect.LOGTAG, t + "unable to parse uploadDate, returning a valid date anyway: " + e1.toString());            
        }
        
        return calendar;
    }
    
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
    
    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }
    
    public void setUploadUri(String uploadUri) {
        this.uploadUri = uploadUri;
    }
    
    public String getUploadUri() {
        return uploadUri;
    }
}
