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

package com.radicaldynamic.groupinform.activities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.ektorp.DbAccessException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormInstanceDocument;
import com.radicaldynamic.groupinform.documents.GenericDocument;
import com.radicaldynamic.groupinform.listeners.InstanceUploaderListener;
import com.radicaldynamic.groupinform.preferences.ServerPreferences;
import com.radicaldynamic.groupinform.tasks.InstanceUploaderTask;
import com.radicaldynamic.groupinform.utilities.PasswordPromptDialogBuilder;
import com.radicaldynamic.groupinform.utilities.WebUtils;
import com.radicaldynamic.groupinform.utilities.PasswordPromptDialogBuilder.OnOkListener;

/**
 * Activity to upload completed forms.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceUploaderActivity extends Activity implements InstanceUploaderListener {
    
    private static final String t = "InstanceUploaderActivity: ";

    private final static int PROGRESS_DIALOG = 1;
    private final static String KEY_TOTALCOUNT = "totalcount";
    private ProgressDialog mProgressDialog;

    private InstanceUploaderTask mInstanceUploaderTask;
    private int totalCount = -1;
    
    private static final class UploadArgs {
        Set<String> hosts;
        ArrayList<String> instances;
        String userEmail;
    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name) + " > " + getString(R.string.send_data));

        // get instances to upload
        Intent i = getIntent();        
        ArrayList<String> instanceDirs = i.getStringArrayListExtra(FormEntryActivity.KEY_INSTANCES);
        if (instanceDirs == null) {
            // nothing to upload
            return;
        }

        // get the task if we've changed orientations. If it's null it's a new upload.
        mInstanceUploaderTask = (InstanceUploaderTask) getLastNonConfigurationInstance();        
        if (mInstanceUploaderTask == null) {
            SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            
            String userEmail = settings.getString(ServerPreferences.KEY_USER_EMAIL, null);

            UploadArgs argSet = new UploadArgs();
            argSet.instances = instanceDirs;
            argSet.hosts = new HashSet<String>();
            argSet.userEmail = userEmail;

            boolean deferForPassword = false;
            if (userEmail != null && userEmail.length() != 0 ) {
                for (int ii = 0; ii < instanceDirs.size(); ii++) {
                    FormInstanceDocument instance = null;
                    String urlString = null;
                    
                    try {
                        instance = Collect.getInstance().getDbService().getDb().get(FormInstanceDocument.class, instanceDirs.get(ii));                        
                        urlString = instance.getOdkSubmissionUri();
                        
                        URL url = new URL(urlString);
                        URI uri = url.toURI();
                        String host = uri.getHost();

                        if ( !WebUtils.hasCredentials(userEmail, host) ) {
                            argSet.hosts.add(host);
                        }                        
                    } catch (DbAccessException e) {
                        Log.w(Collect.LOGTAG, t + e.toString());
                    } catch ( MalformedURLException e ) {
                        e.printStackTrace();
                        Log.e(t, "Invalid url: " + urlString + " for submission " + instance.getId());
                    } catch (URISyntaxException e ) {
                        e.printStackTrace();
                        Log.e(t, "Invalid uri: " + urlString + " for submission " + instance.getId());
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        Log.e(t, "Invalid uri: " + ((urlString == null) ? "null" : urlString) +
                                " for submission " + instance.getId());
                    }                
                }

                // OK. we have the list of distinct hosts...
                if ( !argSet.hosts.isEmpty() ) {
                    deferForPassword = true;
                    launchPasswordDialog(argSet);
                }
            }

            if ( !deferForPassword ) {
                executeUpload(instanceDirs);
            }
        }
    }

    
    private void launchPasswordDialog( UploadArgs args ) {
        if ( args.hosts.isEmpty() ) {
            executeUpload(args.instances);
            return;
        }

        String h = args.hosts.iterator().next();
        args.hosts.remove(h);

        PasswordPromptDialogBuilder b = 
            new PasswordPromptDialogBuilder(this, args.userEmail, h, new OnOkListener() {
                        @Override
                        public void onOk(Object okListenerContext) {
                            UploadArgs args = (UploadArgs) okListenerContext;
                            InstanceUploaderActivity.this.launchPasswordDialog(args);
                        }
                    }, args);
        b.show();
    }
    
    
    private void executeUpload(ArrayList<String> instanceDirs) {
        mInstanceUploaderTask = new InstanceUploaderTask();
        mInstanceUploaderTask.setUploaderListener(this);
        
        // setup dialog and upload task
        showDialog(PROGRESS_DIALOG);

        totalCount = instanceDirs.size();

        // convert array list to an array
        String[] sa = instanceDirs.toArray(new String[totalCount]);
        mInstanceUploaderTask.execute(sa);
    }

    
    // TODO: if uploadingComplete() when activity backgrounded, won't work.
    // just check task status in onResume
    @Override
	public void uploadingComplete(ArrayList<InstanceUploaderListener.UploadOutcome> result) {
        int failureCount = 0;
        for ( UploadOutcome o : result ) {
            if ( !o.isSuccessful ) {
                ++failureCount;
            }
        }
        boolean success = false;        
        if (failureCount == 0) {
            Toast.makeText(this, getString(R.string.upload_all_successful, totalCount),
                    Toast.LENGTH_SHORT).show();
            
            success = true;
        } else {
            String s = getString(R.string.of, failureCount, totalCount);
            Toast.makeText(this, getString(R.string.upload_some_failed, s), Toast.LENGTH_LONG)
                    .show();
        }

        // for each path, update the status
        for ( UploadOutcome o : result ) {
            try {
                FormInstanceDocument iDoc = Collect.getInstance().getDbService().getDb().get(FormInstanceDocument.class, o.instanceDir);
                iDoc.setOdkSubmissionDate(GenericDocument.generateTimestamp());
                
                if (o.isSuccessful) {
                    if (o.notAllFilesUploaded) {
                        iDoc.setOdkSubmissionStatus(FormInstanceDocument.OdkSubmissionStatus.partial);
                    } else {
                        iDoc.setOdkSubmissionStatus(FormInstanceDocument.OdkSubmissionStatus.complete);
                    }
                    
                    iDoc.setOdkSubmissionResultMsg(null);
                } else {
                    iDoc.setOdkSubmissionStatus(FormInstanceDocument.OdkSubmissionStatus.failed);
                    
                    if (o.errorMessage != null) {
                        iDoc.setOdkSubmissionResultMsg(o.errorMessage);
                    }
                }
                
                Collect.getInstance().getDbService().getDb().update(iDoc);
            } catch (DbAccessException e) {
                Log.e(Collect.LOGTAG, t + "unable to update ODK upload outcome for " + o.instanceDir);
            } catch (Exception e) {
                Log.e(Collect.LOGTAG, t + "unhandled exception while updating ODK upload outcome for " + o.instanceDir);
                e.printStackTrace();
            }
        }
        
        Intent in = new Intent();
        in.putExtra(FormEntryActivity.KEY_SUCCESS, success);
        setResult(RESULT_OK, in);
        
        finish();
    }

    
    @Override
	public void progressUpdate(int progress, int total) {
        mProgressDialog.setMessage(getString(R.string.sending_items, progress, total));
    }

    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);                
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
						public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mInstanceUploaderTask.setUploaderListener(null);
                            finish();
                        }
                    };                    
                mProgressDialog.setTitle(getString(R.string.uploading_data));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);                
                return mProgressDialog;
        }        
        return null;
    }
    

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        totalCount = savedInstanceState.getInt(KEY_TOTALCOUNT);
    }

    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TOTALCOUNT, totalCount);
    }

    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mInstanceUploaderTask;
    }

    
    @Override
    protected void onDestroy() {
    	if ( mInstanceUploaderTask != null ) {
    		mInstanceUploaderTask.setUploaderListener(null);
    	}
        super.onDestroy();
    }

    
    @Override
    protected void onResume() {
        if (mInstanceUploaderTask != null) {
            mInstanceUploaderTask.setUploaderListener(this);
        }        
        super.onResume();
    }
    
}
