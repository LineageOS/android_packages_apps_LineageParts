/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.health;

import android.content.Context;
import android.util.AttributeSet;

import org.lineageos.lineageparts.R;

public class TargetTimePreference extends TimePreference {
    private static final String TAG = TargetTimePreference.class.getSimpleName();

    public TargetTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getSummaryResourceId() {
        return R.string.charging_control_target_time_summary;
    }

    @Override
    protected int getTimeSetting() {
        return mHealthInterface.getTargetTime();
    }

    @Override
    protected void setTimeSetting(int secondOfDay) {
        mHealthInterface.setTargetTime(secondOfDay);
    }
}
