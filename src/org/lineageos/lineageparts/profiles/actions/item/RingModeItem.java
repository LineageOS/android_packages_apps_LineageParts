/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.profiles.RingModeSettings;

public class RingModeItem extends Item {
    private final RingModeSettings mSettings;

    public RingModeItem(RingModeSettings ringModeSettings) {
        if (ringModeSettings == null) {
            ringModeSettings = new RingModeSettings();
        }
        mSettings = ringModeSettings;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.ring_mode_title);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(getModeString(mSettings));
    }

    public static int getModeString(RingModeSettings settings) {
        if (settings == null) {
            return R.string.ring_mode_normal;
        }
        if (settings.isOverride()) {
            if (settings.getValue().equals("vibrate")) {
                return R.string.ring_mode_vibrate;
            } else if (settings.getValue().equals("normal")) {
                return R.string.ring_mode_normal;
            } else {
                return R.string.ring_mode_mute;
            }
        } else {
            return R.string.profile_action_none; //"leave unchanged"
        }
    }

    public RingModeSettings getSettings() {
        return mSettings;
    }
}
