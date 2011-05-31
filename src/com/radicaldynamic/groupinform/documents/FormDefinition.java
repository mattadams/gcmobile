package com.radicaldynamic.groupinform.documents;

public class FormDefinition extends Generic
{
    @SuppressWarnings("unused")
    private static final String t = "FormDefinitionDoc: ";
    private static final long serialVersionUID = 8292491291779289389L;   
    
    public static enum Status { active, inactive, removed, temporary };
    
    private String javaRosaId;
    private String modelVersion;
    private String name;
    private Status status;
    private String submissionUri;
    private String uiVersion;    

    public FormDefinition() {
        super("form");
    }

    public void setJavaRosaId(String javaRosaId) {
        this.javaRosaId = javaRosaId;
    }

    public String getJavaRosaId() {
        return javaRosaId;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getModelVersion() {
        return modelVersion;
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

    public void setSubmissionUri(String submissionUri) {
        this.submissionUri = submissionUri;
    }

    public String getSubmissionUri() {
        return submissionUri;
    }

    public void setUiVersion(String uiVersion) {
        this.uiVersion = uiVersion;
    }

    public String getUiVersion() {
        return uiVersion;
    }
}
