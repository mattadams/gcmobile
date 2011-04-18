package com.radicaldynamic.groupinform.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.xform.Bind;
import com.radicaldynamic.groupinform.xform.Field;

public class FormBuilderInstanceEditor extends Activity
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
        setContentView(R.layout.fb_field_editor);
        
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
                    + mFieldType.substring(0, 1).toUpperCase() + mFieldType.substring(1));
            
            // Retrieve field (if any)
            mField = Collect.getInstance().getFormBuilderState().getField();
                       
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
                        + mFieldType.substring(0, 1).toUpperCase() + mFieldType.substring(1));
                
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
        
//        mHidden.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {                
//                if (((CheckBox) v).isChecked()) {
//                    mReadonly.setChecked(false);
//                    mRequired.setChecked(false);
//                    mField.setHidden(true);
//                } else {
//                    mField.setHidden(false);
//                }
//            }
//        });
        
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
        Collect.getInstance().getFormBuilderState().setField(mField);
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.form_builder_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        }
    
        return super.onOptionsItemSelected(item);
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
        
    }
    
    private void loadDateElement()
    {
        loadCommonAttributes();
        
    }
    
    private void loadGeopointElement()
    {
        loadCommonAttributes();
        
    }
    
    private void loadGroupElement()
    {
        loadCommonAttributes();
        
    }
    
    private void loadMediaElement()
    {
        loadCommonAttributes();
        
        RelativeLayout readonlyLayout = (RelativeLayout) findViewById(R.id.readonlyLayout);
        readonlyLayout.setVisibility(View.INVISIBLE);
    }
    
    private void loadNumberElement()
    {
        loadCommonAttributes();
        
    }
    
    private void loadSelectElement()
    {
        loadCommonAttributes();
        
        RelativeLayout readonlyLayout = (RelativeLayout) findViewById(R.id.readonlyLayout);
        readonlyLayout.setVisibility(View.INVISIBLE);
    }
    
    private void loadTextElement()
    {
        loadCommonAttributes();
        
    } 
}
