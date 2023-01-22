/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2017-2023 The LineageOS Project
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

package org.lineageos.lineageparts.batterycare;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import lineageos.preference.LineageSystemSettingDropDownPreference;
import lineageos.preference.LineageSystemSettingMainSwitchPreference;
import lineageos.providers.LineageSettings;

public class BatteryCareSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = BatteryCareSettings.class.getSimpleName();

    private static final String BATTERY_CARE_ENABLED_PREF = "battery_care_enabled";
    private static final String BATTERY_CARE_MODE_PREF = "battery_care_mode";
    private static final String BATTERY_CARE_START_TIME_PREF = "battery_care_start_time";
    private static final String BATTERY_CARE_TARGET_TIME_PREF = "battery_care_target_time";
    private static final String BATTERY_CARE_CHARGING_LIMIT_PREF = "battery_care_charging_limit";

    private LineageSystemSettingMainSwitchPreference mBatteryCareEnabledPref;
    private LineageSystemSettingDropDownPreference mBatteryCareModePref;
    private StartTimePreference mBatteryCareStartTimePref;
    private TargetTimePreference mBatteryCareTargetTimePref;
    private ChargingLimitPreference mBatteryCareChargingLimitPref;
    private int mDefaultBatteryCareMode;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Resources res = getResources();

        addPreferencesFromResource(R.xml.battery_care_settings);
        getActivity().getActionBar().setTitle(R.string.battery_care_title);

        final PreferenceScreen prefSet = getPreferenceScreen();

        mBatteryCareEnabledPref = prefSet.findPreference(BATTERY_CARE_ENABLED_PREF);
        mBatteryCareModePref = prefSet.findPreference(BATTERY_CARE_MODE_PREF);
        mBatteryCareModePref.setOnPreferenceChangeListener(this);
        mBatteryCareStartTimePref = prefSet.findPreference(BATTERY_CARE_START_TIME_PREF);
        mBatteryCareTargetTimePref = prefSet.findPreference(BATTERY_CARE_TARGET_TIME_PREF);
        mBatteryCareChargingLimitPref = prefSet.findPreference(BATTERY_CARE_CHARGING_LIMIT_PREF);

        mDefaultBatteryCareMode = res.getInteger(R.integer.config_defaultBatteryCareMode);

        setHasOptionsMenu(true);

        watch(LineageSettings.System.getUriFor(LineageSettings.System.BATTERY_CARE_ENABLED));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshValues() {
        final ContentResolver resolver = getActivity().getContentResolver();

        if (mBatteryCareModePref != null) {
            final int batteryCareMode = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.BATTERY_CARE_MODE, mDefaultBatteryCareMode);
            mBatteryCareModePref.setValue(Integer.toString(batteryCareMode));
            refreshUi();
        }

        if (mBatteryCareStartTimePref != null) {
            mBatteryCareStartTimePref.setValue(
                    mBatteryCareStartTimePref.getTimeSetting());
        }

        if (mBatteryCareTargetTimePref != null) {
            mBatteryCareTargetTimePref.setValue(
                    mBatteryCareTargetTimePref.getTimeSetting());
        }

        if (mBatteryCareChargingLimitPref != null) {
            mBatteryCareChargingLimitPref.setValue(
                    mBatteryCareChargingLimitPref.getSetting());
        }
    }

    private void refreshUi() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final int batteryCareMode = LineageSettings.System.getInt(resolver,
                LineageSettings.System.BATTERY_CARE_MODE, mDefaultBatteryCareMode);

        refreshUi(batteryCareMode);
    }

    private void refreshUi(final int batteryCareMode) {
        String summary = null;
        boolean isBatteryCareStartTimePrefVisible = false;
        boolean isBatteryCareTargetTimePrefVisible = false;

        final Resources res = getResources();

        if (batteryCareMode == 1) {
            summary = res.getString(R.string.battery_care_mode_auto_summary);
        } else if (batteryCareMode == 2) {
            summary = res.getString(R.string.battery_care_mode_custom_summary);
            isBatteryCareStartTimePrefVisible = true;
            isBatteryCareTargetTimePrefVisible = true;
        }

        mBatteryCareModePref.setSummary(summary);

        if (mBatteryCareStartTimePref != null) {
            mBatteryCareStartTimePref.setVisible(isBatteryCareStartTimePrefVisible);
        }

        if (mBatteryCareTargetTimePref != null) {
            mBatteryCareTargetTimePref.setVisible(isBatteryCareTargetTimePrefVisible);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object objValue) {
        if (BATTERY_CARE_MODE_PREF.equals(preference.getKey())) {
            final int batteryCareMode = Integer.valueOf((String) objValue);
            refreshUi(batteryCareMode);
        }
        return true;
    }

    private void resetToDefaults() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getResources();

        if (mBatteryCareEnabledPref != null) {
            final boolean batteryCareEnabled = res.getBoolean(
                    R.bool.def_battery_care_enabled);
            mBatteryCareEnabledPref.setChecked(batteryCareEnabled);
        }

        LineageSettings.System.putInt(resolver, LineageSettings.System.BATTERY_CARE_MODE,
                mDefaultBatteryCareMode);

        if (mBatteryCareStartTimePref != null) {
            mBatteryCareStartTimePref.resetTimeSetting();
        }

        if (mBatteryCareTargetTimePref != null) {
            mBatteryCareTargetTimePref.resetTimeSetting();
        }

        if (mBatteryCareChargingLimitPref != null) {
            mBatteryCareChargingLimitPref.resetSetting();
        }

        refreshValues();
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        if (LineageSettings.System.getInt(context.getContentResolver(),
                LineageSettings.System.BATTERY_CARE_ENABLED, 1) == 1) {
            return context.getString(R.string.enabled);
        }
        return context.getString(R.string.disabled);
    };
}
