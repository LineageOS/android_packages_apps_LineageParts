/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.input;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class BacklightTimeoutSeekBar extends SeekBar {
    private int mMax;
    private int mGap;

    public BacklightTimeoutSeekBar(Context context) {
        super(context);
    }

    public BacklightTimeoutSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BacklightTimeoutSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
    }

    @Override
    public void setMax(int max) {
        mMax = max;
        mGap = max / 10;
        super.setMax(max + 2 * mGap - 1);
    }

    @Override
    protected int updateTouchProgress(int lastProgress, int newProgress) {
        if (newProgress < mMax) {
            return newProgress;
        }
        if (newProgress < mMax + mGap) {
            return mMax - 1;
        }
        return getMax();
    }
}
