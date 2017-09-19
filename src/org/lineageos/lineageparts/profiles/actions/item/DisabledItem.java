/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lineageos.lineageparts.profiles.actions.ItemListAdapter;

public class DisabledItem extends BaseItem {

    private final int mResTitle;
    private final int mResSummary;

    public DisabledItem(int resTitle, int resSummary) {
       mResTitle = resTitle;
       mResSummary = resSummary;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.DISABLED_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getTitle() {
        return getString(mResTitle);
    }

    @Override
    public String getSummary() {
        return getString(mResSummary);
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
        View view = super.getView(inflater, convertView, parent);
        view.findViewById(android.R.id.title).setEnabled(false);
        view.findViewById(android.R.id.summary).setEnabled(false);
        return view;
    }
}
