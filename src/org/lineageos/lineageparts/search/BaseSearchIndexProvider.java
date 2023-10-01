/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.search;

import android.content.Context;

import java.util.List;
import java.util.Set;

/**
 * Convenience class which can be used to return additional search metadata without
 * having to implement all methods.
 */
public class BaseSearchIndexProvider implements Searchable.SearchIndexProvider {

    @Override
    public List<SearchIndexableRaw> getRawDataToIndex(Context context) {
        return null;
    }

    @Override
    public Set<String> getNonIndexableKeys(Context context) {
        return null;
    }
}
