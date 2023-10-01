/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

public class Header extends Item {
    private final int mNameResId;

    public Header(int nameResId) {
        mNameResId = nameResId;
    }

    @Override
    public boolean isHeader() {
        return true;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(mNameResId);
    }

    @Override
    public String getSummary(Context context) {
        return null;
    }
}
