/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.egg.octo;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.lineageos.lineageparts.R;

public class Ocquarium extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final float dp = getResources().getDisplayMetrics().density;

        getWindow().setBackgroundDrawableResource(R.drawable.octo_bg_lineage);

        FrameLayout bg = new FrameLayout(this);
        setContentView(bg);
        bg.setAlpha(0f);
        bg.animate().setStartDelay(500).setDuration(5000).alpha(1f).start();

        ImageView imageView = new ImageView(this);
        bg.addView(imageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final OctopusDrawable octo = new OctopusDrawable(getApplicationContext());
        octo.setSizePx((int) (OctopusDrawable.randfrange(40f, 180f) * dp));
        imageView.setImageDrawable(octo);
        octo.startDrift();

        imageView.setOnTouchListener(new View.OnTouchListener() {
            boolean touching;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (octo.hitTest(motionEvent.getX(), motionEvent.getY())) {
                            touching = true;
                            octo.stopDrift();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (touching) {
                            octo.moveTo(motionEvent.getX(), motionEvent.getY());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        touching = false;
                        octo.startDrift();
                        break;
                }
                return true;
            }
        });
    }
}
