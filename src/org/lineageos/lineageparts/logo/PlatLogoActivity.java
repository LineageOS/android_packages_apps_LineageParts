/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.logo;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.lineageos.lineageparts.R;

public class PlatLogoActivity extends Activity {
    private FrameLayout mLayout;
    private TimeAnimator mAnim;
    private PBackground mBG;

    private long mTouchHeld = 0;
    private int mTapCount;

    // Color matrix to force the logo to be white regardless of input color.
    private static final float[] WHITE = {
        1,     1,     1,    0,    255, // red
        1,     1,     1,    0,    255, // green
        1,     1,     1,    0,    255, // blue
        0,     0,     0,    1,      0  // alpha
    };
    private static final int BASE_SCALE = 50; // magic number scale multiple. Looks good on all DPI
    private static final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private class PBackground extends Drawable {
        private float mRadius, mX, mY, mDP;
        private int[] mPalette;
        private int mDarkest;
        private float mOffset;

        // LineageOS logo drawable
        private final Drawable mLogo;

        public PBackground(Context context) {
            randomizePalette();
            // LineageOS logo
            mLogo = context.getResources().getDrawable(R.drawable.logo_lineage, context.getTheme());
            mLogo.setColorFilter(new ColorMatrixColorFilter(WHITE)); // apply color filter
            mLogo.setBounds(0, 0, 360, 180); // Aspect ratio 2:1
        }

        /**
         * set inner radius of circles
         */
        public void setRadius(float r) {
            this.mRadius = Math.max(48 * mDP, r);
        }

        /**
         * move the circles
         */
        public void setPosition(float x, float y) {
            this.mX = x;
            this.mY = y;
        }

        /**
         * for animating the circles
         */
        public void setOffset(float o) {
            this.mOffset = o;
        }

        /**
         * rough luminance calculation
         * https://www.w3.org/TR/AERT/#color-contrast
         */
        public float lum(int rgb) {
            return ((Color.red(rgb) * 299f) + (Color.green(rgb) * 587f)
                  + (Color.blue(rgb) * 114f)) / 1000f;
        }

        /**
         * create a random evenly-spaced color palette
         * guaranteed to contrast!
         * PS: This is a lie :(
         */
        public void randomizePalette() {
            final int slots = 2 + (int)(Math.random() * 2);
            float[] color = new float[] { (float) Math.random() * 360f, 1f, 1f };
            mPalette = new int[slots];
            mDarkest = 0;
            for (int i = 0; i < slots; i++) {
                mPalette[i] = Color.HSVToColor(color);
                color[0] += 360f / slots;
                if (lum(mPalette[i]) < lum(mPalette[mDarkest])) mDarkest = i;
            }

            final StringBuilder str = new StringBuilder();
            for (int c : mPalette) {
                str.append(String.format("#%08x ", c));
            }
            Log.v("PlatLogoActivity", "color palette: " + str);
        }

        @Override
        public void draw(Canvas canvas) {
            if (mDP == 0) mDP = getResources().getDisplayMetrics().density;
            final float width = canvas.getWidth();
            final float height = canvas.getHeight();
            if (mRadius == 0) {
                setPosition(width / 2, height / 2);
                setRadius(width / 7);
            }
            final float inner_w = mRadius * 0.667f;

            final Paint paint = new Paint();
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.translate(mX, mY);

            float w = Math.max(canvas.getWidth(), canvas.getHeight())  * 1.414f;
            paint.setStyle(Paint.Style.FILL);

            // Draw all the circles
            int i=0;
            while (w > mRadius * 1.55 + inner_w * 1.55) {
                paint.setColor(0xFF000000 | mPalette[i % mPalette.length]);
                // for a slower but more complete version:
                // paint.setStrokeWidth(w);
                // canvas.drawPath(p, paint);
                canvas.drawOval(-w / 2, -w / 2, w / 2, w / 2, paint);
                w -= inner_w * (1.1f + Math.sin((i / 20f + mOffset) * 3.14159f));
                i++;
            }

            // the innermost circle needs to be a constant color to avoid rapid flashing
            paint.setColor(0xFF000000 | mPalette[(mDarkest + 1) % mPalette.length]);
            canvas.drawOval(-mRadius, -mRadius, mRadius, mRadius, paint);

            /* Draw the logo outline
             * Sort of hacky. The Lineage logo can be built with a bunch of circles.
             * This draws circles we need and clips off unnecessary parts.
             */
            paint.setColor(mPalette[mDarkest]);
            // Draw the logo "arms"
            canvas.save();
            {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(inner_w * 0.82f);
                // clip only what we want to see
                canvas.clipRect(-100 * mRadius / BASE_SCALE, -20 * mRadius / BASE_SCALE,
                                100 * mRadius / BASE_SCALE, 30 * mRadius / BASE_SCALE);
                canvas.translate(0, 239 * mRadius / BASE_SCALE);
                canvas.drawCircle(0, 0, mRadius * 4.8f, paint);
            }
            canvas.restore();

            // center circle outline
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(0, 0, mRadius * 1.3f, paint);

            // left circle outline
            canvas.save();
            {
                canvas.translate(-112.5f * mRadius / BASE_SCALE, 28 * mRadius / BASE_SCALE);
                canvas.drawCircle(0, 0, mRadius * 0.74f, paint);
            }
            canvas.restore();

            // right circle outline
            canvas.save();
            {
                canvas.translate(112.5f * mRadius / BASE_SCALE, 28 * mRadius / BASE_SCALE);
                canvas.drawCircle(0, 0, mRadius * 0.74f, paint);
            }
            canvas.restore();

            // Draw LineageOS Logo drawable
            canvas.save();
            {
                canvas.translate((-360 / 2f) * mRadius / BASE_SCALE,
                                (-180 / 2f) * mRadius / BASE_SCALE);
                canvas.scale(mRadius / BASE_SCALE, mRadius / BASE_SCALE);
                mLogo.draw(canvas);
            }
            canvas.restore();

            checkLongPressTimeout();
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }

    // Long press event checker for onTouch
    private void checkLongPressTimeout() {
        if (mTouchHeld > 0 && mTapCount >= 5) {
            if (System.currentTimeMillis() - mTouchHeld >= LONG_PRESS_TIMEOUT) {
                // reset
                mTouchHeld = 0;
                mTapCount = 0;

                // Launch the Easter Egg
                mLayout.post(() -> {
                    try {
                        startActivity(new Intent("org.lineageos.lineageparts.EASTER_EGG")
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                .addCategory("com.android.internal.category.PLATLOGO"));
                    } catch (ActivityNotFoundException ex) {
                        Log.e("PlatLogoActivity", "No more eggs.");
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = new FrameLayout(this);
        setContentView(mLayout);

        mBG = new PBackground(getApplicationContext());
        mLayout.setBackground(mBG);

        mLayout.setOnTouchListener(new View.OnTouchListener() {
            final PointerCoords pc0 = new PointerCoords();
            final PointerCoords pc1 = new PointerCoords();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // make sure the user doesn't launch stage 2 while zooming
                if (event.getPointerCount() > 1 && mTouchHeld > 0) {
                    mTouchHeld = 0;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mBG.randomizePalette();
                        if (mTapCount < 5) mTapCount++; // avoid overflow
                        mTouchHeld = System.currentTimeMillis(); // get time for long press
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Two finger zoom gesture
                        if (event.getPointerCount() > 1) {
                            event.getPointerCoords(0, pc0);
                            event.getPointerCoords(1, pc1);
                            mBG.setRadius((float) Math.hypot(pc0.x - pc1.x, pc0.y - pc1.y) / 2f);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // reset
                        if (mTouchHeld > 0) {
                            mTouchHeld = 0;
                        }
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mBG.randomizePalette();

        mAnim = new TimeAnimator();
        mAnim.setTimeListener((animation, totalTime, deltaTime) -> {
            mBG.setOffset((float) totalTime / 60000f);
            mBG.invalidateSelf();
        });

        mAnim.start();
    }

    @Override
    public void onStop() {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
        super.onStop();
    }
}
