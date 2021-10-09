/*
 * Copyright (C) 2014 The CyanogenMod Project
 *               2020 The LineageOS Project
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
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

import org.lineageos.lineageparts.R;

import lineageos.profiles.StreamSettings;

public class VolumeStreamItem extends Item {
    private int mStreamId;
    private StreamSettings mStreamSettings;

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
        final boolean volumeLinkNotification = Settings.Secure.getInt(
                context.getContentResolver(), "Settings.Secure.VOLUME_LINK_NOTIFICATION", 1) == 1;
        return !volumeLinkNotification;
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
