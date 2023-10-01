/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TelephonyUtils {

    private static final String TAG = TelephonyUtils.class.getSimpleName();

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
        return telephony != null && telephony.isVoiceCapable();
    }

    private static Resources getPhoneResources(Context context) {
        try {
            final Context packageContext = context.createPackageContext("com.android.phone", 0);
            return packageContext.getResources();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.w(TAG, "couldn't locate resources for com.android.phone!");
        return null;
    }
}
