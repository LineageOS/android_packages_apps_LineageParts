/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package org.lineageos.lineageparts.profiles.actions.item;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.profiles.actions.ItemListAdapter;

import lineageos.profiles.AirplaneModeSettings;

public class AirplaneModeItem extends BaseItem {
    AirplaneModeSettings mSettings;

    public AirplaneModeItem(AirplaneModeSettings airplaneModeSettings) {
        if (airplaneModeSettings == null) {
            airplaneModeSettings = new AirplaneModeSettings();
        }
        mSettings = airplaneModeSettings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.AIRPLANEMODE_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getTitle() {
        return getString(R.string.profile_airplanemode_title);
    }

    @Override
    public String getSummary() {
        return getString(getModeString(mSettings));
    }

    public AirplaneModeSettings getSettings() {
        return mSettings;
    }

    public static int getModeString(AirplaneModeSettings settings) {
        if (settings.isOverride()) {
            if (settings.getValue() == 1) {
                return R.string.profile_action_enable;
            } else {
                return R.string.profile_action_disable;
            }
        } else {
            return R.string.profile_action_none;
        }
    }
}
