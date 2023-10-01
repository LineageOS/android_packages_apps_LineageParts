/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.profiles.AirplaneModeSettings;

public class AirplaneModeItem extends Item {
    private final AirplaneModeSettings mSettings;

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
