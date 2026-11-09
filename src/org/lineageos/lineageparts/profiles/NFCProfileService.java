/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.app.Service;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcOemExtension;
import android.os.IBinder;

public final class NFCProfileService extends Service {
    private NfcOemExtension mExtension;
    private NFCProfileCallback mCallback;
    private boolean mRegistered;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mExtension == null) {
            return START_NOT_STICKY;
        }
        if (mRegistered) {
            mExtension.unregisterCallback(mCallback);
        }
        mExtension.registerCallback(Runnable::run, mCallback);
        mRegistered = true;
        mExtension.triggerInitialization();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            mExtension.unregisterCallback(mCallback);
        }
        super.onDestroy();
    }
}
