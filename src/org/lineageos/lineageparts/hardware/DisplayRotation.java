/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.hardware;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.view.RotationPolicy;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class DisplayRotation extends SettingsPreferenceFragment
        implements OnMainSwitchChangeListener {
    private static final String TAG = "DisplayRotation";

    public static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_LOCKSCREEN_ROTATION = "lockscreen_rotation";
    private static final String ROTATION_0_PREF = "display_rotation_0";
    private static final String ROTATION_90_PREF = "display_rotation_90";
    private static final String ROTATION_180_PREF = "display_rotation_180";
    private static final String ROTATION_270_PREF = "display_rotation_270";

    private MainSwitchPreference mAccelerometer;
    private CheckBoxPreference mRotation0Pref;
    private CheckBoxPreference mRotation90Pref;
    private CheckBoxPreference mRotation180Pref;
    private CheckBoxPreference mRotation270Pref;

    public static final int ROTATION_0_MODE = 1;
    public static final int ROTATION_90_MODE = 2;
    public static final int ROTATION_180_MODE = 4;
    public static final int ROTATION_270_MODE = 8;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.display_rotation);

        PreferenceScreen prefSet = getPreferenceScreen();

        mAccelerometer = findPreference(KEY_ACCELEROMETER);
        mAccelerometer.addOnSwitchChangeListener(this);
        mAccelerometer.setPersistent(false);

        mRotation0Pref = prefSet.findPreference(ROTATION_0_PREF);
        mRotation90Pref = prefSet.findPreference(ROTATION_90_PREF);
        mRotation180Pref = prefSet.findPreference(ROTATION_180_PREF);
        mRotation270Pref = prefSet.findPreference(ROTATION_270_PREF);

        int mode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                ROTATION_0_MODE | ROTATION_90_MODE | ROTATION_270_MODE, UserHandle.USER_CURRENT);

        mRotation0Pref.setChecked((mode & ROTATION_0_MODE) != 0);
        mRotation90Pref.setChecked((mode & ROTATION_90_MODE) != 0);
        mRotation180Pref.setChecked((mode & ROTATION_180_MODE) != 0);
        mRotation270Pref.setChecked((mode & ROTATION_270_MODE) != 0);

        watch(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION));
    }

    @Override
    public void onSettingsChanged(Uri contentUri) {
        super.onSettingsChanged(contentUri);
        updateAccelerometerRotationSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccelerometerRotationSwitch();
    }

    private void updateAccelerometerRotationSwitch() {
        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

    private int getRotationBitmask() {
        int mode = 0;
        if (mRotation0Pref.isChecked()) {
            mode |= ROTATION_0_MODE;
        }
        if (mRotation90Pref.isChecked()) {
            mode |= ROTATION_90_MODE;
        }
        if (mRotation180Pref.isChecked()) {
            mode |= ROTATION_180_MODE;
        }
        if (mRotation270Pref.isChecked()) {
            mode |= ROTATION_270_MODE;
        }
        return mode;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mRotation0Pref ||
                preference == mRotation90Pref ||
                preference == mRotation180Pref ||
                preference == mRotation270Pref) {
            int mode = getRotationBitmask();
            if (mode == 0) {
                mode |= ROTATION_0_MODE;
                mRotation0Pref.setChecked(true);
            }
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_ANGLES, mode, UserHandle.USER_CURRENT);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        RotationPolicy.setRotationLockForAccessibility(getActivity(), !mAccelerometer.isChecked());
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        if (RotationPolicy.isRotationLocked(context)) {
            return context.getString(R.string.display_rotation_disabled);
        }
        return context.getString(R.string.display_rotation_enabled);
    };
}
