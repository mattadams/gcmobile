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

package com.radicaldynamic.turboform.preferences;

import com.radicaldynamic.turboform.R;

import com.radicaldynamic.turboform.utilities.UrlUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class TFGeneralPreferences extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    
    public static String KEY_SERVER   = "server_host_preference";
    public static String KEY_USERNAME = "server_username_preference";
    public static String KEY_PASSWORD = "server_password_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_general_preferences));
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);
        
        updateServer();
        updateUsername();
        updatePassword();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
            this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(KEY_SERVER)) {
            updateServer();
        } else if (key.equals(KEY_USERNAME)) {
            updateUsername();
        } else if (key.equals(KEY_PASSWORD)) {
            updatePassword();
        }
    }
    
    private void updatePassword() {
        EditTextPreference etp =
            (EditTextPreference) this.getPreferenceScreen().findPreference(KEY_PASSWORD);
        etp.setSummary(etp.getText().replaceAll(".", "*"));
    } 
    
    private void updateServer() {
        EditTextPreference etp = (EditTextPreference) this.getPreferenceScreen().findPreference(KEY_SERVER);
        String s = etp.getText().trim();

        if (UrlUtils.isValidUrl(s)) {
            etp.setText(s);
            etp.setSummary(s);
        } else {
            etp.setText((String) etp.getSummary());
            Toast.makeText(
                    getApplicationContext(), 
                    getString(R.string.tf_server_url_error),
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUsername() {
        EditTextPreference etp =
            (EditTextPreference) this.getPreferenceScreen().findPreference(KEY_USERNAME);
        etp.setSummary(etp.getText());
    }
}
