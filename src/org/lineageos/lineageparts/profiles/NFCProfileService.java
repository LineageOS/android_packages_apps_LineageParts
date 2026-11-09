/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcOemExtension;
import android.os.IBinder;
import android.os.UserHandle;

public final class NFCProfileService extends Service {
    private NfcOemExtension mExtension;
    private NFCProfileCallback mCallback;

    public static void installHook(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            context.startServiceAsUser(
                    new Intent(context, NFCProfileService.class), UserHandle.SYSTEM);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            stopSelf();
            return;
        }
        mExtension = adapter.getNfcOemExtension();
        mCallback = new NFCProfileCallback(this);
        mExtension.registerCallback(Runnable::run, mCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mExtension != null && mCallback != null) {
            mExtension.unregisterCallback(mCallback);
        }
        super.onDestroy();
    }
}
