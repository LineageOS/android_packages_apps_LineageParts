/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2017-2023 The LineageOS Project
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

package org.lineageos.lineageparts.batterycare;

import static java.time.format.FormatStyle.SHORT;
import static java.time.temporal.ChronoUnit.HOURS;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.R;

public abstract class TimePreference extends CustomDialogPreference<AlertDialog> {
    private static final String TAG = TimePreference.class.getSimpleName();
    private static final DateTimeFormatter mFormatter =
            DateTimeFormatter.ofLocalizedTime(SHORT);

    private TimePicker mTimePicker;
    private LocalTime mLocalTime;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_time);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.dlg_ok, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
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

        mLocalTime = LocalTime.ofSecondOfDay(getTimeSetting());

        mTimePicker = view.findViewById(R.id.time_picker);
        mTimePicker.setHour(mLocalTime.getHour());
        mTimePicker.setMinute(mLocalTime.getMinute());
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (defaultValue != null) {
            mLocalTime = LocalTime.ofSecondOfDay(
                    Integer.valueOf((String) defaultValue));
        } else {
            mLocalTime = LocalTime.now().truncatedTo(HOURS);
        }
    }

    @Override
    public CharSequence getSummary() {
        return String.format(getContext().getString(getSummaryResourceId()),
                mLocalTime.format(mFormatter));
    }

    public void setValue(int value) {
        mLocalTime = LocalTime.ofSecondOfDay(value);
        setSummary(getSummary());
    }

    protected abstract int getSummaryResourceId();

    protected abstract int getTimeSetting();

    protected abstract void setTimeSetting(int secondOfDay);
}
