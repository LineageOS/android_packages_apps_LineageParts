/**
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.lineageparts.applications;

import android.os.Bundle;

import com.android.settingslib.applications.ApplicationsState;

import org.lineageos.internal.applications.ForceDark;

public class ForceDarkSettings extends ApplicationToggleSettings {
    private ForceDark mForceDark;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mForceDark = new ForceDark(getContext());
    }

    @Override
    boolean shouldShowApplication(ApplicationsState.AppEntry entry) {
        // Show all apps
        return true;
    }

    @Override
    void applicationSelectionChanged(ApplicationsState.AppEntry entry, boolean selected) {
        if (selected) {
            mForceDark.addApp(entry.info.packageName);
        } else {
            mForceDark.removeApp(entry.info.packageName);
        }
    }

    @Override
    boolean isApplicationSelected(ApplicationsState.AppEntry entry) {
        return mForceDark.shouldForceDarkTheme(entry.info.packageName);
    }

}
