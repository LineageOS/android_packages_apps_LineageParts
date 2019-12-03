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

import android.graphics.drawable.Drawable;

/**
 * Class which contains all data necessary for displaying one row in the firewall app list.
 * Some data is read-only like the app name, app icon, etc.
 */
public class FirewallAppInfo {

    /**
     * Read-only data, which can be set via the constructor.
     */
    private int mUid;
    private boolean mIsSystemApp;
    private Drawable mAppIcon;
    private String mName;
    private String mPackageName;

    /**
     * Writeable data, which can be changed via the firewalls user interface.
     */
    private boolean mIsWifiEnabled;
    private boolean mIsCellEnabled;
    private boolean mIsVpnEnabled;

    /**
     * Constructor for setting the read-only data.
     *
     * @param icon     Icon which will be shown for the app.
     * @param name     Name of the app.
     * @param pkgName  Package name of the app.
     * @param uid      Identifier of the app.
     * @param isSysApp Flag for setting if the app is a system app.
     */
    public FirewallAppInfo(Drawable icon, String name, String pkgName, int uid, boolean isSysApp) {
        mAppIcon = icon;
        mName = name;
        mPackageName = pkgName;
        mUid = uid;
        mIsSystemApp = isSysApp;
    }

    /**
     * Returns the identifier of the app.
     *
     * @return Identifier of the app.
     */
    public int getUid() {
        return mUid;
    }

    /**
     * Returns whether the app is a system app.
     *
     * @return True, if the app is a system app.
     */
    public boolean isSystemApp() {
        return mIsSystemApp;
    }

    /**
     * Returns the icon of the app.
     *
     * @return Icon of the app.
     */
    public Drawable getIcon() {
        return mAppIcon;
    }

    /**
     * Returns the name of the app for the user interface.
     *
     * @return Name of the app.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the package name of the app.
     *
     * @return Package name of the app.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns whether the access to wifi network is granted.
     *
     * @return True, when the wifi network access is granted.
     */
    public boolean isWifiEnabled() {
        return mIsWifiEnabled;
    }

    /**
     * Sets if the wifi network access is granted.
     * This method don't changes the real network policies! User interface only.
     *
     * @param enabled True, when the wifi network access is granted.
     */
    public void setWifiEnabled(boolean enabled) {
        mIsWifiEnabled = enabled;
    }

    /**
     * Returns whether the access to cell network is granted.
     *
     * @return True, when the cell network access is granted.
     */
    public boolean isCellEnabled() {
        return mIsCellEnabled;
    }

    /**
     * Sets if the cell network access is granted.
     * This method don't changes the real network policies! User interface only.
     *
     * @param enabled True, when the cell network access is granted.
     */
    public void setCellEnabled(boolean enabled) {
        mIsCellEnabled = enabled;
    }

    /**
     * Returns whether the access to vpn network is granted.
     *
     * @return True, when the vpn network access is granted.
     */
    public boolean isVpnEnabled() {
        return mIsVpnEnabled;
    }

    /**
     * Sets if the vpn network access is granted.
     * This method don't changes the real network policies! User interface only.
     *
     * @param enabled True, when the vpn network access is granted.
     */
    public void setVpnEnabled(boolean enabled) {
        mIsVpnEnabled = enabled;
    }
}