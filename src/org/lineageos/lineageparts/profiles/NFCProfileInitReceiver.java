/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;

public final class NFCProfileInitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            context.startServiceAsUser(
                    new Intent(context, NFCProfileService.class), UserHandle.SYSTEM);
        }
    }
}
