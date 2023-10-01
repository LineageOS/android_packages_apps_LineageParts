/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.health;

import android.content.Context;
import android.util.AttributeSet;

import org.lineageos.lineageparts.R;

public class StartTimePreference extends TimePreference {
    private static final String TAG = StartTimePreference.class.getSimpleName();

    public StartTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getSummaryResourceId() {
        return R.string.charging_control_start_time_summary;
    }

    @Override
    protected int getTimeSetting() {
        return mHealthInterface.getStartTime();
    }

    @Override
    protected void setTimeSetting(int secondOfDay) {
        mHealthInterface.setStartTime(secondOfDay);
    }
}
