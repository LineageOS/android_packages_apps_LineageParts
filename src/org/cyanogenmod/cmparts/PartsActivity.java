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

package org.cyanogenmod.cmparts;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.cyanogenmod.cmparts.notificationlight.BatteryLightSettings;
import org.cyanogenmod.cmparts.notificationlight.NotificationLightSettings;

public class PartsActivity extends PreferenceActivity {

    public static final String EXTRA_PART = "part";
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";

    public static final String FRAGMENT_PREFIX = "cmparts:";

    public static final String FRAGMENT_NOTIFICATION_LIGHTS = "notification_lights";
    public static final String FRAGMENT_BATTERY_LIGHTS = "battery_lights";

    private ActionBar mActionBar;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String partExtra = getIntent().getStringExtra(EXTRA_PART);
        if (partExtra != null && partExtra.startsWith(FRAGMENT_PREFIX)) {
            String[] keys = partExtra.split(":");
            if (keys.length < 2) {
                return;
            }
            String part = keys[1];
            SettingsPreferenceFragment fragment = null;
            if (part.equals(FRAGMENT_NOTIFICATION_LIGHTS)) {
                fragment = new NotificationLightSettings();
            } else if (part.equals(FRAGMENT_BATTERY_LIGHTS)) {
                fragment = new BatteryLightSettings();
            }

            mActionBar = getActionBar();
            if (mActionBar != null) {
                mActionBar.setDisplayHomeAsUpEnabled(true);
                mActionBar.setHomeButtonEnabled(true);
            }

            if (fragment != null) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commitAllowingStateLoss();
                getFragmentManager().executePendingTransactions();
            }
        }
    }


}

