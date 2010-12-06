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

package com.radicaldynamic.groupinform.views;

import com.radicaldynamic.groupinform.logic.HierarchyElement;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HierarchyElementView extends RelativeLayout {

    private TextView mPrimaryTextView;
    private TextView mSecondaryTextView;
    private ImageView mIcon;


    public HierarchyElementView(Context context, HierarchyElement it) {
        super(context);

        setColor(it.getColor());

        mIcon = new ImageView(context);
        mIcon.setImageDrawable(it.getIcon());
        mIcon.setId(1);
        mIcon.setPadding(0, 1, 2, 0);
        addView(mIcon, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        mPrimaryTextView = new TextView(context);
        mPrimaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        mPrimaryTextView.setText(it.getPrimaryText());
        mPrimaryTextView.setId(2);
        LayoutParams l =
            new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        l.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
        addView(mPrimaryTextView, l);

        mSecondaryTextView = new TextView(context);
        mSecondaryTextView.setText(it.getSecondaryText());
        mSecondaryTextView.setPadding(7, 0, 7, 0);
        mSecondaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Small);
        LayoutParams lp =
            new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, mPrimaryTextView.getId());
        lp.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
        addView(mSecondaryTextView, lp);

        setPadding(0, 2, 0, 4);

    }


    public void setPrimaryText(String text) {
        mPrimaryTextView.setText(text);
    }


    public void setSecondaryText(String text) {
        mSecondaryTextView.setText(text);
    }


    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }


    public void setColor(int color) {
        setBackgroundColor(color);
    }


    public void showSecondary(boolean bool) {
        if (bool) {
            mSecondaryTextView.setVisibility(VISIBLE);
        } else {
            mSecondaryTextView.setVisibility(GONE);
        }
    }

}
