/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package org.cyanogenmod.cmparts.livedisplay;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import org.cyanogenmod.cmparts.R;
import org.cyanogenmod.cmparts.SettingsPreferenceFragment;
import org.cyanogenmod.cmparts.search.BaseSearchIndexProvider;
import org.cyanogenmod.cmparts.search.SearchIndexableRaw;
import org.cyanogenmod.cmparts.search.Searchable;
import org.cyanogenmod.cmparts.utils.ResourceUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.DisplayMode;
import cyanogenmod.hardware.LiveDisplayConfig;
import cyanogenmod.hardware.LiveDisplayManager;
import cyanogenmod.preference.SettingsHelper;
import cyanogenmod.providers.CMSettings;

import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_CABC;
import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT;
import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_COLOR_ENHANCEMENT;
import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_DISPLAY_MODES;
import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OFF;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OUTDOOR;

public class LiveDisplay extends SettingsPreferenceFragment implements Searchable,
        Preference.OnPreferenceChangeListener, SettingsHelper.OnSettingsChangeListener {

    private static final String TAG = "LiveDisplay";

    private static final String KEY_CATEGORY_LIVE_DISPLAY = "live_display_options";
    private static final String KEY_CATEGORY_ADVANCED = "advanced";

    private static final String KEY_LIVE_DISPLAY = "live_display";
    private static final String KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE =
            "display_auto_outdoor_mode";
    private static final String KEY_LIVE_DISPLAY_LOW_POWER = "display_low_power";
    private static final String KEY_LIVE_DISPLAY_COLOR_ENHANCE = "display_color_enhance";
    private static final String KEY_LIVE_DISPLAY_TEMPERATURE = "live_display_color_temperature";

    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_PICTURE_ADJUSTMENT = "picture_adjustment";

    private static final String KEY_LIVE_DISPLAY_COLOR_PROFILE = "live_display_color_profile";

    private static final String COLOR_PROFILE_TITLE =
            KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_title";

    private static final String COLOR_PROFILE_SUMMARY =
            KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_summary";

    private final Uri DISPLAY_TEMPERATURE_DAY_URI =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_DAY);
    private final Uri DISPLAY_TEMPERATURE_NIGHT_URI =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT);
    private final Uri DISPLAY_TEMPERATURE_MODE_URI =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_MODE);

    private ListPreference mLiveDisplay;

    private SwitchPreference mColorEnhancement;
    private SwitchPreference mLowPower;
    private SwitchPreference mOutdoorMode;

    private PictureAdjustment mPictureAdjustment;
    private DisplayTemperature mDisplayTemperature;
    private DisplayColor mDisplayColor;

    private ListPreference mColorProfile;
    private String[] mColorProfileSummaries;

    private String[] mModeEntries;
    private String[] mModeValues;
    private String[] mModeSummaries;

    private boolean mHasDisplayModes = false;

    private LiveDisplayManager mLiveDisplayManager;
    private LiveDisplayConfig mConfig;

    private CMHardwareManager mHardware;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getResources();

        mHardware = CMHardwareManager.getInstance(getActivity());
        mLiveDisplayManager = LiveDisplayManager.getInstance(getActivity());
        mConfig = mLiveDisplayManager.getConfig();

        addPreferencesFromResource(R.xml.livedisplay);

        PreferenceCategory liveDisplayPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_LIVE_DISPLAY);
        PreferenceCategory advancedPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_ADVANCED);

        int adaptiveMode = mLiveDisplayManager.getMode();

        mLiveDisplay = (ListPreference) findPreference(KEY_LIVE_DISPLAY);
        mLiveDisplay.setValue(String.valueOf(adaptiveMode));

        mModeEntries = res.getStringArray(
                org.cyanogenmod.platform.internal.R.array.live_display_entries);
        mModeValues = res.getStringArray(
                org.cyanogenmod.platform.internal.R.array.live_display_values);
        mModeSummaries = res.getStringArray(
                org.cyanogenmod.platform.internal.R.array.live_display_summaries);

        // Remove outdoor mode from lists if there is no support
        if (!mConfig.hasFeature(LiveDisplayManager.MODE_OUTDOOR)) {
            int idx = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OUTDOOR));
            String[] entriesTemp = new String[mModeEntries.length - 1];
            String[] valuesTemp = new String[mModeValues.length - 1];
            String[] summariesTemp = new String[mModeSummaries.length - 1];
            int j = 0;
            for (int i = 0; i < mModeEntries.length; i++) {
                if (i == idx) {
                    continue;
                }
                entriesTemp[j] = mModeEntries[i];
                valuesTemp[j] = mModeValues[i];
                summariesTemp[j] = mModeSummaries[i];
                j++;
            }
            mModeEntries = entriesTemp;
            mModeValues = valuesTemp;
            mModeSummaries = summariesTemp;
        }

        mLiveDisplay.setEntries(mModeEntries);
        mLiveDisplay.setEntryValues(mModeValues);
        mLiveDisplay.setOnPreferenceChangeListener(this);

        mDisplayTemperature = (DisplayTemperature) findPreference(KEY_LIVE_DISPLAY_TEMPERATURE);

        mColorProfile = (ListPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_PROFILE);
        if (liveDisplayPrefs != null && mColorProfile != null
                && (!mConfig.hasFeature(FEATURE_DISPLAY_MODES) || !updateDisplayModes())) {
            liveDisplayPrefs.removePreference(mColorProfile);
        } else {
            mHasDisplayModes = true;
            mColorProfile.setOnPreferenceChangeListener(this);
        }

        mOutdoorMode = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
        if (liveDisplayPrefs != null && mOutdoorMode != null
                && !mConfig.hasFeature(MODE_OUTDOOR)) {
            liveDisplayPrefs.removePreference(mOutdoorMode);
            mOutdoorMode = null;
        }

        mLowPower = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_LOW_POWER);
        if (advancedPrefs != null && mLowPower != null
                && !mConfig.hasFeature(FEATURE_CABC)) {
            advancedPrefs.removePreference(mLowPower);
            mLowPower = null;
        }

        mColorEnhancement = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
        if (advancedPrefs != null && mColorEnhancement != null
                && !mConfig.hasFeature(FEATURE_COLOR_ENHANCEMENT)) {
            advancedPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        mPictureAdjustment = (PictureAdjustment) findPreference(KEY_PICTURE_ADJUSTMENT);
        if (advancedPrefs != null && mPictureAdjustment != null &&
                    !mConfig.hasFeature(LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT)) {
            advancedPrefs.removePreference(mPictureAdjustment);
            mPictureAdjustment = null;
        }

        mDisplayColor = (DisplayColor) findPreference(KEY_DISPLAY_COLOR);
        if (advancedPrefs != null && mDisplayColor != null &&
                !mConfig.hasFeature(LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT)) {
            advancedPrefs.removePreference(mDisplayColor);
            mDisplayColor = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateModeSummary();
        updateTemperatureSummary();
        updateColorProfileSummary(null);
        SettingsHelper.get(getActivity()).startWatching(this, DISPLAY_TEMPERATURE_DAY_URI,
                DISPLAY_TEMPERATURE_MODE_URI, DISPLAY_TEMPERATURE_NIGHT_URI);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsHelper.get(getActivity()).stopWatching(this);
    }

    private boolean updateDisplayModes() {
        final DisplayMode[] modes = mHardware.getDisplayModes();
        if (modes == null || modes.length == 0) {
            return false;
        }

        final DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
        int curId = -1;
        String[] entries = new String[modes.length];
        String[] values = new String[modes.length];
        mColorProfileSummaries = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            values[i] = String.valueOf(modes[i].id);
            entries[i] = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_TITLE);

            // Populate summary
            String summary = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_SUMMARY);
            if (summary != null) {
                summary = String.format("%s - %s", entries[i], summary);
            }
            mColorProfileSummaries[i] = summary;

            if (cur != null && modes[i].id == cur.id) {
                curId = cur.id;
            }
        }
        mColorProfile.setEntries(entries);
        mColorProfile.setEntryValues(values);
        if (curId >= 0) {
            mColorProfile.setValue(String.valueOf(curId));
        }

        return true;
    }

    private void updateColorProfileSummary(String value) {
        if (!mHasDisplayModes) {
            return;
        }

        if (value == null) {
            DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                    ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
            if (cur != null && cur.id >= 0) {
                value = String.valueOf(cur.id);
            }
        }

        int idx = mColorProfile.findIndexOfValue(value);
        if (idx < 0) {
            Log.e(TAG, "No summary resource found for profile " + value);
            mColorProfile.setSummary(null);
            return;
        }

        mColorProfile.setValue(value);
        mColorProfile.setSummary(mColorProfileSummaries[idx]);
    }

    private void updateModeSummary() {
        int mode = mLiveDisplayManager.getMode();

        int index = ArrayUtils.indexOf(mModeValues, String.valueOf(mode));
        if (index < 0) {
            index = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OFF));
        }

        mLiveDisplay.setSummary(mModeSummaries[index]);
        mLiveDisplay.setValue(String.valueOf(mode));

        if (mDisplayTemperature != null) {
            mDisplayTemperature.setEnabled(mode != MODE_OFF);
        }
        if (mOutdoorMode != null) {
            mOutdoorMode.setEnabled(mode != MODE_OFF);
        }
    }

    private void updateTemperatureSummary() {
        int day = mLiveDisplayManager.getDayColorTemperature();
        int night = mLiveDisplayManager.getNightColorTemperature();

        mDisplayTemperature.setSummary(getResources().getString(
                R.string.live_display_color_temperature_summary,
                mDisplayTemperature.roundUp(day),
                mDisplayTemperature.roundUp(night)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLiveDisplay) {
            mLiveDisplayManager.setMode(Integer.valueOf((String)objValue));
        } else if (preference == mColorProfile) {
            int id = Integer.valueOf((String)objValue);
            Log.i("LiveDisplay", "Setting mode: " + id);
            for (DisplayMode mode : mHardware.getDisplayModes()) {
                if (mode.id == id) {
                    mHardware.setDisplayMode(mode, true);
                    updateColorProfileSummary((String)objValue);
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        updateModeSummary();
        updateTemperatureSummary();
    }


    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final LiveDisplayConfig config = LiveDisplayManager.getInstance(context).getConfig();
            final Set<String> result = new ArraySet<String>();

            if (!config.hasFeature(FEATURE_DISPLAY_MODES)) {
                result.add(KEY_LIVE_DISPLAY_COLOR_PROFILE);
            }
            if (!config.hasFeature(MODE_OUTDOOR)) {
                result.add(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
            }
            if (!config.hasFeature(FEATURE_COLOR_ENHANCEMENT)) {
                result.add(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
            }
            if (!config.hasFeature(FEATURE_CABC)) {
                result.add(KEY_LIVE_DISPLAY_LOW_POWER);
            }
            if (!config.hasFeature(FEATURE_COLOR_ADJUSTMENT)) {
                result.add(KEY_DISPLAY_COLOR);
            }
            if (!config.hasFeature(FEATURE_PICTURE_ADJUSTMENT)) {
                result.add(KEY_PICTURE_ADJUSTMENT);
            }
            return result;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context) {
            final LiveDisplayConfig config = LiveDisplayManager.getInstance(context).getConfig();
            final Set<String> result = new ArraySet<>();

            // Add keywords for supported color profiles
            if (config.hasFeature(FEATURE_DISPLAY_MODES)) {
                DisplayMode[] modes = CMHardwareManager.getInstance(context).getDisplayModes();
                if (modes != null && modes.length > 0) {
                    for (DisplayMode mode : modes) {
                        result.add(ResourceUtils.getLocalizedString(
                                context.getResources(), mode.name, COLOR_PROFILE_TITLE));
                    }
                }
            }
            final SearchIndexableRaw raw = new SearchIndexableRaw(context);
            raw.entries = TextUtils.join(" ", result);
            raw.key = KEY_LIVE_DISPLAY_COLOR_PROFILE;
            raw.title = context.getString(R.string.live_display_color_profile_title);
            raw.rank = 2;
            return Collections.singletonList(raw);
        }
    };
}
