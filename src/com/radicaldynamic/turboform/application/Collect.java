package com.radicaldynamic.turboform.application;

import java.util.ArrayList;

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

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.logic.FileReferenceFactory;
import com.radicaldynamic.turboform.services.CouchDbService;
import com.radicaldynamic.turboform.xform.Bind;
import com.radicaldynamic.turboform.xform.Field;
import com.radicaldynamic.turboform.xform.Instance;
import com.radicaldynamic.turboform.xform.Translation;

public class Collect extends Application {
    public final static String LOGTAG = "TurboForm";
    public static CouchDbService mDb = null;
	private static Collect singleton = null;
	
	private FormEntryController formEntryController = null;
    private FileReferenceFactory factory = null;
    private IBinder viewToken = null;
    
    private boolean firstReferenceInitialization = true;
    
    private ArrayList<String> instanceBrowseList = new ArrayList<String>();
    
    private FormDocument fbForm                    = null;
    private ArrayList<Bind> fbBindState            = null;
    private ArrayList<Field> fbFieldState          = null;
    private ArrayList<Instance> fbInstanceState    = null;
    private ArrayList<Translation> fbTranslationState = null;  
    private Field fbField                          = null;
    private Instance fbInstance                    = null;
    private ArrayList<Field> fbItemList            = null;

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
	
    public void setInstanceBrowseList(ArrayList<String> instanceBrowseList) { this.instanceBrowseList = instanceBrowseList; }
    public ArrayList<String> getInstanceBrowseList() { return instanceBrowseList; }

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
}
