/*
 * Copyright (C) 2011 University of Washington
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
package com.radicaldynamic.groupinform.application;

import java.io.File;

import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.javarosa.form.api.FormEntryController;
import org.odk.collect.android.utilities.AgingCredentialsProvider;

import android.app.Application;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.xform.FormBuilderState;

/**
 * Extends the Application class to implement 
 * @author carlhartung
 *
 */
public class Collect extends Application {
    public final static String LOGTAG = "Inform";
    
    // Storage paths
    public static final String ODK_ROOT = Environment.getExternalStorageDirectory() + "/odk";
    public static final String FORMS_PATH = ODK_ROOT + "/forms";
    public static final String INSTANCES_PATH = ODK_ROOT + "/instances";
    public static final String CACHE_PATH = ODK_ROOT + "/.cache";
    public static final String METADATA_PATH = ODK_ROOT + "/metadata";
    public static final String TMPFILE_PATH = CACHE_PATH + "/tmp.jpg";
    
    public static final String DEFAULT_FONTSIZE = "21";
	
	// Things from upstream
    private HttpContext localContext = null;
	private static Collect singleton = null;
	
	private FormEntryController formEntryController = null;
    private IBinder viewToken = null;    
    
    // Service connections
    private ServiceConnection couchService = null;
    private DatabaseService dbService = null;
    private InformOnlineService ioService = null;
    
    // Current registration state of this device
    private InformOnlineState informOnlineState = new InformOnlineState();

    // State container for the form builder
    private FormBuilderState formBuilderState = new FormBuilderState();


    public static Collect getInstance()
    {
        return singleton;
    }
    

    /**
     * Creates required directories on the SDCard (or other external storage)
     * @throws RuntimeException if there is no SDCard or the directory exists as a non directory
     */
    public static void createODKDirs() throws RuntimeException {
        String cardstatus = Environment.getExternalStorageState();
        if (cardstatus.equals(Environment.MEDIA_REMOVED)
                || cardstatus.equals(Environment.MEDIA_UNMOUNTABLE)
                || cardstatus.equals(Environment.MEDIA_UNMOUNTED)
                || cardstatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
                || cardstatus.equals(Environment.MEDIA_SHARED)) {
            RuntimeException e =
                new RuntimeException("ODK reports :: SDCard error: "
                        + Environment.getExternalStorageState());
            throw e;
        }

        String[] dirs = {
                ODK_ROOT, FORMS_PATH, INSTANCES_PATH, CACHE_PATH, METADATA_PATH
        };

        for (String dirName : dirs) {
            File dir = new File(dirName);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    RuntimeException e =
                        new RuntimeException("ODK reports :: Cannot create directory: " + dirName);
                    throw e;
                }
            } else {
                if (!dir.isDirectory()) {
                    RuntimeException e =
                        new RuntimeException("ODK reports :: " + dirName
                                + " exists, but is not a directory");
                    throw e;
                }
            }
        }
    }
    
    
    /**
     * Shared HttpContext so a user doesn't have to re-enter login information
     * @return
     */
    public synchronized HttpContext getHttpContext() {
        if (localContext == null) {
            // set up one context for all HTTP requests so that authentication
            // and cookies can be retained.
            localContext = new SyncBasicHttpContext(new BasicHttpContext());

            // establish a local cookie store for this attempt at downloading...
            CookieStore cookieStore = new BasicCookieStore();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // and establish a credentials provider.  Default is 7 minutes.
            CredentialsProvider credsProvider = new AgingCredentialsProvider(7 * 60 * 1000);
            localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
        }
        return localContext;
    }
    

    @Override
    public void onCreate() {
        singleton = this;
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        super.onCreate();        
    }

    
	public void setFormEntryController(FormEntryController formEntryController) 
	{
		this.formEntryController = formEntryController;
	}
	
	public FormEntryController getFormEntryController() 
	{
		return formEntryController;
	}	

    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    public void showSoftKeyboard(View v)
    {
        InputMethodManager inputManager = (InputMethodManager) getBaseContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        IBinder b = v.getWindowToken();
        if (viewToken != null && !viewToken.equals(b)) {
            inputManager.hideSoftInputFromInputMethod(viewToken, 0);
        }

        if (inputManager.isActive(v))
            return;
        inputManager.showSoftInput(v, 0);
        viewToken = b;
    }

    public void hideSoftKeyboard(View c)
    {
        InputMethodManager inputManager = (InputMethodManager) getBaseContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (viewToken != null) {
            inputManager.hideSoftInputFromWindow(viewToken, 0);
        }
        viewToken = null;

        if (c != null) {
            if (inputManager.isActive()) {
                inputManager.hideSoftInputFromWindow(c
                        .getApplicationWindowToken(), 0);
            }
        }
    }

	public void createConstraintToast(String constraintText, int saveStatus) 
	{
		switch (saveStatus) {
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
			if (constraintText == null) {
				constraintText = getString(R.string.invalid_answer_error);
			}
			break;
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
			constraintText = getString(R.string.required_answer_error);
			break;
		}

		showCustomToast(constraintText);
	}

	public void showCustomToast(String message) 
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.toast_view, null);

		// Set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	
    public void setCouchService(ServiceConnection couchService) { this.couchService = couchService; }
    public ServiceConnection getCouchService() { return couchService; }

    public void setDbService(DatabaseService dbService) { this.dbService = dbService; }
    public DatabaseService getDbService() { return dbService; }

    public void setInformOnlineState(InformOnlineState informOnlineState) { this.informOnlineState = informOnlineState; }
    public InformOnlineState getInformOnlineState() { return informOnlineState; }

    public void setIoService(InformOnlineService ioService) { this.ioService = ioService; }
    public InformOnlineService getIoService() { return ioService; }

	public void setFormBuilderState(FormBuilderState formBuilderState) { this.formBuilderState = formBuilderState; }
	public FormBuilderState getFormBuilderState() { return formBuilderState; }
}