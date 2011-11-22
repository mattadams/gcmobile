/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets;

import java.io.File;
import java.io.IOException;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.activities.DrawActivity;
import org.odk.collect.android.utilities.FileUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

public class DrawWidget extends QuestionWidget implements IBinaryWidget 
{
    private static final String t = "DrawWidget: ";
    
    private Button mCaptureButton;
    private FormEntryPrompt mPrompt;
    private ImageView mImageView;
    private TextView mErrorTextView;
    
    private String mBinaryName;
    private String mDrawMode;
    private String mInstanceFolder;
    private boolean mWaitingForData = false;
    
    public DrawWidget(Context context, FormEntryPrompt prompt)
    {
        super(context, prompt);        
        mPrompt = prompt;
        
        setupScreen();
    }
    
    // Alternate constructor used if "appearance" attribute is set on <upload ... />
    public DrawWidget(Context context, FormEntryPrompt prompt, String drawMode)
    {
        super(context, prompt);
        mPrompt = prompt;
        mDrawMode = drawMode;
        
        setupScreen();
    }

    @Override
    public void clearAnswer() 
    {
        // Not enabled here
    }

    @Override
    public IAnswerData getAnswer() 
    {
        if (mBinaryName != null) {
            return new StringData(mBinaryName.toString());
        } else {
            return null;
        }
    }

    @Override
    public void setFocus(Context context) 
    {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l)
    {
        // Not enabled here
    }

    @Override
    public boolean isWaitingForBinaryData() 
    {
        return mWaitingForData;
    }

    @Override
    public void setBinaryData(Object answer)
    {
        // Delete the previous image using the content provider
        if (mBinaryName != null) {
//            deleteMedia();
        }
        
        mBinaryName = (new File(getPathFromUri((Uri) answer))).getName();        
        Log.i(t, "Setting current answer to " + mBinaryName);
        
        mWaitingForData = false;
    }
    
    private String getPathFromUri(Uri uri) 
    {
        if (uri.toString().startsWith("file")) {
            return uri.toString().substring(6);
        } else {
            // Find entry in content provider
            Cursor c = getContext().getContentResolver().query(uri, null, null, null, null);
            c.moveToFirst();

            // Get data path
            String colString = c.getString(c.getColumnIndex("_data"));
            c.close();
            
            return colString;
        }
    }
    
    private void setupScreen()
    {
        setOrientation(LinearLayout.VERTICAL);
        
        // Retrieve answer from data model and update ui
        mBinaryName = mPrompt.getAnswerText();
        
        // Set location of folder containing answer
        mInstanceFolder = FormEntryActivity.mInstancePath.substring(0, FormEntryActivity.mInstancePath.lastIndexOf("/") + 1);
        
        // Error text, if needed
        mErrorTextView = new TextView(getContext());
        mErrorTextView.setText("Selected file is not a valid image");

        // Setup capture button
        LayoutParams layoutParams = new TableLayout.LayoutParams();
        layoutParams.setMargins(7, 5, 7, 5);
        
        mCaptureButton = new Button(getContext());
        mCaptureButton.setText("Open Sketchpad");
        mCaptureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mCaptureButton.setPadding(20, 20, 20, 20);
        mCaptureButton.setEnabled(!mPrompt.isReadOnly());
        mCaptureButton.setLayoutParams(layoutParams);
        
        // Launch capture intent on click
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWaitingForData = true;
                mErrorTextView.setVisibility(View.GONE);
                
                File tmp = new File(FileUtilsExtended.EXTERNAL_CACHE + File.separator + FileUtilsExtended.CAPTURED_IMAGE_FILE);
                
                // Copy existing image into temporary slot (edit existing image)
                if (mBinaryName == null) {
                    // Remove this file if it's still sitting around
                    tmp.delete();
                } else {
                    try {
                        org.apache.commons.io.FileUtils.copyFile(new File(mInstanceFolder, mBinaryName), tmp);
                    } catch (IOException e) {
                        Log.e(Collect.LOGTAG, t + "unable to copy existing binary image to temporary location: " + e.toString());
                        e.printStackTrace();
                    }
                }

                Intent i = new Intent(getContext(), DrawActivity.class);
                i.putExtra(DrawActivity.KEY_DRAW_MODE, mDrawMode);
                i.putExtra(DrawActivity.KEY_OUTPUT_URI, Uri.fromFile(tmp));
                ((Activity) getContext()).startActivityForResult(i, FormEntryActivity.IMAGE_CAPTURE);
            }
        });
        
        addView(mCaptureButton);
        
        // Proceed to add the imageView only if the user has taken a picture
        if (mBinaryName == null)
            return;        

        // Below taken from ImageWidget (w/o onClick for loading a larger image)
        mImageView = new ImageView(getContext());

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();

        File f = new File(mInstanceFolder + "/" + mBinaryName);

        if (f.exists()) {
            Bitmap bmp = FileUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);

            if (bmp == null) {
                mErrorTextView.setVisibility(View.VISIBLE);
            }

            mImageView.setImageBitmap(bmp);
        } else {
            mImageView.setImageBitmap(null);
        }

        mImageView.setPadding(10, 10, 10, 10);
        mImageView.setAdjustViewBounds(true);
        
        addView(mImageView);
    }
}
