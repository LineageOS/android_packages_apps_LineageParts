/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.triggers;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;

import lineageos.app.Profile;

import org.lineageos.lineageparts.R;

import java.util.HashSet;
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
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        final HashSet<String> alreadyAdded = new HashSet<>();

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                WifiTrigger accessPoint = new WifiTrigger(config);
                String ssid = accessPoint.getSSID();
                int state = profile.getTriggerState(Profile.TriggerType.WIFI, ssid);
                initTriggerItemFromState(accessPoint, state, R.drawable.ic_wifi_signal_4);
                if (alreadyAdded.add(ssid)) {
                    triggers.add(accessPoint);
                }
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
