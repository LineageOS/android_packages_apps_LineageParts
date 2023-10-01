/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017,2021-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles.triggers;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import lineageos.app.Profile;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.profiles.NFCProfileTagCallback;
import org.lineageos.lineageparts.profiles.NFCProfileUtils;
import org.lineageos.lineageparts.profiles.ProfilesSettings;

public class NfcTriggerFragment extends Fragment implements NFCProfileTagCallback {
    Profile mProfile;

    private NfcAdapter mNfcAdapter;

    public static NfcTriggerFragment newInstance(Profile profile) {
        NfcTriggerFragment fragment = new NfcTriggerFragment();

        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public NfcTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        }
        ((PartsActivity) getActivity()).setNfcProfileCallback(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((PartsActivity) getActivity()).setNfcProfileCallback(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProfile != null) {
            enableTagWriteMode();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableTagWriteMode();
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(getActivity(), getActivity().getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private void disableTagWriteMode() {
        mNfcAdapter.disableForegroundDispatch(getActivity());
    }

    private void enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                tagDetected
        };
        mNfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), writeTagFilters,
                null);
    }

    @Override
    public void onTagRead(Tag tag) {
        if (NFCProfileUtils.writeTag(NFCProfileUtils.getProfileAsNdef(mProfile), tag)) {
            Toast.makeText(getActivity(), R.string.profile_write_success, Toast.LENGTH_LONG).show();
            NFCProfileUtils.vibrate(getActivity());
        } else {
            Toast.makeText(getActivity(), R.string.profile_write_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nfc_writer, container, false);
    }
}
