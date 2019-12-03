/*
 * Copyright (C) 2018-2019 The LineageOS Project
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
package org.lineageos.lineageparts.firewall;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An asynchronous loader implementation that loads FirewallAppInfo structures.
 */
public class FirewallAppInfoLoader extends AsyncTaskLoader<List<FirewallAppInfo>> {

    /**
     * Reference to the package manager.
     */
    private PackageManager mPackageManager;

    /**
     * Constructor.
     * @param context Reference to a Context.
     */
    public FirewallAppInfoLoader(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
    }

    /**
     * Inherited method from AsyncTaskLoader. It is called on a worker thread.
     * @return List of the loaded FirewallAppInfo.
     */
    @Override
    public List<FirewallAppInfo> loadInBackground() {
        return loadInstalledApps();
    }

    /**
     * Inherited method from AsyncTaskLoader. It is called when loading is requested.
     */
    @Override
    public void onStartLoading() {
        forceLoad();
    }

    /**
     * Inherited method from AsyncTaskLoader. It is called when a cancel is requested.
     */
    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Inherited method from AsyncTaskLoader. It is called when a reset is requested.
     */
    @Override
    protected void onReset() {
        cancelLoad();
    }

    /**
     * Uses the package manager to query for all currently installed apps
     * for the list.
     *
     * @return the complete List off installed applications (@code FirewallAppInfo)
     */
    private List<FirewallAppInfo> loadInstalledApps() {
        List<FirewallAppInfo> apps = new ArrayList<FirewallAppInfo>();
        List<PackageInfo> packages = mPackageManager.getInstalledPackages(
                PackageManager.GET_PERMISSIONS);

        // Iterate through all installed apps.
        for (PackageInfo pkgInfo : packages) {
            final ApplicationInfo appInfo = pkgInfo.applicationInfo;

            // Skip packages that aren't apps (eg system components)
            if (!UserHandle.isApp(appInfo.uid)) {
                continue;
            }
            // Skip packages that don't have INTERNET permission
            if (!hasRequestedPermission(pkgInfo, android.Manifest.permission.INTERNET)) {
                continue;
            }

            // Retrieve information if the app is a system app.
            final boolean isSystemApp = ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

            // add the app to the list.
            apps.add(new FirewallAppInfo(appInfo.loadIcon(mPackageManager),
                    appInfo.loadLabel(mPackageManager).toString(),
                    appInfo.packageName,
                    appInfo.uid, isSystemApp));
        }

        // Sort apps by name
        Collections.sort(apps, new Comparator<FirewallAppInfo>() {
            @Override
            public int compare(FirewallAppInfo lhs, FirewallAppInfo rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        return apps;
    }

    private boolean hasRequestedPermission(PackageInfo pkgInfo, String requestedPermission) {
        if (pkgInfo.requestedPermissions == null) {
            return false;
        }
        for (String permission : pkgInfo.requestedPermissions) {
            if (permission.equals(requestedPermission)) {
                return true;
            }
        }
        return false;
    }
}
