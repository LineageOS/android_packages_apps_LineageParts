/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.egg.octo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;

public class TaperedPathStroke {
    static float sMinStepPx = 4f;
    static final PathMeasure pm = new PathMeasure();
    static final float[] pos = {0, 0};
    static final float[] tan = {0, 0};
    public static void setMinStep(float px) {
        sMinStepPx = px;
    }

    // it's the variable-width brush algorithm from the Markers app, basically
    public static void drawPath(Canvas c, Path p, float r1, float r2, Paint pt) {
        pm.setPath(p, false);
        final float len = pm.getLength();
        float t = 0;
        boolean last = false;
        while (!last) {
            if (t >= len) {
                t = len;
                last = true;
            }
            pm.getPosTan(t, pos, tan);
            // float r = len > 0 ? lerp(t/len, r1, r2) : r1;
            float r = 3f;
            c.drawCircle(pos[0], pos[1], r, pt);
            // walk forward 1/4 radius, not too small though
            t += Math.max(r * 0.25f, sMinStepPx);
        }
    }
}
