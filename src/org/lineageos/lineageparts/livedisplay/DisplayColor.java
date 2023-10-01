/*
 * SPDX-FileCopyrightText: 2013-2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2021-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.livedisplay;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.widget.IntervalSeekBar;
import org.lineageos.lineageparts.R;

import lineageos.hardware.LiveDisplayManager;

/**
 * Special preference type that allows configuration of Color settings
 */
public class DisplayColor extends CustomDialogPreference<AlertDialog> {
    private static final String TAG = "ColorCalibration";

    private final LiveDisplayManager mLiveDisplay;

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.color_red_seekbar,
        R.id.color_green_seekbar,
        R.id.color_blue_seekbar
    };

    private static final int[] SEEKBAR_VALUE_ID = new int[] {
        R.id.color_red_value,
        R.id.color_green_value,
        R.id.color_blue_value
    };

    private final ColorSeekBar[] mSeekBars = new ColorSeekBar[SEEKBAR_ID.length];

    private final float[] mCurrentColors = new float[3];
    private final float[] mOriginalColors = new float[3];

    public DisplayColor(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLiveDisplay = LiveDisplayManager.getInstance(context);

        setDialogLayoutResource(R.layout.display_color_calibration);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        builder.setNeutralButton(R.string.reset, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.dlg_ok, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        System.arraycopy(mLiveDisplay.getColorAdjustment(), 0, mOriginalColors, 0, 3);
        System.arraycopy(mOriginalColors, 0, mCurrentColors, 0, 3);

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            IntervalSeekBar seekBar = view.findViewById(SEEKBAR_ID[i]);
            TextView value = view.findViewById(SEEKBAR_VALUE_ID[i]);
            mSeekBars[i] = new ColorSeekBar(seekBar, value, i);
            mSeekBars[i].mSeekBar.setMinimum(0.1f);
            mSeekBars[i].mSeekBar.setMaximum(1.0f);

            mSeekBars[i].mSeekBar.setProgressFloat(mCurrentColors[i]);
            int percent = Math.round(100F * mCurrentColors[i]);
            value.setText(String.format("%d%%", percent));
        }
    }

    @Override
    protected boolean onDismissDialog(AlertDialog dialog, int which) {
        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            for (int i = 0; i < mSeekBars.length; i++) {
                mSeekBars[i].mSeekBar.setProgressFloat(1.0f);
                mCurrentColors[i] = 1.0f;
            }
            updateColors(mCurrentColors);
            return false;
        }
        return true;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        updateColors(positiveResult ? mCurrentColors : mOriginalColors);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.currentColors = mCurrentColors;
        myState.originalColors = mOriginalColors;

        // Restore the old state when the activity or dialog is being paused
        updateColors(mOriginalColors);

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

        System.arraycopy(myState.originalColors, 0, mOriginalColors, 0, 3);
        System.arraycopy(myState.currentColors, 0, mCurrentColors, 0, 3);
        for (int i = 0; i < mSeekBars.length; i++) {
            mSeekBars[i].mSeekBar.setProgressFloat(mCurrentColors[i]);
        }
        updateColors(mCurrentColors);
    }

    private static class SavedState extends BaseSavedState {
        float[] originalColors;
        float[] currentColors;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalColors = source.createFloatArray();
            currentColors = source.createFloatArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloatArray(originalColors);
            dest.writeFloatArray(currentColors);
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

    private void updateColors(float[] colors) {
        mLiveDisplay.setColorAdjustment(colors);
    }

    private class ColorSeekBar implements SeekBar.OnSeekBarChangeListener {
        private final int mIndex;
        private final IntervalSeekBar mSeekBar;
        private final TextView mValue;

        public ColorSeekBar(IntervalSeekBar seekBar, TextView value, int index) {
            mSeekBar = seekBar;
            mValue = value;
            mIndex = index;

            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            IntervalSeekBar isb = (IntervalSeekBar)seekBar;
            float fp = isb.getProgressFloat();
            if (fromUser) {
                mCurrentColors[mIndex] = Math.min(fp, 1.0f);
                updateColors(mCurrentColors);
            }

            int percent = Math.round(100F * fp);
            mValue.setText(String.format("%d%%", percent));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }
    }
}
