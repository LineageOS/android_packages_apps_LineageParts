/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import lineageos.app.Profile;

import java.io.IOException;
import java.util.UUID;

public class NFCProfileUtils {

    private static final String TAG = "NFCUtils";

    private static final long[] VIBRATION_PATTERN = {
            0, 100, 10000
    };

    public static void vibrate(Context context) {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        VibrationEffect effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, -1);
        vibrator.vibrate(effect);
    }

    /*
     * Writes an NdefMessage to a NFC tag
     */
    public static boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Log.e(TAG, "Tag is not writable!");
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Log.e(TAG,
                            "Tag exceeds max ndef message size! [" + size + " > "
                                    + ndef.getMaxSize() + "]");
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        Log.e(TAG, "Write error!", e);
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Write error!", e);
            return false;
        }
    }

    /* Convert a 16-byte array to a UUID */
    static UUID toUUID(byte[] byteArray) {

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (byteArray[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (byteArray[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    /* Convert a UUID to a 16-byte array */
    static byte[] asByteArray(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
            buffer[i + 8] = (byte) (lsb >>> 8 * (7 - i));
        }

        return buffer;
    }

    /*
     * Convert a profiles into an NdefMessage. The profile UUID is 16 bytes and
     * stored with the "lineage/profile" mime type.
     */
    public static NdefMessage getProfileAsNdef(Profile profile) {
        byte[] profileBytes = NFCProfileUtils.asByteArray(profile.getUuid());

        NdefRecord record = NdefRecord.createMime(NFCProfile.PROFILE_MIME_TYPE, profileBytes);
        return new NdefMessage(new NdefRecord[] { record });
    }
}
