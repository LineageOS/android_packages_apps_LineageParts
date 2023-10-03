/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import lineageos.preference.PartInfo;
import lineageos.preference.PartsList;

import java.lang.reflect.Field;

import lineageos.preference.RemotePreferenceUpdater;
import lineageos.preference.SettingsHelper;

import static lineageos.preference.PartsList.EXTRA_PART;
import static lineageos.preference.RemotePreference.EXTRA_KEY;
import static lineageos.preference.RemotePreference.EXTRA_SUMMARY;

/**
 * PartsRefresher keeps remote UI clients up to date with any changes in the
 * state of the Part which should be reflected immediately. For preferences,
 * the clear use case is refreshing the summary.
 * <p>
 * This works in conjunction with LineagePartsPreference, which will send an
 * ordered broadcast requesting updated information. The part will be
 * looked up, and checked for a static SUMMARY_INFO field. If an
 * instance of SummaryInfo is found in this field, the result of the
 * broadcast will be updated with the new information.
 * <p>
 * Parts can also call refreshPart to send an asynchronous update to any
 * active remote components via broadcast.
 */
public class PartsUpdater extends RemotePreferenceUpdater {

    private static final String TAG = PartsUpdater.class.getSimpleName();

    public static final String FIELD_NAME_SUMMARY_PROVIDER = "SUMMARY_PROVIDER";

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    private Refreshable.SummaryProvider getPartSummary(PartInfo pi) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(pi.getFragmentClass());
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Cannot find class: " + pi.getFragmentClass());
            return null;
        }

        if (!Refreshable.class.isAssignableFrom(clazz)) {
            return null;
        }

        try {
            final Field f = clazz.getField(FIELD_NAME_SUMMARY_PROVIDER);
            return (Refreshable.SummaryProvider) f.get(null);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    protected boolean fillResultExtras(Context context, String key, Bundle bundle) {
        final PartInfo pi = PartsList.get(context).getPartInfo(key);
        if (pi == null) {
            Log.w(TAG, "Part not found: " + key);
            return false;
        }

        bundle.putString(EXTRA_KEY, key);

        final Refreshable.SummaryProvider si = getPartSummary(pi);
        if (si != null) {
            pi.setSummary(si.getSummary(context, key));
            bundle.putString(EXTRA_SUMMARY, pi.getSummary());
        }

        if (DEBUG) Log.d(TAG, "fillResultExtras key=" + key + " part=" + pi);

        bundle.putParcelable(EXTRA_PART, pi);
        return true;
    }

    public interface Refreshable extends SettingsHelper.OnSettingsChangeListener {
        interface SummaryProvider {
            String getSummary(Context context, String key);
        }
    }
}
