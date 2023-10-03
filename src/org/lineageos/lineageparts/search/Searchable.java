/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.search;

import android.content.Context;

import java.util.List;
import java.util.Set;

/**
 * This interface should be implemented by classes which want to provide additional
 * dynamic metadata to the indexer. Since our entrypoints are standardized around
 * the parts catalog, there is no need to enumerate XML resources here. Keywords
 * and non-indexable keys may be supplied by a class.
 * <p>
 * If a class wants to use this functionality, it should contain a static field
 * named SEARCH_INDEX_DATA_PROVIDER which contains an instance of SearchIndexProvider.
 * This is similar to the mechanism used by the Settings app.
 */
public interface Searchable {

    interface SearchIndexProvider {

        List<SearchIndexableRaw> getRawDataToIndex(Context context);

        Set<String> getNonIndexableKeys(Context context);
    }
}
