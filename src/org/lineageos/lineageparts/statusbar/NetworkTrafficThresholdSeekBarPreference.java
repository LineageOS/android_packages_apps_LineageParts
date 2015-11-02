/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.lineageparts.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.widget.SeekBarPreference;

public class NetworkTrafficThresholdSeekBarPreference extends SeekBarPreference {
    private int[] mValues;
    private Formatter.BytesResult mFormattedValue;
    private Resources mResources;
    private TextView mSummaryView;

    public NetworkTrafficThresholdSeekBarPreference(Context context) {
        this(context, null);
    }

    public NetworkTrafficThresholdSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTrafficThresholdSeekBarPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NetworkTrafficThresholdSeekBarPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_nettraffic_slider);
        mResources = context.getResources();
        mValues = mResources.getIntArray(R.array.network_traffic_threshold_values);
        setMax(mValues.length - 1);
    }

    public void setThreshold(int threshold) {
        int delta = Integer.MAX_VALUE;
        int bestIndex = 0;
        for (int i = 0; i < mValues.length; i++) {
            if (Math.abs(mValues[i] - threshold) < delta) {
                bestIndex = i;
                delta = Math.abs(mValues[i] - threshold);
            }
        }
        setProgress(bestIndex);
        updateFormattedValue(bestIndex);
    }

    public int getThreshold() {
        return mValues[getProgress()];
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSummaryView = (TextView) view.findViewById(com.android.internal.R.id.summary);
    }

    @Override
    public CharSequence getSummary() {
        if (mFormattedValue == null) {
            return null;
        }
        return getContext().getString(R.string.network_traffic_autohide_threshold_summary,
                mFormattedValue.value, mFormattedValue.units);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        updateFormattedValue(progress);
        if (mSummaryView != null) {
            mSummaryView.setText(getSummary());
        }
    }

    private void updateFormattedValue(int index) {
        mFormattedValue = Formatter.formatBytes(mResources, mValues[index] * 1024,
                Formatter.FLAG_SHORTER);
    }
}

