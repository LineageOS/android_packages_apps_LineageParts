/*
 * Copyright (C) 2021 The LineageOS Project
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

package org.lineageos.lineageparts.atv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.android.internal.os.DeviceKeyHandler;

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
        String packageName = KEYMAP.get(keyCode);

        if (packageName != null) {
            launchActivity(packageName);
            return null;
        }

        return event;
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TV_USER_SETUP_COMPLETE, 0) != 0;
    }

    private void launchActivity(String packageName) {
        Intent launchIntent;
        if (packageName.equals("com.google.android.katniss")) {
            launchIntent = new Intent("android.intent.action.ASSIST");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (packageName.equals("com.android.tv.settings")) {
            launchIntent = new Intent("android.settings.SETTINGS");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (packageName.equals("com.google.android.intent.action.CONNECT_INPUT")) {
            launchIntent = new Intent("com.google.android.intent.action.CONNECT_INPUT");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.putExtra("no_input_mode", true);
        } else {
            launchIntent = mContext.getPackageManager()
                    .getLaunchIntentForPackage(packageName);
        }

        if (launchIntent != null) {
            mContext.startActivity(launchIntent);
        } else {
            Log.w(TAG, "Cannot launch " + packageName +
                    ": package not found.");
        }
    }
}
