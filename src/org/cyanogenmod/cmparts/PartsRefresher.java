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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import org.cyanogenmod.internal.cmparts.PartInfo;
import org.cyanogenmod.internal.cmparts.PartsList;
import org.cyanogenmod.platform.internal.Manifest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.cyanogenmod.internal.cmparts.PartsList.ACTION_PART_CHANGED;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART_KEY;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART_SUMMARY;

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
public class PartsRefresher {

    private static final String TAG = PartsRefresher.class.getSimpleName();

    public static final String FIELD_NAME_SUMMARY_PROVIDER = "SUMMARY_PROVIDER";

    private static PartsRefresher sInstance;

    private final Context mContext;

    private final Handler mHandler;

    private final SettingsObserver mObserver;

    private PartsRefresher(Context context) {
        super();
        mContext = context;
        mHandler = new Handler();
        mObserver = new SettingsObserver();
    }

    public static synchronized PartsRefresher get(Context context) {
        if (sInstance == null) {
            sInstance = new PartsRefresher(context);
        }
        return sInstance;
    }

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

    boolean updateExtras(String key, Bundle bundle) {
        final PartInfo pi = PartsList.get(mContext).getPartInfo(key);
        if (pi == null) {
            return false;
        }

        final Refreshable.SummaryProvider si = getPartSummary(pi);
        if (si == null) {
            return false;
        }

        String summary = si.getSummary(mContext, key);

        if (Objects.equals(summary, pi.getSummary())) {
            return false;
        }

        pi.setSummary(si.getSummary(mContext, key));
        bundle.putString(EXTRA_PART_KEY, key);
        bundle.putString(EXTRA_PART_SUMMARY, pi.getSummary());
        bundle.putParcelable(EXTRA_PART, pi);
        return true;
    }

    public void refreshPart(String key) {
        final Intent i = new Intent(ACTION_PART_CHANGED);
        final Bundle extras = new Bundle();
        if (updateExtras(key, extras)) {
            i.putExtras(extras);
            mContext.sendBroadcastAsUser(i, UserHandle.CURRENT, Manifest.permission.MANAGE_PARTS);
        }
    }

    public void addTrigger(Refreshable listener, Uri... contentUris) {
        mObserver.register(listener, contentUris);
    }

    public void removeTrigger(Refreshable listener) {
        mObserver.unregister(listener);
    }

    public interface Refreshable {
        public void onRefresh(Context context, Uri what);

        public interface SummaryProvider {
            public String getSummary(Context context, String key);
        }
    }

    private final class SettingsObserver extends ContentObserver {

        private final Map<Refreshable, Set<Uri>> mTriggers = new ArrayMap<>();
        private final List<Uri> mRefs = new ArrayList<>();

        private final ContentResolver mResolver;

        public SettingsObserver() {
            super(mHandler);

            mResolver = mContext.getContentResolver();
        }

        public void register(Refreshable listener, Uri... contentUris) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.get(listener);
                if (uris == null) {
                    uris = new ArraySet<Uri>();
                    mTriggers.put(listener, uris);
                }
                for (Uri contentUri : contentUris) {
                    uris.add(contentUri);
                    if (!mRefs.contains(contentUri)) {
                        mResolver.registerContentObserver(contentUri, false, this);
                        listener.onRefresh(mContext, null);
                    }
                    mRefs.add(contentUri);
                }
            }
        }

        public void unregister(Refreshable listener) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.remove(listener);
                if (uris != null) {
                    for (Uri uri : uris) {
                        mRefs.remove(uri);
                    }
                }
                if (mRefs.size() == 0) {
                    mResolver.unregisterContentObserver(this);
                }
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mRefs) {
                super.onChange(selfChange, uri);

                final Set<Refreshable> notify = new ArraySet<>();
                for (Map.Entry<Refreshable, Set<Uri>> entry : mTriggers.entrySet()) {
                    if (entry.getValue().contains(uri)) {
                        notify.add(entry.getKey());
                    }
                }

                for (Refreshable listener : notify) {
                    listener.onRefresh(mContext, uri);
                }
            }
        }
    }

}
