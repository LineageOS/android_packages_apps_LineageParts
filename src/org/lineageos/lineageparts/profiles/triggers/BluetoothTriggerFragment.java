/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.triggers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
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
        BluetoothManager bluetoothManager = (BluetoothManager)
                requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
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
