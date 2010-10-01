package com.radicaldynamic.turboform.documents;

public class InstanceDocument extends GenericDocument
{    
    private static final long serialVersionUID = -2924171490521236262L;

    public static enum Status {placeholder, incomplete, complete, updated, deleted, nonexistent};
    
    private String form;
    private Status status;
    
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
}
