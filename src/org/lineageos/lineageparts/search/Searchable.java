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
package org.lineageos.lineageparts.search;

import android.content.Context;

import java.util.List;
import java.util.Set;

/**
 * This interface should be implemented by classes which want to provide additional
 * dynamic metadata to the indexer. Since our entrypoints are standardized around
 * the parts catalog, there is no need to enumerate XML resources here. Keywords
 * and non-indexable keys may be supplied by a class.
 *
 * If a class wants to use this functionality, it should contain a static field
 * named SEARCH_INDEX_DATA_PROVIDER which contains an instance of SearchIndexProvider.
 * This is similar to the mechanism used by the Settings app.
 */
public interface Searchable {

    public interface SearchIndexProvider {

        public List<SearchIndexableRaw> getRawDataToIndex(Context context);

        public Set<String> getNonIndexableKeys(Context context);
    }
}
