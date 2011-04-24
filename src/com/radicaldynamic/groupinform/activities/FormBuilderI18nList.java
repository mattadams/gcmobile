package com.radicaldynamic.groupinform.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.TranslationSortByLang;
import com.radicaldynamic.groupinform.xform.Translation;

public class FormBuilderI18nList extends ExpandableListActivity 
{
    private static final int DIALOG_EDIT_TRANSLATION   = 0;
    private static final int DIALOG_RESET_TRANSLATIONS = 1;
    private static final int DIALOG_REMOVE_LANGUAGE    = 2;
    private static final int DIALOG_REMOVE_TRANSLATION = 3;
    private static final int DIALOG_TRANSLATIONS_EXIST = 4;
    
    // Show translations matching this key/this translation ID was selected
    public static final String KEY_TRANSLATION_ID = "key_translation_id";           
    public static final String KEY_FIELDTEXT_TYPE = "key_fieldtext_type";           // Either "label" or "description"
    public static final String KEY_DESCRIPTION    = "description";
    public static final String KEY_LABEL          = "label"; 
    public static final String KEY_ITEM_LABEL     = "the item label";
    
    private static final String KEY_SELECT_SINGLE_MODE = "key_select_single_translation";
    private static final String KEY_GROUP_POSITION = "key_group_position";
    private static final String KEY_CHILD_POSITION = "key_child_position";
    
    private BaseExpandableListAdapter mAdapter;
    private LayoutInflater mInflater;
    
    private Boolean mSelectSingleMode = false;
    private String mTranslationId = "";                                         // Filter translations by this ID
    private String mFieldTextType = "";    
    
    private ArrayList<Translation> mTranslations = Collect.getInstance().getFormBuilderState().getTranslations();
    private ArrayList<String> mAbbreviations = new ArrayList<String>();
    private String [] mLanguages;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);  
        
        mAdapter = new LanguageListAdapter();
        mInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);        
        
        // Load reference lists
        Resources res = getResources();
        mLanguages = res.getStringArray(R.array.i18n);
        mAbbreviations = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.i18n_abbrev)));
        
        // Sort translation list
        Collections.sort(mTranslations, new TranslationSortByLang(mLanguages, mAbbreviations));
        
        if (savedInstanceState == null) {
            Intent i = getIntent();
            
            if (i.hasExtra(KEY_FIELDTEXT_TYPE))
                mFieldTextType = i.getStringExtra(KEY_FIELDTEXT_TYPE);
            
            if (i.hasExtra(KEY_TRANSLATION_ID)) {
                mSelectSingleMode = true;
                mTranslationId = i.getStringExtra(KEY_TRANSLATION_ID);
            }
        } else {
            if (savedInstanceState.containsKey(KEY_FIELDTEXT_TYPE))
                mFieldTextType = savedInstanceState.getString(KEY_FIELDTEXT_TYPE);
                
            if (savedInstanceState.containsKey(KEY_SELECT_SINGLE_MODE))
                mSelectSingleMode = savedInstanceState.getBoolean(KEY_SELECT_SINGLE_MODE);
            
            if (savedInstanceState.containsKey(KEY_TRANSLATION_ID)) 
                mTranslationId = savedInstanceState.getString(KEY_TRANSLATION_ID);                
        }
        
        // Provide a title specific to the context in which this activity is used
        if (mSelectSingleMode)
            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_i18n_translate));  
        else
            setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_i18n_setup));
        
        setListAdapter(mAdapter);
        getExpandableListView().setOnChildClickListener(this);
        registerForContextMenu(getExpandableListView());
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {        
        super.onCreateContextMenu(menu, v, menuInfo);
        
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.formbuilderi18nlist_cmenu, menu);
        }
    }
    
    @Override
    public Dialog onCreateDialog(int id, final Bundle args)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);      
        View view = null;
        
        switch (id) {
        case DIALOG_EDIT_TRANSLATION:
            // Retrieve language
            final Translation translationGroup = mTranslations.get(args.getInt(KEY_GROUP_POSITION));
            Translation translationText = new Translation(mTranslationId, null);
            String language = Translation.expandLangAbbreviation(mLanguages, mAbbreviations, translationGroup.getLang());
            
            /*
             * Find specific translation (unfortunately we cannot use child position because we 
             * fooled the adapter into thinking that there was only one child when in reality 
             * there may be many)
             */
            if (translationGroup.getTexts().isEmpty()) {
                // Adding new translation
            } else {
                Iterator<Translation> it = translationGroup.getTexts().iterator();

                while (it.hasNext()) {
                    Translation t = it.next();

                    if (t.getId().equals(mTranslationId)) {
                        translationText = t;
                    }
                }
            }
            
            view = inflater.inflate(R.layout.dialog_edit_translation, null);
            
            // Set an EditText view to get user input 
            final EditText copy = (EditText) view.findViewById(R.id.copy);
            copy.setText(translationText.toString());
            
            builder.setView(view);
            builder.setInverseBackgroundForced(true);
            builder.setTitle(getString(R.string.tf_i18n_edit_translation, language));
            
            builder.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (copy.getText().toString().trim().length() < 1) {
                        Toast.makeText(FormBuilderI18nList.this, getString(R.string.tf_unable_to_save_empty_translation), Toast.LENGTH_SHORT).show();
                        removeDialog(DIALOG_EDIT_TRANSLATION);
                        showDialog(DIALOG_EDIT_TRANSLATION, args);
                    } else {                    
                        Iterator<Translation> it = translationGroup.getTexts().iterator();
                        boolean textUpdated = false;

                        while (it.hasNext()) {
                            Translation t = it.next();

                            if (t.getId().equals(mTranslationId)) {
                                t.setValue(copy.getText().toString().trim());
                                textUpdated = true;
                                break;
                            }
                        }

                        // Add new translation if an existing text could not be found to be updated
                        if (!textUpdated) {
                            /*
                             * If a translation ID was not passed then this means that the label/description 
                             * has not yet been translated so we initialize it appropriately here
                             */
                            if (mTranslationId == null) {
                                mTranslationId = UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
                                String jr = "jr:itext('" + mTranslationId + "')";                                
                               
                                if (mFieldTextType.equals(KEY_ITEM_LABEL)) {
                                    Collect.getInstance().getFormBuilderState().getItem().setLabel(jr);
                                } else if (mFieldTextType.equals(KEY_LABEL)) {
                                    Collect.getInstance().getFormBuilderState().getField().setLabel(jr);
                                } else if (mFieldTextType.equals(KEY_DESCRIPTION)) {
                                    Collect.getInstance().getFormBuilderState().getField().setHint(jr);
                                }
                            }
                            
                            translationGroup.getTexts().add(new Translation(mTranslationId, copy.getText().toString().trim()));
                        }

                        removeDialog(DIALOG_EDIT_TRANSLATION);
                        refreshView();
                    }
                }
            });
         
            builder.setNeutralButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_EDIT_TRANSLATION);
                }
            });
            
            if (mSelectSingleMode && translationText.getValue() != null) {
                builder.setNegativeButton(getText(R.string.tf_remove), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeDialog(DIALOG_EDIT_TRANSLATION);
                        showDialog(DIALOG_REMOVE_TRANSLATION, args);
                    }
                });
            }
            
            dialog = builder.create();
            break;
            
        case DIALOG_RESET_TRANSLATIONS:            
            builder.setMessage(getString(R.string.tf_i18n_reset_translations_dialog_msg, mFieldTextType));
            builder.setTitle(getString(R.string.tf_i18n_reset_translations_dialog));
            
            builder.setPositiveButton(getText(R.string.tf_i18n_reset_translations), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Iterator<Translation> it = mTranslations.iterator();
                    
                    while (it.hasNext()) {
                        Translation t = it.next();
                        Iterator<Translation> itt = t.getTexts().iterator();
                        
                        while (itt.hasNext()) {
                            Translation tt = itt.next();
                            
                            if (tt.getId().equals(mTranslationId))
                                itt.remove();
                        }
                    }
                    
                    // Mark the current field label or description as being blank/untranslated
                    if (mFieldTextType.equals(KEY_ITEM_LABEL)) {
                        Collect.getInstance().getFormBuilderState().getItem().setLabel("");
                    } else if (mFieldTextType.equals(KEY_LABEL)) {
                        Collect.getInstance().getFormBuilderState().getField().setLabel("");
                    } else if (mFieldTextType.equals(KEY_DESCRIPTION)) {
                        Collect.getInstance().getFormBuilderState().getField().setHint("");
                    }
                    
                    Toast.makeText(FormBuilderI18nList.this, getString(R.string.tf_i18n_translations_reset, mFieldTextType), Toast.LENGTH_SHORT).show();
                    
                    setResult(RESULT_OK);
                    finish();
                }
            });

            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_RESET_TRANSLATIONS);
                }
            });
            
            dialog = builder.create();
            break;
            
        case DIALOG_REMOVE_LANGUAGE:          
            // Retrieve language
            final Translation tgrl = mTranslations.get(args.getInt(KEY_GROUP_POSITION));
            String lrl = Translation.expandLangAbbreviation(mLanguages, mAbbreviations, tgrl.getLang());
            
            builder.setTitle(getString(R.string.tf_i18n_remove_language_dialog, lrl));
            builder.setMessage(getString(R.string.tf_i18n_remove_language_dialog_msg, lrl));
            
            builder.setPositiveButton(getText(R.string.tf_remove), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (tgrl.getTexts().isEmpty()) {
                        // Bah. Aagain?
                        String l = Translation.expandLangAbbreviation(mLanguages, mAbbreviations, tgrl.getLang());
                        
                        Toast.makeText(FormBuilderI18nList.this, getString(R.string.tf_removed_with_param, l), Toast.LENGTH_SHORT).show();
                        mTranslations.remove(args.getInt(KEY_GROUP_POSITION));                        
                        removeDialog(DIALOG_REMOVE_LANGUAGE);
                        refreshView();
                    } else {
                        removeDialog(DIALOG_REMOVE_LANGUAGE);
                        showDialog(DIALOG_TRANSLATIONS_EXIST);
                    }
                }
            });
            
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_REMOVE_LANGUAGE);
                }
            });            
            
            dialog = builder.create();
            break;
    
        case DIALOG_REMOVE_TRANSLATION:
            // Retrieve language
            final Translation tgrt = mTranslations.get(args.getInt(KEY_GROUP_POSITION));
            String lrt = Translation.expandLangAbbreviation(mLanguages, mAbbreviations, tgrt.getLang());

            builder.setTitle(getString(R.string.tf_i18n_remove_translation, lrt));
            
            builder.setPositiveButton(getText(R.string.tf_remove), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Again? Bah.
                    String l = Translation.expandLangAbbreviation(mLanguages, mAbbreviations, tgrt.getLang());
                    
                    // Find and nullify entry (nulled entries will not be written to XML)
                    Iterator<Translation> it = tgrt.getTexts().iterator();

                    while (it.hasNext()) {
                        Translation t = it.next();

                        if (t.getId().equals(mTranslationId)) {
                            it.remove();
                            break;
                        }
                    }
                    
                    Toast.makeText(FormBuilderI18nList.this, getString(R.string.tf_i18n_translation_removed, l), Toast.LENGTH_SHORT).show();
                    
                    removeDialog(DIALOG_REMOVE_TRANSLATION);
                    refreshView();
                }
            });
            
            builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_REMOVE_TRANSLATION);
                }
            });            

            dialog = builder.create();
            break;
            
            
        case DIALOG_TRANSLATIONS_EXIST:
            builder.setTitle(getString(R.string.tf_unable_to_remove_language_dialog));
            builder.setMessage(getString(R.string.tf_unable_to_remove_language_dialog_msg));            
            
            builder.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });        
            
            dialog = builder.create();
            break;
        }
    
        return dialog;
    }

    @Override
    @SuppressWarnings("unused")
    public boolean onContextItemSelected(MenuItem item) 
    {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

//        String title = ((TextView) info.targetView).getText().toString();
        
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition); 
            return true;
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
            
            switch (item.getItemId()) {
            case R.id.removeLanguage:
                Bundle b = new Bundle();
                b.putInt(KEY_GROUP_POSITION, groupPos);
                showDialog(DIALOG_REMOVE_LANGUAGE, b);
                break;
                
            case R.id.makeDefault:
                for (int i = 0; i < mTranslations.size(); i++) {
                    if (i == groupPos)
                        mTranslations.get(i).setFallback(true);                        
                    else
                        mTranslations.get(i).setFallback(false);
                }
                
                refreshView();
            }            
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.formbuilderi18nlist_omenu, menu);

        // "Remove all" translations isn't available unless a specific translation has been selected
        if (!mSelectSingleMode)
            menu.removeItem(R.id.remove);
        
        return true;
    }    
    
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) 
    {        
        if (!mSelectSingleMode)
            mTranslationId = Collect.getInstance().getFormBuilderState().getTranslations()
                .get(groupPosition).getTexts()
                .get(childPosition).getId();
        
        Bundle b = new Bundle();
        b.putInt(KEY_GROUP_POSITION, groupPosition);
        b.putInt(KEY_CHILD_POSITION, childPosition);
        showDialog(DIALOG_EDIT_TRANSLATION, b);
        
        return false;
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.add:
            Intent i = new Intent(this, FormBuilderLanguageList.class);
            startActivity(i);
            break;
            
        case R.id.remove:
            showDialog(DIALOG_RESET_TRANSLATIONS);
            break;
        }
        
        return super.onOptionsItemSelected(item);
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
        
        outState.putString(KEY_FIELDTEXT_TYPE, mFieldTextType);
        outState.putBoolean(KEY_SELECT_SINGLE_MODE, mSelectSingleMode);
        outState.putString(KEY_TRANSLATION_ID, mTranslationId);
    }
    
    /**
     * A simple adapter which maintains an ArrayList of photo resource Ids. 
     * Each photo is displayed as an image. This adapter supports clearing the
     * list of photos and adding a new photo.
     *
     */
    private class LanguageListAdapter extends BaseExpandableListAdapter 
    {        
        public Object getChild(int groupPosition, int childPosition)
        {
            if (mSelectSingleMode) {
                Iterator<Translation> it = mTranslations.get(groupPosition).getTexts().iterator();
                Translation d = new Translation(mTranslationId, "[Translation Missing]");
                
                while (it.hasNext()) {
                    Translation t = it.next();
                    
                    if (t.getId().equals(mTranslationId) 
                            && t.getValue() instanceof String 
                            && t.getValue().length() > 0) {
                        return t;
                    }
                }
                
                return d;
            } else {
                return mTranslations.get(groupPosition).getTexts().get(childPosition);
            }
        }

        public long getChildId(int groupPosition, int childPosition) 
        {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) 
        {
            if (mSelectSingleMode) {
                return 1;
            } else {
                return mTranslations.get(groupPosition).getTexts().size();
            }
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) 
        {
            View v = mInflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            
            TextView t = (TextView) v.findViewById(android.R.id.text1);
            t.setEllipsize(TruncateAt.END);
            t.setSingleLine(true);
            t.setText(getChild(groupPosition, childPosition).toString());
            t.setTextColor(R.color.solid_black);
            
            t = (TextView) v.findViewById(android.R.id.text2);
            t.setTextColor(R.color.solid_black);
            
            // Context specific hints (removal only makes sense from the perspective of a field-specific translation)
            if (mSelectSingleMode) 
                t.setText("Select to change or remove");
            else
                t.setText("Select to change");
            
            return v;
        }

        public Object getGroup(int groupPosition) 
        {
            return mTranslations.get(groupPosition);
        }

        public int getGroupCount() 
        {
            return mTranslations.size();
        }

        public long getGroupId(int groupPosition) 
        {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) 
        {
            View v = mInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            TextView t = (TextView) v.findViewById(android.R.id.text1);
            t.setTextColor(R.color.solid_black);
            
            // Expand any abbreviations for the sake of readability
            if (mAbbreviations.contains(getGroup(groupPosition).toString().toLowerCase())) {
                int i = mAbbreviations.indexOf(getGroup(groupPosition).toString().toLowerCase());
                t.setText(mLanguages[i].toString());
            } else {
                t.setText(getGroup(groupPosition).toString());
            }            
            
            // Identify default/fallback translation
            if (mTranslations.get(groupPosition).isFallback()) {
                t.setTypeface(t.getTypeface(), Typeface.BOLD);
                t.setText(t.getText() + " (Default)");
            }
            
            return v;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return true;
        }

        public boolean hasStableIds()
        {
           return true;
        }
    }
    
    private void refreshView()
    {        
        // User may have added a new language, refresh to display 
        if (mAdapter instanceof BaseExpandableListAdapter) {
            Collections.sort(mTranslations, new TranslationSortByLang(mLanguages, mAbbreviations));
            mAdapter.notifyDataSetChanged();
        }
    }
}
