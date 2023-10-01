/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

public abstract class Item {
    public abstract String getTitle(Context context);
    public abstract String getSummary(Context context);

    public boolean isHeader() {
        return false;
    }

    public boolean isEnabled(Context context) {
        return true;
    }
}
