/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.turboform.activities;

import java.util.ArrayList;
import java.util.List;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.adapters.HierarchyListAdapter;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.logic.HierarchyElement;

public class FormHierarchyList extends ListActivity {
    private static final String t = "FormHierarchyList: ";

    private static final int CHILD = 1;
    private static final int EXPANDED = 2;
    private static final int COLLAPSED = 3;
    private static final int QUESTION = 4;
    
    private final String mIndent = "     ";
    
    public static final String KEY_FORMID = "formpath";             
    public static final String KEY_INSTANCEID = "instancepath";
    public static final String KEY_AUTO_LOAD = "autoload";          // This activity was loaded automatically
    
    private FormEntryController mFormEntryController;
    private FormEntryModel mFormEntryModel;
    
    private RelativeLayout mRelativeLayout;
    private View mBrowserButtons;
    private Button jumpPreviousButton;
    
    private boolean mLoadedAutomatically;
    
    List<HierarchyElement> formList;
    FormIndex mStartIndex;
    TextView mPath;
    
    int state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hierarchy_layout);
        
        // We use a static FormEntryController to make jumping faster
        mFormEntryController = Collect.getInstance().getFormEntryController();
        mFormEntryModel = mFormEntryController.getModel();
        mStartIndex = mFormEntryModel.getFormIndex();
        
        if (Collect.getInstance().getInstanceBrowseList().size() > 1) {           
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRelativeLayout = (RelativeLayout) findViewById(R.id.rl);                        
            mBrowserButtons = inflater.inflate(R.layout.form_browser_buttons, mRelativeLayout, false);
            mRelativeLayout.addView(mBrowserButtons);
            mRelativeLayout.invalidate();            
            
            ((Button) mBrowserButtons.findViewById(R.id.next_instance)).setOnClickListener(
                    new Button.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            setResult(RESULT_OK, (new Intent()).setAction("next_instance"));
                            finish();
                        }                        
                    });
            
            ((Button) mBrowserButtons.findViewById(R.id.previous_instance)).setOnClickListener(
                    new Button.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            setResult(RESULT_OK, (new Intent()).setAction("previous_instance"));
                            finish();
                        }                        
                    });
            
            setTitle(getString(R.string.app_name) + " > " + mFormEntryModel.getFormTitle());
        } else {
            setTitle(getString(R.string.app_name) + " > " + mFormEntryModel.getFormTitle());
        }

        mPath = (TextView) findViewById(R.id.pathtext);

        jumpPreviousButton = (Button) findViewById(R.id.jumpPreviousButton);
        jumpPreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goUpLevel();
            }
        });

        Button jumpBeginningButton = (Button) findViewById(R.id.jumpBeginningButton);
        jumpBeginningButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFormEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Button jumpEndButton = (Button) findViewById(R.id.jumpEndButton);
        jumpEndButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFormEntryController.jumpToIndex(FormIndex.createEndOfFormIndex());
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        
        refreshView();
        
        // Determine whether this activity was loaded automatically or manually
        Intent intent = getIntent();
        mLoadedAutomatically = intent.getBooleanExtra(KEY_AUTO_LOAD, false);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        HierarchyElement h = (HierarchyElement) l.getItemAtPosition(position);
        
        if (h.getFormIndex() == null) {
            goUpLevel();
            return;
        }
    
        switch (h.getType()) {
            case EXPANDED:
                h.setType(COLLAPSED);
                ArrayList<HierarchyElement> children = h.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    formList.remove(position + 1);
                }
                h.setIcon(getResources().getDrawable(R.drawable.expander_ic_minimized));
                h.setColor(Color.WHITE);
                break;
            case COLLAPSED:
                h.setType(EXPANDED);
                ArrayList<HierarchyElement> children1 = h.getChildren();
                for (int i = 0; i < children1.size(); i++) {
                    Log.i(Collect.LOGTAG, t + "adding child: " + children1.get(i).getFormIndex());
                    formList.add(position + 1 + i, children1.get(i));
    
                }
                h.setIcon(getResources().getDrawable(R.drawable.expander_ic_maximized));
                h.setColor(Color.WHITE);
                break;
            case QUESTION:
                mFormEntryController.jumpToIndex(h.getFormIndex());
                setResult(RESULT_CANCELED);
                finish();
                return;
            case CHILD:
                mFormEntryController.jumpToIndex(h.getFormIndex());
                setResult(RESULT_CANCELED);
                refreshView();
                return;
        }
    
        // Should only get here if we've expanded or collapsed a group
        HierarchyListAdapter itla = new HierarchyListAdapter(this);
        itla.setListItems(formList);
        setListAdapter(itla);
        getListView().setSelection(position);
    }


    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0) {
                    mFormEntryController.jumpToIndex(mStartIndex);
                    
                    if (mLoadedAutomatically) {
                        setResult(RESULT_OK, (new Intent()).setAction("return_to_browser"));
                        finish();
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }


    public void refreshView() {
        // Record the current index so we can return to the same place if the user hits 'back'.
        FormIndex currentIndex = mFormEntryModel.getFormIndex();

        // If we're not at the first level, we're inside a repeated group so we want to only display
        // everything enclosed within that group.
        String enclosingGroupRef = "";
        formList = new ArrayList<HierarchyElement>();

        // If we're currently at a repeat node, record the name of the node and step to the next
        // node to display.
        if (mFormEntryModel.getEvent() == FormEntryController.EVENT_REPEAT) {
            enclosingGroupRef = mFormEntryModel.getFormIndex().getReference().toString(false);
            mFormEntryController.stepToNextEvent();
        } else {
            FormIndex startTest = stepIndexOut(currentIndex);
            // If we have a 'group' tag, we want to step back until we hit a repeat or the
            // beginning.
            while (startTest != null
                    && mFormEntryModel.getEvent(startTest) == FormEntryController.EVENT_GROUP) {
                startTest = stepIndexOut(startTest);
            }
            if (startTest == null) {
                // check to see if the question is at the first level of the hierarchy. If it is,
                // display the root level from the beginning.
                mFormEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
            } else {
                // otherwise we're at a repeated group
                mFormEntryController.jumpToIndex(startTest);
            }

            // now test again for repeat. This should be true at this point or we're at the
            // beginning
            if (mFormEntryModel.getEvent() == FormEntryController.EVENT_REPEAT) {
                enclosingGroupRef = mFormEntryModel.getFormIndex().getReference().toString(false);
                mFormEntryController.stepToNextEvent();
            }
        }

        int event = mFormEntryModel.getEvent();
        if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
            // The beginning of form has no valid prompt to display.
            mFormEntryController.stepToNextEvent();
            mPath.setVisibility(View.GONE);
            jumpPreviousButton.setEnabled(false);
        } else {
            mPath.setVisibility(View.VISIBLE);
            mPath.setText(getCurrentPath());
            jumpPreviousButton.setEnabled(true);
        }

        // Refresh the current event in case we did step forward.
        event = mFormEntryModel.getEvent();

        // There may be repeating Groups at this level of the hierarchy, we use this variable to
        // keep track of them.
        String repeatedGroupRef = "";

        event_search: while (event != FormEntryController.EVENT_END_OF_FORM) {
            switch (event) {
                case FormEntryController.EVENT_QUESTION:
                    if (!repeatedGroupRef.equalsIgnoreCase("")) {
                        // We're in a repeating group, so skip this question and move to the next
                        // index.
                        event = mFormEntryController.stepToNextEvent();
                        continue;
                    }

                    FormEntryPrompt fp = mFormEntryModel.getQuestionPrompt();
                    formList.add(new HierarchyElement(fp.getLongText(), fp.getAnswerText(), null,
                            Color.WHITE, QUESTION, fp.getIndex()));
                    break;
                case FormEntryController.EVENT_GROUP:
                    // ignore group events
                    break;
                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    if (enclosingGroupRef.compareTo(mFormEntryModel.getFormIndex().getReference()
                            .toString(false)) == 0) {
                        // We were displaying a set of questions inside of a repeated group. This is
                        // the end of that group.
                        break event_search;
                    }

                    if (repeatedGroupRef.compareTo(mFormEntryModel.getFormIndex().getReference()
                            .toString(false)) != 0) {
                        // We're in a repeating group, so skip this repeat prompt and move to the
                        // next event.
                        event = mFormEntryController.stepToNextEvent();
                        continue;
                    }

                    if (repeatedGroupRef.compareTo(mFormEntryModel.getFormIndex().getReference()
                            .toString(false)) == 0) {
                        // This is the end of the current repeating group, so we reset the
                        // repeatedGroupName variable
                        repeatedGroupRef = "";
                    }
                    break;
                case FormEntryController.EVENT_REPEAT:
                    FormEntryCaption fc = mFormEntryModel.getCaptionPrompt();
                    if (enclosingGroupRef.compareTo(mFormEntryModel.getFormIndex().getReference()
                            .toString(false)) == 0) {
                        // We were displaying a set of questions inside a repeated group. This is
                        // the end of that group.
                        break event_search;
                    }
                    if (repeatedGroupRef.equalsIgnoreCase("") && fc.getMultiplicity() == 0) {
                        // This is the start of a repeating group. We only want to display
                        // "Group #", so we mark this as the beginning and skip all of its children
                        HierarchyElement group =
                            new HierarchyElement(fc.getLongText(), null, getResources()
                                    .getDrawable(R.drawable.expander_ic_minimized), Color.WHITE,
                                    COLLAPSED, fc.getIndex());
                        repeatedGroupRef =
                            mFormEntryModel.getFormIndex().getReference().toString(false);
                        formList.add(group);
                    }

                    if (repeatedGroupRef.compareTo(mFormEntryModel.getFormIndex().getReference()
                            .toString(false)) == 0) {
                        // Add this group name to the drop down list for this repeating group.
                        HierarchyElement h = formList.get(formList.size() - 1);
                        h.addChild(new HierarchyElement(mIndent + fc.getLongText() + " "
                                + (fc.getMultiplicity() + 1), null, null, Color.WHITE, CHILD, fc
                                .getIndex()));
                    }
                    break;
            }
            event = mFormEntryController.stepToNextEvent();
        }

        HierarchyListAdapter itla = new HierarchyListAdapter(this);
        itla.setListItems(formList);
        setListAdapter(itla);

        // set the controller back to the current index in case the user hits 'back'
        mFormEntryController.jumpToIndex(currentIndex);
    }


    /**
     * used to go up one level in the formIndex. That is, if you're at 5_0, 1 (the second question
     * in a repeating group), this method will return a FormInex of 5_0 (the start of the repeating
     * group). If your at index 16 or 5_0, this will return null;
     * 
     * @param index
     * @return index
     */
    public FormIndex stepIndexOut(FormIndex index) {
        if (index.isTerminal()) {
            return null;
        } else {
            return new FormIndex(stepIndexOut(index.getNextLevel()), index);
        }
    }


    private String getCurrentPath() {
        FormIndex index = stepIndexOut(mFormEntryModel.getFormIndex());
    
        String path = "";
        
        while (index != null) {    
            path = mFormEntryModel.getCaptionPrompt(index).getLongText() + " ("
                        + (mFormEntryModel.getCaptionPrompt(index).getMultiplicity() + 1) + ") > "
                        + path;
    
            index = stepIndexOut(index);
        }
        
        // return path?
        return path.substring(0, path.length() - 2);
    }
    
    private void goUpLevel() {
        FormIndex index = stepIndexOut(mFormEntryModel.getFormIndex());
        int currentEvent = mFormEntryModel.getEvent();
    
        // Step out of any group indexes that are present.
        while (index != null && mFormEntryModel.getEvent(index) == FormEntryController.EVENT_GROUP) {
            index = stepIndexOut(index);
        }
    
        if (index == null) {
            mFormEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        } else {
            if (currentEvent == FormEntryController.EVENT_REPEAT) {
                // We were at a repeat, so stepping back brought us to then previous level
                mFormEntryController.jumpToIndex(index);
            } else {
                // We were at a question, so stepping back brought us to either:
                // The beginning. or The start of a repeat. So we need to step
                // out again to go passed the repeat.
                index = stepIndexOut(index);
                
                if (index == null) {
                    mFormEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
                } else {
                    mFormEntryController.jumpToIndex(index);
                }
            }
        }
    
        refreshView();
    }

}
