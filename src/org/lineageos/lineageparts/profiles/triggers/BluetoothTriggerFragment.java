/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2020-2023 The LineageOS Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import lineageos.app.Profile;

import org.lineageos.lineageparts.R;

import java.util.List;
import java.util.Set;

public class BluetoothTriggerFragment extends AbstractTriggerListFragment {
    private BluetoothAdapter mBluetoothAdapter;

    public static BluetoothTriggerFragment newInstance(Profile profile) {
        BluetoothTriggerFragment fragment = new BluetoothTriggerFragment();
        fragment.setArguments(initArgs(profile));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onLoadTriggers(Profile profile, List<AbstractTriggerItem> triggers) {
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                BluetoothTrigger bt = new BluetoothTrigger(device);
                int state = profile.getTriggerState(
                        Profile.TriggerType.BLUETOOTH, bt.getAddress());
                initTriggerItemFromState(bt, state, R.drawable.ic_settings_bluetooth);
                triggers.add(bt);
            }
        } else {
            final List<Profile.ProfileTrigger> origTriggers =
                    profile.getTriggersFromType(Profile.TriggerType.BLUETOOTH);
            for (Profile.ProfileTrigger trigger : origTriggers) {
                BluetoothTrigger bt = new BluetoothTrigger(trigger.getName(), trigger.getId());
                initTriggerItemFromState(bt, trigger.getState(), R.drawable.ic_settings_bluetooth);
                triggers.add(bt);
            }
        }
    }

    @Override
    protected TriggerInfo onConvertToTriggerInfo(AbstractTriggerItem trigger) {
        BluetoothTrigger bt = (BluetoothTrigger) trigger;
        return new TriggerInfo(Profile.TriggerType.BLUETOOTH, bt.getAddress(), bt.getTitle());
    }

    @Override
    protected boolean isTriggerStateSupported(TriggerInfo info, int triggerState) {
        if (triggerState != Profile.TriggerState.ON_A2DP_CONNECT
                && triggerState != Profile.TriggerState.ON_A2DP_DISCONNECT) {
            return true;
        }
        BluetoothDevice dev = mBluetoothAdapter.getRemoteDevice(info.id);
        if (dev == null) {
            return false;
        }
        BluetoothClass btClass = dev.getBluetoothClass();
        return btClass != null && btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP);
    }

    @Override
    protected int getEmptyViewLayoutResId() {
        return R.layout.profile_bluetooth_empty_view;
    }

    @Override
    protected Intent getEmptyViewClickIntent() {
        return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
    }

    @Override
    protected int getOptionArrayResId() {
        return R.array.profile_trigger_bt_options;
    }

    @Override
    protected int getOptionValuesArrayResId() {
        return R.array.profile_trigger_bt_options_values;
    }

    public static class BluetoothTrigger extends AbstractTriggerItem {
        private final String mAddress;

        public BluetoothTrigger(BluetoothDevice device) {
            mAddress = device.getAddress();
            if (device.getAlias() != null) {
                setTitle(device.getAlias());
            } else {
                setTitle(device.getName());
            }
        }

        public BluetoothTrigger(String name, String address) {
            mAddress = address;
            setTitle(name);
        }

        public String getAddress() {
            return mAddress;
        }
    }
}
