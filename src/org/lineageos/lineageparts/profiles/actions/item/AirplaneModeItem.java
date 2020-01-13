/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2020 The LineageOS Project
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
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.profiles.AirplaneModeSettings;

public class AirplaneModeItem extends Item {
    AirplaneModeSettings mSettings;

    public AirplaneModeItem(AirplaneModeSettings airplaneModeSettings) {
        if (airplaneModeSettings == null) {
            airplaneModeSettings = new AirplaneModeSettings();
        }
        mSettings = airplaneModeSettings;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.profile_airplanemode_title);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(getModeString(mSettings));
    }

    public AirplaneModeSettings getSettings() {
        return mSettings;
    }

    public static int getModeString(AirplaneModeSettings settings) {
        if (settings.isOverride()) {
            if (settings.getValue() == 1) {
                return R.string.profile_action_enable;
            } else {
                return R.string.profile_action_disable;
            }
        } else {
            return R.string.profile_action_none;
        }
    }
}
