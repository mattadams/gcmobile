package com.radicaldynamic.groupinform.activities;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;

public class ODKActivityTab extends TabActivity
{
    @SuppressWarnings("unused")
    private static final String t = "ODKActivityTab: ";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);  
        setContentView(R.layout.odk_tab);
        
        // Load our custom window title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_folder_selector);
        
        // Reflect the currently selected folder & window title
        Button b1 = (Button) findViewById(R.id.folderTitleButton);
        b1.setText(BrowserActivity.getSelectedFolderName());

        TextView t1 = (TextView) findViewById(R.id.titleLeftText);
        t1.setText(getString(R.string.app_name) + " > " + getString(R.string.tf_open_data_kit_abbrev));

        final TabHost tabHost = getTabHost();
        
        Resources res = getResources();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(getText(R.string.tf_odk_aggregate), res.getDrawable(R.drawable.ic_tab_upload))
                .setContent(new Intent(this, InstanceUploaderList.class)));
        
        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(getText(R.string.tf_odk_library), res.getDrawable(R.drawable.ic_tab_download))
                .setContent(new Intent(this, FormDownloadList.class)));
    }
}
