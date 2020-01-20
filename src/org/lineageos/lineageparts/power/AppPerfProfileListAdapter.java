/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.lineageparts.power;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;

import org.lineageos.lineageparts.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lineageos.power.PerformanceManager;
import lineageos.power.PerformanceProfile;

import static lineageos.power.PerformanceManager.PROFILE_BALANCED;
import static lineageos.power.PerformanceManager.PROFILE_BIAS_PERFORMANCE;
import static lineageos.power.PerformanceManager.PROFILE_BIAS_POWER_SAVE;
import static lineageos.power.PerformanceManager.PROFILE_HIGH_PERFORMANCE;
import static lineageos.power.PerformanceManager.PROFILE_POWER_SAVE;

public class AppPerfProfileListAdapter extends BaseAdapter
        implements AdapterView.OnItemSelectedListener, SectionIndexer {

    private static final int STATE_NOTHING = 0;
    private static final int STATE_POWER_SAVE = 1;
    private static final int STATE_BIAS_POWER_SAVE = 2;
    private static final int STATE_BALANCED = 3;
    private static final int STATE_BIAS_PERFORMANCE = 4;
    private static final int STATE_HIGH_PERFORMANCE = 5;

    private final LayoutInflater mInflater;
    private final ProfileAdapter mProfileAdapter;
    private final PerformanceManager mPerformanceManager;
    private List<AppPerfProfileSettings.AppInfo> mApps = new ArrayList<>();
    private String[] mSections;
    private int[] mPositions;

    public AppPerfProfileListAdapter(Context context, PerformanceManager performanceManager) {
        mInflater = LayoutInflater.from(context);
        mProfileAdapter = new ProfileAdapter(context);
        mPerformanceManager = performanceManager;
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private int getCheckedItemForPackage(String packageName) {
        int checkedItem = STATE_NOTHING;

        if(mPerformanceManager.hasAppProfile(packageName)) {
            int profile = mPerformanceManager.getAppProfile(packageName).getId();
            switch (profile) {
                case PROFILE_POWER_SAVE:
                    checkedItem = STATE_POWER_SAVE;
                    break;
                case PROFILE_BIAS_POWER_SAVE:
                    checkedItem = STATE_BIAS_POWER_SAVE;
                    break;
                case PROFILE_BALANCED:
                    checkedItem = STATE_BALANCED;
                    break;
                case PROFILE_BIAS_PERFORMANCE:
                    checkedItem = STATE_BIAS_PERFORMANCE;
                    break;
                case PROFILE_HIGH_PERFORMANCE:
                    checkedItem = STATE_HIGH_PERFORMANCE;
                    break;
            }
        }
        return checkedItem;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder(mInflater.inflate(
                    R.layout.app_profile_list_item, parent, false));
            holder.mode.setAdapter(mProfileAdapter);
            holder.mode.setOnItemSelectedListener(this);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppPerfProfileSettings.AppInfo app = mApps.get(position);

        if (app == null) {
            return holder.rootView;
        }

        holder.title.setText(app.mAppLabel);
        holder.icon.setImageDrawable(app.mIcon);
        holder.mode.setSelection(getCheckedItemForPackage(app.mPackageName), false);
        holder.mode.setTag(app);
        return holder.rootView;
    }

    public void setAppInfoList(List<AppPerfProfileSettings.AppInfo> apps,
                                List<String> sections, List<Integer> positions) {
        mApps.clear();
        mApps = apps;
        mSections = sections.toArray(new String[sections.size()]);
        mPositions = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            mPositions[i] = positions.get(i);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final String pkgName = ((AppPerfProfileSettings.AppInfo) parent.getTag()).mPackageName;
        int profileId = -1;
        switch (position) {
            case STATE_POWER_SAVE:
                profileId = PROFILE_POWER_SAVE;
                break;
            case STATE_BIAS_POWER_SAVE:
                profileId = PROFILE_BIAS_POWER_SAVE;
                break;
            case STATE_BALANCED:
                profileId = PROFILE_BALANCED;
                break;
            case STATE_BIAS_PERFORMANCE:
                profileId = PROFILE_BIAS_PERFORMANCE;
                break;
            case STATE_HIGH_PERFORMANCE:
                profileId = PROFILE_HIGH_PERFORMANCE;
                break;
        }

        if (mPerformanceManager.hasAppProfile(pkgName)) {
            PerformanceProfile appProfile = mPerformanceManager.getAppProfile(pkgName);
            if (profileId < 0) {
                mPerformanceManager.removeAppProfile(pkgName);
            } else if(profileId >= 0 && (profileId != appProfile.getId())) {
                mPerformanceManager.removeAppProfile(pkgName);
                mPerformanceManager.addAppProfile(pkgName, mPerformanceManager.getPowerProfile(profileId));
            }
        } else {
            if (profileId >= 0) {
                mPerformanceManager.addAppProfile(pkgName, mPerformanceManager.getPowerProfile(profileId));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public int getPositionForSection(int section) {
        if (section < 0 || section >= mSections.length) {
            return -1;
        }

        return mPositions[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return -1;
        }

        final int index = Arrays.binarySearch(mPositions, position);

        /*
         * Consider this example: section positions are 0, 3, 5; the supplied
         * position is 4. The section corresponding to position 4 starts at
         * position 3, so the expected return value is 1. Binary search will not
         * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
         * To get from that number to the expected value of 1 we need to negate
         * and subtract 2.
         */
        return index >= 0 ? index : -index - 2;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    private static class ViewHolder {

        private TextView title;
        private Spinner mode;
        private View rootView;
        private ImageView icon;

        public ViewHolder(View itemView) {
            this.title = (TextView) itemView.findViewById(R.id.app_name);
            this.mode = (Spinner) itemView.findViewById(R.id.app_profiles);
            this.icon = (ImageView) itemView.findViewById(R.id.app_icon);
            this.rootView = itemView;

            itemView.setTag(this);
        }
    }

    private static class ProfileAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private final int mTextColor;
        private final int[] mProfiles = {
                R.string.app_perf_profiles_apply_nothing,
                org.lineageos.platform.internal.R.string.perf_profile_pwrsv,
                org.lineageos.platform.internal.R.string.perf_profile_bias_power,
                org.lineageos.platform.internal.R.string.perf_profile_bal,
                org.lineageos.platform.internal.R.string.perf_profile_bias_perf,
                org.lineageos.platform.internal.R.string.perf_profile_perf
        };

        public ProfileAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textColorSecondary,
                    typedValue, true);
            mTextColor = context.getColor(typedValue.resourceId);
        }

        @Override
        public int getCount() {
            return mProfiles.length;
        }

        @Override
        public Object getItem(int position) {
            return mProfiles[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = (TextView) mInflater.inflate(android.R.layout.simple_spinner_dropdown_item,
                        parent, false);
            }
            view.setText(mProfiles[position]);
            view.setTextColor(mTextColor);
            view.setTextSize(14f);
            return view;
        }
    }
}
