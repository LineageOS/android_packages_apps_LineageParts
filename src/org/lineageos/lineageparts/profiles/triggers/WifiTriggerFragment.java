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
package org.lineageos.lineageparts.profiles.triggers;

import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;

import lineageos.app.Profile;

import org.lineageos.lineageparts.R;

import java.util.List;

public class WifiTriggerFragment extends AbstractTriggerListFragment {
    private WifiManager mWifiManager;

    public static WifiTriggerFragment newInstance(Profile profile) {
        WifiTriggerFragment fragment = new WifiTriggerFragment();
        fragment.setArguments(initArgs(profile));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = getActivity().getSystemService(WifiManager.class);
    }

    @Override
    protected void onLoadTriggers(Profile profile, List<AbstractTriggerItem> triggers) {
        final Resources res = getResources();
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                WifiTrigger accessPoint = new WifiTrigger(config);
                int state = profile.getTriggerState(
                        Profile.TriggerType.WIFI, accessPoint.getSSID());
                initTriggerItemFromState(accessPoint, state, R.drawable.ic_wifi_signal_4);
                triggers.add(accessPoint);
            }
        } else {
            final List<Profile.ProfileTrigger> origTriggers =
                    profile.getTriggersFromType(Profile.TriggerType.WIFI);
            for (Profile.ProfileTrigger trigger : origTriggers) {
                WifiTrigger accessPoint = new WifiTrigger(trigger.getName());
                initTriggerItemFromState(accessPoint,
                        trigger.getState(), R.drawable.ic_wifi_signal_4);
                triggers.add(accessPoint);
            }
        }
    }

    @Override
    protected TriggerInfo onConvertToTriggerInfo(AbstractTriggerItem trigger) {
        WifiTrigger wifi = (WifiTrigger) trigger;
        return new TriggerInfo(Profile.TriggerType.WIFI, wifi.getSSID(), wifi.getTitle());
    }

    @Override
    protected boolean isTriggerStateSupported(TriggerInfo info, int triggerState) {
        return true;
    }

    @Override
    protected int getEmptyViewLayoutResId() {
        return R.layout.profile_wifi_empty_view;
    }

    @Override
    protected Intent getEmptyViewClickIntent() {
        return new Intent(Settings.ACTION_WIFI_SETTINGS);
    }

    @Override
    protected int getOptionArrayResId() {
        return R.array.profile_trigger_wifi_options;
    }

    @Override
    protected int getOptionValuesArrayResId() {
        return R.array.profile_trigger_wifi_options_values;
    }

    private static class WifiTrigger extends AbstractTriggerItem {
        public String mSSID;
        public WifiConfiguration mConfig;

        public WifiTrigger(WifiConfiguration config) {
            mConfig = config;
            loadConfig(config);
        }

        public WifiTrigger(String ssid) {
            mSSID = ssid;
        }

        public String getSSID() {
            return mSSID;
        }

        @Override
        public String getTitle() {
            return mSSID;
        }

        private void loadConfig(WifiConfiguration config) {
            mSSID = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
            mConfig = config;
        }

        public static String removeDoubleQuotes(String string) {
            final int length = string.length();
            if (length >= 2) {
                if (string.startsWith("\"") && string.endsWith("\"")) {
                    return string.substring(1, length - 1);
                }
            }
            return string;
        }
    }
}
