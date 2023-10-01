/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.app.Profile;

public class LockModeItem extends Item {
    private final Profile mProfile;

    public LockModeItem(Profile profile) {
        mProfile = profile;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.profile_lockmode_title);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(getSummaryString(mProfile));
    }

    public static int getSummaryString(Profile profile) {
        switch (profile.getScreenLockMode().getValue()) {
            case Profile.LockMode.DEFAULT:
                return R.string.profile_action_system; //"leave unchanged"
            case Profile.LockMode.DISABLE:
                return R.string.profile_lockmode_disabled_summary;
            case Profile.LockMode.INSECURE:
                return R.string.profile_lockmode_insecure_summary;
            default: return 0;
        }
    }
}
