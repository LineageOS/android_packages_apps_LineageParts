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

package org.lineageos.lineageparts.health;

import android.content.ContentResolver;
import android.content.Context;
import android.util.AttributeSet;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class TargetTimePreference extends TimePreference {
    private static final String TAG = TargetTimePreference.class.getSimpleName();

    private final ContentResolver mResolver;
    private final int mDefaultBatteryHealthTargetTime;

    public TargetTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();
        mDefaultBatteryHealthTargetTime = context.getResources()
                .getInteger(R.integer.config_defaultBatteryHealthTargetTime);
    }

    @Override
    protected int getSummaryResourceId() {
        return R.string.battery_health_target_time_summary;
    }

    @Override
    protected int getTimeSetting() {
        return LineageSettings.System.getInt(mResolver,
                LineageSettings.System.BATTERY_HEALTH_TARGET_TIME,
                mDefaultBatteryHealthTargetTime);
    }

    @Override
    protected void setTimeSetting(int secondOfDay) {
        LineageSettings.System.putInt(mResolver,
                LineageSettings.System.BATTERY_HEALTH_TARGET_TIME,
                secondOfDay);
    }

    @Override
    protected void resetTimeSetting() {
        setTimeSetting(mDefaultBatteryHealthTargetTime);
    }
}
