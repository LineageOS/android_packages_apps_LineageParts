/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.disabler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

class DependentDisabler {
    private static boolean isDependencyDisabled(PackageManager pm, String[] dependencies) {
        for (String dep : dependencies) {
            try {
                PackageInfo info = pm.getPackageInfo(dep, 0);
                if (!info.applicationInfo.enabled) {
                    return true; // Disable if any dependency is disabled
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                return true; // Disable if any dependency is not installed
            }
        }
        return false;
    }

    public static void enableOrDisableDependent(Context context) {
        Resources res = context.getResources();
        String[] dependencies = res.getStringArray(
                org.lineageos.platform.internal.R.array.config_app_dependencies);
        String[] dependentPackages = res.getStringArray(
                org.lineageos.platform.internal.R.array.config_app_dependent_packages);

        // Return early if either dependencies or dependentPackages is empty
        if (dependencies.length == 0 || dependentPackages.length == 0) {
            return;
        }

        PackageManager pm = context.getPackageManager();
        int flag = isDependencyDisabled(pm, dependencies)
                ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        for (String pkg : dependentPackages) {
            pm.setApplicationEnabledSetting(pkg, flag, 0);
        }
    }
}
