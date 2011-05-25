package com.radicaldynamic.groupinform.xform;

import java.util.ArrayList;

import com.radicaldynamic.groupinform.documents.FormDefinition;

/*
 * An instance of a FormBuilder should be thought of as a "form being edited."
 * It is the place to stash all of the pieces of a form-in-progress until such
 * time as it is saved and written back to disk.  Setting the global formBuilder
 * variable to null is an easy way to reset the form builder.
 */
public class FormBuilderState 
{
    private FormDefinition  formDefDoc  = null;         // The form definition document from the DB for reference
    private Field                   field       = null;         // For passing a form "field" between activities
    private Field                   item        = null;         // For passing a select list item to the translation screen
    
    private ArrayList<Bind>         binds           = null;
    private ArrayList<Field>        fields          = null;
    private ArrayList<Instance>     instance        = null;
    private ArrayList<Translation>  translations    = null;
    
    public FormBuilderState() { }

    public void setField(Field field) { this.field = field; }
    public Field getField() { return field; }
    
    public void setItem(Field item) { this.item = item; }
    public Field getItem() { return item; }

    public void setFormDefDoc(FormDefinition formDefDoc) { this.formDefDoc = formDefDoc; }
    public FormDefinition getFormDefDoc() { return formDefDoc; }

    public void setBinds(ArrayList<Bind> binds) { this.binds = binds; }
    public ArrayList<Bind> getBinds() { return binds; }

    public void setFields(ArrayList<Field> fields) { this.fields = fields; }
    public ArrayList<Field> getFields() { return fields; }

    public void setInstance(ArrayList<Instance> instance) { this.instance = instance; }
    public ArrayList<Instance> getInstance() { return instance; }

    public void setTranslations(ArrayList<Translation> translations) { this.translations = translations; }
    public ArrayList<Translation> getTranslations() { return translations; }
}
