package com.radicaldynamic.groupinform.activities;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.preferences.ServerPreferences;

public class ManageFormsTabs extends TabActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.manage_forms);
        
        setTitle(getString(R.string.app_name) + " > " 
                + getString(R.string.tf_manage_item, getString(R.string.forms)));

        final TabHost tabHost = getTabHost();
        
        Resources res = getResources();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(getText(R.string.tf_my_forms), res.getDrawable(R.drawable.ic_menu_home))
                .setContent(new Intent(this, MyFormsList.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Shared", res.getDrawable(R.drawable.ic_menu_share))
                .setContent(new Intent(this, ServerPreferences.class)));
        
        // This tab sets the intent flag so that it is recreated each time the tab is clicked.
        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator(getText(R.string.tf_odk_library), res.getDrawable(R.drawable.ic_menu_archive))
                .setContent(new Intent(this, FormDownloadList.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
}