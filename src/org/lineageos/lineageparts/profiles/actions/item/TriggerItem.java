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

import java.util.ArrayList;

import lineageos.app.Profile;

public class TriggerItem extends BaseItem {

    // from Profile.TriggerType
    public static final int WIFI = 0;
    public static final int BLUETOOTH = 1;
    // not in Profile.TriggerType, but we need it.
    public static final int NFC = 2;

    Profile mProfile;
    int mTriggerType;

    public TriggerItem(Profile profile, int whichTrigger) {
        mProfile = profile;
        mTriggerType = whichTrigger;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.TRIGGER_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public int getTriggerType() {
        return mTriggerType;
    }

    @Override
    public String getTitle() {
        return getString(getTitleString(mTriggerType));
    }

    @Override
    public String getSummary() {
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
                return getString(R.string.no_triggers_configured_nfc);
            } else {
                return getString(R.string.no_triggers_configured);
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
