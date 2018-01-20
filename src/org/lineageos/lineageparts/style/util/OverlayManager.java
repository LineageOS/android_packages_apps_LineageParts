/*
 * Copyright (C) 2018 The LineageOS Project
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
package org.lineageos.lineageparts.style.util;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.annotation.NonNull;

public class OverlayManager {
    private OverlayHelper mHelper;
    private PackageManager mPackageManager;

    public OverlayManager(Context context) {
        this(context, ServiceManager.getService("overlay") == null ?
                null : new OverlayHelper());
    }

    private OverlayManager(Context context, OverlayHelper helper) {
        mHelper = helper;
        mPackageManager = context.getPackageManager();
    }

    public void setEnabled(@NonNull String pkg, boolean enabled) {
        if (!isChangeableOverlay(pkg)) {
            return;
        }

        mHelper.setEnabled(pkg, enabled, UserHandle.myUserId());
    }

    public boolean isEnabled(@NonNull String pkg) {
        return mHelper.isEnabled(pkg, UserHandle.myUserId());
    }

    private boolean isChangeableOverlay(@NonNull String pkg) {
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(pkg, 0);
            return pi != null && !pi.isStaticOverlay;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static class OverlayHelper {
        private final IOverlayManager mService;

        OverlayHelper() {
            mService = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        }

        public void setEnabled(@NonNull String pkg, boolean enabled, int userId) {
            try {
                mService.setEnabled(pkg, enabled, userId);
            } catch (RemoteException ignored) {
            }
        }

        public boolean isEnabled(@NonNull String pkg, int userId) {
            try {
                return mService.getOverlayInfo(pkg, userId).isEnabled();
            } catch (RemoteException ignored) {
            }
            return false;
        }
    }
}