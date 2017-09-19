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
package org.cyanogenmod.cmparts.search;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesProvider;
import android.util.ArraySet;
import android.util.Log;

import org.cyanogenmod.cmparts.search.Searchable.SearchIndexProvider;
import org.cyanogenmod.internal.cmparts.PartInfo;
import org.cyanogenmod.internal.cmparts.PartsList;
import org.cyanogenmod.platform.internal.R;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
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
import static org.cyanogenmod.internal.cmparts.PartsList.CMPARTS_ACTIVITY;

/**
 * Provides search metadata to the Settings app
 */
public class CMPartsSearchIndexablesProvider extends SearchIndexablesProvider {

    private static final String TAG = CMPartsSearchIndexablesProvider.class.getSimpleName();

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
            if (i == null || i.getXmlRes() <= 0) {
                continue;
            }

            Object[] ref = new Object[7];
            ref[COLUMN_INDEX_XML_RES_RANK] = 2;
            ref[COLUMN_INDEX_XML_RES_RESID] = i.getXmlRes();
            ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = null;
            ref[COLUMN_INDEX_XML_RES_ICON_RESID] = R.drawable.ic_launcher_cyanogenmod;
            ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = i.getAction();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = CMPARTS_ACTIVITY.getPackageName();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = CMPARTS_ACTIVITY.getClassName();
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
            if (i == null) {
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
                Object[] ref = new Object[14];
                ref[COLUMN_INDEX_RAW_RANK] = raw.rank > 0 ?
                        raw.rank : 2;
                ref[COLUMN_INDEX_RAW_TITLE] = raw.title != null ?
                        raw.title : i.getTitle();
                ref[COLUMN_INDEX_RAW_SUMMARY_ON] = i.getSummary();
                ref[COLUMN_INDEX_RAW_KEYWORDS] = raw.keywords;
                ref[COLUMN_INDEX_RAW_ENTRIES] = raw.entries;
                ref[COLUMN_INDEX_RAW_SCREEN_TITLE] = raw.screenTitle != null ?
                        raw.screenTitle : i.getTitle();
                ref[COLUMN_INDEX_RAW_ICON_RESID] = raw.iconResId > 0 ? raw.iconResId :
                        (i.getIconRes() > 0 ? i.getIconRes() : R.drawable.ic_launcher_cyanogenmod);
                ref[COLUMN_INDEX_RAW_INTENT_ACTION] = raw.intentAction != null ?
                        raw.intentAction : i.getAction();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE] = raw.intentTargetPackage != null ?
                        raw.intentTargetPackage : CMPARTS_ACTIVITY.getPackageName();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_CLASS] = raw.intentTargetClass != null ?
                        raw.intentTargetClass : CMPARTS_ACTIVITY.getClassName();
                ref[COLUMN_INDEX_RAW_KEY] = raw.key != null ?
                        raw.key : i.getName();
                ref[COLUMN_INDEX_RAW_USER_ID] = -1;
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
            if (i == null) {
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

        if (clazz == null || !Searchable.class.isAssignableFrom(clazz)) {
            return null;
        }

        try {
            final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return (SearchIndexProvider) f.get(null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
