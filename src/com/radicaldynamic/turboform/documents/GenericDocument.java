package com.radicaldynamic.turboform.documents;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.Attachment;
import org.ektorp.support.CouchDbDocument;

@SuppressWarnings("serial")
public class GenericDocument extends CouchDbDocument
{
    public static enum Status {incomplete, complete, temporary};
    
    private Integer authoredBy;
    private Integer updatedBy;
    
    private Date dateCreated;
    private Date dateUpdated;
    
    private String type;
    private Status status; 
    
    GenericDocument(String type) {
        setType(type);
    }
    
    @JsonProperty("author")
    public void setAuthoredBy(Integer author) {
        this.authoredBy = author;
    }
    
    @JsonProperty("author")
    public Integer getAuthoredBy() {
        return authoredBy;
    }
    
    @JsonProperty("updater")
    public void setUpdatedBy(Integer author) {
        this.updatedBy = author;
    }
    
    @JsonProperty("updater")
    public Integer getUpdatedBy() {
        return updatedBy;
    }
    
    @JsonProperty("date_created")
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    @JsonProperty("date_created")
    public Date getDateCreated() {
        return dateCreated;
    }
    
    @JsonProperty("date_updated")
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
    
    @JsonProperty("date_updated")
    public Date getDateUpdated() {
        return dateUpdated;
    }    
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Status getStatus()
    {
        return status;
    }
    
    public void addInlineAttachment(Attachment a) {
        super.addInlineAttachment(a);
    }
}
