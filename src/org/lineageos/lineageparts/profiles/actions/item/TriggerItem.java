/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import java.util.ArrayList;

import lineageos.app.Profile;

public class TriggerItem extends Item {
    // from Profile.TriggerType
    public static final int WIFI = 0;
    public static final int BLUETOOTH = 1;
    // not in Profile.TriggerType, but we need it.
    public static final int NFC = 2;

    private final Profile mProfile;
    private final int mTriggerType;

    public TriggerItem(Profile profile, int whichTrigger) {
        mProfile = profile;
        mTriggerType = whichTrigger;
    }

    public int getTriggerType() {
        return mTriggerType;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(getTitleString(mTriggerType));
    }

    @Override
    public String getSummary(Context context) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Profile.ProfileTrigger> triggers = mProfile.getTriggersFromType(mTriggerType);

        for (int i = 0; i < triggers.size(); i++) {
            sb.append(triggers.get(i).getName());
            if (i < (triggers.size() - 1)) {
                sb.append("\n");
            }
        }

        if (sb.length() == 0) {
            if (mTriggerType == NFC) {
                return context.getString(R.string.no_triggers_configured_nfc);
            } else {
                return context.getString(R.string.no_triggers_configured);
            }
        }

        return sb.toString();
    }

    public static int getTitleString(int triggerType) {
        switch (triggerType) {
            case WIFI:
                return R.string.profile_tabs_wifi;
            case BLUETOOTH:
                return R.string.profile_tabs_bluetooth;
            case NFC:
                return R.string.profile_tabs_nfc;
            default: return 0;
        }
    }
}
