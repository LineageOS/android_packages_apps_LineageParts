/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
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

package org.lineageos.lineageparts.power;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings.Global;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import org.lineageos.lineageparts.PartsUpdater;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.widget.SeekBarPreference;
import org.lineageos.internal.graphics.drawable.StopMotionVectorDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lineageos.power.PerformanceManager;
import lineageos.power.PerformanceProfile;

import lineageos.providers.LineageSettings;


import static lineageos.power.PerformanceManager.PROFILE_POWER_SAVE;

public class PerfProfileSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {

    private static final String KEY_PERF_PROFILE_CATEGORY = "perf_profile_category";
    private static final String KEY_AUTO_POWER_SAVE  = "auto_power_save";
    private static final String KEY_POWER_SAVE       = "power_save";
    private static final String KEY_PERF_SEEKBAR     = "perf_seekbar";

    private ListPreference mAutoPowerSavePref;
    private SwitchPreference   mPowerSavePref;

    private SeekBarPreference        mPerfSeekBar;
    private StopMotionVectorDrawable mPerfDrawable;
    private PerfIconAnimator         mAnimator;

    private PowerManager       mPowerManager;
    private PerformanceManager mPerf;

    private int mLastSliderValue = 0;

    private List<PerformanceProfile> mProfiles;

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

        mPerfSeekBar = findPreference(KEY_PERF_SEEKBAR);
        mAutoPowerSavePref = findPreference(KEY_AUTO_POWER_SAVE);
        mPowerSavePref = findPreference(KEY_POWER_SAVE);

        mPowerManager = getActivity().getSystemService(PowerManager.class);
        mPerf = PerformanceManager.getInstance(getActivity());

        mProfiles = new ArrayList<>(mPerf.getPowerProfiles());

        int count = mProfiles.size();

        if (count == 0) {
            removePreference(KEY_PERF_PROFILE_CATEGORY);
            mPerfSeekBar = null;
        } else {
            mPerfDrawable = new StopMotionVectorDrawable(
                    (AnimatedVectorDrawable) getActivity().getDrawable(
                            R.drawable.ic_perf_profile_avd));
            mPerfSeekBar.setIconDrawable(mPerfDrawable);
            mAnimator = new PerfIconAnimator(getActivity(), mPerfDrawable);

            mPerfSeekBar.setMax(count - 1);
            mPerfSeekBar.setOnPreferenceChangeListener(this);
            updatePerfSettings();

            watch(LineageSettings.Secure.getUriFor(LineageSettings.Secure.PERFORMANCE_PROFILE));
        }

        mAutoPowerSavePref.setEntries(R.array.auto_power_save_entries);
        mAutoPowerSavePref.setEntryValues(R.array.auto_power_save_values);
        updateAutoPowerSaveValue();
        mAutoPowerSavePref.setOnPreferenceChangeListener(this);
        mPowerSavePref.setOnPreferenceChangeListener(this);
    }


    private static class PerfIconAnimator {

        private final Context mContext;
        private final StopMotionVectorDrawable mDrawable;

        private final ValueAnimator mGradient;
        private final AnimatorSet   mAnimator = new AnimatorSet();

        public PerfIconAnimator(Context context, StopMotionVectorDrawable drawable) {
            mContext = context;
            mDrawable = drawable;
            TypedValue colorAccent = new TypedValue();
            mContext.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                    colorAccent, true);
            mGradient = ValueAnimator.ofArgb(
                    mContext.getResources().getColor(R.color.perf_cold),
                    mContext.getResources().getColor(colorAccent.resourceId),
                    mContext.getResources().getColor(R.color.perf_hot));
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        private int getColorAt(float fraction) {
            mGradient.setCurrentFraction(fraction);
            return (Integer) mGradient.getAnimatedValue();
        }

        public void animateRange(float from, float to) {
            mAnimator.cancel();
            mAnimator.removeAllListeners();

            final ValueAnimator scale = ValueAnimator.ofFloat(from, to);
            final ValueAnimator color = ValueAnimator.ofArgb(
                    getColorAt(from), getColorAt(from + ((to - from) / 2)), getColorAt(to));

            scale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mDrawable.setCurrentFraction(
                            (Float) valueAnimator.getAnimatedValue());
                }
            });
            color.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mDrawable.setColorFilter(
                            (Integer) valueAnimator.getAnimatedValue(),
                            PorterDuff.Mode.SRC_IN);
                }
            });

            mAnimator.play(scale).with(color);
            mAnimator.start();
        }
    }

    private void updatePerfSettings() {
        if (mPerfSeekBar == null) {
            return;
        }

        PerformanceProfile profile = mPowerManager.isPowerSaveMode() ?
                mPerf.getPowerProfile(PROFILE_POWER_SAVE) : mPerf.getActivePowerProfile();
        mPerfSeekBar.setProgress(mProfiles.indexOf(profile));
        mPerfSeekBar.setTitle(getResources().getString(
                R.string.perf_profile_title, profile.getName()));
        mPerfSeekBar.setSummary(profile.getDescription());

        if (mPerfDrawable != null) {
            final float start = mProfiles.get(mLastSliderValue).getWeight();
            final float end = profile.getWeight();
            mAnimator.animateRange(start, end);
        }
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
        if (preference == mPerfSeekBar) {
            mLastSliderValue = mPerfSeekBar.getProgress();
            int index = (Integer) newValue;
            if (!mPerf.setPowerProfile(mProfiles.get(index).getId())) {
                // Don't just fail silently, inform the user as well
                Toast.makeText(getActivity(),
                        R.string.perf_profile_fail_toast, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (preference == mPowerSavePref) {
            if (!mPowerManager.setPowerSaveModeEnabled((boolean) newValue)) {
                // Don't just fail silently, inform the user as well
                Toast.makeText(getActivity(),
                        R.string.perf_profile_fail_toast, Toast.LENGTH_SHORT).show();
                return false;
            }
            updatePowerSaveValue();
        } else if (preference == mAutoPowerSavePref) {
            final int level = Integer.parseInt((String) newValue);
            Global.putInt(getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL, level);
            updateAutoPowerSaveSummary(level);
        }
        return true;
    }

    @Override
    public void onSettingsChanged(Uri contentUri) {
        super.onSettingsChanged(contentUri);
        updatePerfSettings();
    }

    private void updatePowerSaveValue() {
        mPowerSavePref.setChecked(mPowerManager.isPowerSaveMode());
        updatePerfSettings();
        // The profile was changed automatically without updating the preference
        PartsUpdater.notifyChanged(getActivity(), getPreferenceScreen().getKey());
    }

    private void updateAutoPowerSaveValue() {
        final int level = Global.getInt(
                getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        mAutoPowerSavePref.setValue(String.valueOf(level));
        updateAutoPowerSaveSummary(level);
    }

    private void updateAutoPowerSaveSummary(int level) {
        mAutoPowerSavePref.setSummary(level == 0
                ? R.string.auto_power_save_summary_off
                : R.string.auto_power_save_summary_on);
    }

    public static final SummaryProvider SUMMARY_PROVIDER = new SummaryProvider() {
        @Override
        public String getSummary(Context context, String key) {
            final PowerManager powerManager = context.getSystemService(PowerManager.class);
            final PerformanceManager perfManager = PerformanceManager.getInstance(context);
            final PerformanceProfile profile = powerManager.isPowerSaveMode() ?
                    perfManager.getPowerProfile(PROFILE_POWER_SAVE) :
                    perfManager.getActivePowerProfile();
            String summary = context.getString(R.string.perf_profile_settings_summary);
            if (profile != null) {
                summary += "\n\n" + context.getResources().getString(
                        R.string.perf_profile_overview_summary,
                        profile.getName());
            }
            return summary.replace("\\n", System.getProperty("line.separator"));
        }
    };

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<String>();

            final PerformanceManager perfManager = PerformanceManager.getInstance(context);
            final List<PerformanceProfile> profiles =
                    new ArrayList<>(perfManager.getPowerProfiles());

            if (profiles.size() == 0) {
                result.add(KEY_PERF_PROFILE_CATEGORY);
                result.add(KEY_PERF_SEEKBAR);
            }
            return result;
        }
    };
}
