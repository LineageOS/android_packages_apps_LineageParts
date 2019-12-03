/*
 * Copyright (C) 2018-2020 The LineageOS Project
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
package org.lineageos.lineageparts.networkrestriction;

/**
 * Interface for a clicked listener on a network restriction manager app list item.
 */
public interface AppListItemClickedListener {
    /**
     * Callback for an click event on a network restriction manager app list item.
     * @param appInfo the corresponding AppInfo.
     */
    void onAppListItemClicked(AppInfo appInfo);
}
