/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

public class TelephonyUtils {

    private static final String TAG = TelephonyUtils.class.getSimpleName();

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(@NonNull Context context) {
        TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
        return telephony != null && telephony.isVoiceCapable();
    }
}
