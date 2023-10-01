/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

public class DisabledItem extends Item {
    private final int mResTitle;
    private final int mResSummary;

    public DisabledItem(int resTitle, int resSummary) {
        mResTitle = resTitle;
        mResSummary = resSummary;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(mResTitle);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(mResSummary);
    }

    @Override
    public boolean isEnabled(Context context) {
        return false;
    }
}
