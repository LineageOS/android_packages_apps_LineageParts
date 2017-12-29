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

package org.lineageos.lineageparts.egg.octo;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.animation.SpringAnimation;
import android.support.animation.FloatValueHolder;

import org.lineageos.lineageparts.R;

public class OctopusDrawable extends Drawable {
    private static final float BASE_SCALE = 100f;

    private static final int BODY_COLOR   = 0xFFE0F2F1;
    private static final int ARM_COLOR    = 0xFF212121;
    private static final int LINE_COLOR   = 0xFF212121;

    private static final int[] FRONT_ARMS = {0, 1, 2, 3};
    // use a bunch of presets for X to get the arms looking just right
    private static final float[][] ARM_XPOS_FRONT = {
                                        {0, -5f, -10f},
                                        {1f, -1f, -4f},
                                        {-1f, 1f, 4f},
                                        {0, 5f, 10f}};

    private Paint mPaint = new Paint();
    private Arm[] mArms = new Arm[4]; // 8
    final PointF mCenter = new PointF();
    private int mSizePx = 100;
    final Matrix M = new Matrix();
    final Matrix M_inv = new Matrix();
    private TimeAnimator mDriftAnimation;
    private float[] mPtmp = new float[2];
    private float[] mScaledBounds = new float[2];

    private Drawable mEyeLogo;

    public static float randfrange(float a, float b) {
        return (float) (Math.random() * (b - a) + a);
    }
    public static float clamp(float v, float a, float b) {
        return v < a ? a : v > b ? b : v;
    }

    public OctopusDrawable(Context context) {
        float dp = context.getResources().getDisplayMetrics().density;
        setSizePx((int)(100 * dp));
        mPaint.setAntiAlias(true);
        for (int i = 0; i < mArms.length; i++) {
            mArms[i] = new Arm(
                    0, 0, // arm will be repositioned on moveTo
                    ARM_XPOS_FRONT[i][0], 15f,
                    ARM_XPOS_FRONT[i][1], 30f,
                    ARM_XPOS_FRONT[i][2], -5f,
                    14f, 2f);
        }

        mEyeLogo = context.getResources().getDrawable(R.drawable.logo_lineage);
    }

    public void setSizePx(int size) {
        mSizePx = size;
        M.setScale(mSizePx / BASE_SCALE, mSizePx / BASE_SCALE);
        // TaperedPathStroke.setMinStep(20f*BASE_SCALE/mSizePx); // nice little floaty circles
        TaperedPathStroke.setMinStep(8f * BASE_SCALE / mSizePx); // classic tentacles
        M.invert(M_inv);
    }

    public void startDrift() {
        if (mDriftAnimation == null) {
            mDriftAnimation = new TimeAnimator();
            mDriftAnimation.setTimeListener(new TimeAnimator.TimeListener() {
                static final float MAX_VY = 35f;
                static final float JUMP_VY = -100f;
                static final float MAX_VX = 15f;
                private float ax = 0f, ay = 30f;
                private float vx, vy;
                long nextjump = 0;

                @Override
                public void onTimeUpdate(TimeAnimator timeAnimator, long t, long dt) {
                    float t_sec = 0.001f * t;
                    float dt_sec = 0.001f * dt;
                    if (t > nextjump) {
                        vy = JUMP_VY;
                        nextjump = t + (long) randfrange(5000, 10000);
                    }

                    ax = (float) (MAX_VX * Math.sin(t_sec * .25f));

                    vx = clamp(vx + dt_sec * ax, -MAX_VX, MAX_VX);
                    vy = clamp(vy + dt_sec * ay, -100 * MAX_VY, MAX_VY);

                    // oob check
                    if (mCenter.y - BASE_SCALE / 2 > mScaledBounds[1]) {
                        vy = JUMP_VY;
                    } else if (mCenter.y + BASE_SCALE < 0) {
                        vy = MAX_VY;
                    }

                    mCenter.x = clamp(mCenter.x + dt_sec * vx, 0, mScaledBounds[0]);
                    mCenter.y = mCenter.y + dt_sec * vy;

                    repositionArms();
               }
            });
        }
        mDriftAnimation.start();
    }

    public void stopDrift() {
        mDriftAnimation.cancel();
    }

    @Override
    public void onBoundsChange(Rect bounds) {
        final float w = bounds.width();
        final float h = bounds.height();

        lockArms(true);
        moveTo(w/2, h / 2);
        lockArms(false);

        mScaledBounds[0] = w;
        mScaledBounds[1] = h;
        M_inv.mapPoints(mScaledBounds);
    }

    // real pixel coordinates
    public void moveTo(float x, float y) {
        mCenter.x = x;
        mCenter.y = y;
        mapPointF(M_inv, mCenter);
        repositionArms();
    }

    public boolean hitTest(float x, float y) {
        mPtmp[0] = x;
        mPtmp[1] = y;
        M_inv.mapPoints(mPtmp);
        return Math.hypot(mPtmp[0] - mCenter.x, mPtmp[1] - mCenter.y) < BASE_SCALE/2;
    }

    private void lockArms(boolean l) {
        for (Arm arm : mArms) {
            arm.setLocked(l);
        }
    }

    private void repositionArms() {
        for (int i = 0; i < mArms.length; i++) {
            final float bias = (float)i / (mArms.length - 1) - 0.5f;
            mArms[i].setAnchor(mCenter.x + bias * 30f, mCenter.y + 26f);
        }
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        {
            canvas.concat(M);

            // draw the bottom part of the squid, really only the corner rounding is different.
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(BODY_COLOR);
            canvas.drawRoundRect(mCenter.x - 23f, mCenter.y - 10f, mCenter.x + 23f, mCenter.y + 25f,
                                    10f, 10f, mPaint);
            // draw the body outline
            mPaint.setColor(LINE_COLOR);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(4f);
            canvas.drawRoundRect(mCenter.x - 23f, mCenter.y - 10f, mCenter.x + 23f, mCenter.y + 25f,
                                    10f, 10f, mPaint);

            // draw the top part of our squid then clip out the bottom part.
            canvas.save();
            {
                canvas.clipOutRect(mCenter.x - 28f, mCenter.y + 5f,
                                   mCenter.x + 28f, mCenter.y + 30f);

                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(BODY_COLOR);
                canvas.drawRoundRect(mCenter.x - 23f, mCenter.y - 21f,
                                     mCenter.x + 23f, mCenter.y + 25f, 16f, 15f, mPaint);

                mPaint.setColor(LINE_COLOR);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(4f);
                canvas.drawRoundRect(mCenter.x - 23f, mCenter.y - 21f,
                                     mCenter.x + 23f, mCenter.y + 25f, 16f, 15f, mPaint);
            }
            canvas.restore();

            // draw our logo drawable and translate it to the squid's position. Aspect 2:1
            canvas.save();
            {
                canvas.translate(mCenter.x - 23f, mCenter.y - 2f);
                mEyeLogo.setBounds(0, 0, 46, 23);
                mEyeLogo.draw(canvas);
            }
            canvas.restore();

            // arms in front
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(ARM_COLOR);
            for (int i : FRONT_ARMS) {
                mArms[i].draw(canvas, mPaint);
            }

        }
        canvas.restore();
    }

    // Unused. We must implement because inherited drawable class expects it
    @Override
    public void setAlpha(int i) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    static Path pathMoveTo(Path p, PointF pt) {
        p.moveTo(pt.x, pt.y);
        return p;
    }

    static Path pathQuadTo(Path p, PointF p1, PointF p2) {
        p.quadTo(p1.x, p1.y, p2.x, p2.y);
        return p;
    }

    static void mapPointF(Matrix m, PointF point) {
        float[] p = new float[2];
        p[0] = point.x;
        p[1] = point.y;
        m.mapPoints(p);
        point.x = p[0];
        point.y = p[1];
    }

    // he come to town
    private class Link implements DynamicAnimation.OnAnimationUpdateListener {
        final FloatValueHolder[] coords = new FloatValueHolder[2];
        final SpringAnimation[] anims = new SpringAnimation[coords.length];
        private float dx, dy;
        private boolean locked = false;
        Link next;

        Link(int index, float x1, float y1, float dx, float dy) {
            coords[0] = new FloatValueHolder(x1);
            coords[1] = new FloatValueHolder(y1);
            this.dx = dx;
            this.dy = dy;
            for (int i = 0; i < coords.length; i++) {
                anims[i] = new SpringAnimation(coords[i]);
                anims[i].setSpring(new SpringForce()
                        .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                        .setStiffness(
                                index == 0 ? SpringForce.STIFFNESS_LOW
                                        : index == 1 ? SpringForce.STIFFNESS_VERY_LOW
                                                : SpringForce.STIFFNESS_VERY_LOW / 2)
                        .setFinalPosition(0f));
                anims[i].addUpdateListener(this);
            }
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public PointF start() {
            return new PointF(coords[0].getValue(), coords[1].getValue());
        }

        public PointF end() {
            return new PointF(coords[0].getValue() + dx, coords[1].getValue() + dy);
        }

        public PointF mid() {
            return new PointF(
                    0.5f * dx + (coords[0].getValue()),
                    0.5f * dy + (coords[1].getValue()));
        }

        public void animateTo(PointF target) {
            if (locked) {
                setStart(target.x, target.y);
            } else {
                anims[0].animateToFinalPosition(target.x);
                anims[1].animateToFinalPosition(target.y);
            }
        }

        @Override
        public void onAnimationUpdate(DynamicAnimation dynamicAnimation, float v, float v1) {
            if (next != null) {
                next.animateTo(end());
            }
            OctopusDrawable.this.invalidateSelf();
        }

        public void setStart(float x, float y) {
            coords[0].setValue(x);
            coords[1].setValue(y);
            onAnimationUpdate(null, 0, 0);
        }
    }

    private class Arm {
        final Link link1, link2, link3;
        float max, min;

        public Arm(float x, float y, float dx1, float dy1, float dx2, float dy2,
                float dx3, float dy3, float max, float min) {
            link1 = new Link(0, x, y, dx1, dy1);
            link2 = new Link(1, x + dx1, y + dy1, dx2, dy2);
            link3 = new Link(2, x + dx1 + dx2, y + dy1 + dy2, dx3, dy3);
            link1.next = link2;
            link2.next = link3;

            link1.setLocked(true);
            link2.setLocked(false);
            link3.setLocked(false);

            this.max = max;
            this.min = min;
        }

        // when the arm is locked, it moves rigidly, without physics
        public void setLocked(boolean locked) {
            link2.setLocked(locked);
            link3.setLocked(locked);
        }

        private void setAnchor(float x, float y) {
            link1.setStart(x, y);
        }

        public Path getPath() {
            Path p = new Path();
            pathMoveTo(p, link1.start());
            pathQuadTo(p, link2.start(), link2.mid());
            pathQuadTo(p, link2.end(), link3.end());
            return p;
        }

        public void draw(@NonNull Canvas canvas, Paint pt) {
            final Path p = getPath();
            TaperedPathStroke.drawPath(canvas, p, max, min, pt);
        }

    }
}
