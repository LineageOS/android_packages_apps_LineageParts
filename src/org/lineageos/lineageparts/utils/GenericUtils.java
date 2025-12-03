/*
 * SPDX-FileCopyrightText: 2025 The LineageOS project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class GenericUtils {

    public static void setComponentEnabled(Context context, String component, boolean enabled) {
        ComponentName cn = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int newState = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        if (pm.getComponentEnabledSetting(cn) != newState) {
            pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP);
        }
    }
}
