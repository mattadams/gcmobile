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

package com.radicaldynamic.groupinform.preferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.utilities.UrlUtils;

public class ServerPreferences extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static String KEY_SERVER = "server";
    public static String KEY_USERNAME = "username";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.server_preferences);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.server_preferences));
        updateServer();
        updateUsername();
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_SERVER)) {
            updateServer();
        } else if (key.equals(KEY_USERNAME)) {
            updateUsername();
        }
    }


    private void updateServer() {
        EditTextPreference etp =
            (EditTextPreference) this.getPreferenceScreen().findPreference(KEY_SERVER);
        String s = etp.getText().trim();

        if (UrlUtils.isValidUrl(s)) {
            etp.setText(s);
            etp.setSummary(s);
        } else {
            etp.setText((String) etp.getSummary());
            Toast.makeText(getApplicationContext(), getString(R.string.url_error),
                Toast.LENGTH_SHORT).show();
        }
    }


    private void updateUsername() {
        EditTextPreference etp =
            (EditTextPreference) this.getPreferenceScreen().findPreference(KEY_USERNAME);
        String s = etp.getText().trim();

        etp.setText(s);
        etp.setSummary(s);
    }

}
