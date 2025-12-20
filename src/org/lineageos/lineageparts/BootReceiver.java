/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2025 The LineageOS project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.lineageparts.contributors.ContributorsCloudFragment;
import org.lineageos.lineageparts.gestures.TouchscreenGestureSettings;
import org.lineageos.lineageparts.input.ButtonSettings;
import org.lineageos.lineageparts.livedisplay.LiveDisplaySettings;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "PartsBootReceiver";
    private static final String ONE_TIME_TUNABLE_RESTORE = "hardware_tunable_restored";

    private static final List<BootRestorable> RESTORABLE_SETTINGS = List.of(
        new LiveDisplaySettings(),
        new TouchscreenGestureSettings()
    );

    @Override
    public void onReceive(Context ctx, Intent intent) {
        // Extract the contributors database
        ContributorsCloudFragment.extractContributorsCloudDatabase(ctx);

        // Toggle visibility of some settings regardless of user type
        for (BootRestorable part : RESTORABLE_SETTINGS) {
            part.restoreAtBoot(ctx);
        }

        if (!ctx.getSystemService(UserManager.class).isPrimaryUser()) {
            Log.d(TAG, "Not running as the primary user, skipping tunable restoration.");
            return;
        }

        if (!hasRestoredTunable(ctx)) {
            /* Restore the hardware tunable values */
            ButtonSettings.restoreKeyDisabler(ctx);
            setRestoredTunable(ctx);
        }

        ButtonSettings.restoreKeyDisabler(ctx);
        ButtonSettings.restoreKeySwapper(ctx);
        TouchscreenGestureSettings.restoreTouchscreenGestureStates(ctx);
    }

    private boolean hasRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(ONE_TIME_TUNABLE_RESTORE, false);
    }

    private void setRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(ONE_TIME_TUNABLE_RESTORE, true).apply();
    }
}
