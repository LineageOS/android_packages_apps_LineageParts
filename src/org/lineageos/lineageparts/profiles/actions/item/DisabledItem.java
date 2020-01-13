/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2020 The LineageOS Project
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
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class DisabledItem extends Item {
    private final int mResTitle;
    private final int mResSummary;

    public DisabledItem(int resTitle, int resSummary) {
        mResTitle = resTitle;
        mResSummary = resSummary;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(mResTitle);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(mResSummary);
    }

    @Override
    public boolean isEnabled(Context context) {
        return false;
    }
}
