/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.lineageos.lineageparts.R;

public class PlatLogoActivity extends Activity {
    private FrameLayout layout;
    private TimeAnimator anim;
    private PBackground bg;

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
    private static final long LONG_PRESS_TIMEOUT= new Long(ViewConfiguration.getLongPressTimeout());

    private class PBackground extends Drawable {
        private float maxRadius, radius, x, y, dp;
        private int[] palette;
        private int darkest;
        private float offset;

        // LineageOS logo drawable
        private Drawable logo;

        public PBackground(Context context) {
            randomizePalette();
            // LineageOS logo
            logo = context.getResources().getDrawable(R.drawable.logo_lineage);
            logo.setColorFilter(new ColorMatrixColorFilter(WHITE)); // apply color filter
            logo.setBounds(0, 0, 360, 180); // Aspect ratio 2:1
        }

        /**
         * set inner radius of circles
         */
        public void setRadius(float r) {
            this.radius = Math.max(48 * dp, r);
        }

        /**
         * move the circles
         */
        public void setPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * for animating the circles
         */
        public void setOffset(float o) {
            this.offset = o;
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
            palette = new int[slots];
            darkest = 0;
            for (int i = 0; i < slots; i++) {
                palette[i] = Color.HSVToColor(color);
                color[0] += 360f / slots;
                if (lum(palette[i]) < lum(palette[darkest])) darkest = i;
            }

            final StringBuilder str = new StringBuilder();
            for (int c : palette) {
                str.append(String.format("#%08x ", c));
            }
            Log.v("PlatLogoActivity", "color palette: " + str);
        }

        @Override
        public void draw(Canvas canvas) {
            if (dp == 0) dp = getResources().getDisplayMetrics().density;
            final float width = canvas.getWidth();
            final float height = canvas.getHeight();
            if (radius == 0) {
                setPosition(width / 2, height / 2);
                setRadius(width / 7);
            }
            final float inner_w = radius * 0.667f;

            final Paint paint = new Paint();
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.translate(x, y);

            float w = Math.max(canvas.getWidth(), canvas.getHeight())  * 1.414f;
            paint.setStyle(Paint.Style.FILL);

            // Draw all the circles
            int i=0;
            while (w > radius * 1.55 + inner_w * 1.55) {
                paint.setColor(0xFF000000 | palette[i % palette.length]);
                // for a slower but more complete version:
                // paint.setStrokeWidth(w);
                // canvas.drawPath(p, paint);
                canvas.drawOval(-w / 2, -w / 2, w / 2, w / 2, paint);
                w -= inner_w * (1.1f + Math.sin((i / 20f + offset) * 3.14159f));
                i++;
            }

            // the innermost circle needs to be a constant color to avoid rapid flashing
            paint.setColor(0xFF000000 | palette[(darkest + 1) % palette.length]);
            canvas.drawOval(-radius, -radius, radius, radius, paint);

            /* Draw the logo outline
             * Sort of hacky. The Lineage logo can be built with a bunch of circles.
             * This draws circles we need and clips off unnecessary parts.
             */
            paint.setColor(palette[darkest]);
            // Draw the logo "arms"
            canvas.save();
            {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(inner_w * 0.82f);
                // clip only what we want to see
                canvas.clipRect(-100 * radius / BASE_SCALE, -20 * radius / BASE_SCALE,
                                100 * radius / BASE_SCALE, 30 * radius / BASE_SCALE);
                canvas.translate(0, 239 * radius / BASE_SCALE);
                canvas.drawCircle(0, 0, radius * 4.8f, paint);
            }
            canvas.restore();

            // center circle outline
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(0, 0, radius * 1.3f, paint);

            // left circle outline
            canvas.save();
            {
                canvas.translate(-112.5f * radius / BASE_SCALE, 28 * radius / BASE_SCALE);
                canvas.drawCircle(0, 0, radius * 0.74f, paint);
            }
            canvas.restore();

            // right circle outline
            canvas.save();
            {
                canvas.translate(112.5f * radius / BASE_SCALE, 28 * radius / BASE_SCALE);
                canvas.drawCircle(0, 0, radius * 0.74f, paint);
            }
            canvas.restore();

            // Draw LineageOS Logo drawable
            canvas.save();
            {
                canvas.translate((-360 / 2) * radius / BASE_SCALE,
                                (-180 / 2) * radius / BASE_SCALE);
                canvas.scale(radius / BASE_SCALE, radius / BASE_SCALE);
                logo.draw(canvas);
            }
            canvas.restore();

            // Disable until we get a stage 2 easter egg
            // check if a long press event has occured
            // checkLongPressTimeout();
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
                layout.post(new Runnable() {
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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new FrameLayout(this);
        setContentView(layout);

        bg = new PBackground(getApplicationContext());
        layout.setBackground(bg);

        layout.setOnTouchListener(new View.OnTouchListener() {
            final PointerCoords pc0 = new PointerCoords();
            final PointerCoords pc1 = new PointerCoords();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // make sure the user doesnt launch stage 2 while zooming
                if (event.getPointerCount() > 1 && mTouchHeld > 0) {
                    mTouchHeld = 0;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        bg.randomizePalette();
                        if (mTapCount < 5) mTapCount++; // avoid overflow
                        mTouchHeld = System.currentTimeMillis(); // get time for long press
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Two finger zoom gesture
                        if (event.getPointerCount() > 1) {
                            event.getPointerCoords(0, pc0);
                            event.getPointerCoords(1, pc1);
                            bg.setRadius((float) Math.hypot(pc0.x - pc1.x, pc0.y - pc1.y) / 2f);
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

        bg.randomizePalette();

        anim = new TimeAnimator();
        anim.setTimeListener(
            new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                    bg.setOffset((float) totalTime / 60000f);
                    bg.invalidateSelf();
                }
            });

        anim.start();
    }

    @Override
    public void onStop() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
        super.onStop();
    }
}
