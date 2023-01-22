/*
 * Copyright (C) 2023 The LineageOS Project
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
