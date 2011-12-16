/*
 * Copyright (C) 2011 University of Washington
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

package com.radicaldynamic.gcmobile.android.preferences;

import org.odk.collect.android.utilities.UrlUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener 
{
    @SuppressWarnings("unused")
    private static final String t = "PreferencesActivity: ";
    
    // Keys to preference settings
    public static final String KEY_AUTOMATIC_SYNCHRONIZATION    = "automatic_synchronization";
    public static final String KEY_COMPLETE_BY_DEFAULT          = "complete_by_default";
    public static final String KEY_ENCRYPT_SYNCHRONIZATION      = "encrypted_synchronization";
    public static final String KEY_SYNCHRONIZATION_INTERVAL     = "automatic_synchronization_interval";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource(R.xml.gcmobile_preferences);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_preferences));
    }

    @Override
    protected void onPause() 
    {
        super.onPause();        
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() 
    {
        super.onResume();        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {

    }

    @SuppressWarnings("unused")
    private InputFilter getReturnFilter() 
    {
        InputFilter returnFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                    int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.getType((source.charAt(i))) == Character.CONTROL) {
                        return "";
                    }
                }
                return null;
            }
        };
        return returnFilter;
    }

    @SuppressWarnings("unused")
    private InputFilter getWhitespaceFilter() 
    {
        InputFilter whitespaceFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                    int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isWhitespace(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        return whitespaceFilter;
    }

    @SuppressWarnings("unused")
    private void validateUrl(EditTextPreference preference) 
    {
        if (preference != null) {
            String url = preference.getText();
            if (UrlUtils.isValidUrl(url)) {
                preference.setText(url);
                preference.setSummary(url);
            } else {
                // preference.setText((String) preference.getSummary());
                Toast.makeText(getApplicationContext(), getString(R.string.url_error),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
}
