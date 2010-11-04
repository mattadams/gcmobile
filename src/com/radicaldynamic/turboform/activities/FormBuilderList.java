package com.radicaldynamic.turboform.activities;

import java.io.IOException;
import java.util.ArrayList;

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
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;

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
    
    private static final String KEY_FORMID      = "formid";
    private static final String KEY_FORM        = "form";
    private static final String KEY_UTILITY     = "formutility";
    private static final String KEY_CONTROLS    = "formcontrols";
    
    private FormBuilderListAdapter adapter = null;
    
    private LoadFormDefinitionTask mLoadFormDefinitionTask;
    
    private ProgressDialog mDialog;
   
    private String mFormId;
    private FormDocument mForm;
    private FormUtils mFormUtility;
    private ArrayList<Control> mControlState;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        setContentView(R.layout.form_builder_main);     
        
        if (savedInstanceState == null) {
            Intent i = getIntent();        
            mFormId = i.getStringExtra(FormEntryActivity.KEY_FORMID);
        
            mLoadFormDefinitionTask = new LoadFormDefinitionTask();
            mLoadFormDefinitionTask.execute(mFormId);
        }
    }

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
            Log.d(Collect.LOGTAG, t + "Retrieved form " + mForm.getName() + " from database");
            
            Log.d(Collect.LOGTAG, t + "Retreiving form XML from database...");
            AttachmentInputStream ais = Collect.mDb.getDb().getAttachment(formId, "xml");
            mFormUtility = new FormUtils(ais);
            
            try {
                ais.close();
                
                mFormUtility.parseForm();            
                mControlState = mFormUtility.getControlState();
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
            mDialog.setCancelable(true);
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            setTitle(getString(R.string.tf_editing) + " " + mForm.getName());
            
            adapter = new FormBuilderListAdapter(getApplicationContext(), mControlState);
            setListAdapter(adapter);            

            TouchListView tlv = (TouchListView) getListView();

            tlv.setDropListener(onDrop);
            tlv.setRemoveListener(onRemove);
            
            mDialog.cancel();
        }
    }
}
