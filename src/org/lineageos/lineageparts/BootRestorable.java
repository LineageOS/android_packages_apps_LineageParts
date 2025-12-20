/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts;

import android.content.Context;

public interface BootRestorable {
    void restoreAtBoot(Context context);
}
