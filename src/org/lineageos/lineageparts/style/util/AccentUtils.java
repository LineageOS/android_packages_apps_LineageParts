/*
 * Copyright (C) 2018 The LineageOS Project
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
package org.lineageos.lineageparts.style.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.style.models.Accent;

import java.util.ArrayList;
import java.util.List;

public final class AccentUtils {
    private static final String TAG = "AccentUtils";
    private static final String METADATA_COLOR = "lineage_berry_accent_preview";
    private static final int DEFAULT_COLOR = Color.BLACK;

    private AccentUtils() {
    }

    public static List<Accent> getAccents(Context context) {
        List<Accent> accents = new ArrayList<>();

        // Add default accent
        accents.add(getDefaultAccent(context));

        String[] targets = context.getResources().getStringArray(R.array.accent_packages);
        for (String target : targets) {
            try {
                accents.add(getAccent(context, target));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        return accents;
    }

    public static Accent getAccent(Context context, @Nullable String target) 
            throws PackageManager.NameNotFoundException {
        if (TextUtils.isEmpty(target)) {
            return getDefaultAccent(context);
        }

        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai = pm.getApplicationInfo(target, PackageManager.GET_META_DATA);
        int color = ai.metaData == null ? DEFAULT_COLOR :
                ai.metaData.getInt(METADATA_COLOR, DEFAULT_COLOR);
        return new Accent(ai.loadLabel(pm).toString(),ai.packageName, color);
    }


    @NonNull
    private static Accent getDefaultAccent(Context context) {
        return new Accent(context.getString(R.string.style_accent_default_name),
                "", Color.parseColor("#167C80"));
    }
}