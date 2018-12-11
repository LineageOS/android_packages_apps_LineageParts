/*
 * Copyright (C) 2014 The CyanogenMod Project
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
import org.lineageos.lineageparts.profiles.actions.ItemListAdapter;
import lineageos.profiles.ConnectionSettings;

public class ConnectionOverrideItem extends BaseItem {
    int mConnectionId;
    ConnectionSettings mConnectionSettings;

    public static final int Lineage_MODE_SYSTEM_DEFAULT = -1;

    public ConnectionOverrideItem(int connectionId, ConnectionSettings settings) {
        mConnectionId = connectionId;
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
        }
        this.mConnectionSettings = settings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.CONNECTION_ITEM;
    }

    @Override
    public String getTitle() {
        return getConnectionTitle(getContext(), mConnectionSettings);
    }

    @Override
    public String getSummary() {
        return String.valueOf(getSummary(getContext()));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static String getConnectionTitle(Context context, ConnectionSettings settings) {
        int r = 0;
        switch (settings.getConnectionId()) {
            case ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH:
                r = R.string.toggleBluetooth;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA:
                r =R.string.toggleData;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_GPS:
                r = R.string.toggleGPS;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_NFC:
                r = R.string.toggleNfc;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_SYNC:
                r = R.string.toggleSync;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_WIFI:
                r = R.string.toggleWifi;
                break;
            case ConnectionSettings.PROFILE_CONNECTION_WIFIAP:
                r = R.string.toggleWifiAp;
                break;
        }
        return context.getString(r);
    }

    public CharSequence getSummary(Context context) {
        int resId = -1;
        if (mConnectionSettings != null) {
            if (mConnectionSettings.isOverride()) { // enabled, disabled, or none
                if (mConnectionSettings.getValue() == 1) {
                    resId = R.string.profile_action_enable;
                } else {
                    resId = R.string.profile_action_disable;
                }
            } else {
                resId = R.string.profile_action_none;
            }
        } else {
            resId = R.string.profile_action_none;
        }
        return context.getString(resId);
    }

    public ConnectionSettings getSettings() {
        return mConnectionSettings;
    }

    public int getConnectionType() {
        return mConnectionId;
    }
}
