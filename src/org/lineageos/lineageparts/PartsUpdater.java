/*
 * Copyright (C) 2016 The CyanogenMod Project
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
package org.cyanogenmod.cmparts;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.cyanogenmod.internal.cmparts.PartInfo;
import org.cyanogenmod.internal.cmparts.PartsList;

import java.lang.reflect.Field;

import cyanogenmod.preference.RemotePreferenceUpdater;
import cyanogenmod.preference.SettingsHelper;

import static cyanogenmod.preference.RemotePreference.EXTRA_KEY;
import static cyanogenmod.preference.RemotePreference.EXTRA_SUMMARY;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART;

/**
 * PartsRefresher keeps remote UI clients up to date with any changes in the
 * state of the Part which should be reflected immediately. For preferences,
 * the clear use case is refreshing the summary.
 *
 * This works in conjunction with CMPartsPreference, which will send an
 * ordered broadcast requesting updated information. The part will be
 * looked up, and checked for a static SUMMARY_INFO field. If an
 * instance of SummaryInfo is found in this field, the result of the
 * broadcast will be updated with the new information.
 *
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

        if (clazz == null || !Refreshable.class.isAssignableFrom(clazz)) {
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

        if (DEBUG) Log.d(TAG, "fillResultExtras key=" + key +
                         " part=" + pi.toString());

        bundle.putParcelable(EXTRA_PART, pi);
        return true;
    }

    public interface Refreshable extends SettingsHelper.OnSettingsChangeListener {
        public interface SummaryProvider {
            public String getSummary(Context context, String key);
        }
    }
}
