/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.profiles.ConnectionSettings;

public class ConnectionOverrideItem extends Item {
    private final ConnectionSettings mConnectionSettings;

    public ConnectionOverrideItem(int connectionId, ConnectionSettings settings) {
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
        }
        mConnectionSettings = settings;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(getConnectionTitleResId(mConnectionSettings));
    }

    @Override
    public String getSummary(Context context) {
        int resId = R.string.profile_action_none;
        if (mConnectionSettings.isOverride()) {
            if (mConnectionSettings.getValue() == 1) {
                resId = R.string.profile_action_enable;
            } else {
                resId = R.string.profile_action_disable;
            }
        }
        return context.getString(resId);
    }

    public static int getConnectionTitleResId(ConnectionSettings settings) {
        switch (settings.getConnectionId()) {
            case ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH:
                return R.string.toggleBluetooth;
            case ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA:
                return R.string.toggleData;
            case ConnectionSettings.PROFILE_CONNECTION_LOCATION:
                return R.string.toggleLocation;
            case ConnectionSettings.PROFILE_CONNECTION_NFC:
                return R.string.toggleNfc;
            case ConnectionSettings.PROFILE_CONNECTION_SYNC:
                return R.string.toggleSync;
            case ConnectionSettings.PROFILE_CONNECTION_WIFI:
                return R.string.toggleWifi;
            case ConnectionSettings.PROFILE_CONNECTION_WIFIAP:
                return R.string.toggleWifiAp;
        }
        return 0;
    }

    public ConnectionSettings getSettings() {
        return mConnectionSettings;
    }
}
