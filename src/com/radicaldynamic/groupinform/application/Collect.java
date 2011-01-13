package com.radicaldynamic.groupinform.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.form.api.FormEntryController;

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
import com.radicaldynamic.groupinform.documents.FormDocument;
import com.radicaldynamic.groupinform.logic.AccountDevice;
import com.radicaldynamic.groupinform.logic.FileReferenceFactory;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.services.CouchDbService;
import com.radicaldynamic.groupinform.xform.Bind;
import com.radicaldynamic.groupinform.xform.Field;
import com.radicaldynamic.groupinform.xform.Instance;
import com.radicaldynamic.groupinform.xform.Translation;

public class Collect extends Application {
    public final static String LOGTAG = "Inform";
	
	// Things from upstream
	private static Collect singleton = null;
	private FormEntryController formEntryController = null;
    private FileReferenceFactory factory = null;
    private IBinder viewToken = null;    
    private boolean firstReferenceInitialization = true;
    
    // The database connection
    public static CouchDbService mDb = null;
    
    // Current registration state of this device
    private InformOnlineState informOnline = new InformOnlineState();
    
    // Lookup map for account devices, indexed by device ID
    private Map<String, AccountDevice> accountDevicesMap = new HashMap<String, AccountDevice>();
    private Map<String, AccountDevice> accountDevicesSyncMap = Collections.synchronizedMap(accountDevicesMap);
    
    // Instance browse list, used for switching between instances of a given form
    private ArrayList<String> instanceBrowseList = new ArrayList<String>();
    
    /*
     * State variables for the form builder
     * TODO: it would be nice to refactor the form builder so we don't need these here
     */
    private FormDocument fbForm = null;
    private ArrayList<Bind> fbBindState = null;
    private ArrayList<Field> fbFieldState = null;
    private ArrayList<Instance> fbInstanceState = null;
    private ArrayList<Translation> fbTranslationState = null;  
    private Field fbField = null;
    private Instance fbInstance = null;
    private ArrayList<Field> fbItemList = null;

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

	private void showCustomToast(String message) 
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
	
    public void setAccountDevices(Map<String, AccountDevice> accountDevices) { this.accountDevicesSyncMap = accountDevices; }
    public Map<String, AccountDevice> getAccountDevices() { return accountDevicesSyncMap; }

    public void setFbTranslationState(ArrayList<Translation> fbTranslationState) { this.fbTranslationState = fbTranslationState; }
    public ArrayList<Translation> getFbTranslationState() { return fbTranslationState; }
    
    public void setFbFieldState(ArrayList<Field> fbFieldState) { this.fbFieldState = fbFieldState; }
    public ArrayList<Field> getFbFieldState() { return fbFieldState; }
    
    public void setFbBindState(ArrayList<Bind> fbBindState) { this.fbBindState = fbBindState; }
    public ArrayList<Bind> getFbBindState() { return fbBindState; }

    public void setFbInstanceState(ArrayList<Instance> fbInstanceState) { this.fbInstanceState = fbInstanceState; }
    public ArrayList<Instance> getFbInstanceState() { return fbInstanceState; }

    public void setFbForm(FormDocument fbForm) { this.fbForm = fbForm; }
    public FormDocument getFbForm() { return fbForm; }

    public void setFbField(Field fbField) { this.fbField = fbField; }
    public Field getFbField() { return fbField; }

    public void setFbInstance(Instance fbInstance) { this.fbInstance = fbInstance; }
    public Instance getFbInstance() { return fbInstance; }

    public void setFbItemList(ArrayList<Field> fbItemList) { this.fbItemList = fbItemList; }
    public ArrayList<Field> getFbItemList() { return fbItemList; }

    public void setInformOnline(InformOnlineState informOnline) { this.informOnline = informOnline; }
    public InformOnlineState getInformOnline() { return informOnline; }

    public void setInstanceBrowseList(ArrayList<String> instanceBrowseList) { this.instanceBrowseList = instanceBrowseList; }
    public ArrayList<String> getInstanceBrowseList() { return instanceBrowseList; }
}
