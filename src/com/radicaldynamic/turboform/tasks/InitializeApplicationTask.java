package com.radicaldynamic.turboform.tasks;

import com.radicaldynamic.turboform.utilities.FileUtils;

import android.os.AsyncTask;

public class InitializeApplicationTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... nothing)
    {   
        /* Create necessary directories */
        FileUtils.createFolder(FileUtils.ODK_ROOT);
        FileUtils.createFolder(FileUtils.CACHE_PATH);       
        
        return null;
    }
}