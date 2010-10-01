package com.radicaldynamic.turboform.application;

import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.form.api.FormEntryController;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.logic.FileReferenceFactory;
import com.radicaldynamic.turboform.services.CouchDbService;

public class Collect extends Application {
    public final static String LOGTAG = "TurboForm";
    public static CouchDbService mDb = null;

	private static Collect singleton = null;
	
	public static Collect getInstance() {
		return singleton;
	}
	
	private FormEntryController formEntryController = null;

    private FileReferenceFactory factory = null;
    private boolean firstReferenceInitialization = true;

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
		// TODO Auto-generated method stub
		super.onCreate();
		singleton = this;
	}

	public void setFormEntryController( FormEntryController formEntryController) {
		this.formEntryController = formEntryController;
	}
	
	/**
	 * Returns the form entry controller, if there is one.
	 * 
	 * @return the fec or null
	 */
	public FormEntryController getFormEntryController() {
		return formEntryController;
	}


	public void registerMediaPath(String mediaPath) {
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
	public void createConstraintToast(String constraintText, int saveStatus) {
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

	private void showCustomToast(String message) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.toast_view, null);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
}
