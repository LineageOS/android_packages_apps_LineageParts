/*
 * SPDX-FileCopyrightText: 2021-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.atv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = KeyHandler.class.getSimpleName();
    private static Map<Integer, String> KEYMAP;

    private final Context mContext;

    public KeyHandler(Context context) {
        mContext = context;

        int[] keycodes = mContext.getResources().getIntArray(
                org.lineageos.platform.internal.R.array.keyhandler_keycodes);
        String[] packages = mContext.getResources().getStringArray(
                org.lineageos.platform.internal.R.array.keyhandler_packages);

        KEYMAP = IntStream.range(0, keycodes.length).boxed()
                .collect(Collectors.toMap(i -> keycodes[i], i -> packages[i]));
    }

    public KeyEvent handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP || !hasSetupCompleted()) {
            return event;
        }

        int keyCode = event.getKeyCode();
        String targetName = KEYMAP.get(keyCode);

        if (targetName != null) {
            launchTarget(targetName);
            return null;
        }

        return event;
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TV_USER_SETUP_COMPLETE, 0) != 0;
    }

    private void launchTarget(String targetName) {
        PackageManager pm = mContext.getPackageManager();

        // First try to look the name up as a package
        Intent launchIntent = pm.getLaunchIntentForPackage(targetName);

        // If it isn't an installed package, try as an intent
        if (launchIntent == null) {
            launchIntent = new Intent(targetName);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.putExtra("no_input_mode", true);

            if (launchIntent.resolveActivity(pm) == null) {
                launchIntent = null;
            }
        }

        // If something resolved, run it; otherwise log a warning
        if (launchIntent != null) {
            mContext.startActivity(launchIntent);
        } else {
            Log.w(TAG, "Cannot launch " + targetName + ": package/intent not found.");
        }
    }
}
