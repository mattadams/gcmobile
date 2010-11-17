package com.radicaldynamic.turboform.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.ektorp.AttachmentInputStream;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.adapters.FormBuilderListAdapter;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.utilities.FormUtils;
import com.radicaldynamic.turboform.views.TouchListView;
import com.radicaldynamic.turboform.xform.Control;

public class FormBuilderList extends ListActivity
{
    private static final String t = "FormBuilderList: ";
    
    private static final int MENU_ADD  = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_HELP = Menu.FIRST + 2;
    
    private LoadFormDefinitionTask mLoadFormDefinitionTask;
    private FormBuilderListAdapter adapter = null;  
    private Button jumpPreviousButton;
    private ProgressDialog mDialog;
    private TextView mPathText;
   
    private String mFormId;
    private FormDocument mForm;
    private FormUtils mFormUtility;
    private ArrayList<Control> mControlState;
    private ArrayList<String> mPath = new ArrayList<String>();          // Human readable location in mControlState
    private ArrayList<String> mActualPath = new ArrayList<String>();    // Actual location in mControlState
    
    /*
     * FIXME: element icons are not kept consistent when list items are reordered.  
     * I am not sure whether this affects only the items that are actually moved 
     * or the ones that are next to them.
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to)
        {
            Control item = adapter.getItem(from);

            adapter.remove(item);
            adapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which)
        {
            adapter.remove(adapter.getItem(which));
        }
    };    

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        setContentView(R.layout.form_builder_main);
        
        Boolean newForm = true;
        
        // Needed to manipulate the visual representation of our place in the form
        mPathText = (TextView) findViewById(R.id.pathText);

        jumpPreviousButton = (Button) findViewById(R.id.jumpPreviousButton);
        jumpPreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goUpLevel();
            }
        });     
        
        if (savedInstanceState == null) {
            Intent i = getIntent();
        
            // Load new form definition from scratch
            if (i != null) {
                mFormId = i.getStringExtra(FormEntryActivity.KEY_FORMID);
        
                mLoadFormDefinitionTask = new LoadFormDefinitionTask();
                mLoadFormDefinitionTask.execute(mFormId);
            }
        } else {          
            // Restore state information provided by this activity
            if (savedInstanceState.containsKey(FormEntryActivity.KEY_FORMID))
                mFormId = savedInstanceState.getString(FormEntryActivity.KEY_FORMID);
            
            if (savedInstanceState.containsKey(FormEntryActivity.KEY_FORMPATH))
                mPath = savedInstanceState.getStringArrayList(FormEntryActivity.KEY_FORMPATH);
            
            if (savedInstanceState.containsKey(FormEntryActivity.KEY_FORMACTUALPATH))
                mActualPath = savedInstanceState.getStringArrayList(FormEntryActivity.KEY_FORMACTUALPATH);
            
            if (savedInstanceState.containsKey(FormEntryActivity.NEWFORM))
                newForm = savedInstanceState.getBoolean(FormEntryActivity.NEWFORM, true);
            
            // Check to see if this is a screen flip or a new form load
            Object data = getLastNonConfigurationInstance();
            
            if (data instanceof LoadFormDefinitionTask) {
                mLoadFormDefinitionTask = (LoadFormDefinitionTask) data;
            } else if (data == null) {
                if (newForm == false) {
                    // Load important bits of the form definition from memory
                    mControlState = Collect.getInstance().getFormBuilderControlState();
                    mForm = Collect.getInstance().getFormBuilderForm();
                    
                    Control destination = gotoActiveControl(null, true);
                    
                    if (destination == null)
                        refreshView(mControlState);
                    else
                        refreshView(destination.children);
                } else {
                    Collect.getInstance().setFormBuilderControlState(null);
                    Collect.getInstance().setFormBuilderForm(null);
                }
            }            
        } // end if savedInstanceState == null   
    } // end onCreate
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FormEntryActivity.NEWFORM, false);
        outState.putString(FormEntryActivity.KEY_FORMID, mFormId);
        outState.putStringArrayList(FormEntryActivity.KEY_FORMPATH, mPath);
        outState.putStringArrayList(FormEntryActivity.KEY_FORMACTUALPATH, mActualPath);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.form_builder_context, menu);
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
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        Control control = (Control) getListAdapter().getItem(position);
        
        /* 
         * If the form field that has been clicked on is either a group or a repeat the default
         * behaviour is to navigate "down" into the form to display elements contained by others.
         * 
         * These form elements may be edited by using a context menu (or possibly using an
         * option menu that will become enabled if the user has navigated below the top).
         */
        if (control.getType().equals("group") || control.getType().equals("repeat")) {
            // So we can find our way back "up" the tree later
            control.setActive(true);
            
            // Deactivate the parent, if applicable
            if (control.getParent() != null)
                control.getParent().setActive(false);
            
            // Make sure parents of parents are also deactivated (as in the case of nested repeated groups)
            if (control.getParent() != null && control.getParent().getParent() != null)
                control.getParent().getParent().setActive(false);
            
            mPath.add(control.getLabel());
            
            // Special logic to hide the complexity of repeated elements
            if (control.children.size() == 1 && control.children.get(0).getType().equals("repeat")) {
                mActualPath.add(control.getLabel());
                mActualPath.add(control.children.get(0).getLabel());
                refreshView(control.children.get(0).children);
            } else {
                mActualPath.add(control.getLabel());
                refreshView(control.children);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            break;
        case MENU_SAVE:
            break;
        case MENU_HELP:
            break;
        }
    
        return super.onOptionsItemSelected(item);
    }
    
    /*
     * Refresh the main form browser view as requested by the user
     */
    private class LoadFormDefinitionTask extends AsyncTask<String, Void, Void> 
    {
        @Override
        protected Void doInBackground(String... args) 
        {
            String formId = args[0];            
            
            mForm = Collect.mDb.getDb().get(FormDocument.class, formId);
            Collect.getInstance().setFormBuilderForm(mForm);
            Log.d(Collect.LOGTAG, t + "Retrieved form " + mForm.getName() + " from database");
            
            Log.d(Collect.LOGTAG, t + "Retreiving form XML from database...");
            AttachmentInputStream ais = Collect.mDb.getDb().getAttachment(formId, "xml");
            mFormUtility = new FormUtils(ais);
            
            try {
                ais.close();
                
                mFormUtility.parseForm();            
                mControlState = mFormUtility.getControlState();
                Collect.getInstance().setFormBuilderControlState(mControlState);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            mDialog = new ProgressDialog(FormBuilderList.this);
            mDialog.setMessage(getText(R.string.tf_loading_please_wait));
            mDialog.setIndeterminate(true);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            refreshView(mControlState);            
            mDialog.cancel();
        }
    }
    
    public void goUpLevel()
    {
        Control destination;
        
        // Special logic to hide the complexity of repeated elements
        if (mActualPath.size() > mPath.size()) {
            /*
             * This will evaluate to true when we have navigated into a repeated group since
             * the actual representation is <group><label>...</label><repeat ... /></group>
             * and we want to represent it as one control vs. travelling two depths to get at
             * the list of repeated elements.
             */
            mPath.remove(mPath.size() - 1);                 // Remove the "group" label
            mActualPath.remove(mActualPath.size() - 1);     // Remove the repeated element 
            mActualPath.remove(mActualPath.size() - 1);     // Remove the "group" element
        } else {
            mPath.remove(mPath.size() - 1);
            mActualPath.remove(mActualPath.size() - 1);     // Remove the group element
        }
        
        destination = gotoActiveControl(null, false);
        
        if (destination == null)
            refreshView(mControlState);
        else {
            // Special support for nested repeated groups
            if (destination.children.size() == 1 && destination.children.get(0).getType().equals("repeat")) {
                mActualPath.add(destination.getLabel());
                mActualPath.add(destination.children.get(0).getLabel());
                refreshView(destination.children.get(0).children);
            } else {                            
                refreshView(destination.children);
            }
        }
    }
    
    /*
     * Finds the current active control, sets it to inactive and either returns 
     * null to signal that the "top level" of the form has been reached or 
     * sets the parent control to active and returns it.
     * 
     * If returnActiveControl is true then the active control itself will be 
     * returned vs. the parent control.
     */
    public Control gotoActiveControl(Control c, Boolean returnActiveControl)
    {
        Iterator<Control> it = null;
        
        if (c == null)
            it = mControlState.iterator();
        else {
            if (c.isActive()) {
                /* 
                 * This is convoluted logic that lets us use this method both for "go up" navigation 
                 * and also to reset navigation to the correct place on orientation changes
                 */
                if (returnActiveControl)
                    return c;
                else 
                    c.setActive(false);
                
                if (c.getParent() == null) {
                    return c;
                } else {
                    // Special support for nested repeated groups
                    if (c.getParent().getType().equals("repeat")) {
                        // Set the parent of our parent (e.g., a group) active and return it
                        c.getParent().getParent().setActive(true);
                        return c.getParent().getParent();
                    } else {
                        c.getParent().setActive(true);
                        return c.getParent();
                    }
                }
            }            
            
            it = c.children.iterator();
        }        
        
        while (it.hasNext()) {                  
            Control result = gotoActiveControl(it.next(), returnActiveControl);
            
            if (result instanceof Control)
                if (result.isActive() == false)
                    return null;
                else
                    return result;
        }

        return null;        
    }
    
    private void refreshView(ArrayList<Control> controlsToDisplay)
    {
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_editing) + " " + mForm.getName());
        
        String pathText = "";
        
        if (mPath.isEmpty()) {
            pathText = "Viewing Top of Form";
            jumpPreviousButton.setEnabled(false);
        } else {
            Iterator<String> it = mPath.iterator();
            
            while (it.hasNext()) {
                String d = it.next();

                if (pathText.length() > 0)
                    pathText = pathText + " > " + d;
                else
                    pathText = "Top > " + d;
            }
            
            jumpPreviousButton.setEnabled(true);
        }
        
        mPathText.setText(pathText);
        
        adapter = new FormBuilderListAdapter(getApplicationContext(), controlsToDisplay);
        setListAdapter(adapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }
}
