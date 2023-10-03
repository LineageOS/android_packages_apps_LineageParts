/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;

import org.lineageos.lineageparts.R;

import java.util.UUID;

/**
 * Activity to support writing a profile to an NFC tag.
 * The mime type is "lineage/profile" and the payload is the raw bytes of the profile's
 * UUID. The payload was intentionally kept small to support writing on 46-byte tags.
 */
public class NFCProfileWriter extends Activity {

    private static final String TAG = "NFCProfileWriter";

    static final String EXTRA_PROFILE_UUID = "PROFILE_UUID";

    private NfcAdapter mNfcAdapter;

    private Profile mProfile;

    private ProfileManager mProfileManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mProfileManager = ProfileManager.getInstance(this);

        setContentView(R.layout.nfc_writer);
        setTitle(R.string.profile_write_nfc_tag);
    }

    @Override
    public void onResume() {
        super.onResume();
        String profileUuid = getIntent().getStringExtra(EXTRA_PROFILE_UUID);
        if (profileUuid != null) {
            mProfile = mProfileManager.getProfile(UUID.fromString(profileUuid));
            Log.d(TAG, "Profile to write: " + mProfile.getName());
            enableTagWriteMode();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        disableTagWriteMode();
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_IMMUTABLE);
    }

    private void disableTagWriteMode() {
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                tagDetected
        };
        mNfcAdapter.enableForegroundDispatch(this, getPendingIntent(), writeTagFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);
            if (NFCProfileUtils.writeTag(NFCProfileUtils.getProfileAsNdef(mProfile), detectedTag)) {
                Toast.makeText(this, R.string.profile_write_success, Toast.LENGTH_LONG).show();
                NFCProfileUtils.vibrate(this);
            } else {
                Toast.makeText(this, R.string.profile_write_failed, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
