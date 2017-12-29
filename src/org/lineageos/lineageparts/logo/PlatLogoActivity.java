/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.lineageos.lineageparts.logo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PlatLogoActivity extends Activity implements View.OnClickListener,
        View.OnLongClickListener, View.OnKeyListener {

    private FrameLayout mLayout;
    private int mTapCount;
    private int mKeyCount;
    private PathInterpolator mInterpolator = new PathInterpolator(0f, 0f, 0.5f, 1f);
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = new FrameLayout(this);
        setContentView(mLayout);
    }

    @Override
    public void onAttachedToWindow() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final float dp = dm.density;
        final int size = (int)
                (Math.min(Math.min(dm.widthPixels, dm.heightPixels), 600*dp) - 100*dp);

        mImageView = new ImageView(this);
        final int pad = (int)(40*dp);
        mImageView.setPadding(pad, pad, pad, pad);
        mImageView.setTranslationZ(20);
        mImageView.setScaleX(0.5f);
        mImageView.setScaleY(0.5f);
        mImageView.setAlpha(0f);

        mImageView.setBackground(new RippleDrawable(ColorStateList.valueOf(0xFFFFFFFF),
                getDrawable(org.lineageos.lineageparts.R.drawable.platlogo_lineage), null));
        mImageView.setClickable(true);

        mImageView.setOnClickListener(this);
        mImageView.setOnLongClickListener(this);

        // Enable hardware keyboard input for TV compatibility.
        mImageView.setFocusable(true);
        mImageView.requestFocus();
        mImageView.setOnKeyListener(this);

        mLayout.addView(mImageView, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));

        mImageView.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(mInterpolator)
                .setDuration(500)
                .setStartDelay(800)
                .start();
    }

    @Override
    public void onClick(View v) {
        mTapCount++;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mTapCount < 5) return false;

        // Launch the Easter Egg
        mImageView.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        startActivity(new Intent("org.lineageos.lineageparts.EASTER_EGG")
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                .addCategory("com.android.internal.category.PLATLOGO"));
                    } catch (ActivityNotFoundException ex) {
                        Log.e("PlatLogoActivity", "No more eggs.");
                    }
                }
        });

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            ++mKeyCount;
            if (mKeyCount > 2) {
                if (mTapCount > 5) {
                    mImageView.performLongClick();
                } else {
                    mImageView.performClick();
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
