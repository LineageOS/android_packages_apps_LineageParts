/*
 * Copyright (C) 2017 The LineageOS Project
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

package org.lineageos.lineageparts.notificationlight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import lineageos.providers.LineageSettings;

import org.lineageos.internal.notification.LightsCapabilities;
import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.R;

public class NotificationBrightnessDialogDnD extends BrightnessDialog {
    private static String TAG = "NotificationBrightnessDialogDnD";

    private Context mContext;

    public NotificationBrightnessDialogDnD(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected int getBrightnessSetting() {
        return LineageSettings.System.getIntForUser(mContext.getContentResolver(),
                LineageSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_DND,
                LIGHT_BRIGHTNESS_MAXIMUM, UserHandle.USER_CURRENT);
    }

    @Override
    protected void setBrightnessSetting(int brightness) {
        LineageSettings.System.putIntForUser(mContext.getContentResolver(),
                LineageSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_DND,
                brightness, UserHandle.USER_CURRENT);
    }
}
