package com.radicaldynamic.turboform.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.javarosa.form.api.FormEntryController;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
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
import android.widget.Toast;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.adapters.FormBuilderFieldListAdapter;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.listeners.FormLoaderListener;
import com.radicaldynamic.turboform.listeners.FormSavedListener;
import com.radicaldynamic.turboform.tasks.SaveToDiskTask;
import com.radicaldynamic.turboform.views.TouchListView;
import com.radicaldynamic.turboform.xform.Field;
import com.radicaldynamic.turboform.xform.FormReader;
import com.radicaldynamic.turboform.xform.FormWriter;

public class FormBuilderFieldList extends ListActivity implements FormLoaderListener, FormSavedListener
{
    private static final String t = "FormBuilderElementList: ";
    
    private static final int LOADING_DIALOG = 1;
    private static final int SAVING_DIALOG = 2;
    
    private LoadFormDefinitionTask mLoadFormDefinitionTask;
    private SaveFormDefinitionTask mSaveFormDefinitionTask;
    
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private FormBuilderFieldListAdapter adapter = null;  
    private Button jumpPreviousButton;
    private TextView mPathText;
   
    private String mFormId;
    private FormDocument mForm;
    private FormReader mFormReader;
    private boolean mNewForm;
    
    private ArrayList<Field> mFieldState = new ArrayList<Field>();    
    private ArrayList<String> mPath = new ArrayList<String>();          // Human readable location in mFieldState
    private ArrayList<String> mActualPath = new ArrayList<String>();    // Actual location in mFieldState
    
    /*
     * FIXME: element icons are not kept consistent when list items are reordered.  
     * I am not sure whether this affects only the items that are actually moved 
     * or the ones that are next to them.
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to)
        {
            Field item = adapter.getItem(from);

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
                mNewForm = i.getBooleanExtra(FormEntryActivity.NEWFORM, false);
        
                mLoadFormDefinitionTask = new LoadFormDefinitionTask();
                mLoadFormDefinitionTask.setFormLoaderListener(this);
                mLoadFormDefinitionTask.execute(mFormId);
                
                showDialog(LOADING_DIALOG);
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
                mNewForm = savedInstanceState.getBoolean(FormEntryActivity.NEWFORM, false);
            
            // Check to see if this is a screen flip or a new form load
            Object data = getLastNonConfigurationInstance();
            
            if (data instanceof LoadFormDefinitionTask) {
                mLoadFormDefinitionTask = (LoadFormDefinitionTask) data;
            } else if (data instanceof SaveFormDefinitionTask) {
                mSaveFormDefinitionTask = (SaveFormDefinitionTask) data;
            } else if (data == null) {
                // Load important bits of the form definition from memory
                mFieldState = Collect.getInstance().getFbFieldState();
                mForm = Collect.getInstance().getFbForm();

                Field destination = gotoActiveField(null, true);

                if (destination == null)
                    refreshView(mFieldState);
                else
                    refreshView(destination.children);
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
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id) {
        case LOADING_DIALOG:
            mProgressDialog = new ProgressDialog(this);
//
//            DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    dialog.dismiss();
//                    mFormLoaderTask.setFormLoaderListener(null);
//                    mFormLoaderTask.cancel(true);
//                    finish();
//                }
//            };
//
//            mProgressDialog.setIcon(R.drawable.ic_dialog_info);
//            mProgressDialog.setTitle(getString(R.string.loading_form));
//            mProgressDialog.setMessage(getString(R.string.please_wait));
//            mProgressDialog.setIndeterminate(true);
//            mProgressDialog.setCancelable(false);
//            mProgressDialog.setButton(getString(R.string.cancel_loading_form), loadingButtonListener);
            
            mProgressDialog.setMessage(getText(R.string.tf_loading_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            
            return mProgressDialog;
            
        case SAVING_DIALOG:
            mProgressDialog = new ProgressDialog(this);

//            DialogInterface.OnClickListener savingButtonListener = new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    dialog.dismiss();
//                    mSaveFormDefinitionTask.setFormSavedListener(null);
//                    mSaveFormDefinitionTask.cancel(true);
//                }
//            };

//            mProgressDialog.setIcon(R.drawable.ic_dialog_info);
//            mProgressDialog.setTitle(getString(R.string.saving_form));
//            mProgressDialog.setMessage(getString(R.string.please_wait));
//            mProgressDialog.setIndeterminate(true);
//            mProgressDialog.setCancelable(false);
//            mProgressDialog.setButton(getString(R.string.cancel), savingButtonListener);
//            mProgressDialog.setButton(getString(R.string.cancel_saving_form), savingButtonListener);            
            
            mProgressDialog.setMessage(getText(R.string.tf_saving_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            
            return mProgressDialog;
        }

        return null;
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
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            createQuitDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        Field field = (Field) getListAdapter().getItem(position);
        
        /* 
         * If the form field that has been clicked on is either a group or a repeat the default
         * behaviour is to navigate "down" into the form to display elements contained by others.
         * 
         * These form elements may be edited by using a context menu (or possibly using an
         * option menu that will become enabled if the user has navigated below the top).
         */
        if (field.getType().equals("group") || field.getType().equals("repeat")) {
            // So we can find our way back "up" the tree later
            field.setActive(true);
            
            // Deactivate the parent, if applicable
            if (field.getParent() != null)
                field.getParent().setActive(false);
            
            // Make sure parents of parents are also deactivated (as in the case of nested repeated groups)
            if (field.getParent() != null && field.getParent().getParent() != null)
                field.getParent().getParent().setActive(false);
            
            mPath.add(field.getLabel().toString());
            
            // Special logic to hide the complexity of repeated elements
            if (field.children.size() == 1 && field.children.get(0).getType().equals("repeat")) {
                mActualPath.add(field.getLabel().toString());
                mActualPath.add(field.children.get(0).getLabel().toString());
                refreshView(field.children.get(0).children);
            } else {
                mActualPath.add(field.getLabel().toString());
                refreshView(field.children);
            }
        } else {
            /*
             * There is no case here for groups/repeated groups since this is not how
             * 
             */
            String humanFieldType = null;
            
            if (field.getType().equals("input"))
                if (field.getBind() == null || field.getBind().getType().equals("string")) {
                    humanFieldType = "text";
                } else if (field.getBind().getType().equals("decimal") || field.getBind().getType().equals("int")) {
                    humanFieldType = "number";
                } else {
                    humanFieldType = field.getBind().getType();
                }
            else if (field.getType().equals("select") || field.getType().equals("select1"))
                humanFieldType = "select";
            else if (field.getType().equals("upload"))
                humanFieldType = "media";
            else if (field.getType().equals("trigger"))
                humanFieldType = "trigger";
            
            if (humanFieldType != null) 
                startElementEditor(humanFieldType, field);
            else 
                Log.w(Collect.LOGTAG, t + "Unable to determine field type and start element editor");            
        } // end if field type is group or repeat        
    } // end onListItemClick()
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        
        case R.id.barcode:  startElementEditor("barcode",   null);  break;
        case R.id.date:     startElementEditor("date",      null);  break;
        case R.id.geopoint: startElementEditor("geopoint",  null);  break;
        case R.id.group:    startElementEditor("group",     null);  break;
        case R.id.media:    startElementEditor("media",     null);  break;
        case R.id.number:   startElementEditor("number",    null);  break;
        case R.id.select:   startElementEditor("select",    null);  break;
        case R.id.text:     startElementEditor("text",      null);  break;
        
        case R.id.view_instance:
            Intent i = new Intent(this, FormBuilderInstanceList.class);       
            startActivity(i);
            break;            
            
        case R.id.save_form:
            mSaveFormDefinitionTask = new SaveFormDefinitionTask();
            mSaveFormDefinitionTask.setFormSavedListener(FormBuilderFieldList.this);
            mSaveFormDefinitionTask.execute(SaveToDiskTask.SAVED);
            
            showDialog(SAVING_DIALOG);
            break;
            
        case R.id.help:
            break;            
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /*
     * Parse and load the form so that it can be displayed and manipulated
     */
    private class LoadFormDefinitionTask extends AsyncTask<String, Void, Void> 
    {
        FormLoaderListener mStateListener;
        String mError = null;
        String mErrorMsg = "Unexpected error: ";
        
        @Override
        protected Void doInBackground(String... args) 
        {
            String formId = args[0];      
            
            mForm = Collect.mDb.getDb().get(FormDocument.class, formId);
            Collect.getInstance().setFbForm(mForm);
            Log.d(Collect.LOGTAG, t + "Retrieved form " + mForm.getName() + " from database");
            
            Log.d(Collect.LOGTAG, t + "Retreiving form XML from database...");
            AttachmentInputStream ais = Collect.mDb.getDb().getAttachment(formId, "xml");
            mFormReader = new FormReader(ais, mNewForm);
            
            resetStates();
            
            try {
                ais.close();
                
                mFormReader.parseForm();            
                mFieldState = mFormReader.getFieldState();
                Collect.getInstance().setFbBindState(mFormReader.getBindState());
                Collect.getInstance().setFbFieldState(mFieldState);
                Collect.getInstance().setFbInstanceState(mFormReader.getInstanceState());
                Collect.getInstance().setFbTranslationState(mFormReader.getTranslationState());
            } catch (IOException e) {
                mError = e.toString();
            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(Void nothing) {
            synchronized (this) {
                if (mStateListener != null) {
                    if (mError == null) {
                        mStateListener.loadingComplete(null);                        
                    } else {
                        mStateListener.loadingError(mErrorMsg + mError); 
                    }
                }                    
            }
        }
        
        public void setFormLoaderListener(FormLoaderListener sl) {
            synchronized (this) {
                mStateListener = sl;
            }
        }
        
        private void resetStates()
        {
            // States stored in this object
            mFieldState = new ArrayList<Field>();
            mPath = new ArrayList<String>();
            mActualPath = new ArrayList<String>();
            
            // States stored in the global application context
            Collect.getInstance().setFbBindState(null);
            Collect.getInstance().setFbFieldState(null);
            Collect.getInstance().setFbInstanceState(null);
            Collect.getInstance().setFbTranslationState(null);
        }
    }
    
    /*
     * Save the form as-is given the state of the XForm in memory 
     */
    private class SaveFormDefinitionTask extends AsyncTask<Integer, Void, Integer> 
    {
        private FormSavedListener mSavedListener;
        
        @Override
        protected Integer doInBackground(Integer... resultCode)
        {
            Log.d(Collect.LOGTAG, t + "Saving form to XML and attaching to database document...");
            
            Integer result = resultCode[0];
            
            try {
                // Write out XML to database
                mForm.addInlineAttachment(
                        new Attachment(
                                "xml", 
                                Base64.encodeToString(FormWriter.writeXml(mFormReader.getInstanceRoot()), Base64.DEFAULT), 
                                "text/xml"));
                
                mForm.setStatus(FormDocument.Status.inactive);
                Collect.mDb.getDb().update(mForm);
            } catch (Exception e) {
                result = SaveToDiskTask.SAVE_ERROR;
            }

            return result;
        }  

        @Override
        protected void onPostExecute(Integer result) {
            synchronized (this) {
                if (mSavedListener != null)
                    mSavedListener.savingComplete(result);
            }
        }
        
        public void setFormSavedListener(FormSavedListener fsl) {
            synchronized (this) {
                mSavedListener = fsl;
            }
        }
    }
    
    /*
     * Finds the current active field, sets it to inactive and either returns 
     * null to signal that the "top level" of the form has been reached or 
     * sets the parent field to active and returns it.
     * 
     * If returnActiveField is true then the active field itself will be 
     * returned vs. the parent field.
     */
    public Field gotoActiveField(Field c, Boolean returnActiveField)
    {
        Iterator<Field> it = null;
        
        if (c == null)
            it = mFieldState.iterator();
        else {
            if (c.isActive()) {
                /* 
                 * This is convoluted logic that lets us use this method both for "go up" navigation 
                 * and also to reset navigation to the correct place on orientation changes
                 */
                if (returnActiveField)
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
            Field result = gotoActiveField(it.next(), returnActiveField);
            
            if (result instanceof Field)
                if (result.isActive() == false)
                    return null;
                else
                    return result;
        }

        return null;        
    }
    
    public void goUpLevel()
    {
        Field destination;
        
        // Special logic to hide the complexity of repeated elements
        if (mActualPath.size() > mPath.size()) {
            /*
             * This will evaluate to true when we have navigated into a repeated group since
             * the actual representation is <group><label>...</label><repeat ... /></group>
             * and we want to represent it as one field vs. travelling two depths to get at
             * the list of repeated elements.
             */
            mPath.remove(mPath.size() - 1);                 // Remove the "group" label
            mActualPath.remove(mActualPath.size() - 1);     // Remove the repeated element 
            mActualPath.remove(mActualPath.size() - 1);     // Remove the "group" element
        } else {
            mPath.remove(mPath.size() - 1);
            mActualPath.remove(mActualPath.size() - 1);     // Remove the group element
        }
        
        destination = gotoActiveField(null, false);
        
        if (destination == null)
            refreshView(mFieldState);
        else {
            // Special support for nested repeated groups
            if (destination.children.size() == 1 && destination.children.get(0).getType().equals("repeat")) {
                mActualPath.add(destination.getLabel().toString());
                mActualPath.add(destination.children.get(0).getLabel().toString());
                refreshView(destination.children.get(0).children);
            } else {                            
                refreshView(destination.children);
            }
        }
    }
    
    /*
     * This is repurposed from the FormLoadListener used for FormEntryActivity and as such
     * the FormEntryController parameter has no use here and will be passed a null value.
     * 
     * (non-Javadoc)
     * @see com.radicaldynamic.turboform.listeners.FormLoaderListener#loadingComplete(org.javarosa.form.api.FormEntryController)
     */
    @Override
    public void loadingComplete(FormEntryController fec)
    {
        dismissDialog(LOADING_DIALOG);        
        refreshView(mFieldState);
    }

    @Override
    public void loadingError(String errorMsg)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void savingComplete(int saveStatus)
    {
        dismissDialog(SAVING_DIALOG);

        switch (saveStatus) {
        case SaveToDiskTask.SAVED:
            Toast.makeText(getApplicationContext(), getString(R.string.data_saved_ok), Toast.LENGTH_SHORT).show();
            break;
        case SaveToDiskTask.SAVED_AND_EXIT:
            Toast.makeText(getApplicationContext(), getString(R.string.data_saved_ok), Toast.LENGTH_SHORT).show();
            finish();
            break;
        case SaveToDiskTask.SAVE_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.data_saved_error), Toast.LENGTH_LONG).show();
            break;
        }
    }   

    private void createQuitDialog()
    {
        String[] items = {
                getString(R.string.do_not_save),
                getString(R.string.quit_entry), 
                getString(R.string.do_not_exit)
        };
    
        mAlertDialog = new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.quit_application))
            .setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        case 0:
                            // Discard any changes and exit
                            if (mForm.getStatus() == FormDocument.Status.temporary)
                                Collect.mDb.getDb().delete(mForm);
                            
                            finish();
                            break;
    
                        case 1:
                            // Save and exit
                            mSaveFormDefinitionTask = new SaveFormDefinitionTask();
                            mSaveFormDefinitionTask.setFormSavedListener(FormBuilderFieldList.this);
                            mSaveFormDefinitionTask.execute(SaveToDiskTask.SAVED_AND_EXIT);
                            
                            showDialog(SAVING_DIALOG);
                            break;
    
                        case 2:
                            // Do nothing
                            break;    
                        }
                    }
                }).create();
    
        mAlertDialog.show();
    }

    private void refreshView(ArrayList<Field> fieldsToDisplay)
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
        
        adapter = new FormBuilderFieldListAdapter(getApplicationContext(), fieldsToDisplay);
        setListAdapter(adapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }
    
    /*
     * Launch the element editor either to add a new field or to modify an existing one 
     */
    private void startElementEditor(String type, Field loadField)
    {
        Collect.getInstance().setFbField(loadField);
        
        Intent i = new Intent(this, FormBuilderFieldEditor.class);
        i.putExtra(FormBuilderFieldEditor.ELEMENT_TYPE, type);        
        startActivity(i);
    }
}
