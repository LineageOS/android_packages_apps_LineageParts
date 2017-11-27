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

public class NotificationBrightnessDialog extends CustomDialogPreference<AlertDialog>
        implements SeekBar.OnSeekBarChangeListener {

    private static String TAG = "NotificationBrightnessDialog";

    public static final int LIGHT_BRIGHTNESS_MINIMUM = 1;
    public static final int LIGHT_BRIGHTNESS_MAXIMUM = 255;

    // Minimum delay between LED notification updates
    private final static long LED_UPDATE_DELAY_MS = 250;

    private SeekBar mBrightnessBar;

    // The user selected brightness level (past or present if dialog is OKed).
    private int mSelectedBrightness;
    // Current position of brightness seekbar
    private int mSeekBarBrightness;
    // LED brightness currently on display (0 means notification is not showing)
    private int mVisibleLedBrightness;

    private final Context mContext;
    private final Handler mHandler;

    private final Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    public NotificationBrightnessDialog(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.notification_brightness_dialog);

        mContext = context;

        // Message handler used for led notification update throttling.
        mHandler = new Handler();

        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Bundle bundle = new Bundle();
        bundle.putBoolean(Notification.EXTRA_FORCE_SHOW_LIGHTS, true);

        mNotificationBuilder = new Notification.Builder(mContext);
        mNotificationBuilder.setExtras(bundle)
                .setContentTitle(mContext.getString(R.string.led_notification_title))
                .setContentText(mContext.getString(R.string.led_notification_text))
                .setSmallIcon(R.drawable.ic_settings_24dp)
                .setOngoing(true);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        // Set no-op handler for RESET button (note, the reset itself is handled in onDismissDialog())
        builder.setNeutralButton(R.string.reset,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected boolean onDismissDialog(AlertDialog dialog, int which) {
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            // Reset brightness to default (max).
            mBrightnessBar.setProgress(LIGHT_BRIGHTNESS_MAXIMUM);
            return false;
        }
        return true;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mSelectedBrightness = mSeekBarBrightness;
        } else {
            mSeekBarBrightness = mSelectedBrightness;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        updateSelectedBrightness();
        mSeekBarBrightness = mSelectedBrightness;
        mVisibleLedBrightness = 0; // LED notification is not showing.

        mBrightnessBar = (SeekBar) view.findViewById(R.id.notification_brightness_seekbar);
        mBrightnessBar.setMax(LIGHT_BRIGHTNESS_MAXIMUM);
        mBrightnessBar.setMin(LIGHT_BRIGHTNESS_MINIMUM);
        mBrightnessBar.setOnSeekBarChangeListener(this);
        mBrightnessBar.setProgress(mSeekBarBrightness);
        mBrightnessBar.setThumb(mContext.getResources().getDrawable(
                R.drawable.ic_brightness_thumb, mContext.getTheme()));
    }

    @Override
    protected void onResume() {
        // Get current notification brightness setting.
        updateSelectedBrightness();

        // Set maximum system brightness so that the notification preview
        // is relative to the real maximum.
        LineageSettings.System.putIntForUser(mContext.getContentResolver(),
                LineageSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                LIGHT_BRIGHTNESS_MAXIMUM, UserHandle.USER_CURRENT);

        updateNotification();
    }

    @Override
    protected void onPause() {
        cancelNotification();
        // Restore original or save new brightness
        LineageSettings.System.putIntForUser(mContext.getContentResolver(),
                LineageSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                mSelectedBrightness, UserHandle.USER_CURRENT);
    }

    @Override
    public void onStartTrackingTouch (SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch (SeekBar seekBar) {}

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mSeekBarBrightness = progress;
        updateNotification();
    }

    private void updateSelectedBrightness() {
        mSelectedBrightness = LineageSettings.System.getIntForUser(mContext.getContentResolver(),
                LineageSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                LIGHT_BRIGHTNESS_MAXIMUM, UserHandle.USER_CURRENT);
    }

    private void updateNotification() {
        // Exit if there's nothing to do or we're throttled.
        if (mVisibleLedBrightness == mSeekBarBrightness || mLedHandler.hasMessages(0)) {
            return;
        }

        mLedHandler.sendEmptyMessageDelayed(0, LED_UPDATE_DELAY_MS);

        // Instead of canceling the notification, force it to update with the color.
        // Use a white light for a better preview of the brightness.
        int notificationColor = 0xFFFFFF | (mSeekBarBrightness << 24);
        mNotificationBuilder.setLights(notificationColor, 1, 0);
        mNotificationManager.notify(1, mNotificationBuilder.build());
        mVisibleLedBrightness = mSeekBarBrightness;
    }

    private void cancelNotification() {
        if (mVisibleLedBrightness > 0) {
            mLedHandler.removeMessages(0);
            mNotificationManager.cancel(1);
            mVisibleLedBrightness = 0;
        }
    }

    private Handler mLedHandler = new Handler() {
        public void handleMessage(Message msg) {
            updateNotification();
        }
    };

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.seekBarBrightness = mSeekBarBrightness;

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        mSeekBarBrightness = myState.seekBarBrightness;
    }

    private static class SavedState extends BaseSavedState {
        int seekBarBrightness;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            seekBarBrightness = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(seekBarBrightness);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
