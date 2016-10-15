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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import org.cyanogenmod.cmparts.R;
import org.cyanogenmod.cmparts.widget.SeekBarPreference;
import org.cyanogenmod.cmparts.SettingsPreferenceFragment;
import org.cyanogenmod.graphics.drawable.StopMotionVectorDrawable;

import java.util.ArrayList;
import java.util.List;

import cyanogenmod.power.PerformanceManager;
import cyanogenmod.power.PerformanceProfile;
import cyanogenmod.providers.CMSettings;

public class PerfProfileSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_PERF_PROFILE     = "perf_profile";
    private static final String KEY_AUTO_POWER_SAVE  = "auto_power_save";
    private static final String KEY_POWER_SAVE       = "power_save";
    private static final String KEY_PER_APP_PROFILES = "app_perf_profiles";
    private static final String KEY_PERF_SEEKBAR     = "perf_seekbar";

    private ListPreference mAutoPowerSavePref;
    private SwitchPreference   mPowerSavePref;

    private SeekBarPreference        mPerfSeekBar;
    private StopMotionVectorDrawable mPerfDrawable;
    private PerfIconAnimator         mAnimator;
    private SwitchPreference         mPerAppProfilesPref;

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

        mPerfSeekBar = (SeekBarPreference) findPreference(KEY_PERF_SEEKBAR);
        mAutoPowerSavePref = (ListPreference) findPreference(KEY_AUTO_POWER_SAVE);
        mPowerSavePref = (SwitchPreference) findPreference(KEY_POWER_SAVE);
        mPerAppProfilesPref = (SwitchPreference) findPreference(KEY_PER_APP_PROFILES);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPerf = PerformanceManager.getInstance(getActivity());

        mProfiles = new ArrayList<>(mPerf.getPowerProfiles());

        int count = mProfiles.size();

        if (count == 0) {
            removePreference(KEY_PER_APP_PROFILES);
            removePreference(KEY_PERF_SEEKBAR);
            mPerfSeekBar = null;
            mPerAppProfilesPref = null;

        } else {

            mPerfDrawable = new StopMotionVectorDrawable(
                    (AnimatedVectorDrawable) getActivity().getDrawable(
                            R.drawable.ic_perf_profile_avd));
            mPerfSeekBar.setIconDrawable(mPerfDrawable);
            mAnimator = new PerfIconAnimator(getActivity(), mPerfDrawable);

            mPerfSeekBar.setMax(count - 1);
            mPerfSeekBar.setOnPreferenceChangeListener(this);
            updatePerfSettings();

            addTrigger(CMSettings.Secure.getUriFor(CMSettings.Secure.PERFORMANCE_PROFILE));
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
            mGradient = ValueAnimator.ofArgb(
                    mContext.getResources().getColor(R.color.perf_cold),
                    mContext.getResources().getColor(R.color.theme_accent),
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

        PerformanceProfile profile = mPerf.getActivePowerProfile();
        mPerfSeekBar.setProgress(mProfiles.indexOf(profile));
        mPerfSeekBar.setTitle(getResources().getString(
                R.string.perf_profile_title, profile.getName()));
        mPerfSeekBar.setSummary(profile.getDescription());

        if (mPerfDrawable != null) {
            final float start = mProfiles.get(mLastSliderValue).getWeight();
            final float end = profile.getWeight();
            mAnimator.animateRange(start, end);
        }

        if (mPerAppProfilesPref != null) {
            mPerAppProfilesPref.setEnabled(profile.isBoostEnabled());
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
        updatePerfSettings();
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
            String summary = context.getString(R.string.perf_profile_settings_summary);
            if (profile != null) {
                summary += "\n\n" + context.getResources().getString(
                        R.string.perf_profile_overview_summary,
                        profile.getName());
            }
            return summary.replace("\\n", System.getProperty("line.separator"));
        }
    };
}
