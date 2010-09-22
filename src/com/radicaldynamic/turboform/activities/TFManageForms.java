package com.radicaldynamic.turboform.activities;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.preferences.ServerPreferences;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class TFManageForms extends TabActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tf_manage_forms);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_manage_item, getString(R.string.forms)));

        final TabHost tabHost = getTabHost();
        Resources res = getResources();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Local", res.getDrawable(R.drawable.ic_menu_home))
                .setContent(new Intent(this, ServerPreferences.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Shared", res.getDrawable(android.R.drawable.ic_menu_share))
                .setContent(new Intent(this, ServerPreferences.class)));
        
        // This tab sets the intent flag so that it is recreated each time
        // the tab is clicked.
        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator("ODK Library", res.getDrawable(R.drawable.ic_menu_archive))
                .setContent(new Intent(this, FormDownloadList.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
}
