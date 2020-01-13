/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2020 The LineageOS Project
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

import android.app.NotificationGroup;
import android.content.Context;

import org.lineageos.lineageparts.R;

import java.util.UUID;

import lineageos.app.Profile;
import lineageos.app.ProfileGroup;

public class AppGroupItem extends Item {
    Profile mProfile;
    ProfileGroup mGroup;
    NotificationGroup mNotifGroup;

    public AppGroupItem() {
        // empty app group will act as a "Add/remove app groups" item
    }

    public AppGroupItem(Profile profile, ProfileGroup group, NotificationGroup nGroup) {
        mProfile = profile;
        if (group == null) {
            throw new UnsupportedOperationException("profile group can't be null");
        }
        mGroup = group;
        mNotifGroup = nGroup;
    }

    public UUID getGroupUuid() {
        if (mGroup != null) {
            return mGroup.getUuid();
        }
        return null;
    }

    @Override
    public String getTitle(Context context) {
        if (mGroup == null) {
            return context.getString(R.string.profile_app_group_item_instructions);
        }
        if (mNotifGroup != null) {
            return mNotifGroup.getName();
        }
        return "<unknown>";
    }

    @Override
    public String getSummary(Context context) {
        if (mGroup == null) {
            return context.getString(R.string.profile_app_group_item_instructions_summary);
        }
        return null;
    }
}
