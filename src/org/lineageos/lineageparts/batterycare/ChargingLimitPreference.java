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
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class ChargingLimitPreference extends Preference
        implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = ChargingLimitPreference.class.getSimpleName();

    private final ContentResolver mResolver;
    private final int mDefaultBatteryCareChargingLimit;

    private TextView mChargingLimitValue;
    private SeekBar mChargingLimitBar;

    public ChargingLimitPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.preference_charging_limit);

        mResolver = context.getContentResolver();
        mDefaultBatteryCareChargingLimit = context.getResources()
                .getInteger(R.integer.config_defaultBatteryCareChargingLimit);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mChargingLimitValue = (TextView) holder.findViewById(R.id.value);

        mChargingLimitBar = (SeekBar) holder.findViewById(R.id.seekbar_widget);
        mChargingLimitBar.setOnSeekBarChangeListener(this);
        mChargingLimitBar.setProgress(getSetting());
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        setSetting(seekBar.getProgress());
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress,
            final boolean fromUser) {
        updateValue(progress);
    }

    public void setValue(final int value) {
        mChargingLimitBar.setProgress(value);
        updateValue(value);
    }

    protected int getSetting() {
        return LineageSettings.System.getInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                mDefaultBatteryCareChargingLimit);
    }

    protected void setSetting(final int chargingLimit) {
        LineageSettings.System.putInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                chargingLimit);
    }

    protected void resetSetting() {
        setSetting(mDefaultBatteryCareChargingLimit);
    }

    private void updateValue(final int value) {
        mChargingLimitValue.setText(String.format("%d%%", value));
    }
}
