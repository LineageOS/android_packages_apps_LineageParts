/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.profiles.BrightnessSettings;

public class BrightnessItem extends Item {
    private final BrightnessSettings mSettings;

    public BrightnessItem(BrightnessSettings brightnessSettings) {
        if (brightnessSettings == null) {
            brightnessSettings = new BrightnessSettings();
        }
        mSettings = brightnessSettings;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.profile_brightness_title);
    }

    @Override
    public String getSummary(Context context) {
        if (mSettings.isOverride()) {
            return context.getResources().getString(
                    R.string.profile_brightness_override_summary,
                    (int)((mSettings.getValue() * 100f)/255));
        }
        return context.getString(R.string.profile_action_none);
    }

    public BrightnessSettings getSettings() {
        return mSettings;
    }
}
