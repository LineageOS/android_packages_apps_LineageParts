/*
 * Copyright (C) 2023 The LineageOS Project
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

public class ChargingControlSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = ChargingControlSettings.class.getSimpleName();

    private static final String CHARGING_CONTROL_ENABLED_PREF = "charging_control_enabled";
    private static final String CHARGING_CONTROL_MODE_PREF = "charging_control_mode";
    private static final String CHARGING_CONTROL_START_TIME_PREF = "charging_control_start_time";
    private static final String CHARGING_CONTROL_TARGET_TIME_PREF = "charging_control_target_time";
    private static final String CHARGING_CONTROL_LIMIT_PREF = "charging_control_charging_limit";

    private LineageSystemSettingMainSwitchPreference mChargingControlEnabledPref;
    private LineageSystemSettingDropDownPreference mChargingControlModePref;
    private StartTimePreference mChargingControlStartTimePref;
    private TargetTimePreference mChargingControlTargetTimePref;
    private ChargingLimitPreference mChargingControlLimitPref;
    private int mDefaultChargingControlMode;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Resources res = getResources();

        addPreferencesFromResource(R.xml.charging_control_settings);
        getActivity().getActionBar().setTitle(R.string.charging_control_title);

        final PreferenceScreen prefSet = getPreferenceScreen();

        mChargingControlEnabledPref = prefSet.findPreference(CHARGING_CONTROL_ENABLED_PREF);
        mChargingControlModePref = prefSet.findPreference(CHARGING_CONTROL_MODE_PREF);
        mChargingControlModePref.setOnPreferenceChangeListener(this);
        mChargingControlStartTimePref = prefSet.findPreference(CHARGING_CONTROL_START_TIME_PREF);
        mChargingControlTargetTimePref = prefSet.findPreference(CHARGING_CONTROL_TARGET_TIME_PREF);
        mChargingControlLimitPref = prefSet.findPreference(CHARGING_CONTROL_LIMIT_PREF);

        mDefaultChargingControlMode = res.getInteger(R.integer.config_defaultChargingControlMode);

        if (mChargingControlLimitPref != null) {
            boolean isBatteryBypassSupported = res.getBoolean(
                    org.lineageos.platform.internal.R.bool.
                            config_deviceChargingControlBypassBattery);
            mChargingControlLimitPref.setVisible(isBatteryBypassSupported);
            if (!isBatteryBypassSupported) {
                mChargingControlLimitPref.resetSetting();
            }
        }

        setHasOptionsMenu(true);

        watch(LineageSettings.System.getUriFor(LineageSettings.System.CHARGING_CONTROL_ENABLED));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshValues() {
        final ContentResolver resolver = getActivity().getContentResolver();

        if (mChargingControlModePref != null) {
            final int chargingControlMode = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.CHARGING_CONTROL_MODE, mDefaultChargingControlMode);
            mChargingControlModePref.setValue(Integer.toString(chargingControlMode));
            refreshUi();
        }

        if (mChargingControlStartTimePref != null) {
            mChargingControlStartTimePref.setValue(
                    mChargingControlStartTimePref.getTimeSetting());
        }

        if (mChargingControlTargetTimePref != null) {
            mChargingControlTargetTimePref.setValue(
                    mChargingControlTargetTimePref.getTimeSetting());
        }

        if (mChargingControlLimitPref != null) {
            mChargingControlLimitPref.setValue(
                    mChargingControlLimitPref.getSetting());
        }
    }

    private void refreshUi() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final int chargingControlMode = LineageSettings.System.getInt(resolver,
                LineageSettings.System.CHARGING_CONTROL_MODE, mDefaultChargingControlMode);

        refreshUi(chargingControlMode);
    }

    private void refreshUi(final int chargingControlMode) {
        String summary = null;
        boolean isChargingControlStartTimePrefVisible = false;
        boolean isChargingControlTargetTimePrefVisible = false;

        final Resources res = getResources();

        if (chargingControlMode == 1) {
            summary = res.getString(R.string.charging_control_mode_auto_summary);
        } else if (chargingControlMode == 2) {
            summary = res.getString(R.string.charging_control_mode_custom_summary);
            isChargingControlStartTimePrefVisible = true;
            isChargingControlTargetTimePrefVisible = true;
        }

        mChargingControlModePref.setSummary(summary);

        if (mChargingControlStartTimePref != null) {
            mChargingControlStartTimePref.setVisible(isChargingControlStartTimePrefVisible);
        }

        if (mChargingControlTargetTimePref != null) {
            mChargingControlTargetTimePref.setVisible(isChargingControlTargetTimePrefVisible);
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
        if (CHARGING_CONTROL_MODE_PREF.equals(preference.getKey())) {
            final int chargingControlMode = Integer.valueOf((String) objValue);
            refreshUi(chargingControlMode);
        }
        return true;
    }

    private void resetToDefaults() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getResources();

        if (mChargingControlEnabledPref != null) {
            final boolean chargingControlEnabled = res.getBoolean(
                    R.bool.def_charging_control_enabled);
            mChargingControlEnabledPref.setChecked(chargingControlEnabled);
        }

        LineageSettings.System.putInt(resolver, LineageSettings.System.CHARGING_CONTROL_MODE,
                mDefaultChargingControlMode);

        if (mChargingControlStartTimePref != null) {
            mChargingControlStartTimePref.resetTimeSetting();
        }

        if (mChargingControlTargetTimePref != null) {
            mChargingControlTargetTimePref.resetTimeSetting();
        }

        if (mChargingControlLimitPref != null) {
            mChargingControlLimitPref.resetSetting();
        }

        refreshValues();
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        if (LineageSettings.System.getInt(context.getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_ENABLED, 1) == 1) {
            return context.getString(R.string.enabled);
        }
        return context.getString(R.string.disabled);
    };
}
