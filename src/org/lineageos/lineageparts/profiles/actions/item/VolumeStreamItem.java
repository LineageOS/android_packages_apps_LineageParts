/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

import org.lineageos.lineageparts.R;

import lineageos.profiles.StreamSettings;

public class VolumeStreamItem extends Item {
    private final int mStreamId;
    private final StreamSettings mStreamSettings;

    public VolumeStreamItem(int streamId, StreamSettings streamSettings) {
        mStreamId = streamId;
        mStreamSettings = streamSettings;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(getNameForStream(mStreamId));
    }

    @Override
    public String getSummary(Context context) {
        if (mStreamSettings.isOverride()) {
            final AudioManager am = context.getSystemService(AudioManager.class);

            int denominator = mStreamSettings.getValue();
            int numerator = am.getStreamMaxVolume(mStreamId);
            return context.getResources().getString(
                    R.string.volume_override_summary,
                    denominator, numerator);
        }
        return context.getString(R.string.profile_action_none);
    }

    @Override
    public boolean isEnabled(Context context) {
        // all streams are enabled, except notification stream if linking to ring volume is enabled
        if (mStreamId != AudioManager.STREAM_NOTIFICATION) {
            return true;
        }
        return false;
    }

    public static int getNameForStream(int stream) {
        switch (stream) {
            case AudioManager.STREAM_ALARM:
                return R.string.alarm_volume_title;
            case AudioManager.STREAM_MUSIC:
                return R.string.media_volume_title;
            case AudioManager.STREAM_RING:
                return R.string.incoming_call_volume_title;
            case AudioManager.STREAM_NOTIFICATION:
                return R.string.notification_volume_title;
            default: return 0;
        }
    }

    public int getStreamType() {
        return mStreamId;
    }

    public StreamSettings getSettings() {
        return mStreamSettings;
    }
}
