/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.health;

import static java.time.format.FormatStyle.SHORT;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.R;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import lineageos.health.HealthInterface;

public abstract class TimePreference extends CustomDialogPreference<AlertDialog> {
    private static final String TAG = TimePreference.class.getSimpleName();
    private static final DateTimeFormatter mFormatter = DateTimeFormatter.ofLocalizedTime(SHORT);

    private TimePicker mTimePicker;
    private LocalTime mLocalTime;

    protected final HealthInterface mHealthInterface;

    public TimePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_time);
        mHealthInterface = HealthInterface.getInstance(context);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        mLocalTime = LocalTime.ofSecondOfDay(getTimeSetting());
        super.onBindViewHolder(holder);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder,
            final DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.dlg_ok, null);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mLocalTime = LocalTime.of(mTimePicker.getHour(),
                    mTimePicker.getMinute());
            setTimeSetting(mLocalTime.toSecondOfDay());
            setSummary(getSummary());
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mTimePicker = view.findViewById(R.id.time_picker);
        mTimePicker.setHour(mLocalTime.getHour());
        mTimePicker.setMinute(mLocalTime.getMinute());
    }

    @Override
    public CharSequence getSummary() {
        return String.format(getContext().getString(getSummaryResourceId()),
                mLocalTime.format(mFormatter));
    }

    public void setValue(final int value) {
        mLocalTime = LocalTime.ofSecondOfDay(value);
        setSummary(getSummary());
    }

    protected abstract int getSummaryResourceId();

    protected abstract int getTimeSetting();

    protected abstract void setTimeSetting(int secondOfDay);
}
