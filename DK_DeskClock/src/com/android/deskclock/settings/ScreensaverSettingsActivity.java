/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.deskclock.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

/**
 * Settings for Clock screen saver
 */
public final class ScreensaverSettingsActivity extends AppCompatActivity {

    public static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    public static final String KEY_NIGHT_MODE = "screensaver_night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screensaver_settings);
        //bv zhangjiachu add for fixbug:屏幕保护设置无标题并概率闪退
        TextView Title = (TextView) findViewById(R.id.bv_toolbar_title);
        Title.setText(getApplicationContext().getString(R.string.screensaver_settings));
        Utils.initToolBar(this);
        //bv zhangjiachu add for fixbug:屏幕保护设置无标题并概率闪退 end
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
    public static class PrefsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        private boolean mIsRemoveClockStylePref;
        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (Utils.isNOrLater()) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.screensaver_settings);
            //bv zhangjiachu add for fixbug:屏幕保护设置无标题并概率闪退
            mIsRemoveClockStylePref = getActivity().getResources().getBoolean(R.bool.removeClockStylePref);
            //bv zhangjiachu add for fixbug:屏幕保护设置无标题并概率闪退 end
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }


        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            //bv zhangjiachu modify for fixbug:屏幕保护设置无标题并概率闪退
            if (!mIsRemoveClockStylePref) {
                if (KEY_CLOCK_STYLE.equals(pref.getKey())) {
                    final ListPreference clockStylePref = (ListPreference) pref;
                    final int index = clockStylePref.findIndexOfValue((String) newValue);
                    clockStylePref.setSummary(clockStylePref.getEntries()[index]);
                }
            }
            //bv zhangjiachu modify for fixbug:屏幕保护设置无标题并概率闪退 end
            return true;
        }

        private void refresh() {
            final ListPreference clockStylePref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
            //bv zhangjiachu modify for fixbug:屏幕保护设置无标题并概率闪退
            if (mIsRemoveClockStylePref) {
                if (clockStylePref != null) {
                    getPreferenceScreen().removePreference(clockStylePref);
                }
            } else {
                clockStylePref.setSummary(clockStylePref.getEntry());
                clockStylePref.setOnPreferenceChangeListener(this);
            }
            //bv zhangjiachu modify for fixbug:屏幕保护设置无标题并概率闪退 end
        }
    }
}
