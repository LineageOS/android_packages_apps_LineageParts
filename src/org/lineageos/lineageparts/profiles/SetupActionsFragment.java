/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
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
package org.lineageos.lineageparts.profiles;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationGroup;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.SeekBarVolumizer;
import android.provider.Settings;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import lineageos.app.Profile;
import lineageos.app.ProfileGroup;
import lineageos.app.ProfileManager;
import lineageos.profiles.AirplaneModeSettings;
import lineageos.profiles.BrightnessSettings;
import lineageos.profiles.ConnectionSettings;
import lineageos.profiles.LockSettings;
import lineageos.profiles.RingModeSettings;
import lineageos.profiles.StreamSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.profiles.actions.ItemListAdapter;
import org.lineageos.lineageparts.profiles.actions.item.AirplaneModeItem;
import org.lineageos.lineageparts.profiles.actions.item.BrightnessItem;
import org.lineageos.lineageparts.profiles.actions.item.ConnectionOverrideItem;
import org.lineageos.lineageparts.profiles.actions.item.DisabledItem;
import org.lineageos.lineageparts.profiles.actions.item.DozeModeItem;
import org.lineageos.lineageparts.profiles.actions.item.Header;
import org.lineageos.lineageparts.profiles.actions.item.Item;
import org.lineageos.lineageparts.profiles.actions.item.LockModeItem;
import org.lineageos.lineageparts.profiles.actions.item.NotificationLightModeItem;
import org.lineageos.lineageparts.profiles.actions.item.ProfileNameItem;
import org.lineageos.lineageparts.profiles.actions.item.RingModeItem;
import org.lineageos.lineageparts.profiles.actions.item.TriggerItem;
import org.lineageos.lineageparts.profiles.actions.item.VolumeStreamItem;
import org.lineageos.lineageparts.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_LOCATION;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_NFC;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_SYNC;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_WIFI;
import static lineageos.profiles.ConnectionSettings.PROFILE_CONNECTION_WIFIAP;

public class SetupActionsFragment extends SettingsPreferenceFragment
        implements ItemListAdapter.OnItemClickListener {

    private static final int RINGTONE_REQUEST_CODE = 1000;
    private static final int NEW_TRIGGER_REQUEST_CODE = 1001;
    private static final int SET_NETWORK_MODE_REQUEST_CODE = 1002;

    public static final String EXTRA_NETWORK_MODE_PICKED = "network_mode_picker::chosen_value";

    private static final int MENU_REMOVE = Menu.FIRST;
    private static final int MENU_FILL_PROFILE = Menu.FIRST + 1;

    private static final int DIALOG_FILL_FROM_SETTINGS = 1;
    private static final int DIALOG_AIRPLANE_MODE = 2;
    private static final int DIALOG_BRIGHTNESS = 3;
    private static final int DIALOG_LOCK_MODE = 4;
    private static final int DIALOG_DOZE_MODE = 5;
    private static final int DIALOG_RING_MODE = 6;
    private static final int DIALOG_CONNECTION_OVERRIDE = 7;
    private static final int DIALOG_VOLUME_STREAM = 8;
    private static final int DIALOG_PROFILE_NAME = 9;

    private static final String LAST_SELECTED_POSITION = "last_selected_position";
    private static final int DIALOG_REMOVE_PROFILE = 10;

    private static final int DIALOG_NOTIFICATION_LIGHT_MODE = 11;

    private int mLastSelectedPosition = -1;
    private Item mSelectedItem;

    Profile mProfile;
    ItemListAdapter mAdapter;
    ProfileManager mProfileManager;
    RecyclerView mRecyclerView;

    boolean mNewProfileMode;

    private static final int[] LOCKMODE_MAPPING = new int[] {
            Profile.LockMode.DEFAULT, Profile.LockMode.INSECURE, Profile.LockMode.DISABLE
    };
    private static final int[] DOZE_MAPPING = new int[] {
            Profile.DozeMode.DEFAULT,
            Profile.DozeMode.ENABLE,
            Profile.DozeMode.DISABLE
    };
    private static final int[] NOTIFICATION_LIGHT_MAPPING = new int[] {
            Profile.NotificationLightMode.DEFAULT,
            Profile.NotificationLightMode.ENABLE,
            Profile.NotificationLightMode.DISABLE
    };
    private List<Item> mItems = new ArrayList<Item>();

    public static SetupActionsFragment newInstance(Profile profile, boolean newProfile) {
        SetupActionsFragment fragment = new SetupActionsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, newProfile);

        fragment.setArguments(args);
        return fragment;
    }

    public SetupActionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
            mNewProfileMode = getArguments().getBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
        }

        mProfileManager = ProfileManager.getInstance(getActivity());
        mAdapter = new ItemListAdapter(getActivity(), mItems, this);
        rebuildItemList();

        setHasOptionsMenu(true);
        if (mNewProfileMode && savedInstanceState == null) {
            // only pop this up on first creation
            showDialog(DIALOG_FILL_FROM_SETTINGS);
        } else if (savedInstanceState != null) {
            mLastSelectedPosition = savedInstanceState.getInt("last_selected_position", -1);
            if (mLastSelectedPosition != -1) {
                mSelectedItem = mItems.get(mLastSelectedPosition);
            }
        }
    }

    private void rebuildItemList() {
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        mItems.clear();

        // general prefs
        mItems.add(new Header(R.string.profile_name_title));
        mItems.add(new ProfileNameItem(mProfile));

        if (!mNewProfileMode) {
            // triggers
            mItems.add(new Header(R.string.profile_triggers_header));
            mItems.add(generateTriggerItem(TriggerItem.WIFI));
            if (DeviceUtils.deviceSupportsBluetooth()) {
                mItems.add(generateTriggerItem(TriggerItem.BLUETOOTH));
            }
            if (DeviceUtils.deviceSupportsNfc(context)) {
                mItems.add(generateTriggerItem(TriggerItem.NFC));
            }
        }

        // connection overrides
        mItems.add(new Header(R.string.wireless_networks_settings_title));
        if (DeviceUtils.deviceSupportsBluetooth()) {
            mItems.add(new ConnectionOverrideItem(PROFILE_CONNECTION_BLUETOOTH,
                    mProfile.getSettingsForConnection(PROFILE_CONNECTION_BLUETOOTH)));
        }
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_LOCATION));
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFI));
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_SYNC));
        if (DeviceUtils.deviceSupportsMobileData(getActivity())) {
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_MOBILEDATA));
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFIAP));
        }
        //if (WimaxHelper.isWimaxSupported(getActivity())) {
        //    mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIMAX));
        //}
        if (DeviceUtils.deviceSupportsNfc(getActivity())) {
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_NFC));
        }

        // add volume streams
        mItems.add(new Header(R.string.profile_volumeoverrides_title));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_ALARM));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_MUSIC));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_RING));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_NOTIFICATION));

        // system settings
        mItems.add(new Header(R.string.profile_system_settings_title));
        mItems.add(new RingModeItem(mProfile.getRingMode()));
        mItems.add(new AirplaneModeItem(mProfile.getAirplaneMode()));
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        if (!dpm.requireSecureKeyguard()) {
            mItems.add(new LockModeItem(mProfile));
        } else {
            mItems.add(new DisabledItem(R.string.profile_lockmode_title,
                    R.string.profile_lockmode_policy_disabled_summary));
        }
        mItems.add(new BrightnessItem(mProfile.getBrightness()));

        if (DeviceUtils.isDozeAvailable(context)) {
            mItems.add(new DozeModeItem(mProfile));
        }

        if (getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            mItems.add(new NotificationLightModeItem(mProfile));
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mNewProfileMode) {
            menu.add(0, MENU_REMOVE, 0, R.string.profile_menu_delete_title)
                    .setIcon(R.drawable.ic_actionbar_delete)
                    .setAlphabeticShortcut('d')
                    .setEnabled(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                            MenuItem.SHOW_AS_ACTION_WITH_TEXT);

            menu.add(0, MENU_FILL_PROFILE, 0, R.string.profile_menu_fill_from_state)
                    .setAlphabeticShortcut('f')
                    .setEnabled(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REMOVE:
                mLastSelectedPosition = -1; // reset
                showDialog(DIALOG_REMOVE_PROFILE);
                return true;
            case MENU_FILL_PROFILE:
                showDialog(DIALOG_FILL_FROM_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ConnectionOverrideItem generateConnectionOverrideItem(int connectionId) {
        ConnectionSettings settings = mProfile.getSettingsForConnection(connectionId);
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
            mProfile.setConnectionSettings(settings);
        }
        return new ConnectionOverrideItem(connectionId, settings);
    }

    private VolumeStreamItem generateVolumeStreamItem(int stream) {
        StreamSettings settings = mProfile.getSettingsForStream(stream);
        if (settings == null) {
            settings = new StreamSettings(stream);
            mProfile.setStreamSettings(settings);
        }
        return new VolumeStreamItem(stream, settings);
    }

    private TriggerItem generateTriggerItem(int whichTrigger) {
        return new TriggerItem(mProfile, whichTrigger);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);

        if (mNewProfileMode) {
            TextView desc = new TextView(getActivity());
            int descPadding = getResources().getDimensionPixelSize(
                    R.dimen.profile_instruction_padding);
            desc.setPadding(descPadding, descPadding, descPadding, descPadding);
            desc.setText(R.string.profile_setup_actions_description);
            setHeaderView(desc);
        }
    }

    private void updateProfile() {
        mProfileManager.updateProfile(mProfile);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            if (mNewProfileMode) {
                getActivity().getActionBar().setTitle(R.string.profile_setup_actions_title);
            } else {
                getActivity().getActionBar().setTitle(mProfile.getName());
            }
        }
    }

    private AlertDialog requestFillProfileFromSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.profile_populate_profile_from_state);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fillProfileFromCurrentSettings();
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    private void fillProfileFromCurrentSettings() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                fillProfileWithCurrentSettings(getActivity(), mProfile);
                updateProfile();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                rebuildItemList();
            }
        }.execute((Void) null);
    }

    public static void fillProfileWithCurrentSettings(Context context, Profile profile) {
        // bt
        if (DeviceUtils.deviceSupportsBluetooth()) {
            profile.setConnectionSettings(
                    new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH,
                            BluetoothAdapter.getDefaultAdapter().isEnabled() ? 1 : 0,
                            true));
        }

        // location
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        profile.setConnectionSettings(
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_LOCATION,
                        locationManager.isLocationEnabled() ? 1 : 0, true));

        // wifi
        WifiManager wifiManager = context.getSystemService(WifiManager.class);
        profile.setConnectionSettings(
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_WIFI,
                        wifiManager.isWifiEnabled() ? 1 : 0, true));

        // auto sync data
        profile.setConnectionSettings(
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_SYNC,
                        ContentResolver.getMasterSyncAutomatically() ? 1 : 0, true));

        // mobile data
        if (DeviceUtils.deviceSupportsMobileData(context)) {
            ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
            profile.setConnectionSettings(
                    new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA,
                            cm.getMobileDataEnabled() ? 1 : 0, true));
        }

        // wifi hotspot
        profile.setConnectionSettings(
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_WIFIAP,
                        wifiManager.isWifiApEnabled() ? 1 : 0, true));

        // 2g/3g/4g
        // skipping this one

        // nfc
        if (DeviceUtils.deviceSupportsNfc(context)) {
            NfcManager nfcManager = context.getSystemService(NfcManager.class);
            profile.setConnectionSettings(
                    new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_NFC,
                            nfcManager.getDefaultAdapter().isEnabled() ? 1 : 0, true));
        }

        // alarm volume
        final AudioManager am = context.getSystemService(AudioManager.class);
        profile.setStreamSettings(new StreamSettings(AudioManager.STREAM_ALARM,
                am.getStreamVolume(AudioManager.STREAM_ALARM), true));

        // media volume
        profile.setStreamSettings(new StreamSettings(AudioManager.STREAM_MUSIC,
                am.getStreamVolume(AudioManager.STREAM_MUSIC), true));

        // ringtone volume
        profile.setStreamSettings(new StreamSettings(AudioManager.STREAM_RING,
                am.getStreamVolume(AudioManager.STREAM_RING), true));

        // notification volume
        profile.setStreamSettings(new StreamSettings(AudioManager.STREAM_NOTIFICATION,
                am.getStreamVolume(AudioManager.STREAM_NOTIFICATION), true));

        // ring mode
        String ringValue;
        switch (am.getRingerMode()) {
            default:
            case AudioManager.RINGER_MODE_NORMAL:
                ringValue = "normal";
                break;
            case AudioManager.RINGER_MODE_SILENT:
                ringValue = "mute";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                ringValue = "vibrate";
                break;
        }
        profile.setRingMode(new RingModeSettings(ringValue, true));

        // airplane mode
        boolean airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        profile.setAirplaneMode(new AirplaneModeSettings(airplaneMode ? 1 : 0, true));

        // lock screen mode
        // populated only from profiles, so we can read the current profile,
        // but let's skip this one
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_FILL_FROM_SETTINGS:
                return requestFillProfileFromSettingsDialog();

            case DIALOG_AIRPLANE_MODE:
                return requestAirplaneModeDialog(((AirplaneModeItem) mSelectedItem).getSettings());

            case DIALOG_BRIGHTNESS:
                return requestBrightnessDialog(((BrightnessItem) mSelectedItem).getSettings());

            case DIALOG_LOCK_MODE:
                return requestLockscreenModeDialog();

            case DIALOG_DOZE_MODE:
                return requestDozeModeDialog();

            case DIALOG_NOTIFICATION_LIGHT_MODE:
                return requestNotificationLightModeDialog();

            case DIALOG_RING_MODE:
                return requestRingModeDialog(((RingModeItem) mSelectedItem).getSettings());

            case DIALOG_CONNECTION_OVERRIDE:
                ConnectionOverrideItem connItem = (ConnectionOverrideItem) mSelectedItem;
                return requestConnectionOverrideDialog(connItem.getSettings());

            case DIALOG_VOLUME_STREAM:
                VolumeStreamItem volumeItem = (VolumeStreamItem) mSelectedItem;
                return requestVolumeDialog(volumeItem.getStreamType(), volumeItem.getSettings());

            case DIALOG_PROFILE_NAME:
                return requestProfileName();

            case DIALOG_REMOVE_PROFILE:
                return requestRemoveProfileDialog();

        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLastSelectedPosition != -1) {
            outState.putInt(LAST_SELECTED_POSITION, mLastSelectedPosition);
        }
    }

    private AlertDialog requestRemoveProfileDialog() {
        Profile current = mProfileManager.getActiveProfile();
        if (mProfile.getUuid().equals(current.getUuid())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.profile_remove_current_profile));
            builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            return builder.create();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.profile_remove_dialog_message, mProfile.getName()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mProfileManager.removeProfile(mProfile);
                finishFragment();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        return builder.create();
    }

    private AlertDialog requestLockscreenModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] lockEntries =
                getResources().getStringArray(R.array.profile_lockmode_entries);

        int defaultIndex = 0; // no action
        for (int i = 0; i < LOCKMODE_MAPPING.length; i++) {
            if (LOCKMODE_MAPPING[i] == mProfile.getScreenLockMode().getValue()) {
                defaultIndex = i;
                break;
            }
        }

        builder.setTitle(R.string.profile_lockmode_title);
        builder.setSingleChoiceItems(lockEntries, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        mProfile.setScreenLockMode(new LockSettings(LOCKMODE_MAPPING[item]));
                        updateProfile();
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private AlertDialog requestDozeModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] dozeEntries =
                getResources().getStringArray(R.array.profile_doze_entries);

        int defaultIndex = 0; // no action
        for (int i = 0; i < DOZE_MAPPING.length; i++) {
            if (DOZE_MAPPING[i] == mProfile.getDozeMode()) {
                defaultIndex = i;
                break;
            }
        }

        builder.setTitle(R.string.doze_title);
        builder.setSingleChoiceItems(dozeEntries, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        mProfile.setDozeMode(DOZE_MAPPING[item]);
                        updateProfile();
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private AlertDialog requestNotificationLightModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] notificationLightEntries =
                getResources().getStringArray(R.array.profile_notification_light_entries);

        int defaultIndex = 0; // no action
        for (int i = 0; i < NOTIFICATION_LIGHT_MAPPING.length; i++) {
            if (NOTIFICATION_LIGHT_MAPPING[i] == mProfile.getNotificationLightMode()) {
                defaultIndex = i;
                break;
            }
        }

        builder.setTitle(R.string.notification_light_title);
        builder.setSingleChoiceItems(notificationLightEntries, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        mProfile.setNotificationLightMode(NOTIFICATION_LIGHT_MAPPING[item]);
                        updateProfile();
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private AlertDialog requestAirplaneModeDialog(final AirplaneModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] connectionNames =
                getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(R.string.profile_airplanemode_title);
        builder.setSingleChoiceItems(connectionNames, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(0);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(1);
                                break;
                        }
                        mProfile.setAirplaneMode(setting);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private void requestProfileRingMode() {
        // Launch the ringtone picker
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        startActivityForResult(intent, RINGTONE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_TRIGGER_REQUEST_CODE) {
            mProfile = mProfileManager.getProfile(mProfile.getUuid());
            rebuildItemList();
        }
    }

    private AlertDialog requestRingModeDialog(final RingModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] values = getResources().getStringArray(R.array.ring_mode_values);
        final String[] names = getResources().getStringArray(R.array.ring_mode_entries);

        int defaultIndex = 0; // normal by default
        if (setting.isOverride()) {
            if (setting.getValue().equals(values[0] /* normal */)) {
                defaultIndex = 0;
            } else if (setting.getValue().equals(values[1] /* vibrate */)) {
                defaultIndex = 1; // enabled
            } else if (setting.getValue().equals(values[2] /* mute */)) {
                defaultIndex = 2; // mute
            }
        } else {
            defaultIndex = 3;
        }

        builder.setTitle(R.string.ring_mode_title);
        builder.setSingleChoiceItems(names, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // enable override, normal
                                setting.setOverride(true);
                                setting.setValue(values[0]);
                                break;
                            case 1: // enable override, vibrate
                                setting.setOverride(true);
                                setting.setValue(values[1]);
                                break;
                            case 2: // enable override, mute
                                setting.setOverride(true);
                                setting.setValue(values[2]);
                                break;
                            case 3:
                                setting.setOverride(false);
                                break;
                        }
                        mProfile.setRingMode(setting);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private AlertDialog requestConnectionOverrideDialog(final ConnectionSettings setting) {
        if (setting == null) {
            throw new UnsupportedOperationException("connection setting cannot be null");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] connectionNames =
                getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(ConnectionOverrideItem.getConnectionTitleResId(setting));
        builder.setSingleChoiceItems(connectionNames, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(0);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(1);
                                break;
                        }
                        mProfile.setConnectionSettings(setting);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    public AlertDialog requestVolumeDialog(int streamId,
                                    final StreamSettings streamSettings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(VolumeStreamItem.getNameForStream(streamId));

        final AudioManager am = getActivity().getSystemService(AudioManager.class);
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.dialog_profiles_volume_override, null);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        final CheckBox override = (CheckBox) view.findViewById(R.id.checkbox);
        override.setChecked(streamSettings.isOverride());
        override.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekBar.setEnabled(isChecked);
            }
        });
        final SeekBarVolumizer volumizer = new SeekBarVolumizer(getActivity(), streamId, null,
                null);
        volumizer.start();
        volumizer.setSeekBar(seekBar);
        seekBar.setEnabled(streamSettings.isOverride());

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = seekBar.getProgress();
                streamSettings.setOverride(override.isChecked());
                streamSettings.setValue(value);
                mProfile.setStreamSettings(streamSettings);
                mAdapter.notifyDataSetChanged();
                updateProfile();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (volumizer != null) {
                    volumizer.stop();
                }
                setOnDismissListener(null); // re-set this for next dialog
            }
        });
        return builder.create();
    }

    public AlertDialog requestBrightnessDialog(final BrightnessSettings brightnessSettings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile_brightness_title);

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.dialog_profiles_brightness_override, null);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        final CheckBox override = (CheckBox) view.findViewById(R.id.checkbox);
        override.setChecked(brightnessSettings.isOverride());
        override.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekBar.setEnabled(isChecked);
            }
        });
        seekBar.setEnabled(brightnessSettings.isOverride());
        seekBar.setMax(255);
        seekBar.setProgress(brightnessSettings.getValue());
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = seekBar.getProgress();
                brightnessSettings.setValue(value);
                brightnessSettings.setOverride(override.isChecked());
                mProfile.setBrightness(brightnessSettings);
                mAdapter.notifyDataSetChanged();
                updateProfile();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    private AlertDialog requestProfileName() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.profile_name_dialog, null);

        final EditText entry = (EditText) dialogView.findViewById(R.id.name);
        entry.setText(mProfile.getName());
        entry.setSelectAllOnFocus(true);

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rename_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = entry.getText().toString();
                        mProfile.setName(value);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        entry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String str = s.toString();
                final boolean empty = TextUtils.isEmpty(str)
                        || TextUtils.getTrimmedLength(str) == 0;
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!empty);
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = getActivity().getSystemService(InputMethodManager.class);
                imm.showSoftInput(entry, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        return alertDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_actions, container, false);

        if (mNewProfileMode) {
            showButtonBar(true);

            getBackButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finishPreferencePanel(SetupActionsFragment.this, Activity.RESULT_OK, null);
                }
            });

            getNextButton().setText(R.string.finish);
            getNextButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProfileManager.addProfile(mProfile);
                    finishPreferencePanel(SetupActionsFragment.this, Activity.RESULT_OK, null);
                }
            });
        }
        return view;
    }

    @Override
    public void onItemClick(Item item, int position) {
        mSelectedItem = item;
        mLastSelectedPosition = position;

        if (item instanceof AirplaneModeItem) {
            showDialog(DIALOG_AIRPLANE_MODE);
        } else if (item instanceof BrightnessItem) {
            showDialog(DIALOG_BRIGHTNESS);
        } else if (item instanceof LockModeItem) {
            showDialog(DIALOG_LOCK_MODE);
        } else if (item instanceof DozeModeItem) {
            showDialog(DIALOG_DOZE_MODE);
        } else if (item instanceof NotificationLightModeItem) {
            showDialog(DIALOG_NOTIFICATION_LIGHT_MODE);
        } else if (item instanceof RingModeItem) {
            showDialog(DIALOG_RING_MODE);
        } else if (item instanceof ConnectionOverrideItem) {
            showDialog(DIALOG_CONNECTION_OVERRIDE);
        } else if (item instanceof VolumeStreamItem) {
            showDialog(DIALOG_VOLUME_STREAM);
        } else if (item instanceof ProfileNameItem) {
            showDialog(DIALOG_PROFILE_NAME);
        } else if (item instanceof TriggerItem) {
            openTriggersFragment(((TriggerItem) item).getTriggerType());
        }
    }

    private void openTriggersFragment(int openTo) {
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, mProfile);
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
        args.putInt(SetupTriggersFragment.EXTRA_INITIAL_PAGE, openTo);

        PartsActivity pa = (PartsActivity) getActivity();
        pa.startPreferencePanel(SetupTriggersFragment.class.getCanonicalName(), args,
                R.string.profile_profile_manage, null, this, NEW_TRIGGER_REQUEST_CODE);
    }
}
