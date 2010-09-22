package com.radicaldynamic.turboform.documents;

public class FormDocument extends GenericDocument
{
    private static final long serialVersionUID = 8292491291779289389L;   
    
    private String name;

    FormDocument() {
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
}
