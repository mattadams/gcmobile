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

package org.odk.collect.android.widgets;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.activities.FormEntryActivity;
import org.odk.collect.android.activities.GeoPointActivity;
import org.odk.collect.android.activities.GeoPointMapActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

/**
 * GeoPointWidget is the widget that allows the user to get GPS readings.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class GeoPointWidget extends QuestionWidget implements IBinaryWidget {
    private Button mGetLocationButton;
    private Button mViewButton;

    private TextView mStringAnswer;
    private TextView mAnswerDisplay;
    private boolean mWaitingForData;
    private boolean mUseMaps;
    private String mAppearance;
    public static String LOCATION = "gp";


    public GeoPointWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        mWaitingForData = false;
        mUseMaps = false;
        mAppearance = prompt.getAppearanceHint();

        setOrientation(LinearLayout.VERTICAL);

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);
        
        mGetLocationButton = new Button(getContext());
        mGetLocationButton.setPadding(20, 20, 20, 20);
        mGetLocationButton.setText(getContext().getString(R.string.get_location));
        mGetLocationButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mGetLocationButton.setEnabled(!prompt.isReadOnly());
        mGetLocationButton.setLayoutParams(params);

        // setup play button
        mViewButton = new Button(getContext());
        mViewButton.setText(getContext().getString(R.string.show_location));
        mViewButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mViewButton.setPadding(20, 20, 20, 20);
        mViewButton.setLayoutParams(params);
        
        // on play, launch the appropriate viewer
        mViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String s = mStringAnswer.getText().toString();
                String[] sa = s.split(" ");
                double gp[] = new double[4];
                gp[0] = Double.valueOf(sa[0]).doubleValue();
                gp[1] = Double.valueOf(sa[1]).doubleValue();
                gp[2] = Double.valueOf(sa[2]).doubleValue();
                gp[3] = Double.valueOf(sa[3]).doubleValue();
                Intent i = new Intent(getContext(), GeoPointMapActivity.class);
                i.putExtra(LOCATION, gp);
                ((Activity) getContext()).startActivity(i);

            }
        });

        mStringAnswer = new TextView(getContext());

        mAnswerDisplay = new TextView(getContext());
        mAnswerDisplay.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mAnswerDisplay.setGravity(Gravity.CENTER);

        String s = prompt.getAnswerText();
        if (s != null && !s.equals("")) {
            mGetLocationButton.setText(getContext().getString(R.string.replace_location));
            setBinaryData(s);
            mViewButton.setEnabled(true);
        } else {
            mViewButton.setEnabled(false);
        }
        
        // use maps or not
        if (mAppearance != null && mAppearance.equalsIgnoreCase("maps")) {
            try {
                // do google maps exist on the device
                Class.forName("com.google.android.maps.MapActivity");
                mUseMaps = true;
            } catch (ClassNotFoundException e) {
                mUseMaps = false;
            }
        } 

        
        // when you press the button
        mGetLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = null;
                if (mUseMaps) {
                    i = new Intent(getContext(), GeoPointMapActivity.class);
                } else {
                    i = new Intent(getContext(), GeoPointActivity.class);
                }
                ((Activity) getContext()).startActivityForResult(i,
                    FormEntryActivity.LOCATION_CAPTURE);
                mWaitingForData = true;

            }
        });

        // finish complex layout
        // retrieve answer from data model and update ui

        addView(mGetLocationButton);
        if (mUseMaps) {
            addView(mViewButton);
        }
        addView(mAnswerDisplay);
    }


    @Override
    public void clearAnswer() {
        mStringAnswer.setText(null);
        mAnswerDisplay.setText(null);
        mGetLocationButton.setText(getContext().getString(R.string.get_location));

    }


    @Override
    public IAnswerData getAnswer() {
        String s = mStringAnswer.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            try {
                // segment lat and lon
                String[] sa = s.split(" ");
                double gp[] = new double[4];
                gp[0] = Double.valueOf(sa[0]).doubleValue();
                gp[1] = Double.valueOf(sa[1]).doubleValue();
                gp[2] = Double.valueOf(sa[2]).doubleValue();
                gp[3] = Double.valueOf(sa[3]).doubleValue();

                return new GeoPointData(gp);
            } catch (Exception NumberFormatException) {
                return null;
            }
        }
    }


    private String truncateDouble(String s) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(Double.valueOf(s));
    }


    private String formatGps(double coordinates, String type) {
        String location = Double.toString(coordinates);
        String degreeSign = "\u00B0";
        String degree = location.substring(0, location.indexOf(".")) + degreeSign;
        location = "0." + location.substring(location.indexOf(".") + 1);
        double temp = Double.valueOf(location) * 60;
        location = Double.toString(temp);
        String mins = location.substring(0, location.indexOf(".")) + "'";

        location = "0." + location.substring(location.indexOf(".") + 1);
        temp = Double.valueOf(location) * 60;
        location = Double.toString(temp);
        String secs = location.substring(0, location.indexOf(".")) + '"';
        if (type.equalsIgnoreCase("lon")) {
            if (degree.startsWith("-")) {
                degree = "W " + degree.replace("-", "") + mins + secs;
            } else
                degree = "E " + degree.replace("-", "") + mins + secs;
        } else {
            if (degree.startsWith("-")) {
                degree = "S " + degree.replace("-", "") + mins + secs;
            } else
                degree = "N " + degree.replace("-", "") + mins + secs;
        }
        return degree;
    }


    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }


    @Override
    public void setBinaryData(Object answer) {
        String s = (String) answer;
        mStringAnswer.setText(s);

        String[] sa = s.split(" ");
        mAnswerDisplay.setText(getContext().getString(R.string.latitude) + ": "
                + formatGps(Double.parseDouble(sa[0]), "lat") + "\n"
                + getContext().getString(R.string.longitude) + ": "
                + formatGps(Double.parseDouble(sa[1]), "lon") + "\n"
                + getContext().getString(R.string.altitude) + ": " + truncateDouble(sa[2]) + "m\n"
                + getContext().getString(R.string.accuracy) + ": " + truncateDouble(sa[3]) + "m");
        mWaitingForData = false;
    }


    @Override
    public boolean isWaitingForBinaryData() {
        return mWaitingForData;
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mViewButton.setOnLongClickListener(l);
        mGetLocationButton.setOnLongClickListener(l);
        mStringAnswer.setOnLongClickListener(l);
        mAnswerDisplay.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mViewButton.cancelLongPress();
        mGetLocationButton.cancelLongPress();
        mStringAnswer.cancelLongPress();
        mAnswerDisplay.cancelLongPress();
    }

}
