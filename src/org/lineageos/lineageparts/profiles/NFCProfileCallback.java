/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcOemExtension;
import android.nfc.OemLogItems;
import android.nfc.Tag;
import android.nfc.cardemulation.ApduServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class NFCProfileCallback implements NfcOemExtension.Callback {
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    NFCProfileCallback(Context context) {
        mContext = context;
    }

    @Override
    public void onNdefMessage(Tag tag, NdefMessage message, Consumer<Boolean> handled) {
        NdefRecord[] records = message.getRecords();
        if (records.length == 0
                || !NFCProfile.PROFILE_MIME_TYPE.equals(records[0].toMimeType())) {
            handled.accept(false);
            return;
        }

        byte[] payload = records[0].getPayload();
        int userId = ActivityManager.getCurrentUser();
        handled.accept(true);
        if (payload.length != 16) {
            return;
        }
        mHandler.post(() -> {
            if (ActivityManager.getCurrentUser() != userId) {
                return;
            }
            Context context = mContext.createContextAsUser(UserHandle.of(userId), 0);
            new NFCProfile(context).handleProfileMimeType(payload);
        });
    }

    @Override
    public void onTagConnected(boolean connected) { }

    @Override
    public void onStateUpdated(int state) { }

    @Override
    public void onApplyRouting(Consumer<Boolean> isSkipped) {
        isSkipped.accept(false);
    }

    @Override
    public void onNdefRead(Consumer<Boolean> isSkipped) {
        isSkipped.accept(false);
    }

    @Override
    public void onEnableRequested(Consumer<Boolean> isAllowed) {
        isAllowed.accept(true);
    }

    @Override
    public void onDisableRequested(Consumer<Boolean> isAllowed) {
        isAllowed.accept(true);
    }

    @Override
    public void onBootStarted() { }

    @Override
    public void onEnableStarted() { }

    @Override
    public void onDisableStarted() { }

    @Override
    public void onBootFinished(int status) { }

    @Override
    public void onEnableFinished(int status) { }

    @Override
    public void onDisableFinished(int status) { }

    @Override
    public void onTagDispatch(Consumer<Boolean> isSkipped) {
        isSkipped.accept(false);
    }

    @Override
    public void onRoutingChanged(Consumer<Boolean> isCommitRoutingSkipped) {
        isCommitRoutingSkipped.accept(false);
    }

    @Override
    public void onHceEventReceived(int action) { }

    @Override
    public void onReaderOptionChanged(boolean enabled) { }

    @Override
    public void onCardEmulationActivated(boolean isActivated) { }

    @Override
    public void onRfFieldDetected(boolean isActive) { }

    @Override
    public void onRfDiscoveryStarted(boolean isDiscoveryStarted) { }

    @Override
    public void onEeListenActivated(boolean isActivated) { }

    @Override
    public void onEeUpdated() { }

    @Override
    public void onGetOemAppSearchIntent(List<String> packages, Consumer<Intent> intentConsumer) {
        intentConsumer.accept(null);
    }

    @Override
    public void onLaunchHceAppChooserActivity(String selectedAid, List<ApduServiceInfo> services,
            ComponentName failedComponent, String category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onLaunchHceTapAgainDialog(ApduServiceInfo service, String category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onRoutingTableFull() { }

    @Override
    public void onLogEventNotified(OemLogItems item) { }

    @Override
    public void onExtractOemPackages(NdefMessage message, Consumer<List<String>> packageConsumer) {
        packageConsumer.accept(Collections.emptyList());
    }
}
