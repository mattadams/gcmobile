package org.odk.collect.android.widgets;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.views.MediaLayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class QuestionWidget extends LinearLayout {
    
    @SuppressWarnings("unused")
    private final static String t = "QuestionWidget";

    private LinearLayout.LayoutParams mLayout;
    protected FormEntryPrompt mPrompt;
    
    protected final int question_fontsize;
    protected final int answer_fontsize;


    public QuestionWidget(Context context, FormEntryPrompt p) {
        super(context);
        
        SharedPreferences settings =
            PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
        String question_font = settings.getString(PreferencesActivity.KEY_FONT_SIZE, Collect.DEFAULT_FONTSIZE);
        question_fontsize = new Integer(question_font).intValue();
        answer_fontsize = question_fontsize + 2;
        
        mPrompt = p;
 
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.TOP);
        setPadding(0, 7, 0, 0);

        mLayout =
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        mLayout.setMargins(10, 0, 10, 0);

        addQuestionText(p);
        addHelpText(p);
    }
    
    public FormEntryPrompt getPrompt() {
        return mPrompt;
    }


    public abstract IAnswerData getAnswer();

    public abstract void clearAnswer();
    
    public abstract void setFocus(Context context);
     

    /**
     * Add a Views containing the question text, audio (if applicable), and image (if applicable).
     * To satisfy the RelativeLayout constraints, we add the audio first if it exists, then the
     * TextView to fit the rest of the space, then the image if applicable.
     */
    private void addQuestionText(FormEntryPrompt p) {
        String imageURI = p.getImageText();
        String audioURI = p.getAudioText();
        String videoURI = p.getSpecialFormQuestionText("video");

        // shown when image is clicked
        String bigImageURI = p.getSpecialFormQuestionText("big-image");

        // Add the text view. Textview always exists, regardless of whether there's text.
        TextView questionText = new TextView(getContext());
        questionText.setText(p.getLongText());
        questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, question_fontsize);
        questionText.setTypeface(null, Typeface.BOLD);
        questionText.setPadding(0, 0, 0, 7);
        questionText.setId(38475483); // assign random id

        // Wrap to the size of the parent view
        questionText.setHorizontallyScrolling(false);

        if (p.getLongText() == null) {
            questionText.setVisibility(GONE);
        }
            
        // Create the layout for audio, image, text
        MediaLayout mediaLayout = new MediaLayout(getContext());
        mediaLayout.setAVT(questionText, audioURI, imageURI, videoURI, bigImageURI);

        addView(mediaLayout, mLayout);
    }


    /**
     * Add a TextView containing the help text.
     */
    private void addHelpText(FormEntryPrompt p) {

        String s = p.getHelpText();

        if (s != null && !s.equals("")) {
            TextView tv = new TextView(getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, question_fontsize - 5);
            tv.setPadding(0, -5, 0, 7);
            // wrap to the widget of view
            tv.setHorizontallyScrolling(false);
            tv.setText(s);
            tv.setTypeface(null, Typeface.ITALIC);

            addView(tv, mLayout);
        }
    }
    
    
    

}
