/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2017,2019-2021 The LineageOS Project
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

package org.lineageos.lineageparts.sounds;

import android.app.Activity;
import android.database.Cursor;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;

import androidx.preference.Preference;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class ChargingSoundsSettings extends SettingsPreferenceFragment {

    private static final String KEY_CHARGING_VIBRATION_ENABLED = "charging_vibration_enabled";
    private static final String KEY_WIRED_CHARGING_SOUNDS = "charging_sounds";
    private static final String KEY_WIRELESS_CHARGING_SOUNDS = "wireless_charging_sounds";

    // Used for power notification uri string if set to silent
    private static final String RINGTONE_SILENT_URI_STRING = "silent";

    private static final String DEFAULT_WIRED_CHARGING_SOUND =
            "/product/media/audio/ui/ChargingStarted.ogg";
    private static final String DEFAULT_WIRELESS_CHARGING_SOUND =
            "/product/media/audio/ui/WirelessChargingStarted.ogg";

    // Request code for charging notification ringtone picker
    private static final int REQUEST_CODE_WIRED_CHARGING_SOUND = 1;
    private static final int REQUEST_CODE_WIRELESS_CHARGING_SOUND = 2;

    private Preference mWiredChargingSounds;
    private Preference mWirelessChargingSounds;

    private Uri mDefaultWiredChargingSoundUri;
    private Uri mDefaultWirelessChargingSoundUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.charging_sounds_settings);

        Vibrator vibrator = getActivity().getSystemService(Vibrator.class);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_CHARGING_VIBRATION_ENABLED);
        }

        mWiredChargingSounds = findPreference(KEY_WIRED_CHARGING_SOUNDS);
        mWirelessChargingSounds = findPreference(KEY_WIRELESS_CHARGING_SOUNDS);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String currentWiredChargingSound = Settings.Global.getString(getContentResolver(),
                Settings.Global.CHARGING_STARTED_SOUND);
        final String currentWirelessChargingSound = Settings.Global.getString(getContentResolver(),
                Settings.Global.WIRELESS_CHARGING_STARTED_SOUND);

        // Convert default sound file path to a media uri so that we can
        // set a proper default for the ringtone picker.
        mDefaultWiredChargingSoundUri = audioFileToUri(getContext(),
                DEFAULT_WIRED_CHARGING_SOUND);
        mDefaultWirelessChargingSoundUri = audioFileToUri(getContext(),
                DEFAULT_WIRELESS_CHARGING_SOUND);

        updateChargingSounds(currentWiredChargingSound, false /* wireless */);
        updateChargingSounds(currentWirelessChargingSound, true /* wireless */);
    }

    private Uri audioFileToUri(Context context, String audioFile) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media._ID },
                MediaStore.Audio.Media.DATA + "=? ",
                new String[] { audioFile }, null);
        if (cursor == null) {
            return null;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
        cursor.close();
        return Uri.withAppendedPath(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                Integer.toString(id));
    }

    private void updateChargingSounds(String toneUriString, boolean wireless) {
        final String toneTitle;

        if (wireless) {
            if ((toneUriString == null || toneUriString.equals(DEFAULT_WIRELESS_CHARGING_SOUND))
                    && mDefaultWirelessChargingSoundUri != null) {
                toneUriString = mDefaultWirelessChargingSoundUri.toString();
            }
        } else {
            if ((toneUriString == null || toneUriString.equals(DEFAULT_WIRED_CHARGING_SOUND))
                    && mDefaultWiredChargingSoundUri != null) {
                toneUriString = mDefaultWiredChargingSoundUri.toString();
            }
        }

        if (toneUriString != null && !toneUriString.equals(RINGTONE_SILENT_URI_STRING)) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(),
                    Uri.parse(toneUriString));
            if (ringtone != null) {
                toneTitle = ringtone.getTitle(getActivity());
            } else {
                // Unlikely to ever happen, but is possible if the ringtone
                // previously chosen is removed during an upgrade
                toneTitle = "";
                toneUriString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
            }
        } else {
            // Silent
            toneTitle = getString(R.string.charging_sounds_ringtone_silent);
            toneUriString = RINGTONE_SILENT_URI_STRING;
        }

        if (wireless) {
            mWirelessChargingSounds.setSummary(toneTitle);
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.WIRELESS_CHARGING_STARTED_SOUND, toneUriString);
        } else {
            mWiredChargingSounds.setSummary(toneTitle);
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.CHARGING_STARTED_SOUND, toneUriString);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mWiredChargingSounds) {
            launchNotificationSoundPicker(REQUEST_CODE_WIRED_CHARGING_SOUND,
                    Settings.Global.getString(getContentResolver(),
                            Settings.Global.CHARGING_STARTED_SOUND));
        } else if (preference == mWirelessChargingSounds) {
            launchNotificationSoundPicker(REQUEST_CODE_WIRELESS_CHARGING_SOUND,
                    Settings.Global.getString(getContentResolver(),
                            Settings.Global.WIRELESS_CHARGING_STARTED_SOUND));
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void launchNotificationSoundPicker(int requestCode, String toneUriString) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        if (requestCode == REQUEST_CODE_WIRED_CHARGING_SOUND) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                    getString(R.string.wired_charging_sounds_title));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    mDefaultWiredChargingSoundUri);
        } else if (requestCode == REQUEST_CODE_WIRELESS_CHARGING_SOUND) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                    getString(R.string.wireless_charging_sounds_title));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    mDefaultWirelessChargingSoundUri);
        }
        if (toneUriString != null && !toneUriString.equals(RINGTONE_SILENT_URI_STRING)) {
            Uri uri = Uri.parse(toneUriString);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            }
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIRED_CHARGING_SOUND
                && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            updateChargingSounds(uri != null ? uri.toString() : null, false /* wireless */);
        } else if (requestCode == REQUEST_CODE_WIRELESS_CHARGING_SOUND
                && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            updateChargingSounds(uri != null ? uri.toString() : null, true /* wireless */);
        }
    }
}
