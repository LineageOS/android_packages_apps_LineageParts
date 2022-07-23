/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2017,2021-2022 The LineageOS Project
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

import java.util.UUID;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;

import org.lineageos.lineageparts.R;

/**
 * Activity to support attaching a unknown NFC tag to an existing profile.
 */
public class NFCProfileSelect extends Activity {

    private static final String TAG = "NFCProfileSelect";

    private static final int DEFAULT_CHOICE = -1;

    public static final String EXTRA_PROFILE_UUID = "PROFILE_UUID";

    private ProfileManager mProfileManager;
    private UUID mProfileUuid;
    private int mCurrentChoice = DEFAULT_CHOICE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileManager = ProfileManager.getInstance(this);

        setContentView(R.layout.nfc_select);
        setTitle(R.string.profile_unknown_nfc_tag);

        findViewById(R.id.add_tag).setOnClickListener(v -> showProfileSelectionDialog());
    }

    @Override
    public void onResume() {
        super.onResume();

        String profileUuid = getIntent().getStringExtra(EXTRA_PROFILE_UUID);
        if (profileUuid != null) {
            mProfileUuid = UUID.fromString(profileUuid);
        } else {
            finish();
        }
    }

    void showProfileSelectionDialog() {
        final Profile[] profiles = mProfileManager.getProfiles();
        final String[] profileNames = new String[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            profileNames[i] = profiles[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.profile_settings_title);
        builder.setSingleChoiceItems(profileNames, mCurrentChoice,
                (DialogInterface.OnClickListener) (dialog, which) -> mCurrentChoice = which);
        builder.setPositiveButton(android.R.string.ok,
                (DialogInterface.OnClickListener) (dialog, which) -> {
            if (mCurrentChoice != DEFAULT_CHOICE) {
                Profile profile = profiles[mCurrentChoice];
                profile.addSecondaryUuid(mProfileUuid);
                mProfileManager.updateProfile(profile);
                Toast.makeText(NFCProfileSelect.this, R.string.profile_write_success,
                        Toast.LENGTH_LONG).show();
            }
            finish();
        });
        builder.setNegativeButton(android.R.string.cancel,
                (DialogInterface.OnClickListener) (dialog, which) -> finish());
        builder.show();
    }
}
