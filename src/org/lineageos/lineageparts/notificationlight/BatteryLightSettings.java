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

package org.lineageos.lineageparts.notificationlight;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import org.lineageos.internal.notification.LightsCapabilities;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;

import lineageos.preference.LineageSystemSettingMainSwitchPreference;
import lineageos.preference.LineageSystemSettingSwitchPreference;
import lineageos.providers.LineageSettings;

import java.util.Set;

public class BatteryLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Searchable {
    private static final String TAG = "BatteryLightSettings";

    private static final String KEY_BATTERY_LIGHTS = "battery_lights";
    private static final String GENERAL_SECTION = "general_section";
    private static final String COLORS_SECTION = "colors_list";
    private static final String BRIGHTNESS_SECTION = "brightness_section";

    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String FULL_COLOR_PREF = "full_color";
    private static final String LIGHT_ENABLED_PREF = "battery_light_enabled";
    private static final String LIGHT_FULL_CHARGE_DISABLED_PREF =
            "battery_light_full_charge_disabled";
    private static final String PULSE_ENABLED_PREF = "battery_light_pulse";
    private static final String BRIGHTNESS_PREFERENCE = "battery_light_brightness_level";
    private static final String BRIGHTNESS_ZEN_PREFERENCE = "battery_light_brightness_level_zen";

    private ApplicationLightPreference mLowColorPref;
    private ApplicationLightPreference mMediumColorPref;
    private ApplicationLightPreference mFullColorPref;
    private LineageSystemSettingMainSwitchPreference mLightEnabledPref;
    private LineageSystemSettingSwitchPreference mLightFullChargeDisabledPref;
    private LineageSystemSettingSwitchPreference mPulseEnabledPref;
    private BatteryBrightnessPreference mBatteryBrightnessPref;
    private BatteryBrightnessZenPreference mBatteryBrightnessZenPref;
    private int mDefaultLowColor;
    private int mDefaultMediumColor;
    private int mDefaultFullColor;
    private boolean mMultiColorLed;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getContext();
        final Resources res = getResources();

        // Collect battery led capabilities.
        mMultiColorLed =
                LightsCapabilities.supports(context, LightsCapabilities.LIGHTS_RGB_BATTERY_LED);
        // liblights supports brightness control
        final boolean halAdjustableBrightness = LightsCapabilities.supports(context,
                LightsCapabilities.LIGHTS_ADJUSTABLE_BATTERY_LED_BRIGHTNESS);
        final boolean pulsatingLed = LightsCapabilities.supports(context,
                LightsCapabilities.LIGHTS_PULSATING_LED);
        final boolean segmentedBatteryLed = LightsCapabilities.supports(context,
                LightsCapabilities.LIGHTS_SEGMENTED_BATTERY_LED);

        addPreferencesFromResource(R.xml.battery_light_settings);
        getActivity().getActionBar().setTitle(R.string.battery_light_title);

        PreferenceScreen prefSet = getPreferenceScreen();

        PreferenceGroup generalPrefs = prefSet.findPreference(GENERAL_SECTION);

        mLightEnabledPref = prefSet.findPreference(LIGHT_ENABLED_PREF);
        mLightFullChargeDisabledPref = prefSet.findPreference(LIGHT_FULL_CHARGE_DISABLED_PREF);
        mPulseEnabledPref = prefSet.findPreference(PULSE_ENABLED_PREF);
        mBatteryBrightnessPref = prefSet.findPreference(BRIGHTNESS_PREFERENCE);
        mBatteryBrightnessZenPref = prefSet.findPreference(BRIGHTNESS_ZEN_PREFERENCE);

        mDefaultLowColor = res.getInteger(
                com.android.internal.R.integer.config_notificationsBatteryLowARGB);
        mDefaultMediumColor = res.getInteger(
                com.android.internal.R.integer.config_notificationsBatteryMediumARGB);
        mDefaultFullColor = res.getInteger(
                com.android.internal.R.integer.config_notificationsBatteryFullARGB);

        int batteryBrightness = mBatteryBrightnessPref.getBrightnessSetting();

        if (!pulsatingLed || segmentedBatteryLed) {
            generalPrefs.removePreference(mPulseEnabledPref);
        }

        if (mMultiColorLed) {
            generalPrefs.removePreference(mLightFullChargeDisabledPref);
            setHasOptionsMenu(true);

            // Low, Medium and full color preferences
            mLowColorPref = prefSet.findPreference(LOW_COLOR_PREF);
            mLowColorPref.setOnPreferenceChangeListener(this);
            mLowColorPref.setDefaultValues(mDefaultLowColor, 0, 0);
            mLowColorPref.setBrightness(batteryBrightness);

            mMediumColorPref = prefSet.findPreference(MEDIUM_COLOR_PREF);
            mMediumColorPref.setOnPreferenceChangeListener(this);
            mMediumColorPref.setDefaultValues(mDefaultMediumColor, 0, 0);
            mMediumColorPref.setBrightness(batteryBrightness);

            mFullColorPref = prefSet.findPreference(FULL_COLOR_PREF);
            mFullColorPref.setOnPreferenceChangeListener(this);
            mFullColorPref.setDefaultValues(mDefaultFullColor, 0, 0);
            mFullColorPref.setBrightness(batteryBrightness);

            final BrightnessPreference.OnBrightnessChangedListener brightnessListener =
                    brightness -> {
                mLowColorPref.setBrightness(brightness);
                mMediumColorPref.setBrightness(brightness);
                mFullColorPref.setBrightness(brightness);
            };
            mBatteryBrightnessPref.setOnBrightnessChangedListener(brightnessListener);
        } else {
            prefSet.removePreference(prefSet.findPreference(COLORS_SECTION));
            resetColors();
        }

        // Remove battery LED brightness controls if we can't support them.
        if (!mMultiColorLed && !halAdjustableBrightness) {
            prefSet.removePreference(prefSet.findPreference(BRIGHTNESS_SECTION));
        }

        watch(LineageSettings.System.getUriFor(LineageSettings.System.BATTERY_LIGHT_ENABLED));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshColors();
    }

    private void refreshColors() {
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        if (mLowColorPref != null) {
            int lowColor = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_LOW_COLOR, mDefaultLowColor);
            mLowColorPref.setAllValues(lowColor, 0, 0, false);
        }

        if (mMediumColorPref != null) {
            int mediumColor = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, mDefaultMediumColor);
            mMediumColorPref.setAllValues(mediumColor, 0, 0, false);
        }

        if (mFullColorPref != null) {
            int fullColor = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_FULL_COLOR, mDefaultFullColor);
            mFullColorPref.setAllValues(fullColor, 0, 0, false);
            updateBrightnessPrefColor(fullColor);
        }
    }

    private void updateBrightnessPrefColor(int color) {
        // If the user has selected no light (ie black) for
        // full charge, use white for the brightness preference.
        if (color == 0) {
            color = 0xFFFFFF;
        }
        mBatteryBrightnessPref.setLedColor(color);
        mBatteryBrightnessZenPref.setLedColor(color);
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param key of the specific setting to update
     */
    protected void updateValues(String key, Integer color) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (key.equals(LOW_COLOR_PREF)) {
            LineageSettings.System.putInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_LOW_COLOR, color);
        } else if (key.equals(MEDIUM_COLOR_PREF)) {
            LineageSettings.System.putInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, color);
        } else if (key.equals(FULL_COLOR_PREF)) {
            LineageSettings.System.putInt(resolver,
                    LineageSettings.System.BATTERY_LIGHT_FULL_COLOR, color);
            updateBrightnessPrefColor(color);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mMultiColorLed) {
            menu.add(0, MENU_RESET, 0, R.string.reset)
                    .setIcon(R.drawable.ic_settings_backup_restore)
                    .setAlphabeticShortcut('r')
                    .setShowAsActionFlags(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }
        return false;
    }

    protected void resetColors() {
        ContentResolver resolver = getActivity().getContentResolver();

        // Reset to the framework default colors
        LineageSettings.System.putInt(resolver, LineageSettings.System.BATTERY_LIGHT_LOW_COLOR,
                mDefaultLowColor);
        LineageSettings.System.putInt(resolver, LineageSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                mDefaultMediumColor);
        LineageSettings.System.putInt(resolver, LineageSettings.System.BATTERY_LIGHT_FULL_COLOR,
                mDefaultFullColor);
        refreshColors();
    }

    protected void resetToDefaults() {
        final Resources res = getResources();
        final boolean batteryLightEnabled = res.getBoolean(R.bool.def_battery_light_enabled);
        final boolean batteryLightFullChargeDisabled =
                res.getBoolean(R.bool.def_battery_light_full_charge_disabled);
        final boolean batteryLightPulseEnabled = res.getBoolean(R.bool.def_battery_light_pulse);

        if (mLightEnabledPref != null) mLightEnabledPref.setChecked(batteryLightEnabled);
        if (mLightFullChargeDisabledPref != null) {
            mLightFullChargeDisabledPref.setChecked(batteryLightFullChargeDisabled);
        }
        if (mPulseEnabledPref != null) mPulseEnabledPref.setChecked(batteryLightPulseEnabled);

        resetColors();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ApplicationLightPreference lightPref = (ApplicationLightPreference) preference;
        updateValues(lightPref.getKey(), lightPref.getColor());
        return true;
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        if (LineageSettings.System.getInt(context.getContentResolver(),
                LineageSettings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            return context.getString(R.string.enabled);
        }
        return context.getString(R.string.disabled);
    };

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (!LightsCapabilities.supports(context, LightsCapabilities.LIGHTS_BATTERY_LED)) {
                result.add(KEY_BATTERY_LIGHTS);
                result.add(LIGHT_ENABLED_PREF);
                result.add(GENERAL_SECTION);
                result.add(LIGHT_FULL_CHARGE_DISABLED_PREF);
                result.add(COLORS_SECTION);
                result.add(LOW_COLOR_PREF);
                result.add(MEDIUM_COLOR_PREF);
                result.add(FULL_COLOR_PREF);
            } else if (LightsCapabilities.supports(context,
                    LightsCapabilities.LIGHTS_RGB_BATTERY_LED)) {
                result.add(LIGHT_FULL_CHARGE_DISABLED_PREF);
            } else {
                result.add(COLORS_SECTION);
                result.add(LOW_COLOR_PREF);
                result.add(MEDIUM_COLOR_PREF);
                result.add(FULL_COLOR_PREF);
            }
            if (!LightsCapabilities.supports(context,
                    LightsCapabilities.LIGHTS_ADJUSTABLE_BATTERY_LED_BRIGHTNESS)) {
                result.add(BRIGHTNESS_SECTION);
                result.add(BRIGHTNESS_PREFERENCE);
                result.add(BRIGHTNESS_ZEN_PREFERENCE);
            }
            if (!LightsCapabilities.supports(context, LightsCapabilities.LIGHTS_PULSATING_LED) ||
                    LightsCapabilities.supports(context,
                            LightsCapabilities.LIGHTS_SEGMENTED_BATTERY_LED)) {
                result.add(PULSE_ENABLED_PREF);
            }
            return result;
        }
    };
}
