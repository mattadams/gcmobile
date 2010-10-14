package com.radicaldynamic.turboform.activities;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.preferences.ServerPreferences;

public class SynchronizeTabs extends TabActivity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.synchronize_tabs);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_synchronize));

        final TabHost tabHost = getTabHost();
        
        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("TurboForm")
                .setContent(new Intent(this, ServerPreferences.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("ODK Aggregate")
                .setContent(new Intent(this, InstanceUploaderList.class)));
    }
}
