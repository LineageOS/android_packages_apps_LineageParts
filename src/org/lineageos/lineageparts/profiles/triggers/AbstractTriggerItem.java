/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.triggers;

import lineageos.app.Profile;

public class AbstractTriggerItem {
    private int mIcon;
    private String mSummary;
    private String mTitle;

    private int mTriggerState = Profile.TriggerState.DISABLED;

    public void setTriggerState(int trigger) {
        mTriggerState = trigger;
    }

    public int getTriggerState() {
        return mTriggerState;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSummary() {
        return mSummary;
    }

    public void setIcon(int icon) {
        mIcon = icon;
    }

    public int getIcon() {
        return mIcon;
    }
}
