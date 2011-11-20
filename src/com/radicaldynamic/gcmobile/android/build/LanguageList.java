/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.radicaldynamic.gcmobile.android.build;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.xform.Translation;


/**
 * Another variation of the list of cheeses. In this case, we use
 * {@link AbsListView#setOnScrollListener(AbsListView.OnScrollListener) 
 * AbsListView#setOnItemScrollListener(AbsListView.OnItemScrollListener)} to display the
 * first letter of the visible range of cheeses.
 */
public class LanguageList extends ListActivity implements ListView.OnScrollListener
{
    private final class RemoveWindow implements Runnable 
    {
        public void run() 
        {
            removeWindow();
        }
    }

    private RemoveWindow mRemoveWindow = new RemoveWindow();
    Handler mHandler = new Handler();
    private WindowManager mWindowManager;
    private TextView mDialogText;
    private boolean mShowing;
    private boolean mReady;
    private char mPrevLetter = Character.MIN_VALUE;
    private String [] mLanguages;
    private String [] mAbbrevLanguages;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_i18n_add_language));
        
        // Use an existing ListAdapter that will map an array of strings to TextViews
        Resources res = getResources();
        mLanguages = res.getStringArray(R.array.i18n);
        mAbbrevLanguages = res.getStringArray(R.array.i18n_abbrev);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mLanguages));

        getListView().setOnScrollListener(this);
        
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mDialogText = (TextView) inflate.inflate(R.layout.list_position, null);
        mDialogText.setVisibility(View.INVISIBLE);
        
        mHandler.post(new Runnable()
        {
            public void run() 
            {
                mReady = true;
                
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        LayoutParams.WRAP_CONTENT, 
                        LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                
                mWindowManager.addView(mDialogText, lp);
            }
        });
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        mWindowManager.removeView(mDialogText);
        mReady = false;
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int pos, long id)
    {        
        ArrayList<String> formLanguages = new ArrayList<String>();
        
        // Build an index of languages that already exist
        Iterator<Translation> it = Collect.getInstance().getFormBuilderState().getTranslations().iterator();
       
        while (it.hasNext()) {
            Translation t = (Translation) it.next();
            formLanguages.add(t.getLang().toLowerCase());
        }        
        
        if (formLanguages.contains(mLanguages[pos].toLowerCase()) || formLanguages.contains(mAbbrevLanguages[pos].toLowerCase())) {
            Toast.makeText(this, getString(R.string.tf_i18n_add_language_failed, mLanguages[pos]), Toast.LENGTH_SHORT).show();
        } else {
            Collect.getInstance().getFormBuilderState().getTranslations().add(new Translation(mAbbrevLanguages[pos]));
            Toast.makeText(this, getString(R.string.tf_i18n_added, mLanguages[pos]), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        removeWindow();
        mReady = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mReady = true;
        
        Toast.makeText(this, getString(R.string.tf_i18n_add_language_hint), Toast.LENGTH_SHORT).show();
    }
    
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) 
    {
        if (mReady && firstVisibleItem > 0) {
            char firstLetter = mLanguages[firstVisibleItem].charAt(0);
            
            if (!mShowing && firstLetter != mPrevLetter) {
                mShowing = true;
                mDialogText.setVisibility(View.VISIBLE);
            }
            
            mDialogText.setText(((Character)firstLetter).toString());
            mHandler.removeCallbacks(mRemoveWindow);
            mHandler.postDelayed(mRemoveWindow, 2000);
            mPrevLetter = firstLetter;
        }
    }    

    public void onScrollStateChanged(AbsListView view, int scrollState) 
    {
    }    
    
    private void removeWindow()
    {
        if (mShowing) {
            mShowing = false;
            mDialogText.setVisibility(View.INVISIBLE);
        }
    }
}
