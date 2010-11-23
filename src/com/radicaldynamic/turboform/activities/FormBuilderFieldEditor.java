package com.radicaldynamic.turboform.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.xform.Bind;
import com.radicaldynamic.turboform.xform.Field;

public class FormBuilderFieldEditor extends Activity
{
    private static final String t = "FormBuilderElementEditor: ";
    
    public static final String ELEMENT_TYPE = "elementtype";
    
    private Field mField = null;
    private String mFieldType = null;
    
    private EditText mLabel;
    private EditText mHint;
    private EditText mDefaultValue;
    private CheckBox mReadonly;
    private CheckBox mRequired;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        setContentView(R.layout.form_builder_field_editor);
        
        mLabel          = (EditText) findViewById(R.id.label);
        mHint           = (EditText) findViewById(R.id.hint);
        mDefaultValue   = (EditText) findViewById(R.id.defaultValue);
        mReadonly       = (CheckBox) findViewById(R.id.readonly);
        mRequired       = (CheckBox) findViewById(R.id.required);
        
        if (savedInstanceState == null) {           
            Intent i = getIntent();
            mFieldType = i.getStringExtra(ELEMENT_TYPE);
            
            setTitle(getString(R.string.app_name) + " > " 
                    + getString(R.string.tf_add_new) + " "
                    + mFieldType.substring(0, 1).toUpperCase() + mFieldType.substring(1) + " " + getString(R.string.tf_field));
            
            // Retrieve field (if any)
            mField = Collect.getInstance().getFbField();
                       
            if (mField == null) {
                mField = new Field();
                mField.setBind(new Bind());
                
                if (mFieldType.equals("barcode")) {                    
                    mField.setType("input");                    
                    mField.getBind().setType(mFieldType);
                    loadBarcodeElement();
                    
                } else if (mFieldType.equals("date")) {
                    mField.setType("input");                    
                    mField.getBind().setType(mFieldType);
                    loadDateElement();
                    
                } else if (mFieldType.equals("geopoint")) {
                    mField.setType("input");
                    mField.getBind().setType(mFieldType);
                    loadGeopointElement();
                    
                } else if (mFieldType.equals("group")) {
                    mField.setType("group");
                    loadGroupElement();
                    
                } else if (mFieldType.equals("media")) {
                    // Make this an image media upload by default (changeable via radio field)
                    mField.setType("upload");
                    mField.getBind().setType("binary");
                    mField.getAttributes().put("mediatype", "image/*");
                    loadMediaElement();
                    
                } else if (mFieldType.equals("number")) {
                    // Make this integer by default (changeable via radio field)
                    mField.setType("input");
                    mField.getBind().setType("int");
                    loadNumberElement();
                    
                } else if (mFieldType.equals("select")) {
                    // Make this a multiple select input by default (changeable via radio field)
                    mField.setType(mFieldType);
                    mField.getBind().setType(mFieldType);
                    loadSelectElement();
                    
                } else if (mFieldType.equals("text")) {
                    mField.setType("input");
                    mField.getBind().setType("string");
                    loadTextElement();
                    
                } else {
                    Log.w(Collect.LOGTAG, t + "unhandled field type");
                }

            } else {
                setTitle(getString(R.string.app_name) + " > " 
                        + getString(R.string.tf_edit) + " "
                        + mFieldType.substring(0, 1).toUpperCase() + mFieldType.substring(1) + " " + getString(R.string.tf_field));
                
                if (mFieldType.equals("barcode"))         loadBarcodeElement();                    
                else if (mFieldType.equals("date"))       loadDateElement();                
                else if (mFieldType.equals("geopoint"))   loadGeopointElement();                  
                else if (mFieldType.equals("group"))      loadGroupElement();                    
                else if (mFieldType.equals("media"))      loadMediaElement();                    
                else if (mFieldType.equals("number"))     loadNumberElement();                    
                else if (mFieldType.equals("select"))     loadSelectElement();                    
                else if (mFieldType.equals("text"))       loadTextElement();                    
                else 
                    Log.w(Collect.LOGTAG, t + "unhandled field type");
            }
            
            loadCommonAttributes();
        }
        
        mReadonly.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {                
                if (((CheckBox) v).isChecked()) {
                    mRequired.setChecked(false);
                    mField.getBind().setReadonly(true);
                } else {
                    mField.getBind().setReadonly(false);
                }
            }
        });
        
        mRequired.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {                
                if (((CheckBox) v).isChecked()) {
                    mReadonly.setChecked(false);
                    mField.getBind().setRequired(true);
                } else {
                    mField.getBind().setRequired(false);
                }
            }
        });
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        Collect.getInstance().setFbField(mField);
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.form_builder_field_editor_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {

        }
    
        return super.onOptionsItemSelected(item);
    }
    
    private void disableFormComponent(int componentResource)
    {
        ViewGroup component = (ViewGroup) findViewById(componentResource);
        component.setVisibility(View.GONE);
    }
    
    private void loadCommonAttributes()
    {         
         mLabel.setText(mField.getLabel().toString());
         mHint.setText(mField.getHint().toString());
         mDefaultValue.setText(mField.getInstance().getDefaultValue());
         
         if (mField.getBind().isReadonly())
             mReadonly.setChecked(true);
         
         if (mField.getBind().isRequired())
             mRequired.setChecked(true);
    }
    
    private void loadBarcodeElement()
    {
        loadCommonAttributes();
        
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
        disableFormComponent(R.id.readonlyLayout);
    }
    
    // TODO: add support for selecting times as well as dates once this becomes available
    private void loadDateElement()
    {
        loadCommonAttributes();
     
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
        
        // TODO: we should probably display a "default" date using the date widget
    }
    
    private void loadGeopointElement()
    {
        loadCommonAttributes();
     
        disableFormComponent(R.id.defaultValueInput);
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
        disableFormComponent(R.id.readonlyLayout);
    }
    
    private void loadGroupElement()
    {
        loadCommonAttributes();
    
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
    }
    
    private void loadMediaElement()
    {
        loadCommonAttributes();

        disableFormComponent(R.id.defaultValueInput);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
        disableFormComponent(R.id.readonlyLayout);        
        
        // Set up listener for radio buttons so that they influence the field type
        OnClickListener radioListener = new OnClickListener() {
            public void onClick(View v) {
                RadioButton rb = (RadioButton) v;
                
                switch (rb.getId()) {
                case R.id.mediaTypeAudio: mField.getAttributes().put("mediatype", "audio/*"); break;                    
                case R.id.mediaTypeImage: mField.getAttributes().put("mediatype", "image/*"); break;
                case R.id.mediaTypeVideo: mField.getAttributes().put("mediatype", "video/*"); break;
                }
            }
        };
        
        final RadioButton radioAudio = (RadioButton) findViewById(R.id.mediaTypeAudio);
        final RadioButton radioImage = (RadioButton) findViewById(R.id.mediaTypeImage);
        final RadioButton radioVideo = (RadioButton) findViewById(R.id.mediaTypeVideo);
        
        radioAudio.setOnClickListener(radioListener);
        radioImage.setOnClickListener(radioListener);
        radioVideo.setOnClickListener(radioListener);
        
        // Represent media type as it is currently stored in memory
        if (mField.getAttributes().get("mediatype").equals("audio/*"))
            radioAudio.setChecked(true);
        else if (mField.getAttributes().get("mediatype").equals("image/*"))
            radioImage.setChecked(true);
        else if (mField.getAttributes().get("mediatype").equals("video/*"))
            radioVideo.setChecked(true);
    }
    
    private void loadNumberElement()
    {
        loadCommonAttributes();
        
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
        
        final EditText defaultValue = (EditText) findViewById(R.id.defaultValue);
        
        // Set up listener for radio buttons so that they influence the field type
        OnClickListener radioListener = new OnClickListener() {
            public void onClick(View v) {
                RadioButton rb = (RadioButton) v;
                
                switch (rb.getId()) {
                case R.id.numberTypeInteger:
                    mField.getBind().setType("int");
                    defaultValue.setKeyListener(new DigitsKeyListener(false, false));
                    
                    // Remove any occurrences of a decimal
                    if (defaultValue.getText().toString().contains(".")) {
                        String txt = defaultValue.getText().toString();                        
                        defaultValue.setText(txt.replace(".", ""));
                    }
                    
                    break;
                    
                case R.id.numberTypeDecimal:
                    mField.getBind().setType("decimal");                   
                    defaultValue.setKeyListener(new DigitsKeyListener(false, true)); 
                    break;                    
                }
            }
        };
        
        final RadioButton radioInteger = (RadioButton) findViewById(R.id.numberTypeInteger);
        final RadioButton radioDecimal = (RadioButton) findViewById(R.id.numberTypeDecimal);
        
        radioInteger.setOnClickListener(radioListener);
        radioDecimal.setOnClickListener(radioListener);
        
        // Represent number type as it is currently stored in memory (set default input listener)
        if (mField.getBind().getType().equals("int")) { 
            radioInteger.setChecked(true);
            // false, false supports only integer input
            defaultValue.setKeyListener(new DigitsKeyListener(false, false));
        } else {
            radioDecimal.setChecked(true);
            // false, true supports decimal input
            defaultValue.setKeyListener(new DigitsKeyListener(false, true));
        }
    }
    
    private void loadSelectElement()
    {
        loadCommonAttributes();
        
        disableFormComponent(R.id.defaultValueInput);
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.readonlyLayout);
        
        // Set up listener for radio buttons so that they influence the field type
        OnClickListener radioListener = new OnClickListener() {
            public void onClick(View v) {
                RadioButton rb = (RadioButton) v;
                
                switch (rb.getId()) {
                case R.id.selectTypeMultiple:
                    mField.setType("select");
                    mField.getBind().setType("select");
                    break;
                    
                case R.id.selectTypeSingle:
                    mField.setType("select1");
                    mField.getBind().setType("select1");
                    
                    // TODO: reduce possible multiple default values to a single one
                    
                    break;
                }
            }
        };
        
        final RadioButton radioMultiple = (RadioButton) findViewById(R.id.selectTypeMultiple);
        final RadioButton radioSingle = (RadioButton) findViewById(R.id.selectTypeSingle);
        
        radioMultiple.setOnClickListener(radioListener);
        radioSingle.setOnClickListener(radioListener);

        // Represent number type as it is currently stored in memory
        if (mField.getType().equals("select"))
            radioMultiple.setChecked(true);
        else
            radioSingle.setChecked(true);        
    }
    
    private void loadTextElement()
    {
        loadCommonAttributes();
        
        disableFormComponent(R.id.mediaFieldTypeSelection);
        disableFormComponent(R.id.numberFieldTypeSelection);
        disableFormComponent(R.id.selectFieldTypeSelection);
    } 
}
