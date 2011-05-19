package com.radicaldynamic.groupinform.application;

import java.util.ArrayList;

import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.form.api.FormEntryController;
import org.odk.collect.android.logic.FileReferenceFactory;
import org.odk.collect.android.utilities.AgingCredentialsProvider;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.couchdb.InformCouchService;
import com.radicaldynamic.groupinform.logic.InformDependencies;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.services.DatabaseService;
import com.radicaldynamic.groupinform.services.InformOnlineService;
import com.radicaldynamic.groupinform.xform.FormBuilderState;

public class Collect extends Application {
    public final static String LOGTAG = "Inform";
	
	// Things from upstream
    private HttpContext localContext = null;
	private static Collect singleton = null;
	private FormEntryController formEntryController = null;
    private FileReferenceFactory factory = null;
    private IBinder viewToken = null;    
    private boolean firstReferenceInitialization = true;
    
    // Service connections
    private InformCouchService couchService = null; 
    private DatabaseService dbService = null;
    private InformOnlineService ioService = null;
    
    // Current registration state of this device
    private InformOnlineState informOnlineState = new InformOnlineState();
    
    // Installed dependency state
    private InformDependencies informDependencies = new InformDependencies();

    // Instance browse list, used for switching between instances of a given form
    private ArrayList<String> instanceBrowseList = new ArrayList<String>();

    // State container for the form builder
    private FormBuilderState formBuilderState = new FormBuilderState();

	/* (non-Javadoc)
	 * @see android.app.Application#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}
	
    public static Collect getInstance()
    {
        return singleton;
    }
    
    public synchronized HttpContext getHttpContext() {
        if (localContext == null) {
            // set up one context for all HTTP requests so that authentication
            // and cookies can be retained.
            localContext = new SyncBasicHttpContext(new BasicHttpContext());

            // establish a local cookie store for this attempt at downloading...
            CookieStore cookieStore = new BasicCookieStore();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // and establish a credentials provider...
            CredentialsProvider credsProvider = new AgingCredentialsProvider(7 * 60 * 1000);
            localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
        }
        return localContext;
    }

	public void setFormEntryController(FormEntryController formEntryController) 
	{
		this.formEntryController = formEntryController;
	}
	
	public FormEntryController getFormEntryController() 
	{
		return formEntryController;
	}	

	public void registerMediaPath(String mediaPath)
	{
	    Log.d(LOGTAG, "Registering media path " + mediaPath);
	    
	    if (factory != null) {
    		ReferenceManager._().removeReferenceFactory(factory);
        }               
        
    	factory = new FileReferenceFactory(mediaPath);
        ReferenceManager._().addReferenceFactory(factory);
        
    	if (firstReferenceInitialization) {
    		firstReferenceInitialization = false;
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://images/", "jr://file/"));
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://audio/", "jr://file/"));
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://video/", "jr://file/"));
        }
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

    public void setCouchService(InformCouchService couchService) { this.couchService = couchService; }
    public InformCouchService getCouchService() { return couchService; }    

    public void setDbService(DatabaseService dbService) { this.dbService = dbService; }
    public DatabaseService getDbService() { return dbService; }

    public void setInformDependencies(InformDependencies informDependencies) { this.informDependencies = informDependencies; }
    public InformDependencies getInformDependencies() { return informDependencies; }

    public void setInformOnlineState(InformOnlineState informOnlineState) { this.informOnlineState = informOnlineState; }
    public InformOnlineState getInformOnlineState() { return informOnlineState; }

    public void setInstanceBrowseList(ArrayList<String> instanceBrowseList) { this.instanceBrowseList = instanceBrowseList; }
    public ArrayList<String> getInstanceBrowseList() { return instanceBrowseList; }

    public void setIoService(InformOnlineService ioService) { this.ioService = ioService; }
    public InformOnlineService getIoService() { return ioService; }

	public void setFormBuilderState(FormBuilderState formBuilderState) { this.formBuilderState = formBuilderState; }
	public FormBuilderState getFormBuilderState() { return formBuilderState; }
}