/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles;

import android.nfc.Tag;

public interface NFCProfileTagCallback {
    void onTagRead(Tag tag);
}
