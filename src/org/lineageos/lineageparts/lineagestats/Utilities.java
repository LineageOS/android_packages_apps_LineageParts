/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2021-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.lineagestats;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import lineageos.providers.LineageSettings;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Locale;

public class Utilities {
    public static String getUniqueID(Context context) {
        final String id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return digest(context.getPackageName() + id);
    }

    public static String getCarrier(Context context) {
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        String carrier = tm.getNetworkOperatorName();
        if (TextUtils.isEmpty(carrier)) {
            String simOperator = tm.getSimOperatorName();
            if (!TextUtils.isEmpty(simOperator)) {
                carrier = simOperator;
            } else {
                carrier = "Unknown";
            }
        }
        return carrier;
    }

    public static String getCarrierId(Context context) {
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        String carrierId = tm.getNetworkOperator();
        if (TextUtils.isEmpty(carrierId)) {
            carrierId = "0";
        }
        return carrierId;
    }

    public static String getCountryCode(Context context) {
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        String countryCode = tm.getNetworkCountryIso().toUpperCase();
        if (TextUtils.isEmpty(countryCode) || isCdmaPhone(tm)) {
            String localeCountryCode = Locale.getDefault().getCountry();
            if (localeCountryCode.length() == 2) {
                countryCode = localeCountryCode;
            } else {
                countryCode = "Unknown";
            }
        }
        return countryCode;
    }

    public static String getDevice() {
        return SystemProperties.get("ro.lineage.device", Build.PRODUCT);
    }

    public static String getModVersion() {
        return SystemProperties.get("ro.lineage.version", Build.DISPLAY);
    }

    public static String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check to see if global stats are enabled.
     * @return Whether or not stats collection is enabled.
     */
    public static boolean isStatsCollectionEnabled(Context context) {
        return LineageSettings.Secure.getInt(context.getContentResolver(),
                LineageSettings.Secure.STATS_COLLECTION, 1) != 0;
    }

    /**
     * Enabled or disable stats collection
     * @param enabled Boolean that sets collection being enabled.
     */
    public static void setStatsCollectionEnabled(Context context, boolean enabled) {
        int enable = (enabled) ? 1 : 0;
        LineageSettings.Secure.putInt(context.getContentResolver(),
                LineageSettings.Secure.STATS_COLLECTION, enable);
    }

    private static boolean isCdmaPhone(TelephonyManager tm) {
        return tm != null && tm.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA;
    }
}
