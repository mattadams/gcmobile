package com.radicaldynamic.groupinform.documents;

public class FormDefinitionDocument extends GenericDocument
{
    @SuppressWarnings("unused")
    private static final String t = "FormDefinitionDocument: ";
    private static final long serialVersionUID = 8292491291779289389L;   
    
    public static enum Status {active, inactive, temporary};
    
    private String name;
    private Status status;

    public FormDefinitionDocument() {
        super("form");
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
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
