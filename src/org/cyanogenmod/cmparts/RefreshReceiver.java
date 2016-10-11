/*
 * Copyright (C) 2016 The CyanogenMod Project
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
package org.cyanogenmod.cmparts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static org.cyanogenmod.internal.cmparts.PartsList.ACTION_REFRESH_PART;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART_KEY;

public class RefreshReceiver extends BroadcastReceiver {

    /* for Settings dashboard tiles */
    private static final String ACTION_REFRESH_SUMMARY =
            "org.cyanogenmod.settings.REFRESH_SUMMARY";

    /**
     * Receiver which handles clients requesting a summary update. A client may send
     * the REFERSH_PART or REFRESH_SUMMARY actions via sendOrderedBroadcast,
     * and we will reply immediately.
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("REFRESH-PARTS", intent.toString());
        if (isOrderedBroadcast() && (ACTION_REFRESH_PART.equals(intent.getAction()) ||
                ACTION_REFRESH_SUMMARY.equals(intent.getAction()))) {
            final String key = intent.getStringExtra(EXTRA_PART_KEY);
            if (key != null &&
                    PartsRefresher.get(context).updateExtras(key, getResultExtras(true))) {
                setResultCode(Activity.RESULT_OK);
                return;
            }
        }
        abortBroadcast();
    }
}
