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

import java.util.EnumSet;

/**
 * Interface for an checked change listener of the network restriction manager app list.
 */
public interface AppListCheckedChangeListener {
    /**
     * Callback for an checked change event. This callback is called, when the user changes
     * a checkbox in the network restriction manager app list.
     * @param info The corresponding AppInfo.
     * @param flags Flags for determining which checkbox was checked.
     */
    void onAppListCheckedChange(AppInfo info, EnumSet<AppListChangeFlag> flags);
}
