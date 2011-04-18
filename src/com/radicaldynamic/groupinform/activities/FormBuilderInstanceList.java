package com.radicaldynamic.groupinform.activities;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.FormBuilderInstanceListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;
import com.radicaldynamic.groupinform.views.TouchListView;
import com.radicaldynamic.groupinform.xform.Instance;

public class FormBuilderInstanceList extends ListActivity
{
//    private static final String t = "FormBuilderInstanceList: ";
    
    private FormBuilderInstanceListAdapter adapter = null;
    private Button jumpPreviousButton;
    private TextView mPathText;
   
    private FormDefinitionDocument mForm;
    private ArrayList<Instance> mInstanceState;
    private ArrayList<String> mPath = new ArrayList<String>();          // Human readable location in mInstanceState
    
    /*
     * FIXME: element icons are not kept consistent when list items are reordered.  
     * I am not sure whether this affects only the items that are actually moved 
     * or the ones that are next to them.
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to)
        {
            Instance item = adapter.getItem(from);

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
        
        mInstanceState = Collect.getInstance().getFormBuilderState().getInstance();                    
        mForm = Collect.getInstance().getFormBuilderState().getFormDefDoc();
        
        if (savedInstanceState == null) {
            // Intent i = getIntent();
            refreshView(mInstanceState);
        } else {          
            // Restore state information provided by this activity            
            if (savedInstanceState.containsKey(FormEntryActivity.KEY_FORMPATH))
                mPath = savedInstanceState.getStringArrayList(FormEntryActivity.KEY_FORMPATH);
            
            // Check to see if this is a screen flip or a new form load
            //Object data = getLastNonConfigurationInstance();
                    
            Instance destination = gotoActiveInstance(null, true);
                    
            if (destination == null)
                refreshView(mInstanceState);
            else
                refreshView(destination.getChildren());
        }
    } // end onCreate
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(FormEntryActivity.KEY_FORMPATH, mPath);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.form_builder_context, menu);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.form_builder_options, menu);
        return true;
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        Instance instance = (Instance) getListAdapter().getItem(position);
        
        if (instance.getChildren().isEmpty()) {
//          //startInstanceEditor();                
        } else {
            instance.setActive(true);      

            // Deactivate the parent, if applicable
            if (instance.getParent() != null)
                instance.getParent().setActive(false);
            
            mPath.add(instance.getName());
            refreshView(instance.getChildren());
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
//        switch (item.getItemId()) {        
//        case R.id.barcode:  startElementEditor("barcode",   null);  break;
//        case R.id.date:     startElementEditor("date",      null);  break;
//        case R.id.geopoint: startElementEditor("geopoint",  null);  break;
//        case R.id.group:    startElementEditor("group",     null);  break;
//        case R.id.media:    startElementEditor("media",     null);  break;
//        case R.id.number:   startElementEditor("number",    null);  break;
//        case R.id.select:   startElementEditor("select",    null);  break;
//        case R.id.text:     startElementEditor("text",      null);  break;
//            
//        case MENU_SAVE:
//            break;
//        case MENU_HELP:
//            break;
//        }
        
        return super.onOptionsItemSelected(item);
    }
    
    public void goUpLevel()
    {
        Instance destination;
        
        mPath.remove(mPath.size() - 1);
        
        destination = gotoActiveInstance(null, false);
        
        if (destination == null)
            refreshView(mInstanceState);
        else {
            refreshView(destination.getChildren());
        }
    }
    
    /*
     * Finds the current active instance, sets it to inactive and either returns 
     * null to signal that the "top level" of the form has been reached or 
     * sets the parent instance to active and returns it.
     * 
     * If returnActiveInstance is true then the active instance itself will be 
     * returned vs. the parent instance.
     */
    public Instance gotoActiveInstance(Instance i, Boolean returnActiveInstance)
    {
        Iterator<Instance> it = null;
        
        if (i == null)
            it = mInstanceState.iterator();
        else {
            if (i.isActive()) {
                /* 
                 * This is convoluted logic that lets us use this method both for "go up" navigation 
                 * and also to reset navigation to the correct place on orientation changes
                 */
                if (returnActiveInstance)
                    return i;
                else
                    i.setActive(false);                    
                
                if (i.getParent() == null)                    
                    return i;
                else {                    
                    i.getParent().setActive(true);
                    return i.getParent();
                }
            }
            
            it = i.getChildren().iterator();
        }        
        
        while (it.hasNext()) {
            Instance result = gotoActiveInstance(it.next(), returnActiveInstance);
            
            if (result instanceof Instance)
                if (result.isActive() == false)
                    return null;
                else
                    return result;
        }

        return null;        
    }
    
    private void refreshView(ArrayList<Instance> instancesToDisplay)
    {
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_viewing_instance) + " " + mForm.getName());
        
        String pathText = "";
        
        if (mPath.isEmpty()) {
            pathText = "Viewing Top of Form Output";
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
        
        adapter = new FormBuilderInstanceListAdapter(getApplicationContext(), instancesToDisplay);
        setListAdapter(adapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }
    
    /*
     * Launch the element editor either to add a new instance or to modify an existing one 
     */
//    private void startInstanceEditor(String type, Instance instance)
//    {
//        Intent i = new Intent(this, FormBuilderInstanceEditor.class);
//        startActivity(i);
//    }
}
