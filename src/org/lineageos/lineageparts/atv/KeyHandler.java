/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.atv;

import android.content.ComponentName;
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
        String targetName = KEYMAP.get(event.getKeyCode());
        if (targetName == null || !hasSetupCompleted()) {
            return event;
        }

        // Consume DOWN as well as UP, so apps never see a DOWN without its UP.
        // Keys without a target are consumed too: some (e.g. BUTTON_3/4/5)
        // have a DPAD_CENTER fallback and would otherwise act as OK.
        if (event.getAction() == KeyEvent.ACTION_UP) {
            Intent launchIntent = resolveTarget(targetName);
            if (launchIntent != null) {
                mContext.startActivity(launchIntent);
            }
        }
        return null;
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TV_USER_SETUP_COMPLETE, 0) != 0;
    }

    private Intent resolveTarget(String targetName) {
        PackageManager pm = mContext.getPackageManager();

        ComponentName component = ComponentName.unflattenFromString(targetName);
        Intent launchIntent = null;

        // Explicit components avoid package visibility and unusual intent-filter issues
        if (component != null) {
            Intent componentIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setComponent(component)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Intent#resolveActivity returns explicit components as is, so ask
            // PackageManager whether the activity actually exists
            if (pm.resolveActivity(componentIntent, 0) != null) {
                launchIntent = componentIntent;
            }
        }

        if (launchIntent == null) {
            // Fall back to the component's package, or look the name up as one
            String packageName = component != null ? component.getPackageName() : targetName;
            launchIntent = pm.getLaunchIntentForPackage(packageName);

            // TV-only apps may expose a leanback launcher activity without a regular one
            if (launchIntent == null) {
                launchIntent = pm.getLeanbackLaunchIntentForPackage(packageName);
            }
        }

        // If it isn't an installed package or component, try as an intent
        if (launchIntent == null) {
            launchIntent = new Intent(targetName);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.putExtra("no_input_mode", true);

            if (launchIntent.resolveActivity(pm) == null) {
                launchIntent = null;
            }
        }

        if (launchIntent == null) {
            Log.w(TAG, "Cannot launch " + targetName + ": package/intent not found.");
        }
        return launchIntent;
    }
}
