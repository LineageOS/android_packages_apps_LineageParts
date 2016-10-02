/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.cyanogenmod.cmparts.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings.Global;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.cyanogenmod.cmparts.R;
import org.cyanogenmod.cmparts.SettingsPreferenceFragment;

import java.util.SortedSet;

import cyanogenmod.power.PerformanceManager;
import cyanogenmod.power.PerformanceProfile;
import cyanogenmod.providers.CMSettings;

public class PerfProfileSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_PERF_PROFILE = "perf_profile";
    private static final String KEY_AUTO_POWER_SAVE = "auto_power_save";
    private static final String KEY_POWER_SAVE = "power_save";
    private static final String KEY_PER_APP_PROFILES = "app_perf_profiles";

    private ListPreference mPerfProfilePref;
    private ListPreference mAutoPowerSavePref;
    private SwitchPreference mPowerSavePref;
    private SwitchPreference mPerAppProfilesPref;

    private PowerManager mPowerManager;
    private PerformanceManager mPerf;

    private final BroadcastReceiver mPowerSaveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePowerSaveValue();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.perf_profile_settings);

        mPerfProfilePref = (ListPreference) findPreference(KEY_PERF_PROFILE);
        mAutoPowerSavePref = (ListPreference) findPreference(KEY_AUTO_POWER_SAVE);
        mPowerSavePref = (SwitchPreference) findPreference(KEY_POWER_SAVE);
        mPerAppProfilesPref = (SwitchPreference) findPreference(KEY_PER_APP_PROFILES);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPerf = PerformanceManager.getInstance(getActivity());

        final SortedSet<PerformanceProfile> profiles = mPerf.getPowerProfiles();
        int count = profiles.size();

        if (count == 0) {
            removePreference(KEY_PERF_PROFILE);
            removePreference(KEY_PER_APP_PROFILES);
            mPerfProfilePref = null;
            mPerAppProfilesPref = null;

            updatePowerSaveValue();
            mPowerSavePref.setOnPreferenceChangeListener(this);
        } else {
            // The perf profiles pref supersedes the power save pref
            removePreference(KEY_POWER_SAVE);
            mPowerSavePref = null;

            // Prepare the list, it is sorted in the correct
            // order and filtered according to device capabilities
            final String[] entries = new String[count];
            final String[] entryValues = new String[count];

            int i = 0;
            for (PerformanceProfile profile : profiles) {
                entries[i] = profile.getName();
                entryValues[i] = String.valueOf(profile.getId());
                i++;
            }

            mPerfProfilePref.setEntries(entries);
            mPerfProfilePref.setEntryValues(entryValues);
            mPerfProfilePref.setOnPreferenceChangeListener(this);

            addTrigger(CMSettings.Secure.getUriFor(CMSettings.Secure.PERFORMANCE_PROFILE));
        }

        mAutoPowerSavePref.setEntries(R.array.auto_power_save_entries);
        mAutoPowerSavePref.setEntryValues(R.array.auto_power_save_values);
        updateAutoPowerSaveValue();
        mAutoPowerSavePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPowerSavePref != null) {
            updatePowerSaveValue();
            getActivity().registerReceiver(mPowerSaveReceiver,
                    new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPowerSavePref != null) {
            getActivity().unregisterReceiver(mPowerSaveReceiver);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPerfProfilePref) {
            if (!mPerf.setPowerProfile(Integer.parseInt((String) newValue))) {
                // Don't just fail silently, inform the user as well
                Toast.makeText(getActivity(),
                        R.string.perf_profile_fail_toast, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (preference == mPowerSavePref) {
            if (!mPowerManager.setPowerSaveMode((boolean) newValue)) {
                // Don't just fail silently, inform the user as well
                Toast.makeText(getActivity(),
                        R.string.perf_profile_fail_toast, Toast.LENGTH_SHORT).show();
                return false;
            }
            updatePowerSaveValue();
        } else if (preference == mAutoPowerSavePref) {
            Global.putInt(getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                    Integer.parseInt((String) newValue));
            updateAutoPowerSaveSummary();
        }
        return true;
    }

    @Override
    public void onRefresh(Context context, Uri uri) {
        super.onRefresh(context, uri);

        final PerformanceProfile profile = mPerf.getActivePowerProfile();
        if (profile == null || mPerfProfilePref == null) {
            return;
        }
        Log.d("PERF", "onRefresh: " + profile.toString());

        if (TextUtils.isEmpty(mPerfProfilePref.getValue()) ||
                Integer.valueOf(mPerfProfilePref.getValue()) != profile.getId()) {
            mPerfProfilePref.setValue(String.valueOf(profile.getId()));
        }
        if (mPerAppProfilesPref != null) {
            mPerAppProfilesPref.setEnabled(mPerf.getProfileHasAppProfiles(profile.getId()));
        }
        mPerfProfilePref.setSummary(profile.getDescription());
    }

    private void updatePowerSaveValue() {
        mPowerSavePref.setChecked(mPowerManager.isPowerSaveMode());
    }

    private void updateAutoPowerSaveValue() {
        final int level = Global.getInt(
                getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        mAutoPowerSavePref.setValue(String.valueOf(level));
        updateAutoPowerSaveSummary();
    }

    private void updateAutoPowerSaveSummary() {
        final int level = Global.getInt(
                getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        final String summary;
        if (level > 0 && level < 100) {
            summary = getResources().getString(R.string.auto_power_save_summary_on, level);
        } else {
            summary = getResources().getString(R.string.auto_power_save_summary_off);
        }
        mAutoPowerSavePref.setSummary(summary);
    }

    public static final SummaryProvider SUMMARY_PROVIDER = new SummaryProvider() {
        @Override
        public String getSummary(Context context, String key) {
            final PerformanceProfile profile =
                    PerformanceManager.getInstance(context).getActivePowerProfile();
            return profile == null ? null : profile.getDescription();
        }
    };
}
