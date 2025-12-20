/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts;

import android.content.Context;

import org.lineageos.lineageparts.gestures.TouchscreenGestureSettings;
import org.lineageos.lineageparts.livedisplay.LiveDisplaySettings;

import java.util.List;

public final class BootRestoreRegistry {
    private static final List<BootRestorable> ITEMS = List.of(
        new LiveDisplaySettings(),
        new TouchscreenGestureSettings()
    );

    public static void restoreAll(Context context) {
        for (BootRestorable r : ITEMS) {
            r.restoreAtBoot(context);
        }
    }
}
