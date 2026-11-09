/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

import java.util.UUID;

/**
 * Handles profile UUIDs received through the private NFC OEM callback.
 * Tags should be encoded with the 16-byte UUID of the profile to be activated.
 * Tapping a tag while that profile is already active will select the previously
 * active profile.
 */
final class NFCProfile {

    private static final String PREFS_NAME = "NFCProfile";

    private static final String PREFS_PREVIOUS_PROFILE = "previous-profile";

    static final String PROFILE_MIME_TYPE = "lineage/profile";

    private final Context mContext;
    private final ProfileManager mProfileManager;

    NFCProfile(Context context) {
        mContext = context;
        mProfileManager = ProfileManager.getInstance(context);
    }

    void handleProfileMimeType(byte[] payload) {
        UUID profileUuid = NFCProfileUtils.toUUID(payload);

        boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;

        if (enabled) {
            // Only do NFC profile changing if System Profile support is enabled
            Profile currentProfile = mProfileManager.getActiveProfile();
            Profile targetProfile = mProfileManager.getProfile(profileUuid);

            if (targetProfile == null) {
                // show profile selection for unknown tag
                Intent i = new Intent(mContext, NFCProfileSelect.class);
                i.putExtra(NFCProfileSelect.EXTRA_PROFILE_UUID, profileUuid.toString());
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                mContext.startActivity(i);
            } else {
                // switch to profile
                if (currentProfile == null || !currentProfile.getUuid().equals(profileUuid)) {
                    saveCurrentProfile();
                    switchTo(profileUuid);
                } else {
                    Profile lastProfile = getPreviouslySelectedProfile();
                    if (lastProfile != null) {
                        switchTo(lastProfile.getUuid());
                        clearPreviouslySelectedProfile();
                    }
                }
            }
        }
    }

    private void switchTo(UUID uuid) {
        Profile p = mProfileManager.getProfile(uuid);
        if (p != null) {
            mProfileManager.setActiveProfile(uuid);

            Toast.makeText(
                    mContext,
                    mContext.getString(R.string.profile_selected, p.getName()),
                    Toast.LENGTH_LONG).show();
            NFCProfileUtils.vibrate(mContext);
        }
    }

    private Profile getPreviouslySelectedProfile() {
        Profile previous = null;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        String uuid = prefs.getString(PREFS_PREVIOUS_PROFILE, null);
        if (uuid != null) {
            previous = mProfileManager.getProfile(UUID.fromString(uuid));
        }
        return previous;
    }

    private void clearPreviouslySelectedProfile() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME, 0).edit();
        editor.remove(PREFS_PREVIOUS_PROFILE);
        editor.commit();
    }

    private void saveCurrentProfile() {
        Profile currentProfile = mProfileManager.getActiveProfile();
        if (currentProfile != null) {
            SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME, 0).edit();
            editor.putString(PREFS_PREVIOUS_PROFILE, currentProfile.getUuid().toString());
            editor.commit();
        }
    }
}
