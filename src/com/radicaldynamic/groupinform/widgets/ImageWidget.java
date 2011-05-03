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

package com.radicaldynamic.groupinform.widgets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.utilities.DateUtils;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.views.AbstractFolioView;
import com.radicaldynamic.groupinform.widgets.AbstractQuestionWidget.OnDescendantRequestFocusChangeListener.FocusChangeState;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class ImageWidget extends AbstractQuestionWidget implements IBinaryWidget {

    private final static String t = "MediaWidget";

    private Button mCaptureButton;
    private ImageView mImageView;

    private String mBinaryName;
    private TextView mDisplayText;

    private Uri mExternalUri;
    private String mCaptureIntent;
    private String mInstanceFolder;
    private String mInstanceId;
    private int mRequestCode;
    private int mCaptureText;
    private int mReplaceText;


    public ImageWidget(Handler handler, Context context, FormEntryPrompt prompt, String instanceDirPath) {
        super(handler, context, prompt);
        initialize(instanceDirPath);
    }


    private void initialize(String instanceDirPath) {
        mInstanceId = instanceDirPath.substring(instanceDirPath.lastIndexOf(File.separator) + 1, instanceDirPath.length());
        mInstanceFolder = instanceDirPath.substring(0, instanceDirPath.lastIndexOf(File.separator)) + "/";
        mExternalUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        mCaptureIntent = android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
        mRequestCode = FormEntryActivity.IMAGE_CAPTURE;
        mCaptureText = R.string.capture_image;
        mReplaceText = R.string.replace_image;
    }


    private void deleteMedia() {
    	// non-existent?
    	if ( mBinaryName == null ) return;

    	Log.i(t, "Deleting current answer: " + mBinaryName);
    	
    	// release image...
    	mImageView.setImageBitmap(null);
        // get the file path and delete the file
    	//
        // There's only 1 in this case, but android 1.6 doesn't implement delete on
        // android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI only on
        // android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI + a #
        String[] projection = {
            Images.ImageColumns._ID
        };
        Cursor c =
            getContext().getContentResolver().query(mExternalUri, projection,
                "_data='" + mInstanceFolder + mBinaryName + "'", null, null);
        int del = 0;
        if (c.getCount() > 0) {
            c.moveToFirst();
            String id = c.getString(c.getColumnIndex(Images.ImageColumns._ID));

            Log.i(t, "attempting to delete: " + Uri.withAppendedPath(mExternalUri, id));
            del =
                getContext().getContentResolver().delete(Uri.withAppendedPath(mExternalUri, id),
                    null, null);
        }
        c.close();

        // clean up variables
        mBinaryName = null;
        Log.i(t, "Deleted " + del + " rows from media content provider");
    }

    @Override
	public IAnswerData getAnswer() {
        if (mBinaryName != null) {
            return new StringData(mBinaryName.toString());
        } else {
            return null;
        }
    }

    @Override
    protected void buildViewBodyImpl() {
        
        // setup capture button
        mCaptureButton = new Button(getContext());
        mCaptureButton.setText(getContext().getString(mCaptureText));
        mCaptureButton
                .setTextSize(TypedValue.COMPLEX_UNIT_DIP, AbstractFolioView.APPLICATION_FONTSIZE);
        mCaptureButton.setPadding(20, 20, 20, 20);
        mCaptureButton.setEnabled(!prompt.isReadOnly());

        // launch capture intent on click
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
            	if ( signalDescendant(FocusChangeState.DIVERGE_VIEW_FROM_MODEL) ) {
	                Intent i = new Intent(mCaptureIntent);
	                // We give the camera an absolute filename/path where to put the
	                // picture because of bug:
	                // http://code.google.com/p/android/issues/detail?id=1480
	                // The bug appears to be fixed in Android 2.0+, but as of feb 2,
	                // 2010, G1 phones only run 1.6. Without specifying the path the
	                // images returned by the camera in 1.6 (and earlier) are ~1/4
	                // the size. boo.
	
	                // if this gets modified, the onActivityResult in
	                // FormEntyActivity will also need to be updated.
	                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(FileUtils.EXTERNAL_CACHE + "/",
	                        FileUtils.CAPTURED_IMAGE_FILE)));
	                ((Activity) getContext()).startActivityForResult(i, mRequestCode);
            	}
            }
        });

        // finish complex layout
        addView(mCaptureButton);

        mDisplayText = new TextView(getContext());
        mDisplayText.setPadding(5, 0, 0, 0);

        mImageView = new ImageView(getContext());
        mImageView.setPadding(10, 10, 10, 10);
        mImageView.setAdjustViewBounds(true);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if ( signalDescendant(FocusChangeState.DIVERGE_VIEW_FROM_MODEL)) {
            		// do nothing if no image
            		if ( mBinaryName == null ) return;
                    Intent i = new Intent("android.intent.action.VIEW");
                    String[] projection = {"_id"};
                    Cursor c =
                        getContext().getContentResolver().query(mExternalUri, projection,
                            "_data='" + mInstanceFolder + mBinaryName + "'", null, null);
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        String id = c.getString(c.getColumnIndex("_id"));

                        Log.i(t, "setting view path to: " + Uri.withAppendedPath(mExternalUri, id));

                        i.setDataAndType(Uri.withAppendedPath(mExternalUri, id), "image/*");
                        getContext().startActivity(i);
                    }
                    c.close();
            	}
            }
        });
        addView(mImageView);
    }

    protected void updateViewAfterAnswer() {
    	
    	String newAnswer = prompt.getAnswerText();
    	if ( mBinaryName != null && !mBinaryName.equals(newAnswer)) {
    		deleteMedia();
    	}
    	mBinaryName = newAnswer;
    	
        if (mBinaryName != null) {
            mCaptureButton.setText(getContext().getString(mReplaceText));
            mDisplayText.setText(getContext().getString(R.string.one_capture));
            
            Display display =
                ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
            int screenWidth = display.getWidth();
            int screenHeight = display.getHeight();

            File f = new File(mInstanceFolder + "/" + mBinaryName);
            Log.v(Collect.LOGTAG, t + "attempting to getBitmapScaledToDisplay from " + mInstanceFolder + "/" + mBinaryName);
            Bitmap bmp = FileUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
            mImageView.setImageBitmap(bmp);
        } else {
            mCaptureButton.setText(getContext().getString(mCaptureText));
            mDisplayText.setText(getContext().getString(R.string.no_capture));
            
            mImageView.setImageBitmap(null);
        }
    }

    private String getPathFromUri(Uri uri) {
        // find entry in content provider
        Cursor c = getContext().getContentResolver().query(uri, null, null, null, null);
        c.moveToFirst();

        // get data path
        String colString = c.getString(c.getColumnIndex("_data"));
        c.close();
        return colString;
    }


    @Override
	public void setBinaryData(Object binaryuri) {
        // you are replacing an answer. delete the previous image using the
        // content provider.
        if (mBinaryName != null) {
            deleteMedia();
        }
        String binarypath = getPathFromUri((Uri) binaryuri);
        File f = new File(binarypath);
        
        String s = mInstanceFolder + File.separator + mInstanceId + DateUtils.now("yyyyMMdd-HHmmss") + "." + binarypath.substring(binarypath.lastIndexOf('.') + 1);        
      
        if (f.renameTo(new File(s))) {
            // Resize image (full sized images are too large for the system)
            try {                                
                Bitmap bmp = FileUtils.getBitmapResizedToStore(new File(s), FileUtils.IMAGE_WIDGET_MAX_WIDTH, FileUtils.IMAGE_WIDGET_MAX_HEIGHT);                
                FileOutputStream out = new FileOutputStream(s);
                bmp.compress(Bitmap.CompressFormat.JPEG, FileUtils.IMAGE_WIDGET_QUALITY, out);
                out.close();
                bmp.recycle();
            } catch (FileNotFoundException e) {
                Log.e(Collect.LOGTAG, t + "failed to find file " + e.toString());  
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(Collect.LOGTAG, t + "failed to close output stream " + e.toString());
                e.printStackTrace();
            }
        } else {
            Log.e(Collect.LOGTAG, t + "failed to rename " + f.getAbsolutePath());
        }
        
        // remove the database entry and update the name
        getContext().getContentResolver().delete(getUriFromPath(binarypath), null, null);
        mBinaryName = s.substring(s.lastIndexOf(File.separator) + 1);
        
        Log.i(t, "Setting current answer to " + mBinaryName);
        saveAnswer(true); // and evaluate constraints and trigger UI update...
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        if (mBinaryName != null) {
        	mImageView.setEnabled(isEnabled);
            mCaptureButton.setEnabled(isEnabled && !prompt.isReadOnly());
        } else {
        	mImageView.setEnabled(false);
            mCaptureButton.setEnabled(isEnabled && !prompt.isReadOnly());
        }
    }
    
    private Uri getUriFromPath(String path) {
        // find entry in content provider
        Cursor c =
            getContext().getContentResolver().query(mExternalUri, null, "_data='" + path + "'",
                null, null);
        c.moveToFirst();

        // create uri from path
        String newPath = mExternalUri + "/" + c.getInt(c.getColumnIndex("_id"));
        c.close();
        return Uri.parse(newPath);
    }
}
