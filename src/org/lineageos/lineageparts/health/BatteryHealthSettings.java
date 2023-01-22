/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2023 The LineageOS Project
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

package org.lineageos.lineageparts.health;

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

public class BatteryHealthSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = BatteryHealthSettings.class.getSimpleName();

    private static final String BATTERY_HEALTH_ENABLED_PREF = "battery_health_enabled";
    private static final String BATTERY_HEALTH_MODE_PREF = "battery_health_mode";
    private static final String BATTERY_HEALTH_START_TIME_PREF = "battery_health_start_time";
    private static final String BATTERY_HEALTH_TARGET_TIME_PREF = "battery_health_target_time";
    private static final String BATTERY_HEALTH_CHARGING_LIMIT_PREF = "battery_health_charging_limit";

    private LineageSystemSettingMainSwitchPreference mBatteryHealthEnabledPref;
    private LineageSystemSettingDropDownPreference mBatteryHealthModePref;
    private StartTimePreference mBatteryHealthStartTimePref;
    private TargetTimePreference mBatteryHealthTargetTimePref;
    private ChargingLimitPreference mBatteryHealthChargingLimitPref;
    private int mDefaultBatteryHealthMode;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Resources res = getResources();

        addPreferencesFromResource(R.xml.battery_health_settings);
        getActivity().getActionBar().setTitle(R.string.battery_health_title);

        final PreferenceScreen prefSet = getPreferenceScreen();

        mBatteryHealthEnabledPref = prefSet.findPreference(BATTERY_HEALTH_ENABLED_PREF);
        mBatteryHealthModePref = prefSet.findPreference(BATTERY_HEALTH_MODE_PREF);
        mBatteryHealthModePref.setOnPreferenceChangeListener(this);
        mBatteryHealthStartTimePref = prefSet.findPreference(BATTERY_HEALTH_START_TIME_PREF);
        mBatteryHealthTargetTimePref = prefSet.findPreference(BATTERY_HEALTH_TARGET_TIME_PREF);
        mBatteryHealthChargingLimitPref = prefSet.findPreference(BATTERY_HEALTH_CHARGING_LIMIT_PREF);

        mDefaultBatteryHealthMode = res.getInteger(R.integer.config_defaultBatteryHealthMode);

        setHasOptionsMenu(true);

        watch(LineageSettings.System.getUriFor(LineageSettings.System.BATTERY_HEALTH_ENABLED));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshValues() {
        final ContentResolver resolver = getActivity().getContentResolver();

        if (mBatteryHealthModePref != null) {
            final int batteryHealthMode = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.BATTERY_HEALTH_MODE, mDefaultBatteryHealthMode);
            mBatteryHealthModePref.setValue(Integer.toString(batteryHealthMode));
            refreshUi();
        }

        if (mBatteryHealthStartTimePref != null) {
            mBatteryHealthStartTimePref.setValue(
                    mBatteryHealthStartTimePref.getTimeSetting());
        }

        if (mBatteryHealthTargetTimePref != null) {
            mBatteryHealthTargetTimePref.setValue(
                    mBatteryHealthTargetTimePref.getTimeSetting());
        }

        if (mBatteryHealthChargingLimitPref != null) {
            mBatteryHealthChargingLimitPref.setValue(
                    mBatteryHealthChargingLimitPref.getSetting());
        }
    }

    private void refreshUi() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final int batteryHealthMode = LineageSettings.System.getInt(resolver,
                LineageSettings.System.BATTERY_HEALTH_MODE, mDefaultBatteryHealthMode);

        refreshUi(batteryHealthMode);
    }

    private void refreshUi(final int batteryHealthMode) {
        String summary = null;
        boolean isBatteryHealthStartTimePrefVisible = false;
        boolean isBatteryHealthTargetTimePrefVisible = false;

        final Resources res = getResources();

        if (batteryHealthMode == 1) {
            summary = res.getString(R.string.battery_health_mode_auto_summary);
        } else if (batteryHealthMode == 2) {
            summary = res.getString(R.string.battery_health_mode_custom_summary);
            isBatteryHealthStartTimePrefVisible = true;
            isBatteryHealthTargetTimePrefVisible = true;
        }

        mBatteryHealthModePref.setSummary(summary);

        if (mBatteryHealthStartTimePref != null) {
            mBatteryHealthStartTimePref.setVisible(isBatteryHealthStartTimePrefVisible);
        }

        if (mBatteryHealthTargetTimePref != null) {
            mBatteryHealthTargetTimePref.setVisible(isBatteryHealthTargetTimePrefVisible);
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
        if (BATTERY_HEALTH_MODE_PREF.equals(preference.getKey())) {
            final int batteryHealthMode = Integer.valueOf((String) objValue);
            refreshUi(batteryHealthMode);
        }
        return true;
    }

    private void resetToDefaults() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getResources();

        if (mBatteryHealthEnabledPref != null) {
            final boolean batteryHealthEnabled = res.getBoolean(
                    R.bool.def_battery_health_enabled);
            mBatteryHealthEnabledPref.setChecked(batteryHealthEnabled);
        }

        LineageSettings.System.putInt(resolver, LineageSettings.System.BATTERY_HEALTH_MODE,
                mDefaultBatteryHealthMode);

        if (mBatteryHealthStartTimePref != null) {
            mBatteryHealthStartTimePref.resetTimeSetting();
        }

        if (mBatteryHealthTargetTimePref != null) {
            mBatteryHealthTargetTimePref.resetTimeSetting();
        }

        if (mBatteryHealthChargingLimitPref != null) {
            mBatteryHealthChargingLimitPref.resetSetting();
        }

        refreshValues();
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        if (LineageSettings.System.getInt(context.getContentResolver(),
                LineageSettings.System.BATTERY_HEALTH_ENABLED, 1) == 1) {
            return context.getString(R.string.enabled);
        }
        return context.getString(R.string.disabled);
    };
}
