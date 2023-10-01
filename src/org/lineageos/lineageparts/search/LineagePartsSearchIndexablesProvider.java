/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.search;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesProvider;
import android.util.ArraySet;
import android.util.Log;

import lineageos.preference.PartInfo;
import lineageos.preference.PartsList;

import org.lineageos.lineageparts.search.Searchable.SearchIndexProvider;
import org.lineageos.platform.internal.R;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_PAYLOAD;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_PAYLOAD_TYPE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

import static lineageos.preference.PartsList.LINEAGEPARTS_ACTIVITY;

/**
 * Provides search metadata to the Settings app
 */
public class LineagePartsSearchIndexablesProvider extends SearchIndexablesProvider {

    private static final String TAG = LineagePartsSearchIndexablesProvider.class.getSimpleName();

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    @Override
    public Cursor queryXmlResources(String[] strings) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        final Set<String> keys = PartsList.get(getContext()).getPartsList();

        // return all of the xml resources listed in the resource: attribute
        // from parts_catalog.xml for indexing
        for (String key : keys) {
            PartInfo i = PartsList.get(getContext()).getPartInfo(key);
            if (i == null || i.getXmlRes() <= 0 || !i.isAvailable()) {
                continue;
            }

            Object[] ref = new Object[INDEXABLES_XML_RES_COLUMNS.length];
            ref[COLUMN_INDEX_XML_RES_RANK] = 2;
            ref[COLUMN_INDEX_XML_RES_RESID] = i.getXmlRes();
            ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = null;
            ref[COLUMN_INDEX_XML_RES_ICON_RESID] = R.drawable.ic_launcher_lineageos;
            ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = i.getAction();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = LINEAGEPARTS_ACTIVITY.
                    getPackageName();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = LINEAGEPARTS_ACTIVITY.getClassName();
            cursor.addRow(ref);
        }
        return cursor;

    }

    @Override
    public Cursor queryRawData(String[] strings) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        final Set<String> keys = PartsList.get(getContext()).getPartsList();

        // we also submit keywords and metadata for all top-level items
        // which don't have an associated XML resource
        for (String key : keys) {
            PartInfo i = PartsList.get(getContext()).getPartInfo(key);
            if (i == null || !i.isAvailable()) {
                continue;
            }

            // look for custom keywords
            SearchIndexProvider sip = getSearchIndexProvider(i.getFragmentClass());
            if (sip == null) {
                continue;
            }

            // don't create a duplicate entry if no custom keywords are provided
            // and a resource was already indexed
            List<SearchIndexableRaw> rawList = sip.getRawDataToIndex(getContext());
            if (rawList == null || rawList.size() == 0) {
                if (i.getXmlRes() > 0) {
                    continue;
                }
                rawList = Collections.singletonList(new SearchIndexableRaw(getContext()));
            }

            for (SearchIndexableRaw raw : rawList) {
                Object[] ref = new Object[INDEXABLES_RAW_COLUMNS.length];
                ref[COLUMN_INDEX_RAW_RANK] = raw.rank > 0 ?
                        raw.rank : 2;
                ref[COLUMN_INDEX_RAW_TITLE] = raw.title != null ?
                        raw.title : i.getTitle();
                ref[COLUMN_INDEX_RAW_SUMMARY_ON] = i.getSummary();
                ref[COLUMN_INDEX_RAW_SUMMARY_OFF] = null;
                ref[COLUMN_INDEX_RAW_ENTRIES] = raw.entries;
                ref[COLUMN_INDEX_RAW_KEYWORDS] = raw.keywords;
                ref[COLUMN_INDEX_RAW_SCREEN_TITLE] = raw.screenTitle != null ?
                        raw.screenTitle : i.getTitle();
                ref[COLUMN_INDEX_RAW_CLASS_NAME] = null;
                ref[COLUMN_INDEX_RAW_ICON_RESID] = raw.iconResId > 0 ? raw.iconResId :
                        (i.getIconRes() > 0 ? i.getIconRes() : R.drawable.ic_launcher_lineageos);
                ref[COLUMN_INDEX_RAW_INTENT_ACTION] = raw.intentAction != null ?
                        raw.intentAction : i.getAction();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE] = raw.intentTargetPackage != null ?
                        raw.intentTargetPackage : LINEAGEPARTS_ACTIVITY.getPackageName();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_CLASS] = raw.intentTargetClass != null ?
                        raw.intentTargetClass : LINEAGEPARTS_ACTIVITY.getClassName();
                ref[COLUMN_INDEX_RAW_KEY] = raw.key != null ?
                        raw.key : i.getName();
                ref[COLUMN_INDEX_RAW_USER_ID] = -1;
                ref[COLUMN_INDEX_RAW_PAYLOAD_TYPE] = null;
                ref[COLUMN_INDEX_RAW_PAYLOAD] = null;
                cursor.addRow(ref);
            }
        }
        return cursor;
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] strings) {
        MatrixCursor cursor = new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);

        final Set<String> keys = PartsList.get(getContext()).getPartsList();
        final Set<String> nonIndexables = new ArraySet<>();

        for (String key : keys) {
            PartInfo i = PartsList.get(getContext()).getPartInfo(key);
            if (i == null || !i.isAvailable()) {
                continue;
            }

            // look for non-indexable keys
            SearchIndexProvider sip = getSearchIndexProvider(i.getFragmentClass());
            if (sip == null) {
                continue;
            }

            Set<String> nik = sip.getNonIndexableKeys(getContext());
            if (nik == null) {
                continue;
            }

            nonIndexables.addAll(nik);
        }

        for (String nik : nonIndexables) {
            Object[] ref = new Object[1];
            ref[COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE] = nik;
            cursor.addRow(ref);
        }
        return cursor;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private SearchIndexProvider getSearchIndexProvider(final String className) {

        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Cannot find class: " + className);
            return null;
        }

        if (!Searchable.class.isAssignableFrom(clazz)) {
            return null;
        }

        try {
            final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return (SearchIndexProvider) f.get(null);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Cannot find field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (SecurityException se) {
            Log.e(TAG,
                    "Security exception for field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access to field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument when accessing field '"
                    + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        }
        return null;
    }
}
