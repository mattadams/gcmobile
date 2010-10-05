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

package com.radicaldynamic.turboform.activities;

import java.io.File;
import java.util.regex.Pattern;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormsModule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.documents.InstanceDocument;
import com.radicaldynamic.turboform.listeners.FormLoaderListener;
import com.radicaldynamic.turboform.listeners.FormSavedListener;
import com.radicaldynamic.turboform.logic.PropertyManager;
import com.radicaldynamic.turboform.tasks.FormLoaderTask;
import com.radicaldynamic.turboform.tasks.SaveToDiskTask;
import com.radicaldynamic.turboform.utilities.FileUtils;
import com.radicaldynamic.turboform.utilities.GestureDetector;
import com.radicaldynamic.turboform.views.AbstractFolioView;
import com.radicaldynamic.turboform.views.GroupView;
import com.radicaldynamic.turboform.views.QuestionView;
import com.radicaldynamic.turboform.views.layout.GroupLayoutFactory;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormEntryActivity extends Activity implements AnimationListener,
        FormLoaderListener, FormSavedListener
{
    private static final String t = "FormEntryActivity";

    // Request codes for returning data from specified intent
    public static final int IMAGE_CAPTURE = 1;
    public static final int BARCODE_CAPTURE = 2;
    public static final int AUDIO_CAPTURE = 3;
    public static final int VIDEO_CAPTURE = 4;
    public static final int LOCATION_CAPTURE = 5;

    public static final String LOCATION_RESULT = "LOCATION_RESULT";

    // Identifies the location of the form used to launch form entry
    public static final String KEY_FORMID = "formpath";
    public static final String KEY_FORMPATH = "formpath";
    public static final String KEY_INSTANCEID = "instancepath";
    public static final String KEY_INSTANCEPATH = "instancepath";
    public static final String KEY_INSTANCES = "instances";
    public static final String KEY_SUCCESS = "success";

    // Identifies whether this is a new form, or reloading a form after a screen
    // rotation (or similar)
    private static final String NEWFORM = "newform";

    private static final int MENU_CLEAR = Menu.FIRST;
    private static final int MENU_DELETE_REPEAT = Menu.FIRST + 1;
    private static final int MENU_LANGUAGES = Menu.FIRST + 2;
    private static final int MENU_HIERARCHY_VIEW = Menu.FIRST + 3;
    private static final int MENU_SAVE = Menu.FIRST + 4;

    private static final int PROGRESS_DIALOG = 1;
    private static final int SAVING_DIALOG = 2;

    // TODO: Uncomment when ProgressBar slow down is fixed.
    // private ProgressBar mProgressBar;

    private String mFormId = null;
    private String mInstanceId = null;

    private String mInstancePath;

    public FormEntryModel mFormEntryModel;

    private GestureDetector mGestureDetector;
    private Animation mInAnimation;
    private Animation mOutAnimation;

    private Handler mHandler;
    private RelativeLayout mRelativeLayout;
    private View mCurrentView;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;

    // Used to limit forward/backward swipes to one per question
    private boolean mBeenSwiped;

    private CheckBox mInstanceComplete;

    private FormLoaderTask mFormLoaderTask;
    private SaveToDiskTask mSaveToDiskTask;

    enum AnimationType {
        LEFT, RIGHT, FADE
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.form_entry);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.loading_form));

        // Create handler for IFolioView UI updates from model
        mHandler = new Handler(Collect.getInstance().getMainLooper());

        // mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.rl);

        mBeenSwiped = false;
        mAlertDialog = null;
        mCurrentView = null;
        mInAnimation = null;
        mOutAnimation = null;
        mGestureDetector = new GestureDetector();

        // Load JavaRosa modules, needed to restore forms
        new XFormsModule().registerModule();

        // Needed to override RMS property manager
        org.javarosa.core.services.PropertyManager.setPropertyManager(new PropertyManager(getApplicationContext()));

        Boolean newForm = true;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_FORMID)) {
                mFormId = savedInstanceState.getString(KEY_FORMID);
            }

            if (savedInstanceState.containsKey(KEY_INSTANCEID)) {
                mInstanceId = savedInstanceState.getString(KEY_INSTANCEID);    
                mInstancePath = FileUtils.CACHE_PATH + mInstanceId + ".";
            }

            if (savedInstanceState.containsKey(NEWFORM)) {
                newForm = savedInstanceState.getBoolean(NEWFORM, true);
            }
        }

        // Check to see if this is a screen flip or a new form load
        Object data = getLastNonConfigurationInstance();

        if (data instanceof FormLoaderTask) {
            mFormLoaderTask = (FormLoaderTask) data;
        } else if (data instanceof SaveToDiskTask) {
            mSaveToDiskTask = (SaveToDiskTask) data;
        } else if (data == null) {
            FormEntryController fec = Collect.getInstance().getFormEntryController();

            if (fec != null && !newForm) {
                mFormEntryModel = fec.getModel();
                refreshCurrentView();
                return;
            }

            // Not a restart from a screen orientation change (or other)
            Collect.getInstance().setFormEntryController(null);

            Intent intent = getIntent();

            if (intent != null) {
                mFormId = intent.getStringExtra(KEY_FORMID);
                mInstanceId = intent.getStringExtra(KEY_INSTANCEID);
                mFormLoaderTask = new FormLoaderTask();
                mFormLoaderTask.execute(mFormId, mInstanceId);
                showDialog(PROGRESS_DIALOG);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        /**
         * Unregister the existing current view from the underlying model.
         * Otherwise, the model will retain references to the visited Views, and
         * we'll burn memory.
         */
        if (mCurrentView != null && mCurrentView instanceof AbstractFolioView) {
            ((AbstractFolioView) mCurrentView).unregister();
        }
    
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the
            // FEC in memory.
            mFormLoaderTask.cancel(true);
            mFormLoaderTask.destroy();
        }
    
        if (mSaveToDiskTask != null) {
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the
            // FEC in memory.
            mSaveToDiskTask.cancel(false);
            mSaveToDiskTask.setFormSavedListener(null);
        }
    
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        dismissDialogs();
        super.onPause();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(this);
    
            if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                dismissDialog(PROGRESS_DIALOG);
                refreshCurrentView();
            }
        }
    
        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(this);
        }
    
        super.onResume();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FORMID, mFormId);
        outState.putString(KEY_INSTANCEID, mInstanceId);
        outState.putBoolean(NEWFORM, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    
        if (resultCode == RESULT_CANCELED) {
            // request was cancelled, so do nothing
            return;
        }
    
        switch (requestCode) {
        case BARCODE_CAPTURE:
            String sb = intent.getStringExtra("SCAN_RESULT");
            ((AbstractFolioView) mCurrentView).setBinaryData(sb);
            saveCurrentAnswer(false);
            break;
        case IMAGE_CAPTURE:
            /*
             * We saved the image to the tempfile_path but we really want it to
             * be in INSTANCES_PATH + [current instance]/something.jpg so we
             * move it there before inserting it into the content provider.
             */
            File fi = new File(FileUtils.TMPFILE_PATH);            
    
            // Add the new image to the Media content provider so that the viewing is fast in Android 2.0+
            ContentValues values = new ContentValues(6);
            values.put(Images.Media.TITLE, fi.getName());
            values.put(Images.Media.DISPLAY_NAME, fi.getName());
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(Images.Media.MIME_TYPE, "image/jpeg");
            values.put(Images.Media.DATA, fi.getAbsolutePath());
    
            Uri imageuri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    
            Log.i(t, "URI returned by image insertion: " + imageuri.toString());
            
            ((AbstractFolioView) mCurrentView).setBinaryData(imageuri);
            saveCurrentAnswer(false);
            refreshCurrentView();
            break;        
        case AUDIO_CAPTURE:
        case VIDEO_CAPTURE:
            Uri um = intent.getData();
            ((AbstractFolioView) mCurrentView).setBinaryData(um);
            saveCurrentAnswer(false);
            refreshCurrentView();
            break;
        case LOCATION_CAPTURE:
            String sl = intent.getStringExtra(LOCATION_RESULT);
            ((AbstractFolioView) mCurrentView).setBinaryData(sl);
            saveCurrentAnswer(false);
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#dispatchTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent mv)
    {
        boolean handled = onTouchEvent(mv);

        if (!handled) {
            return super.dispatchTouchEvent(mv);
        }

        // This is always true
        return handled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id) {
        case PROGRESS_DIALOG:
            mProgressDialog = new ProgressDialog(this);

            DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    mFormLoaderTask.setFormLoaderListener(null);
                    mFormLoaderTask.cancel(true);
                    finish();
                }
            };

            mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
            mProgressDialog.setTitle(getString(R.string.loading_form));
            mProgressDialog.setMessage(getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(getString(R.string.cancel_loading_form),
                    loadingButtonListener);
            return mProgressDialog;
        case SAVING_DIALOG:
            mProgressDialog = new ProgressDialog(this);

            DialogInterface.OnClickListener savingButtonListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    mSaveToDiskTask.setFormSavedListener(null);
                    mSaveToDiskTask.cancel(true);
                }
            };

            mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
            mProgressDialog.setTitle(getString(R.string.saving_form));
            mProgressDialog.setMessage(getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(getString(R.string.cancel),
                    savingButtonListener);
            mProgressDialog.setButton(getString(R.string.cancel_saving_form),
                    savingButtonListener);
            return mProgressDialog;

        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            createQuitDialog();
            return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (event.isAltPressed() && !mBeenSwiped) {
                mBeenSwiped = true;
                showNextView();
                return true;
            }
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (event.isAltPressed() && !mBeenSwiped) {
                mBeenSwiped = true;
                showPreviousView();
                return true;
            }
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.animation.Animation.AnimationListener#onAnimationEnd(android
     * .view.animation.Animation)
     */
    @Override
    public void onAnimationEnd(Animation arg0)
    {
        mBeenSwiped = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.animation.Animation.AnimationListener#onAnimationRepeat(
     * android.view.animation.Animation)
     */
    @Override
    public void onAnimationRepeat(Animation animation)
    {
        // Added by AnimationListener interface.
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.animation.Animation.AnimationListener#onAnimationStart(android
     * .view.animation.Animation)
     */
    @Override
    public void onAnimationStart(Animation animation)
    {
        // Added by AnimationListener interface.
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_LANGUAGES:
            createLanguageDialog();
            return true;
        case MENU_CLEAR:
            createClearDialog();
            return true;
        case MENU_DELETE_REPEAT:
            createDeleteRepeatConfirmDialog();
            return true;
        case MENU_SAVE:
            // Don't exit
            saveDataToDisk(false, isInstanceComplete());
            return true;
        case MENU_HIERARCHY_VIEW:
            if (currentPromptIsQuestion()) {
                saveCurrentAnswer(false);
            }
    
            Intent i = new Intent(this, FormHierarchyActivity.class);
            startActivity(i);
        }
    
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.removeItem(MENU_CLEAR);
        menu.removeItem(MENU_DELETE_REPEAT);
        menu.removeItem(MENU_LANGUAGES);
        menu.removeItem(MENU_HIERARCHY_VIEW);
        menu.removeItem(MENU_SAVE);
    
        menu.add(0, MENU_SAVE, 0, getString(R.string.save_all_answers))
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CLEAR, 0, getString(R.string.clear_answer)).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel).setEnabled(
                !mFormEntryModel.isIndexReadonly() ? true : false);
        menu.add(0, MENU_DELETE_REPEAT, 0, getString(R.string.delete_repeat))
                .setIcon(R.drawable.ic_menu_clear_playlist).setEnabled(
                        indexContainsRepeatableGroup(mFormEntryModel
                                .getFormIndex()) ? true : false);
        menu.add(0, MENU_HIERARCHY_VIEW, 0, getString(R.string.view_hierarchy))
                .setIcon(R.drawable.ic_menu_goto);
        menu
                .add(0, MENU_LANGUAGES, 0, getString(R.string.change_language))
                .setIcon(R.drawable.ic_menu_start_conversation)
                .setEnabled(
                        (mFormEntryModel.getLanguages() == null || mFormEntryModel
                                .getLanguages().length == 1) ? false : true);
    
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance() If we're
     * loading, then we pass the loading thread to our next instance.
     */
    @Override
    public Object onRetainNonConfigurationInstance()
    {
        // if a form is loading, pass the loader task
        if (mFormLoaderTask != null
                && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
            return mFormLoaderTask;
    
        // if a form is writing to disk, pass the save to disk task
        if (mSaveToDiskTask != null
                && mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSaveToDiskTask;
    
        // mFormEntryController is static so we don't need to pass it.
        if (Collect.getInstance().getFormEntryController() != null
                && currentPromptIsQuestion()) {
            saveCurrentAnswer(false);
        }
    
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        /*
         * constrain the user to only be able to swipe (that causes a view
         * transition) once per screen with the mBeenSwiped variable.
         */
        boolean handled = false;
    
        if (!mBeenSwiped) {
            switch (mGestureDetector.getGesture(motionEvent)) {
            case SWIPE_RIGHT:
                mBeenSwiped = true;
                showPreviousView();
                handled = true;
                break;
            case SWIPE_LEFT:
                mBeenSwiped = true;
                showNextView();
                handled = true;
                break;
            }
        }
    
        return handled;
    }

    /**
     * loadingComplete() is called by FormLoaderTask once it has finished
     * loading a form.
     */
    @Override
    public void loadingComplete(FormEntryController fec)
    {
        dismissDialog(PROGRESS_DIALOG);

        if (fec == null) {
            createErrorDialog(getString(R.string.load_error, Collect.mDb
                    .getDb().get(FormDocument.class, mFormId).getName()), true);
        } else {
            Collect.getInstance().setFormEntryController(fec);
            mFormEntryModel = fec.getModel();

            // Initialize new instance document
            if (mInstanceId == null) {
                InstanceDocument instance = new InstanceDocument();
                instance.setForm(mFormId);
                Collect.mDb.getDb().create(instance);
                mInstanceId = instance.getId();
                mInstancePath = FileUtils.CACHE_PATH + mInstanceId + ".";
            } else {
                mInstancePath = FileUtils.CACHE_PATH + mInstanceId + ".";
                
                // We've just loaded a saved form, so start in the hierarchy view
                Intent i = new Intent(this, FormHierarchyActivity.class);
                startActivity(i);

                // So we don't show the introduction screen before jumping to
                // the hierarchy
                return;
            }

            refreshCurrentView();
        }
    }

    /**
     * Refreshes the current view. the controller and the displayed view can get
     * out of sync due to dialogs and restarts caused by screen orientation
     * changes, so they're resynchronised here.
     */
    public void refreshCurrentView()
    {
        int event = mFormEntryModel.getEvent();
    
        // When we refresh, if we're at a repeat prompt then step back to the
        // last question
        while (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT
                || event == FormEntryController.EVENT_GROUP
                || event == FormEntryController.EVENT_REPEAT) {
            if (currentPromptIsGroupFolio())
                break;
    
            event = stepToPreviousEvent();
        }
    
        // And reset index to the containing folio
        FormIndex index = mFormEntryModel.getFormIndex();
        event = jumpToContainingFolio(index);
    
        Log.e(t, "refreshing view for event: " + event);
    
        View current = createView(event, index);
        showView(current, AnimationType.FADE);
    }

    @Override
    public void savingComplete(int saveStatus)
    {
        dismissDialog(SAVING_DIALOG);

        switch (saveStatus) {
        case SaveToDiskTask.SAVED:
            Toast.makeText(getApplicationContext(),
                    getString(R.string.data_saved_ok), Toast.LENGTH_SHORT)
                    .show();
            break;
        case SaveToDiskTask.SAVED_AND_EXIT:
            Toast.makeText(getApplicationContext(),
                    getString(R.string.data_saved_ok), Toast.LENGTH_SHORT)
                    .show();
            finish();
            break;
        case SaveToDiskTask.SAVE_ERROR:
            Toast.makeText(getApplicationContext(),
                    getString(R.string.data_saved_error), Toast.LENGTH_LONG)
                    .show();
            break;
        case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
        case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
            // Form index will be of the offending constraint
            refreshCurrentView();
            Toast.makeText(getApplicationContext(),
                    getString(R.string.data_saved_error), Toast.LENGTH_LONG)
                    .show();
            break;
        }
    }

    /**
     * Displays the View specified by the parameter 'next', animating both the
     * current view and next appropriately given the AnimationType. Also updates
     * the progress bar.
     */
    public void showView(View next, AnimationType from)
    {
        switch (from) {
        case RIGHT:
            mInAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.push_left_in);
            mOutAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.push_left_out);
            break;
        case LEFT:
            mInAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.push_right_in);
            mOutAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.push_right_out);
            break;
        case FADE:
            mInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            mOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            break;
        }
    
        if (mCurrentView != null) {
            mCurrentView.startAnimation(mOutAnimation);
            mRelativeLayout.removeView(mCurrentView);
        }
    
        mInAnimation.setAnimationListener(this);
    
        // We must call setMax() first because it doesn't redraw the progress
        // bar.
    
        // UnComment to make progress bar work.
        // WARNING: will currently slow large forms considerably
        // TODO: make the progress bar fast. Must be done in javarosa.
        // mProgressBar.setMax(mFormEntryModel.getTotalRelevantQuestionCount());
        // mProgressBar.setProgress(mFormEntryModel.getCompletedRelevantQuestionCount());
    
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        // lp.addRule(RelativeLayout.ABOVE, R.id.progressbar);
    
        mCurrentView = next;
        mRelativeLayout.addView(mCurrentView, lp);
    
        mCurrentView.startAnimation(mInAnimation);
    
        if (mCurrentView instanceof AbstractFolioView)
            ((AbstractFolioView) mCurrentView).setFocus(this);
        else {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
                    0);
        }
    }

    /**
     * Clears the answer on the screen.
     */
    private void clearCurrentAnswer()
    {
        // since we have group and composite views, we have to ask the
        // view what the actual form index is...
        if (!(mCurrentView instanceof AbstractFolioView))
            return;
    
        ((AbstractFolioView) mCurrentView).clearAnswer(true);
    }

    /**
     * Confirm clear dialog
     */
    private void createClearDialog()
    {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_alert);
    
        mAlertDialog.setTitle(getString(R.string.clear_answer_ask));
    
        String question = mFormEntryModel.getQuestionPrompt().getLongText();
    
        if (question.length() > 50) {
            question = question.substring(0, 50) + "...";
        }
    
        mAlertDialog.setMessage(getString(R.string.clearanswer_confirm,
                question));
    
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i)
            {
                switch (i) {
                case DialogInterface.BUTTON1: // yes
                    clearCurrentAnswer();
                    saveCurrentAnswer(false);
                    break;
                case DialogInterface.BUTTON2: // no
                    break;
                }
            }
        };
    
        mAlertDialog.setCancelable(false);
        mAlertDialog
                .setButton(getString(R.string.discard_answer), quitListener);
        mAlertDialog.setButton2(getString(R.string.clear_answer_no),
                quitListener);
        mAlertDialog.show();
    }

    /**
     * Creates a confirm/cancel dialog for deleting repeats.
     */
    private void createDeleteRepeatConfirmDialog()
    {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_alert);
    
        String name = getLastRepeatedGroupName(getGroupsForCurrentIndex());
    
        int repeatcount = getLastRepeatedGroupRepeatCount(getGroupsForCurrentIndex());
    
        if (repeatcount != -1) {
            name += " (" + (repeatcount + 1) + ")";
        }
    
        mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
        mAlertDialog
                .setMessage(getString(R.string.delete_repeat_confirm, name));
    
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i)
            {
                switch (i) {
                case DialogInterface.BUTTON1: // yes
                    FormEntryController fec = Collect.getInstance()
                            .getFormEntryController();
                    FormIndex validIndex = fec.deleteRepeat();
                    jumpToContainingFolio(validIndex);
                    showPreviousView();
                    break;
                case DialogInterface.BUTTON2: // no
                    break;
                }
            }
        };
    
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.discard_group), quitListener);
        mAlertDialog.setButton2(getString(R.string.delete_repeat_no),
                quitListener);
        mAlertDialog.show();
    }

    // TODO: use managed dialogs when the bugs are fixed
    /*
     * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
     * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
     * (cupcake). We do use managed dialogs for our static loading
     * ProgressDialog. The main issue we noticed and are waiting to see fixed
     * is: onPrepareDialog() is not called after a screen orientation change.
     * http://code.google.com/p/android/issues/detail?id=1639
     */
    
    //
    
    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit)
    {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
    
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i)
            {
                switch (i) {
                case DialogInterface.BUTTON1:
                    if (shouldExit) {
                        finish();
                    }
                    break;
                }
            }
        };
    
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    /**
     * Creates and displays a dialog allowing the user to set the language for
     * the form.
     */
    private void createLanguageDialog()
    {
        final String[] languages = mFormEntryModel.getLanguages();
        int selected = -1;
    
        if (languages != null) {
            String language = mFormEntryModel.getLanguage();
            for (int i = 0; i < languages.length; i++) {
                if (language.equals(languages[i])) {
                    selected = i;
                }
            }
        }
    
        mAlertDialog = new AlertDialog.Builder(this)
                .setSingleChoiceItems(languages, selected,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton)
                            {
                                FormEntryController fec = Collect.getInstance()
                                        .getFormEntryController();
                                fec.setLanguage(languages[whichButton]);
                                dialog.dismiss();
    
                                if (currentPromptIsQuestion()) {
                                    saveCurrentAnswer(false);
                                }
    
                                refreshCurrentView();
                            }
                        }).setTitle(getString(R.string.change_language))
                .setNegativeButton(getString(R.string.do_not_change),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton)
                            {
                            }
                        }).create();
    
        mAlertDialog.show();
    }

    // TODO: use managed dialogs when the bugs are fixed
    /*
     * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
     * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
     * (cupcake). We do use managed dialogs for our static loading
     * ProgressDialog. The main issue we noticed and are waiting to see fixed
     * is: onPrepareDialog() is not called after a screen orientation change.
     * http://code.google.com/p/android/issues/detail?id=1639
     */
    
    //
    
    /**
     * Creates and displays a dialog asking the user if they'd like to create a
     * repeat of the current group.
     */
    private void createRepeatDialog()
    {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
    
        DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i)
            {
                switch (i) {
                case DialogInterface.BUTTON1: // yes, repeat
                    FormEntryController fec = Collect.getInstance()
                            .getFormEntryController();
                    fec.newRepeat();
                    showNextView();
                    break;
                case DialogInterface.BUTTON2: // no, no repeat
                    showNextView();
                    break;
                }
            }
        };
    
        if (getLastRepeatCount(getGroupsForCurrentIndex()) > 0) {
            mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_another_repeat,
                    getLastGroupText(getGroupsForCurrentIndex())));
            mAlertDialog.setButton(getString(R.string.add_another),
                    repeatListener);
            mAlertDialog.setButton2(getString(R.string.leave_repeat_yes),
                    repeatListener);
        } else {
            mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_repeat,
                    getLastGroupText(getGroupsForCurrentIndex())));
            mAlertDialog.setButton(getString(R.string.entering_repeat),
                    repeatListener);
            mAlertDialog.setButton2(getString(R.string.add_repeat_no),
                    repeatListener);
        }
    
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();
        mBeenSwiped = false;
    }

    private void createQuitDialog()
    {
        String[] items = { getString(R.string.do_not_save),
                getString(R.string.quit_entry), getString(R.string.do_not_exit) };
    
        mAlertDialog = new AlertDialog.Builder(this).setIcon(
                android.R.drawable.ic_dialog_alert).setTitle(
                getString(R.string.quit_application)).setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        // Discard changes and exit
                        case 0:
                            // Remove an instance if it has no status (e.g.,
                            // recently created)
                            InstanceDocument instance = Collect.mDb.getDb()
                                    .get(InstanceDocument.class, mInstanceId);
    
                            if (instance.getStatus() == InstanceDocument.Status.placeholder) {
                                Collect.mDb.getDb().delete(instance);
                                Log.d(Collect.LOGTAG, mFormId
                                        + ": removed placeholder instance "
                                        + mInstanceId);
                            }
    
                            // Remove any temporary media files that have not
                            // been sent to the database
                            File cacheDir = new File(FileUtils.CACHE_PATH);
                            String[] fileNames = cacheDir.list();
    
                            for (String file : fileNames) {
                                Log.v(Collect.LOGTAG, mFormId + ": evaluating "
                                        + file + " for removal");
    
                                if (Pattern.matches(
                                        "^" + mInstanceId + "[.].*", file)) {
                                    if (FileUtils
                                            .deleteFile(FileUtils.CACHE_PATH
                                                    + file)) {
                                        Log.d(Collect.LOGTAG, mFormId
                                                + ": removed unused file "
                                                + file);
                                    } else {
                                        Log.e(Collect.LOGTAG, mFormId
                                                + ": unable to remove file "
                                                + file);
                                    }
                                }
                            }
    
                            finish();
                            break;
                        // Save and exit
                        case 1:
                            saveDataToDisk(true, isInstanceComplete());
                            break;
                        // Do nothing
                        case 2:
                            break;
    
                        }
                    }
                }).create();
    
        mAlertDialog.show();
    }

    /**
     * Creates a view given the View type and an event
     * 
     * @param event
     * @return newly created View
     */
    private View createView(int event)
    {
        return createView(event, null);
    }

    /**
     * Creates a view given the View type and an event
     * 
     * @param event
     * @param subIndex
     *            index within the folio to highlight...
     * @return newly created View
     */
    private View createView(int event, FormIndex subIndex)
    {
        setTitle(getString(R.string.app_name) + " > "
                + mFormEntryModel.getFormTitle());
    
        /**
         * Unregister the existing current view from the underlying model.
         * Otherwise, the model will retain references to the visited Views, and
         * we'll burn memory.
         */
        if (mCurrentView != null && mCurrentView instanceof AbstractFolioView) {
            ((AbstractFolioView) mCurrentView).unregister();
        }
    
        switch (event) {
        case FormEntryController.EVENT_BEGINNING_OF_FORM:
            View startView;
            startView = View.inflate(this, R.layout.form_entry_start, null);
            setTitle(getString(R.string.app_name) + " > "
                    + mFormEntryModel.getFormTitle());
            ((TextView) startView.findViewById(R.id.form_full_title))
                    .setText(mFormEntryModel.getFormTitle());
            Drawable image = null;
    
            try {
                // attempt to load the configured default splash screen
                BitmapDrawable bitImage = new BitmapDrawable(getResources(),
                        FileUtils.FORM_LOGO_FILE_PATH);
    
                if (bitImage.getBitmap() != null
                        && bitImage.getIntrinsicHeight() > 0
                        && bitImage.getIntrinsicWidth() > 0) {
                    image = bitImage;
                }
            } catch (Exception e) {
                // TODO: log exception for debugging?
            }
    
            if (image == null) {
                // Show the opendatakit zig
                image = getResources().getDrawable(R.drawable.opendatakit_zig);
            }
    
            ((ImageView) startView.findViewById(R.id.form_start_bling))
                    .setImageDrawable(image);
    
            return startView;
        case FormEntryController.EVENT_END_OF_FORM:
            View endView = View.inflate(this, R.layout.form_entry_end, null);
    
            ((TextView) endView.findViewById(R.id.description))
                    .setText(getString(R.string.save_enter_data_description,
                            mFormEntryModel.getFormTitle()));
    
            // Checkbox for if finished or ready to send
            mInstanceComplete = ((CheckBox) endView
                    .findViewById(R.id.mark_finished));
            mInstanceComplete.setChecked(isInstanceComplete());
    
            // Create 'save for later' button
            ((Button) endView.findViewById(R.id.save_exit_button))
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v)
                        {
                            // Form is marked as 'saved' here.
                            saveDataToDisk(true, mInstanceComplete.isChecked());
                        }
                    });
    
            return endView;
        case FormEntryController.EVENT_GROUP:
            GroupView gv = new GroupView(mHandler, mFormEntryModel
                    .getFormIndex(), this);
            gv.buildView(mInstancePath, getGroupsForCurrentIndex());
    
            // If we came from a constraint violation, set the focus to the
            // violated field
            if (subIndex != null)
                gv.setSubFocus(subIndex);
    
            return gv;
        case FormEntryController.EVENT_QUESTION:
            QuestionView qv = new QuestionView(mHandler, mFormEntryModel
                    .getFormIndex(), this);
            qv.buildView(mInstancePath, getGroupsForCurrentIndex());
            return qv;
        default:
            Log.e(t, "Attempted to create a view that does not exist.");
            return null;
        }
    }

    /**
     * @return true if the current javarosa index is a group that should render
     *         as a multi-widget folio.
     */
    private boolean currentPromptIsGroupFolio()
    {
        if (mFormEntryModel.getEvent() != FormEntryController.EVENT_GROUP)
            return false;
    
        FormDef fd = mFormEntryModel.getForm();
        FormIndex index = mFormEntryModel.getFormIndex();
        GroupDef gd = (GroupDef) fd.getChild(index);
    
        if (gd.getRepeat())
            return false;
    
        return isAppearanceGroupFolio(gd.getAppearanceAttr());
    }

    /**
     * @return true if the current View represents a question or group folio in
     *         the form
     */
    private boolean currentPromptIsQuestion()
    {
        return (mFormEntryModel.getEvent() == FormEntryController.EVENT_QUESTION)
                || currentPromptIsGroupFolio();
    }

    /**
     * Dismiss any showing dialogs that we manage.
     */
    private void dismissDialogs()
    {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    private FormEntryCaption[] getGroupsForCurrentIndex()
    {
        if (!(mFormEntryModel.getEvent() == FormEntryController.EVENT_QUESTION
                || mFormEntryModel.getEvent() == FormEntryController.EVENT_PROMPT_NEW_REPEAT || currentPromptIsGroupFolio()))
            return null;
    
        int lastquestion = 1;
    
        if (mFormEntryModel.getEvent() != FormEntryController.EVENT_QUESTION)
            lastquestion = 0;
    
        FormEntryCaption[] v = mFormEntryModel.getCaptionHierarchy();
        FormEntryCaption[] groups = new FormEntryCaption[v.length
                - lastquestion];
    
        for (int i = 0; i < v.length - lastquestion; i++) {
            groups[i] = v[i];
        }
    
        return groups;
    }

    /**
     * Loops through the FormEntryController until a non-group event is found,
     * or a group with an field list appearance attribute is found.
     * 
     * @return The event found
     */
    private int getNextFolioEvent()
    {
        int event = stepToNextEvent();

        while (event == FormEntryController.EVENT_GROUP
                || event == FormEntryController.EVENT_REPEAT) {

            if (currentPromptIsGroupFolio())
                break;

            event = stepToNextEvent();
        }

        return event;
    }

    /**
     * The repeat count of closest group the prompt belongs to.
     */
    private int getLastRepeatCount(FormEntryCaption[] groups)
    {
        // no change
        if (getLastGroup(groups) != null) {
            return getLastGroup(groups).getMultiplicity();
        }

        return -1;
    }

    /**
     * The text of closest group the prompt belongs to.
     */
    private String getLastGroupText(FormEntryCaption[] groups)
    {
        // no change
        if (getLastGroup(groups) != null) {
            return getLastGroup(groups).getLongText();
        }

        return null;
    }

    /**
     * The closest group the prompt belongs to.
     * 
     * @return FormEntryCaption
     */
    private FormEntryCaption getLastGroup(FormEntryCaption[] groups)
    {
        if (groups == null || groups.length == 0)
            return null;
        else
            return groups[groups.length - 1];
    }

    /**
     * The name of the closest group that repeats or null.
     */
    private String getLastRepeatedGroupName(FormEntryCaption[] groups)
    {
        // no change
        if (groups.length > 0) {
            for (int i = groups.length - 1; i > -1; i--) {
                if (groups[i].repeats()) {
                    return groups[i].getLongText();
                }
            }
        }

        return null;
    }

    /**
     * The count of the closest group that repeats or -1.
     */
    private int getLastRepeatedGroupRepeatCount(FormEntryCaption[] groups)
    {
        if (groups.length > 0) {
            for (int i = groups.length - 1; i > -1; i--) {
                if (groups[i].repeats()) {
                    return groups[i].getMultiplicity();

                }
            }
        }

        return -1;
    }

    private boolean indexContainsRepeatableGroup(FormIndex index)
    {
        FormEntryCaption[] groups = mFormEntryModel.getCaptionHierarchy(index);

        if (groups.length == 0) {
            return false;
        }

        for (int i = 0; i < groups.length; i++) {
            if (groups[i].repeats())
                return true;
        }

        return false;
    }

    /**
     * @param appearance
     *            appearance attribute of an xform group tag
     * @return true if the group should render as a multi-widget folio
     */
    private boolean isAppearanceGroupFolio(String appearance)
    {
        return (GroupLayoutFactory.FIELD_LIST_APPEARANCE.equals(appearance) || GroupLayoutFactory.CONDITIONAL_FIELD_LIST_APPEARANCE
                .equals(appearance));
    }

    private boolean isInstanceComplete()
    {
        InstanceDocument instance = Collect.mDb.getDb().get(
                InstanceDocument.class, mInstanceId);

        if (instance.getStatus() == InstanceDocument.Status.complete)
            return true;
        else
            return false;
    }

    /**
     * Jumps to the given index within the current form. If that index is nested
     * within a group folio, it then patches up the location to be that of the
     * enclosing group folio.
     * 
     * @param index
     * @return
     */
    private int jumpToContainingFolio(FormIndex index)
    {
        FormEntryController fec = Collect.getInstance()
                .getFormEntryController();
        int event = fec.jumpToIndex(index);
    
        if (event == FormEntryController.EVENT_QUESTION) {
            // caption[0..len-1]
            // caption[len-1] == the question itself
            // caption[len-2] == the first group it is contained in.
            FormEntryCaption[] captions = mFormEntryModel.getCaptionHierarchy();
    
            if (captions.length < 2)
                return event;
    
            FormEntryCaption grp = captions[captions.length - 2];
    
            if (isAppearanceGroupFolio(grp.getAppearanceHint())) {
                return fec.jumpToIndex(grp.getIndex());
            }
        }
    
        return event;
    }

    /**
     * Attempt to save the answer to the current prompt into the data model.
     * 
     * @param evaluateConstraints
     * @return true on success, false otherwise
     */
    private boolean saveCurrentAnswer(boolean evaluateConstraints)
    {
    
        // we can get here as part of the save-and-exit page, in which
        // case we have already saved all values. We can also get here
        // via a quit-app pop-up menu or a menu...save.
        if (!(mCurrentView instanceof AbstractFolioView))
            return true;
    
        return ((AbstractFolioView) mCurrentView)
                .saveCurrentAnswer(evaluateConstraints);
    }

    /**
     * Called during a 'save and exit' command. The form is not 'done' here.
     */
    private boolean saveDataToDisk(boolean exit, boolean complete)
    {
        // Save current answer
        if (!saveCurrentAnswer(true)) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.data_saved_error), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
    
        mSaveToDiskTask = new SaveToDiskTask();
        mSaveToDiskTask.setFormSavedListener(this);
    
        // TODO move to constructor <--? No. the mInstancePath isn't set until
        // the form loads.
        // TODO remove context
        mSaveToDiskTask.setExportVars(getApplicationContext(), mInstanceId,
                exit, complete);
        mSaveToDiskTask.execute();
        showDialog(SAVING_DIALOG);
    
        return true;
    }

    /**
     * Determines what should be displayed on the screen. Possible options are:
     * a question, an ask repeat dialog, or the submit screen. Also saves
     * answers to the data model after checking constraints.
     */
    private void showNextView()
    {
        if (currentPromptIsQuestion()) {
            if (!saveCurrentAnswer(true)) {
                // A constraint was violated so a dialog should be showing.
                mBeenSwiped = false;
                return;
            }
        }
    
        if (mFormEntryModel.getEvent() != FormEntryController.EVENT_END_OF_FORM) {
            int event = getNextFolioEvent();
    
            switch (event) {
            case FormEntryController.EVENT_GROUP:
            case FormEntryController.EVENT_QUESTION:
            case FormEntryController.EVENT_END_OF_FORM:
                View next = createView(event);
                showView(next, AnimationType.RIGHT);
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                createRepeatDialog();
                break;
            }
        } else {
            mBeenSwiped = false;
        }
    }

    /**
     * Determines what should be displayed between a question, or the start
     * screen and displays the appropriate view. Also saves answers to the data
     * model without checking constraints.
     */
    private void showPreviousView()
    {
        // The answer is saved on a back swipe, but question constraints are
        // ignored
        if (currentPromptIsQuestion()) {
            saveCurrentAnswer(false);
        }
    
        if (mFormEntryModel.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
            int event = stepToPreviousEvent();
    
            while (event != FormEntryController.EVENT_BEGINNING_OF_FORM
                    && event != FormEntryController.EVENT_QUESTION) {
                if (currentPromptIsGroupFolio())
                    break;
                event = stepToPreviousEvent();
            }
    
            // And reset index to the containing folio
            FormIndex index = mFormEntryModel.getFormIndex();
            event = jumpToContainingFolio(index);
    
            Log.e(t, "refreshing view for event: " + event);
    
            View next = createView(event, index);
            showView(next, AnimationType.LEFT);
        } else {
            mBeenSwiped = false;
        }
    }

    private int stepToNextEvent()
    {
        FormEntryController fec = Collect.getInstance()
                .getFormEntryController();
    
        if (currentPromptIsGroupFolio()) {
            // advance to the group after this group...
            FormIndex idx = mFormEntryModel.getFormIndex();
            // advance past this group...
            FormIndex current = mFormEntryModel.getForm().incrementIndex(idx,
                    false);
            // and update the current formIndex...
            mFormEntryModel.setQuestionIndex(current);
            return mFormEntryModel.getEvent();
        } else {
            return fec.stepToNextEvent();
        }
    }

    private int stepToPreviousEvent()
    {
        FormEntryController fec = Collect.getInstance()
                .getFormEntryController();
        return fec.stepToPreviousEvent();
    }
}