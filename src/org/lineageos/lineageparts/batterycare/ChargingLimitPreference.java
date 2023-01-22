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

import androidx.core.content.res.TypedArrayUtils;
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
    private int mChargingLimit;

    public ChargingLimitPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                R.attr.preferenceStyle, android.R.attr.preferenceStyle));
    }

    public ChargingLimitPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ChargingLimitPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLayoutResource(R.layout.preference_charging_limit);

        mResolver = context.getContentResolver();
        mDefaultBatteryCareChargingLimit = context.getResources()
                .getInteger(R.integer.config_defaultBatteryCareChargingLimit);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mChargingLimit = LineageSettings.System.getInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                mDefaultBatteryCareChargingLimit);

        mChargingLimitValue = (TextView) holder.findViewById(R.id.charging_limit_value);

        mChargingLimitBar = (SeekBar) holder.findViewById(R.id.charging_limit_seekbar);
        mChargingLimitBar.setOnSeekBarChangeListener(this);
        mChargingLimitBar.setProgress(mChargingLimit);
    }

    @Override
    public void onStartTrackingTouch (SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch (SeekBar seekBar) {
        mChargingLimit = mChargingLimitBar.getProgress();
        LineageSettings.System.putInt(mResolver,
                LineageSettings.System.BATTERY_CARE_CHARGING_LIMIT,
                mChargingLimit);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mChargingLimitValue.setText(String.format("%d%%", progress));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (defaultValue != null) {
            mChargingLimit = Integer.valueOf((String) defaultValue);
        }
    }

    public void setValue(int value) {
        mChargingLimit = value;
    }
}
