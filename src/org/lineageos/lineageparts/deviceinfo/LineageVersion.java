/*
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

package org.lineageos.lineageparts.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class LineageVersion extends SettingsPreferenceFragment {

    private static final String TAG = "LineageVersionPref";

    private static final String KEY_LINEAGE_VERSION = "lineage_version";
    private static final String KEY_LINEAGE_VERSION_PROP = "ro.lineage.version";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .putExtra("is_lineage", true)
                .setClassName(
                        "android", com.android.internal.app.PlatLogoActivity.class.getName());
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start activity " + intent.toString());
        }
        finish();
    }

    public static final SummaryProvider SUMMARY_PROVIDER = new SummaryProvider() {
        @Override
        public String getSummary(Context context, String key) {
            return SystemProperties.get(KEY_LINEAGE_VERSION_PROP, "");
        }
    };
}
