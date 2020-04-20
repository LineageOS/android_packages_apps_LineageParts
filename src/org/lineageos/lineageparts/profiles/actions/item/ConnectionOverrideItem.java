/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;
import android.telephony.SubscriptionManager;

import org.lineageos.lineageparts.R;

import lineageos.profiles.ConnectionSettings;

public class ConnectionOverrideItem extends Item {
    int mConnectionId;
    ConnectionSettings mConnectionSettings;

    public static final int Lineage_MODE_SYSTEM_DEFAULT = -1;

    public ConnectionOverrideItem(int connectionId, ConnectionSettings settings) {
        mConnectionId = connectionId;
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
        if (mConnectionSettings != null && mConnectionSettings.isOverride()) {
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

    public int getConnectionType() {
        return mConnectionId;
    }
}
