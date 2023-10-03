/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.lineagestats;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceScreen;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class PreviewData extends SettingsPreferenceFragment {
    private static final String UNIQUE_ID = "preview_id";
    private static final String DEVICE = "preview_device";
    private static final String VERSION = "preview_version";
    private static final String COUNTRY = "preview_country";
    private static final String CARRIER = "preview_carrier";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preview_data);

        final PreferenceScreen prefSet = getPreferenceScreen();
        final Context context = requireActivity();

        prefSet.findPreference(UNIQUE_ID).setSummary(Utilities.getUniqueID(context));
        prefSet.findPreference(DEVICE).setSummary(Utilities.getDevice());
        prefSet.findPreference(VERSION).setSummary(Utilities.getModVersion());
        prefSet.findPreference(COUNTRY).setSummary(Utilities.getCountryCode(context));
        prefSet.findPreference(CARRIER).setSummary(Utilities.getCarrier(context));
    }
}
