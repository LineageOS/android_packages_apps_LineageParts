/*
 * SPDX-FileCopyrightText: 2016 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lineageparts.R;

public class DividerPreference extends Preference {

    private Boolean mAllowAbove;
    private Boolean mAllowBelow;

    public DividerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DividerPreference, 0, 0);
        if (a.hasValue(R.styleable.DividerPreference_allowDividerAbove)) {
            mAllowAbove = a.getBoolean(R.styleable.DividerPreference_allowDividerAbove, false);
        }
        if (a.hasValue(R.styleable.DividerPreference_allowDividerBelow)) {
            mAllowBelow = a.getBoolean(R.styleable.DividerPreference_allowDividerBelow, false);
        }
        a.recycle();
    }

    public DividerPreference(Context context) {
        this(context, null);
    }

    public void setDividerAllowedAbove(boolean allowed) {
        mAllowAbove = allowed;
        notifyChanged();
    }

    public void setDividerAllowedBelow(boolean allowed) {
        mAllowBelow = allowed;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (mAllowAbove != null) {
            holder.setDividerAllowedAbove(mAllowAbove);
        }
        if (mAllowBelow != null) {
            holder.setDividerAllowedBelow(mAllowBelow);
        }
    }
}
