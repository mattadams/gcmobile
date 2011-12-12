package com.radicaldynamic.gcmobile.android.build;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.utilities.FileUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.adapters.FormBuilderFieldListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;
import com.radicaldynamic.groupinform.listeners.FormLoaderListener;
import com.radicaldynamic.groupinform.listeners.FormSavedListener;
import com.radicaldynamic.groupinform.tasks.SaveToDiskTask;
import com.radicaldynamic.groupinform.utilities.Base64Coder;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;
import com.radicaldynamic.groupinform.views.TouchListView;
import com.radicaldynamic.groupinform.xform.Bind;
import com.radicaldynamic.groupinform.xform.Field;
import com.radicaldynamic.groupinform.xform.FormBuilderState;
import com.radicaldynamic.groupinform.xform.FormReader;
import com.radicaldynamic.groupinform.xform.FormWriter;
import com.radicaldynamic.groupinform.xform.Instance;
import com.radicaldynamic.groupinform.xform.FormWriter.FormSanityException;

public class FieldList extends ListActivity implements FormLoaderListener, FormSavedListener
{
    private static final String t = "FormBuilderElementList: ";
    
    private static final int LOADING_DIALOG = 1;
    private static final int SAVING_DIALOG = 2;
    private static final int SANITY_DIALOG = 3;
    
    private static final int REQUEST_EDITFIELD = 1;
    private static final int REQUEST_TRANSLATIONS = 2;
    
    private static final String KEY_ACTUALPATH = "actualpath";
    private static final String KEY_INSTANCE_ROOT = "instanceroot";
    private static final String KEY_INSTANCE_ROOT_ID = "instancerootid";
    private static final String KEY_PATH = "path";
    
    private LoadFormDefinitionTask mLoadFormDefinitionTask;
    private SaveFormDefinitionTask mSaveFormDefinitionTask;
    
    private AlertDialog mAlertDialog;
    private String mAlertDialogMsg;
    private ProgressDialog mProgressDialog;
    
    private FormBuilderFieldListAdapter mAdapter = null;  
    private Button jumpPreviousButton;
    private TextView mPathText;

    private String mFormId;
    private String mInstanceRoot;                                       // XForm instance root tag name
    private String mInstanceRootId;                                     // Value of XForm instance root tag id attribute
    private FormDefinition mForm;
    private FormReader mFormReader;    
    
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
            
            mAlertDialog = new AlertDialog.Builder(FieldList.this)
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_confirm_removal)
                .setMessage(getString(R.string.tf_confirm_removal_msg, item.getLabel().toString()))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeItem(item);
                        
                        // Trigger a refresh of the view (and display any pertinent messages)
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

        private void displayRemovalFailed(String msg)
        {
            mAlertDialog = new AlertDialog.Builder(FieldList.this)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_unable_to_remove)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create();
            
            mAlertDialog.show();
        }
        
        private void displayRemovedMsg(String msg)        
        {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
        
        private void removeItem(Field item)
        {            
            /*
             * Group removal must be dealt with separately from regular items and repeated groups differ from regular groups.  
             * This code takes the easy way out and refuses to remove groups that are not empty.  This saves us from having to
             * worry about recursive removal of binds and instances.
             */
            if (item.getType().equals("group")) {
                if (Field.isRepeatedGroup(item)) {
                    if (item.getRepeat().getChildren().isEmpty()) {
                        removeByXPath(item.getRepeat().getXPath());
                    } else {
                        displayRemovalFailed(getString(R.string.tf_removal_failed, item.getLabel().toString()));
                        return;
                    }
                } else {
                    if (!item.getChildren().isEmpty()) {
                        displayRemovalFailed(getString(R.string.tf_removal_failed, item.getLabel().toString()));
                        return;
                    }
                }
            } else {
                removeByXPath(item.getXPath());
            }
            
            // This removes the (control) field from mFieldState
            mAdapter.remove(item);
            
            displayRemovedMsg(getString(R.string.tf_removed_with_param, item.getLabel().toString()));       
        }
        
        private void removeByXPath(String xpath)
        {
	    if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "removing instances and binds matching XPath " + xpath);

            // Also remove the related instance
            removeInstanceByXPath(xpath, null);
            
            // Also remove the related bind
            Iterator<Bind> it = Collect.getInstance().getFormBuilderState().getBinds().iterator();
            
            while (it.hasNext()) {
                Bind bind = it.next();
                
                if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "evaluating bind for XPath " + bind.getXPath() + " for removal");
                
                // Remove any binds with an identical XPath to the field in question or those that are logical children
                if (bind.getXPath().equals(xpath) || bind.getXPath().matches("^" + xpath + "/*$")) {
	            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "removing bind for XPath " + bind.getXPath());
                    it.remove();
                }
            }
        }        
        
        /* 
         * Iterate through the instances recursively and remove the instance
         * (and all children) that match the XPath passed to this method.
         * 
         * This is intended to be called when a (control) field is removed 
         * from a list.
         */
        private void removeInstanceByXPath(String xpath, Instance incomingInstance)
        {
            Iterator<Instance> it;
            
            if (incomingInstance == null)
                it = Collect.getInstance().getFormBuilderState().getInstance().iterator();
            else
                it = incomingInstance.getChildren().iterator();
            
            while (it.hasNext()) {
                Instance i = it.next();
                
                if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "evaluating instance with XPath " + i.getXPath() + " for removal");
                
                if (i.getXPath().equals(xpath)) {
                    if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "removing instance with XPath " + i.getXPath());
                    it.remove();
                    return;
                }
                    
                removeInstanceByXPath(xpath, i);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        setContentView(R.layout.fb_main);
        
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
                mFormId = i.getStringExtra(FormEntryActivity.KEY_FORMPATH);
        
                mLoadFormDefinitionTask = new LoadFormDefinitionTask();
                mLoadFormDefinitionTask.setFormLoaderListener(this);
                mLoadFormDefinitionTask.execute(mFormId);
                
                showDialog(LOADING_DIALOG);
            }
        } else {          
            // Restore state information provided by this activity
            if (savedInstanceState.containsKey(FormEntryActivity.KEY_FORMPATH))
                mFormId = savedInstanceState.getString(FormEntryActivity.KEY_FORMPATH);
            
            if (savedInstanceState.containsKey(KEY_PATH))
                mPath = savedInstanceState.getStringArrayList(KEY_PATH);
            
            if (savedInstanceState.containsKey(KEY_ACTUALPATH))
                mActualPath = savedInstanceState.getStringArrayList(KEY_ACTUALPATH);
            
            if (savedInstanceState.containsKey(KEY_INSTANCE_ROOT))
                mInstanceRoot = savedInstanceState.getString(KEY_INSTANCE_ROOT);
            
            if (savedInstanceState.containsKey(KEY_INSTANCE_ROOT_ID))
                mInstanceRootId = savedInstanceState.getString(KEY_INSTANCE_ROOT_ID);
            
            // Check to see if this is a screen flip or a new form load
            Object data = getLastNonConfigurationInstance();
            
            if (data instanceof LoadFormDefinitionTask) {
                mLoadFormDefinitionTask = (LoadFormDefinitionTask) data;
            } else if (data instanceof SaveFormDefinitionTask) {
                mSaveFormDefinitionTask = (SaveFormDefinitionTask) data;
            } else if (data == null) {
                // Load important bits of the form definition from memory
                mFieldState = Collect.getInstance().getFormBuilderState().getFields();
                mForm = Collect.getInstance().getFormBuilderState().getFormDefinition();
                
                refreshView();
            }          
        } // end if savedInstanceState == null
    }
    
    // Listen for results
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_CANCELED)
            return;
        
        switch (requestCode) {
        case REQUEST_EDITFIELD:
            if (Collect.getInstance().getFormBuilderState().getField().isSaved())
                Collect.getInstance().getFormBuilderState().getField().setSaved(false);

            // New fields require further init after being saved
            if (Collect.getInstance().getFormBuilderState().getField().isNewField())
                addNewField(Collect.getInstance().getFormBuilderState().getField());
            
            // Display with the new field included
            mFieldState = Collect.getInstance().getFormBuilderState().getFields();
            refreshView();
            
            break;
            
        case REQUEST_TRANSLATIONS:
            // User may have updated default translations and these need to be reflected on-screen
            refreshView();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.form_builder_context, menu);
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
            mProgressDialog.setMessage(getText(R.string.tf_loading_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);            
            return mProgressDialog;
            
        case SAVING_DIALOG:
            mProgressDialog = new ProgressDialog(this);   
            mProgressDialog.setMessage(getText(R.string.tf_saving_please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);            
            return mProgressDialog;
            
        case SANITY_DIALOG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            
            builder
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.tf_form_builder_sanity_dialog)
                .setMessage(mAlertDialogMsg);
            
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    removeDialog(SANITY_DIALOG);
                }
            });
            
            return builder.create();
        }

        return null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.formbuilderfieldlist_omenu, menu);
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
            /*
             * So we can find our way back "up" the tree later
             * 
             * Note that the "repeated" portions of repeated groups never become active; 
             * we just hide this from the user using specific control decisions.  This might
             * become a candidate for refactoring later, though.  It is a bit hairy.
             */
            field.setActive(true);
            
            // Deactivate the parent, if applicable
            if (field.getParent() != null)
                field.getParent().setActive(false);
            
            // Make sure parents of parents are also deactivated (as in the case of nested repeated groups)
            if (field.getParent() != null && field.getParent().getParent() != null)
                field.getParent().getParent().setActive(false);
            
            if (field.getLabel().toString().length() == 0)
                mPath.add("Label missing");
            else
                mPath.add(field.getLabel().toString());
            
            // Special logic to hide the complexity of repeated groups
            if (Field.isRepeatedGroup(field)) {
                mActualPath.add(field.getLabel().toString());
                mActualPath.add(field.getRepeat().getLabel().toString());
                refreshView(field.getRepeat().getChildren());
            } else {
                mActualPath.add(field.getLabel().toString());
                refreshView(field.getChildren());
            }
        } else {
            /*
             * There is no case here for groups/repeated groups since this is not how
             * we access them via the form builder field editor
             */
            String humanFieldType = null;
            
            if (field.getType().equals("input") || field.getType().equals("trigger")) {
                if (field.getBind() == null || field.getBind().getType().equals("string")) {
                    humanFieldType = "text";
                } else if (field.getBind().getType().equals("decimal") || field.getBind().getType().equals("int")) {
                    humanFieldType = "number";
                } else {
                    humanFieldType = field.getBind().getType();
                }
            } else if (field.getType().equals("select") || field.getType().equals("select1")) {
                humanFieldType = "select";
            } else if (field.getType().equals("upload")) {
                humanFieldType = "media";
            } else if (field.getType().equals("trigger")) {
                humanFieldType = "trigger";
            }
            
            if (humanFieldType != null) {
                startFieldEditor(humanFieldType, field);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.tf_unable_to_edit_unknown_field_type), Toast.LENGTH_LONG).show();
            }
        }    
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        
        case R.id.barcode:  startFieldEditor("barcode",   null);  break;
        case R.id.date:     startFieldEditor("date",      null);  break;
        case R.id.draw:     startFieldEditor("draw",      null);  break;    // Note: draw is a virtual type - it will be turned into media once created
        case R.id.geopoint: startFieldEditor("geopoint",  null);  break;
        case R.id.group:    startFieldEditor("group",     null);  break;
        case R.id.media:    startFieldEditor("media",     null);  break;
        case R.id.number:   startFieldEditor("number",    null);  break;
        case R.id.select:   startFieldEditor("select",    null);  break;
        case R.id.text:     startFieldEditor("text",      null);  break;
        
        case R.id.i18n_setup:
            Intent i = new Intent(this, I18nList.class);
            startActivityForResult(i, REQUEST_TRANSLATIONS);
            break;
        
        case R.id.view_instance:
            startActivity(new Intent(this, InstanceList.class));
            break;        
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(FormEntryActivity.KEY_FORMPATH, mFormId);
        outState.putString(KEY_INSTANCE_ROOT, mInstanceRoot);
        outState.putString(KEY_INSTANCE_ROOT_ID, mInstanceRootId);
        outState.putStringArrayList(KEY_PATH, mPath);
        outState.putStringArrayList(KEY_ACTUALPATH, mActualPath);
    }

    /*
     * Parse and load the form so that it can be displayed and manipulated
     */
    private class LoadFormDefinitionTask extends AsyncTask<String, Void, Void> 
    {
        FormLoaderListener mStateListener;
        
        String mError = null;
        
        @Override
        protected Void doInBackground(String... args) 
        {
            String formId = args[0];
            
            resetStates();
            
            try {
                mForm = Collect.getInstance().getDbService().getDb().get(FormDefinition.class, formId);
                Collect.getInstance().getFormBuilderState().setFormDefinition(mForm);
                if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "Retrieved form " + mForm.getName() + " from database");
                
                if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "Retreiving form XML from database...");
                AttachmentInputStream ais = Collect.getInstance().getDbService().getDb().getAttachment(formId, "xml");
                mFormReader = new FormReader(ais, false);
                ais.close();
                
                mFieldState = mFormReader.getFields();
                mInstanceRoot = mFormReader.getInstanceRoot();
                mInstanceRootId = mFormReader.getInstanceRootId();
                
                Collect.getInstance().getFormBuilderState().setBinds(mFormReader.getBinds());
                Collect.getInstance().getFormBuilderState().setFields(mFieldState);
                Collect.getInstance().getFormBuilderState().setInstance(mFormReader.getInstance());
                Collect.getInstance().getFormBuilderState().setTranslations(mFormReader.getTranslations());
            } catch (Exception e) {
                e.printStackTrace();
                mError = e.toString();
            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(Void nothing) 
        {
            synchronized (this) {
                if (mStateListener != null) {
                    if (mError == null) {
                        mStateListener.loadingComplete(null, null, null);
                    } else {
                        mStateListener.loadingError(mError); 
                    }
                }                    
            }
        }
        
        public void setFormLoaderListener(FormLoaderListener sl) 
        {
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
            Collect.getInstance().setFormBuilderState(null);
            Collect.getInstance().setFormBuilderState(new FormBuilderState());
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
            if (Collect.Log.DEBUG) Log.d(Collect.LOGTAG, t + "converting form to XML and attaching to database document...");
            
            Integer result = resultCode[0];
            
            try {
                // Save to file first so we can get md5 hash
                String name = Collect.getInstance().getFormBuilderState().getFormDefinition().getName();
                byte[] xml = FormWriter.writeXml(name, mInstanceRoot, mInstanceRootId);
                
                File f = new File(FileUtilsExtended.EXTERNAL_CACHE + File.separator + mForm.getId() + ".xml");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(xml);
                fos.close();
                                
                // Write out XML to database
                mForm.addInlineAttachment(new Attachment("xml", new String(Base64Coder.encode(xml)).toString(), FormWriter.CONTENT_TYPE));
                mForm.setStatus(FormDefinition.Status.active);
                mForm.setXmlHash(FileUtils.getMd5Hash(f));
                
                Collect.getInstance().getDbService().getDb().update(mForm);
                f.delete();
            } catch (FormSanityException e) {
                if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "sanity exception while writing XForm " + e.toString());
                e.printStackTrace();
                mAlertDialogMsg = e.getMessage();
                result = SaveToDiskTask.VALIDATE_ERROR;
            } catch (Exception e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "failed writing XForm to XML: " + e.toString());
                e.printStackTrace();
                
                result = SaveToDiskTask.SAVE_ERROR;
            }

            return result;
        }  

        @Override
        protected void onPostExecute(Integer result) {
            synchronized (this) {
                if (mSavedListener != null)
                    mSavedListener.savingComplete(result, null);
            }
        }
        
        public void setFormSavedListener(FormSavedListener fsl) {
            synchronized (this) {
                mSavedListener = fsl;
            }
        }
    }
    
    /*
     * This is repurposed from the FormLoadListener used for FormEntryActivity and as such
     * the FormEntryController parameter has no use here and will be passed a null value.
     * 
     * (non-Javadoc)
     * @see com.radicaldynamic.turboform.listeners.FormLoaderListener#loadingComplete
     */
    @Override
    public void loadingComplete(FormController fec, FormDefinition fdd, FormInstance fid)
    {
        dismissDialog(LOADING_DIALOG);        
        refreshView(mFieldState);
    }

    @Override
    public void loadingError(String errorMsg)
    {
        dismissDialog(LOADING_DIALOG);

        mAlertDialog = new AlertDialog.Builder(FieldList.this)
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.tf_form_builder_load_error)
            .setMessage(errorMsg)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();                    
                }
            })
            .create();

        mAlertDialog.show();
    }

    @Override
    public void savingComplete(int saveStatus, FormInstance fi)
    {
        dismissDialog(SAVING_DIALOG);

        switch (saveStatus) {
        case SaveToDiskTask.VALIDATE_ERROR:
            showDialog(SANITY_DIALOG);
            break;
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
    
    /*
     * Perform final initialization of the new field and assign it and it's related 
     * objects a place in the respective field, bind, instance (and itext) state arrays.
     */
    private void addNewField(Field f)
    {
        // No longer a virgin
        f.setNewField(false);
        
        // Handle groups, repeated groups and other control field types separately
        if (f.getType().equals("group")) {
            if (Field.isRepeatedGroup(f)) {
                addNewRepeatedGroupField(f);
            } else {
                addNewGroupField(f);
            }
        } else {
            addNewRegularField(f);
        }
    }
    
    private void addNewGroupField(Field f)
    {
        // Assign this group field as a child of the currently active field
        Field parent = returnActiveField(null);
        
        if (Field.isRepeatedGroup(parent)) {
            f.setParent(parent.getRepeat());
            parent.getRepeat().getChildren().add(f);
        } else {
            f.setParent(parent);
            
            // If parent is null then we are at the top of the form
            if (parent == null) {
                Collect.getInstance().getFormBuilderState().getFields().add(f);
            } else {
                parent.getChildren().add(f);
            }
        }
    }
    
    private void addNewRepeatedGroupField(Field f)
    {
        /* 
         * Assign this repeated group field as a child of the currently active field (its parent)
         * (taking into account the complexity of repeated groups)
         */
        Field p = returnActiveField(null);
        String xpath = determineXPathInheritance(p, f);
        
        // Set xpath of repeat
        f.getRepeat().setXPath(xpath);
        
        // Set XPath of bind
        f.getRepeat().getBind().setXPath(xpath);
        
        // Set xpath of instance
        f.getRepeat().getInstance().setXPath(xpath);
        
        // Associate field with instance
        f.getRepeat().getInstance().setField(f);
        
        // Add the field either at the top level or as the child of a group
        if (Field.isRepeatedGroup(p)) {
            f.setParent(p.getRepeat());
            p.getRepeat().getChildren().add(f);
        } else {
            f.setParent(p);
            
            // If parent is null then we are at the top of the form
            if (p == null) {
                Collect.getInstance().getFormBuilderState().getFields().add(f);
            } else {
                p.getChildren().add(f);
            }
        }

        // Also add the bind
        Collect.getInstance().getFormBuilderState().getBinds().add(f.getRepeat().getBind());

        attachFieldToInstanceParent(p, f.getRepeat());
    }
    
    private void addNewRegularField(Field f)
    {
        // Assign this field as a child of the currently active field (its parent)
        Field p = returnActiveField(null);
        String xpath = determineXPathInheritance(p, f);
        
        // Use XPath for associated instance and bind as well as this field
        f.getInstance().setXPath(xpath);
        f.getBind().setXPath(xpath);
        f.setXPath(xpath);
        
        // Associate field with instance
        f.getInstance().setField(f);
        
        // Add the field either at the top level or as the child of a group
        if (Field.isRepeatedGroup(p)) {
            f.setParent(p.getRepeat());
            p.getRepeat().getChildren().add(f);
        } else {
            f.setParent(p);

            if (p == null) {
                Collect.getInstance().getFormBuilderState().getFields().add(f);
            } else {
                p.getChildren().add(f);
            }
        }
        
        // Binds are a flat list, so it does not matter where they are added
        Collect.getInstance().getFormBuilderState().getBinds().add(f.getBind());

        attachFieldToInstanceParent(p, f);
    }

    /*
     * Associate the field with an instance parent.  This will either be the nearest repeated group OR 
     * the root of the instance, which ever comes first.  
     */
    private void attachFieldToInstanceParent(Field p, Field f)
    {
        if (Field.isRepeatedGroup(p)) {
            p.getRepeat().getInstance().getChildren().add(f.getInstance());
        } else if (p == null) {
            Collect.getInstance().getFormBuilderState().getInstance().add(f.getInstance());
        } else {
            attachFieldToInstanceParent(p.getParent(), f);
        }
    }

    /*
     * Prompt shown to the user before they leave the field list 
     * (discard changes & quit, save changes & quit, return to form field list)
     */
    private void createQuitDialog()
    {
        String[] items = {
                getString(R.string.keep_changes),
                getString(R.string.do_not_save)
        };
    
        mAlertDialog = new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.tf_form_builder_exit))
            .setNeutralButton(getString(R.string.do_not_exit), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            })
            .setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        case 0:
                            // Save and exit
                            mSaveFormDefinitionTask = new SaveFormDefinitionTask();
                            mSaveFormDefinitionTask.setFormSavedListener(FieldList.this);
                            mSaveFormDefinitionTask.execute(SaveToDiskTask.SAVED_AND_EXIT);

                            showDialog(SAVING_DIALOG);
                            break;

                        case 1:
                            // Discard any changes and exit
                            try {
                                if (mForm.getStatus() == FormDefinition.Status.placeholder)
                                    Collect.getInstance().getDbService().getDb().delete(mForm);
                            } catch (Exception e) {
                                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "unable to remove temporary document");
                                e.printStackTrace();
                            }
                            
                            finish();
                            break;
                        }
                    }
                }).create();
    
        mAlertDialog.show();
    }

    /*
     * Iterate through all fields at the same depth as the new one and 
     * ensure that the XPath is unique.  Modify it with a counter if it
     * is not.
     */
    private String createUniqueXPath(final ArrayList<Field> fieldsAtSameDepth, String path)
    {
        Iterator<Field> it = fieldsAtSameDepth.iterator();

        while (it.hasNext()) {
            Field f = it.next();

            // XPath of repeated groups is located at a different level
            if ((Field.isRepeatedGroup(f) && f.getRepeat().hasXPath() && f.getRepeat().getXPath().equals(path)) ||
                    (f.hasXPath() && f.getXPath().equals(path))) {
                
                // Capture counter from path
                Pattern pattern = Pattern.compile(".*([0-9]+)$");
                Matcher matcher = pattern.matcher(path);
                Integer counter = 1;

                if (matcher.find()) {
                    path = path.replaceFirst(matcher.group(1) + "$", "");
                    counter = Integer.valueOf(matcher.group(1));
                    counter++;   
                } 

                return createUniqueXPath(fieldsAtSameDepth, path + counter);
            }
        }

        return path;
    }
    
    /*
     * Determine XPath inheritance by traversing upwards in the tree from the current location.
     * 
     * The XPath will be determined from the XPath of the closest repeated group or from the 
     * root of the instance, which ever comes first. 
     */
    private String determineXPathInheritance(Field parent, Field field)
    {
        String xpath = "";
        
        if (parent == null) {
            xpath = createUniqueXPath(Collect.getInstance().getFormBuilderState().getFields(), File.separator + mInstanceRoot + File.separator + Field.makeFieldName(field.getLabel()));            
        } else {
            if (Field.isRepeatedGroup(parent)) {
                xpath = createUniqueXPath(parent.getRepeat().getChildren(), parent.getRepeat().getXPath() + File.separator + Field.makeFieldName(field.getLabel()));
            } else {
                xpath = determineXPathInheritance(parent.getParent(), field);
            }
        }
        
        return xpath;
    }
    
    /*
     * Finds the current active field, sets it to inactive and either returns 
     * null to signal that the "top level" of the form has been reached or 
     * sets the parent field to active and returns it.
     */
    private Field gotoParentField(Field f)
    {
        @SuppressWarnings("unused")
        final String tt = t + "gotoParentField(): ";
        
        Iterator<Field> it = null;
        
        if (f == null)
            it = mFieldState.iterator();
        else {
            if (f.isActive()) {
                f.setActive(false);
                
                if (f.getParent() == null) {
                    return null;
                } else if (f.getParent().getType().equals("repeat")) {                   
                    // Set the parent of our parent (e.g., a group) active and return it
                    f.getParent().getParent().setActive(true);
                    return f.getParent().getParent();
                } else {
                    f.getParent().setActive(true);
                    return f.getParent();
                }
            }
            
            it = f.getChildren().iterator();
        }        
        
        while (it.hasNext()) {                  
            Field result = gotoParentField(it.next());
            
            if (result instanceof Field)
                return result;
        }

        return null;        
    }

    private void goUpLevel()
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
        
        destination = gotoParentField(null);
        
        if (destination == null)
            refreshView(mFieldState);
        else {
            // Special support for nested repeated groups
            if (Field.isRepeatedGroup(destination)) {
                mActualPath.add(destination.getLabel().toString());
                mActualPath.add(destination.getRepeat().getLabel().toString());
                refreshView(destination.getRepeat().getChildren());
            } else {
                refreshView(destination.getChildren());
            }
        }
    }

    /* 
     * Refresh the view (displaying the currently active field 
     * or the top level of the form if no field is currently active)
     */
    private void refreshView()
    {
        // Don't attempt to refresh the view if loading or saving
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        
        Field destination = returnActiveField(null);

        if (destination == null)
            refreshView(mFieldState);
        else {
            // Special support for nested repeated groups
            if (Field.isRepeatedGroup(destination))                        
                refreshView(destination.getRepeat().getChildren());
            else 
                refreshView(destination.getChildren());
        }
    }

    /*
     * Refresh the view and display whatever fields are passed here
     */
    private void refreshView(ArrayList<Field> fieldsToDisplay)
    {
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_editing) + " " + mForm.getName());
        
        String pathText = "";
        
        if (mPath.isEmpty()) {
            pathText = getString(R.string.tf_at_top_of_form);
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
        
        mAdapter = new FormBuilderFieldListAdapter(getApplicationContext(), fieldsToDisplay);
        
        // Provide a hint to users if the list is empty
        if (mAdapter.isEmpty()) {
            TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
            nothingToDisplay.setVisibility(View.VISIBLE);
        } else {
            TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
            nothingToDisplay.setVisibility(View.INVISIBLE);
        }
        
        setListAdapter(mAdapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }
    
    /*
     * Finds the currently active fields and returns it (or null to indicate no active field)
     * 
     * Does not change the state of the currently active field, as opposed to gotoParentField()
     */
    private Field returnActiveField(Field f)
    {
        @SuppressWarnings("unused")
        final String tt = t + "returnActiveField(): ";
        
        Iterator<Field> it = null;
        
        if (f == null) {
            it = mFieldState.iterator();
        } else {
            if (f.isActive())
                return f;
            
            it = f.getChildren().iterator();
        }        
        
        while (it.hasNext()) {                  
            Field result = returnActiveField(it.next());
            
            if (result instanceof Field)
                return result;             
        }

        return null;
    }

    /*
     * Launch the element editor either to add a new field or to modify an existing one 
     */
    private void startFieldEditor(String type, Field field)
    {
        Collect.getInstance().getFormBuilderState().setField(field);
        
        Intent i = new Intent(this, FieldEditorActivity.class);
        i.putExtra(FieldEditorActivity.KEY_FIELDTYPE, type);
        startActivityForResult(i, REQUEST_EDITFIELD);
    }
}
