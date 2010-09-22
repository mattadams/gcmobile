package com.radicaldynamic.turboform.documents;

public class RecordDocument extends GenericDocument
{    
    private static final long serialVersionUID = -2924171490521236262L;

    private String form;
    
    public RecordDocument() {
        super("record");       
    }

    public void setForm(String form)
    {
        this.form = form;
    }

    public String getForm()
    {
        return form;
    }
}
