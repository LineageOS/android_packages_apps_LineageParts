/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.app.Dialog;

import org.lineageos.lineageparts.SettingsPreferenceFragment;

/**
 * Letting the class, assumed to be Fragment, create a Dialog on it. Should be useful
 * you want to utilize some capability in {@link SettingsPreferenceFragment} but don't want
 * the class inherit the class itself (See {@link ProxySelector} for example).
 */
public interface DialogCreatable {

    Dialog onCreateDialog(int dialogId);
}
