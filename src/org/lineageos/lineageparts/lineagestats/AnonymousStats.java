/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.lineagestats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class AnonymousStats extends SettingsPreferenceFragment {

    private static final String PREF_FILE_NAME = "LineageStats";
    static final String ANONYMOUS_OPT_IN = "pref_anonymous_opt_in";
    static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";

    static final String KEY_LAST_JOB_ID = "last_job_id";
    static final int QUEUE_MAX_THRESHOLD = 1000;

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.anonymous_stats);
    }

    public static void updateLastSynced(Context context) {
        getPreferences(context)
                .edit()
                .putLong(ANONYMOUS_LAST_CHECKED,System.currentTimeMillis())
                .commit();
    }

    public static int getLastJobId(Context context) {
        return getPreferences(context).getInt(KEY_LAST_JOB_ID, 0);
    }

    private static void setLastJobId(Context context, int id) {
        getPreferences(context)
                .edit()
                .putInt(KEY_LAST_JOB_ID, id)
                .commit();
    }

    public static int getNextJobId(Context context) {
        int lastId = getLastJobId(context);
        if (lastId >= QUEUE_MAX_THRESHOLD) {
            lastId = 1;
        } else {
            lastId += 1;
        }
        setLastJobId(context, lastId);
        return lastId;
    }
}
