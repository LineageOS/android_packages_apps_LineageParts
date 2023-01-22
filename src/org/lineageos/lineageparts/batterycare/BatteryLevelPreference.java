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
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.R;

public class BatteryLevelPreference extends CustomDialogPreference<AlertDialog>
        implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = BatteryLevelPreference.class.getSimpleName();

    public static final int BATTERY_LEVEL_MINIMUM = 70;
    public static final int BATTERY_LEVEL_MAXIMUM = 90;

    private final ContentResolver mResolver;
    private final int mDefaultBatteryCareChargingLimit;

    private TextView mDialogPercent;
    private SeekBar mBatteryLevelBar;
    private int mBatteryLevel;

    public BatteryLevelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_battery_level);

        mResolver = context.getContentResolver();
        mDefaultBatteryCareChargingLimit = context.getResources()
                .getInteger(R.integer.config_defaultBatteryCareChargingLimit);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.dlg_ok, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mBatteryLevel = mBatteryLevelBar.getProgress();
            LineageSettings.System.putInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                mBatteryLevel);
            setSummary(getSummary());
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mBatteryLevel = LineageSettings.System.getInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                mDefaultBatteryCareChargingLimit);

        mDialogPercent = view.findViewById(R.id.battery_level_percent);

        mBatteryLevelBar = view.findViewById(R.id.battery_level_seekbar);
        mBatteryLevelBar.setMax(BATTERY_LEVEL_MAXIMUM);
        mBatteryLevelBar.setMin(BATTERY_LEVEL_MINIMUM);
        mBatteryLevelBar.setOnSeekBarChangeListener(this);
        mBatteryLevelBar.setProgress(mBatteryLevel);
    }

    @Override
    public void onStartTrackingTouch (SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch (SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mDialogPercent.setText(percentString(progress));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (defaultValue != null) {
            mBatteryLevel = Integer.valueOf((String) defaultValue);
        } else {
            mBatteryLevel = BATTERY_LEVEL_MAXIMUM;
        }
    }

    @Override
    public CharSequence getSummary() {
        return percentString(mBatteryLevel);
    }

    public void setValue(int value) {
        mBatteryLevel = value;
    }

    private String percentString(int progress) {
        return String.format("%d%%", progress);
    }
}
