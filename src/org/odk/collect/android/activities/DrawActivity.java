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

package org.odk.collect.android.activities;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;

public class DrawActivity extends Activity implements ColorPickerDialog.OnColorChangedListener 
{      
    private static final String t = "DrawActivity: ";
    
    public static final String KEY_DRAW_MODE        = "draw_mode";
    public static final String KEY_OUTPUT_URI       = "output_uri";
    
    private static final String KEY_ANNOTATE_MODE   = "annotation";
    private static final String KEY_SIGNATURE_MODE  = "signature";
    private static final String KEY_SKETCH_MODE     = "sketch";
    
    private static final int DIALOG_CANCEL  = 0;
    private static final int DIALOG_CLEAR   = 1;    
    private static final int DIALOG_SAVE    = 2;
    
    private static final int COLOR_MENU_ID      = Menu.FIRST;
    private static final int EMBOSS_MENU_ID     = Menu.FIRST + 1;
    private static final int BLUR_MENU_ID       = Menu.FIRST + 2;
    private static final int ERASE_MENU_ID      = Menu.FIRST + 3;
    private static final int SRCATOP_MENU_ID    = Menu.FIRST + 4;
    
    private Bitmap      mBitmap;
    
    private Paint       mPaint;
    private MaskFilter  mEmboss;
    private MaskFilter  mBlur;
    
    private Dialog      mDialog;
    private FrameLayout mDrawFrame;
    private View        mDrawPreview;
    
    private String mDrawMode = KEY_SKETCH_MODE;
    
    private Uri mSaveUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.draw_layout);        
        
        if (getIntent().getStringExtra(KEY_DRAW_MODE) != null) {
            mDrawMode = getIntent().getStringExtra(KEY_DRAW_MODE);
            
            // Lazy language change
            if (mDrawMode.contains("annotate"))
                mDrawMode = "annotation";
        }
        
        setTitle(getString(R.string.app_name) + " > " + mDrawMode.substring(0, 1).toUpperCase() + mDrawMode.substring(1));
            
        mSaveUri = (Uri) getIntent().getExtras().getParcelable(KEY_OUTPUT_URI);
        Log.d(Collect.LOGTAG, t + "saveUri at " + mSaveUri.toString());
        Log.d(Collect.LOGTAG, t + "saveUri.getPath at " + mSaveUri.getPath()); 

        // Initalize drawables
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(8);
        
        // Specific drawables for signature mode
        if (mDrawMode.equals(KEY_SIGNATURE_MODE)) {
            mPaint.setColor(0xFF000000);
            mPaint.setStrokeWidth(6);
        }
        
        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        
        ((Button) findViewById(R.id.drawCancel)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { showDialog(DIALOG_CANCEL); }
        });        
        
        ((Button) findViewById(R.id.drawClear)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { showDialog(DIALOG_CLEAR); }
        });     
        
        ((Button) findViewById(R.id.drawSave)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { showDialog(DIALOG_SAVE); }
        });
    }
    
    public Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mDialog = null;
        
        switch (id) {
        case DIALOG_CANCEL:
            builder
            .setMessage("Do you want to abandon this " + mDrawMode + "?");
            
            builder.setPositiveButton(getString(R.string.tf_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    finish();
                }
            });
            
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
            
        case DIALOG_CLEAR:
            builder
            .setMessage("Do you want to clear this " + mDrawMode + "?");
            
            builder.setPositiveButton(getString(R.string.tf_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mDrawPreview = new DrawView(DrawActivity.this);                    
                    mDrawFrame.removeAllViews(); 
                    mDrawFrame.addView(mDrawPreview);
                }
            });
            
            builder.setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
            
        case DIALOG_SAVE:
            builder
            .setMessage("Do you want to save this " + mDrawMode + "?");
            
            builder.setPositiveButton(getString(R.string.tf_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {                    
                    OutputStream outputStream = null;
                    
                    try {
                        // Replace alpha with white
                        Bitmap noAlpha = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
                        Canvas canvas = new Canvas(noAlpha);
                        canvas.drawColor(Color.WHITE);
                        canvas.drawBitmap(mBitmap, 0, 0, null);
                        
                        // Write non-alpha image
                        outputStream = getContentResolver().openOutputStream(mSaveUri);
                        noAlpha.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                        outputStream.close();
                        
                        setResult(RESULT_OK);
                        finish();
                    } catch (IOException e) {
                        // Ignore exception
                    }
                    
                    setResult(RESULT_OK);
                    finish();
                }
            });
            
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            
            mDialog = builder.create();
            break;
        }
        
        return mDialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, COLOR_MENU_ID, 0, "Pick Color").setShortcut('3', 'c');
        menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's');
        menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z');
        menu.add(0, ERASE_MENU_ID, 0, "Eraser").setShortcut('5', 'z');
//        menu.add(0, SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z');
        
        // Only enable menu in annotate or sketch modes (signature uses reasonable presets)
        return (mDrawMode.equals(KEY_ANNOTATE_MODE) || mDrawMode.equals(KEY_SKETCH_MODE));
    }
    
    @Override
    protected void onDestroy() 
    {
        if (mBitmap != null)
            mBitmap.recycle();
        
        // Logic above
        super.onDestroy();
    }
    
    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {        
        mPaint.setAlpha(0xFF);
    
        switch (item.getItemId()) {
            case COLOR_MENU_ID:
                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                return true;
            case EMBOSS_MENU_ID:
                if (mPaint.getMaskFilter() != mEmboss) {
                    mPaint.setMaskFilter(mEmboss);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case BLUR_MENU_ID:
                if (mPaint.getMaskFilter() != mBlur) {
                    mPaint.setMaskFilter(mBlur);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case ERASE_MENU_ID:
                if (mPaint.getXfermode() == null) {
                    mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                } else {
                    mPaint.setXfermode(null);
                }
                return true;
            case SRCATOP_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                mPaint.setAlpha(0x80);
                return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        super.onPrepareOptionsMenu(menu);
        
        if (mPaint.getMaskFilter() == mEmboss) {
            menu.getItem(1).setTitle("Emboss ON");
        } else {
            menu.getItem(1).setTitle("Emboss");
        }
        
        if (mPaint.getMaskFilter() == mBlur) {
            menu.getItem(2).setTitle("Blur ON");
        } else {
            menu.getItem(2).setTitle("Blur");
        }
        
        if (mPaint.getXfermode() == null) {
            menu.getItem(3).setTitle("Erase");
        } else {
            menu.getItem(3).setTitle("Erase ON");
        }
        
        return true;
    }
    
    @Override
    protected void onResume()
    {
        // Logic below
        super.onResume();
        
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onWindowFocusChanged(boolean)
     * 
     * DrawView depends on the dimensions of mDrawFrame being known
     * so this must happen here vs. onCreate() 
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) 
    {
        if (hasFocus && mDrawFrame == null) {
            mDrawFrame = (FrameLayout) findViewById(R.id.drawPreview); 
            mDrawPreview = new DrawView(this);                       
            mDrawFrame.addView(mDrawPreview);    
        }
    }

    public class DrawView extends View 
    {
        private Display mDisplay;
        
        private Canvas  mCanvas;
        private Path    mPath;
        private Paint   mBitmapPaint;
        
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 3;
        
        public DrawView(Context c) 
        {
            super(c);
            
            mDisplay = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            
            if (mBitmap == null) {
                // Load from existing file if one exists
                mBitmap = BitmapFactory.decodeFile(mSaveUri.getPath());
                
                // Fall back to empty image
                if (mBitmap == null) {
                    mBitmap = Bitmap.createBitmap(mDisplay.getWidth(), mDrawFrame.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                } else {
                    // Make the loaded bitmap non-immutable
                    // FIXME: known problem - ERASE function will not work with this image
                    mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                }
            } else {
                // If called a second time this will clear the screen
                mBitmap = Bitmap.createBitmap(mDisplay.getWidth(), mDrawFrame.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            }
                        
            mCanvas = new Canvas(mBitmap);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }
        
        @Override
        protected void onDraw(Canvas canvas) 
        {
            canvas.drawColor(0xFFAAAAAA);            
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);            
            canvas.drawPath(mPath, mPaint);
        }
    
        @Override
        protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
        {
            setMeasuredDimension(mDisplay.getWidth(), mDrawFrame.getMeasuredHeight());
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) 
        {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            
            return true;
        }
    
        private void touch_start(float x, float y) 
        {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }
        
        private void touch_move(float x, float y) 
        {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }
        
        private void touch_up() 
        {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }
    }

    public void colorChanged(int color) 
    {
        mPaint.setColor(color);
    }
}
