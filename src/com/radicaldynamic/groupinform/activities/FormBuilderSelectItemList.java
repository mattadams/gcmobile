package com.radicaldynamic.groupinform.activities;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.FormBuilderSelectItemListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.views.TouchListView;
import com.radicaldynamic.groupinform.xform.Field;

public class FormBuilderSelectItemList extends ListActivity
{
    private static final String t = "FormBuilderSelectItemList: ";
    
    private static final int MENU_ADD = Menu.FIRST;
    
    public static final String KEY_SINGLE = "singleselect";    
    public static final String KEY_DEFAULT = "instancedefault";

    private AlertDialog mAlertDialog;
    
    private FormBuilderSelectItemListAdapter mAdapter = null;
    
    // The control field as saved to the application global state
    private Field mField;                                   
    
    // A singular item for adding or editing
    private Field mItem;                                    
    
    // The list of select items (used by only this activity)
    private ArrayList<Field> mItemList = new ArrayList<Field>();                     
    
    // Whether this is a single select field (see KEY_SINGLE) 
    private boolean mSingleSelect;
    
    /*
     * FIXME: element icons are not kept consistent when list items are reordered.  
     * I am not sure whether this affects only the items that are actually moved 
     * or the ones that are next to them.
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to)
        {
            Field item = mAdapter.getItem(from);

            mAdapter.remove(item);
            mAdapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which)
        {
            final Field item = mAdapter.getItem(which);
            
            mAlertDialog = new AlertDialog.Builder(FormBuilderSelectItemList.this)
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_confirm_removal)
                .setMessage(getString(R.string.tf_confirm_removal_msg, item.getLabel().toString()))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {                                              
                        mAdapter.remove(item);                        
                                                
                        Toast.makeText(
                                getApplicationContext(), 
                                getString(R.string.tf_removed_item, item.getLabel().toString()), 
                                Toast.LENGTH_SHORT).show();
                        
                        // Trigger a refresh of the view (and display any pertenent messages)
                        if (mAdapter.isEmpty())
                            refreshView();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
    
            mAlertDialog.show();
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fb_main);

        // Remove things from this view that are not needed
        findViewById(R.id.pathText).setVisibility(View.GONE);
        findViewById(R.id.jumpPreviousButton).setVisibility(View.GONE);        
        
        mField = Collect.getInstance().getFbField();
        
        if (mField.getLabel().toString().length() == 0)
            setTitle(getString(R.string.app_name) + " > " + "List Items for Select Field");
        else
            setTitle(getString(R.string.app_name) + " > " + "List Items for " + mField.getLabel().toString());
        
        if (savedInstanceState == null) {
            Intent intent = getIntent();
        
            if (intent != null) {                
                // Determine if this is a single or multiple select type (see key comments)
                mSingleSelect = intent.getBooleanExtra(KEY_SINGLE, false);
                
                // Select field types keep their items in the children array list         
                if (Collect.getInstance().getFbItemList() == null)
                    mItemList = (ArrayList<Field>) mField.getChildren().clone();
                else
                    mItemList = Collect.getInstance().getFbItemList();
                
                // Set list item preselect states
                Iterator<Field> it = mItemList.iterator();
                
                while (it.hasNext()) {
                    Field i = it.next();
                    
                    // Default to "off"
                    i.setItemDefault(false);
                    
                    Log.v(Collect.LOGTAG, t + "evaluating preselect status of list item " + i.getLabel().toString() + " with value \"" + i.getItemValue() + "\"");
                    
                    /*
                     * Items must be explicitly enabled/disabled because
                     * the defaults may have changed since last loading
                     */
                    
                    for (String def : intent.getStringExtra(KEY_DEFAULT).split("\\s+")) {
                        Log.v(Collect.LOGTAG, t + "processing instance default \"" + def + "\"");
                        
                        if (i.getItemValue().equals(def))
                            i.setItemDefault(true);                            
                    }
                }
            }
        } else {           
            mItemList = Collect.getInstance().getFbItemList();            
            mSingleSelect = savedInstanceState.getBoolean(KEY_SINGLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_list_item)).setIcon(R.drawable.ic_menu_add);
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            setResult(RESULT_OK);
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        mItem = (Field) getListAdapter().getItem(position);
        createItemDialog();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            mItem = new Field();
            mItem.setType("item");            
            createItemDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause()
    {
        // Save off the state of the item list before loosing this activity
        Collect.getInstance().setFbItemList(mItemList);
        
        super.onPause();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshView();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SINGLE, mSingleSelect);
        
        Collect.getInstance().setFbItemList(mItemList);
    }
    
    private static class NoSpaces implements InputFilter  
    {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend)
        {
            return source.subSequence(start, end).toString().replaceAll("\\s+", "");
        }        
    }

    private void createItemDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        
        // We don't want a translucent alert dialog
        alert.setInverseBackgroundForced(true);
        
        // The user must use positive or negative response to close this dialog
        alert.setCancelable(false);
        
        // Attach the layout to this dialog    
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_dialog, null);
        
        final EditText label = (EditText) view.findViewById(R.id.label);
        final EditText value = (EditText) view.findViewById(R.id.value);
        final CheckBox preselected = (CheckBox) view.findViewById(R.id.preselected);
        
        // Labels default to upper case characters at the beginning
        label.setKeyListener(new QwertyKeyListener(TextKeyListener.Capitalize.SENTENCES, false));
        
        // Prevent spaces from being entered into the value field        
        value.setFilters(new InputFilter[] { new NoSpaces() });
        
        /*
         * If the user moves to input a value and one is not already provided then
         * attempt to prepopulate this field by using the label as a source and
         * removing all spaces & other special characters.
         */
        value.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus == false)
                    return;
                
                if (value.getText().toString().length() == 0 && label.getText().toString().length() > 0)
                    value.setText(label.getText().toString().replaceAll("[^a-zA-Z0-9]", "").toLowerCase());
            }
        });        
        
        if (mItem.isEmpty()) {
            alert.setTitle(getText(R.string.tf_create_list_item));            
        } else {
            alert.setTitle(getText(R.string.tf_edit_list_item));
            
            label.setText(mItem.getLabel().toString());
            value.setText(mItem.getItemValue());
            
            if (mItem.isItemDefault())
                preselected.setChecked(true);
        }

        alert.setView(view);

        alert.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                boolean error = false;
                
                // Get rid of the whitespace
                label.setText(label.getText().toString().trim());
                value.setText(value.getText().toString().trim());
                
                // Both label and value are required
                if (label.getText().toString().length() == 0 || value.getText().toString().length() == 0) {
                    Toast.makeText(
                            getApplicationContext(), 
                            getString(R.string.tf_item_missing_label_or_value), 
                            Toast.LENGTH_LONG).show();
                    
                    // Preserve user input
                    mItem.setLabel(label.getText().toString());
                    mItem.setItemValue(value.getText().toString());
                    mItem.setItemDefault(preselected.isChecked());
                    
                    error = true;
                }
                
                // Ensure uniqueness of value
                Iterator<Field> it = mItemList.iterator();
                
                // User input not preserved here (doing so makes the routine much more complex)
                while (it.hasNext()) {
                    Field i = it.next();
                    
                    if (i.getItemValue().equals(value.getText().toString()) &&
                            !i.getLabel().toString().equals(mItem.getLabel().toString())) {                       
                            
                            Toast.makeText(
                                    getApplicationContext(), 
                                    getString(R.string.tf_item_value_not_unique), 
                                    Toast.LENGTH_LONG).show();
                        
                            error = true;
                    }
                }

                // TODO: process preselected defaults
                
                if (error)
                    createItemDialog();
                else {
                    mItem.setLabel(label.getText().toString());
                    mItem.setItemValue(value.getText().toString());
                    
                    // Special handling for the preselected checkbox (this is sensitive to the select type)
                    if (preselected.isChecked()) {
                        // Ensure that no other item is selected
                        if (mSingleSelect) {
                            Iterator<Field> itemIterator = mItemList.iterator();
                            
                            while (itemIterator.hasNext())
                                itemIterator.next().setItemDefault(false);
                        }                       
                        
                        mItem.setItemDefault(true);
                    } else {
                        mItem.setItemDefault(false);
                    }
                    
                    // Add new items to the list
                    if (mItem.isEmpty()) {
                        mItem.setEmpty(false);
                        mItemList.add(mItem);
                    }
                    
                    refreshView();
                }
            }
        });

        alert.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mItem = null;
            }
        });

        alert.show();
    }
    
    /* 
     * Refresh the view (displaying the currently active field 
     * or the top level of the form if no field is currently active)
     */
    private void refreshView()
    {
        mAdapter = new FormBuilderSelectItemListAdapter(getApplicationContext(), mItemList);        
        
        // Provide a hint to users if the list is empty
        if (mAdapter.isEmpty()) {
            TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
            nothingToDisplay.setVisibility(View.VISIBLE);
            
            // FIXME: figure out why this does not work (shouldn't it?)
//            openOptionsMenu();
        } else {
            TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
            nothingToDisplay.setVisibility(View.INVISIBLE);
        }
        
        setListAdapter(mAdapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }
}
