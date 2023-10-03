/*
 * SPDX-FileCopyrightText: 2018-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.applications;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.settingslib.applications.ApplicationsState;

import org.lineageos.internal.applications.LongScreen;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LongScreenSettings extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks {

    private AllPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;

    private LongScreen mLongScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplicationsState = ApplicationsState.getInstance(requireActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.onResume();
        mActivityFilter = new ActivityFilter(requireActivity().getPackageManager());
        mAllPackagesAdapter = new AllPackagesAdapter(getActivity());

        mLongScreen = new LongScreen(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.long_screen_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView userListView = view.findViewById(R.id.user_list_view);
        userListView.setAdapter(mAllPackagesAdapter);
        userListView.setEmptyView(view.findViewById(R.id.user_list_empty_view));
    }

    @Override
    public void onResume() {
        super.onResume();

        rebuild();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSession.onPause();
        mSession.onDestroy();
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            handleAppEntries(entries);
            mAllPackagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {}

    @Override
    public void onLauncherInfoChanged() {}

    @Override
    public void onPackageIconChanged() {}

    @Override
    public void onPackageSizeChanged(String packageName) {}

    @Override
    public void onRunningStateChanged(boolean running) {}

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        final ArrayList<String> sections = new ArrayList<>();
        final ArrayList<Integer> positions = new ArrayList<>();
        final PackageManager pm = getPackageManager();
        String lastSectionIndex = null;
        int offset = 0;

        for (int i = 0; i < entries.size(); i++) {
            final ApplicationInfo info = entries.get(i).info;
            final String label = (String) info.loadLabel(pm);
            final String sectionIndex;

            if (!info.enabled) {
                sectionIndex = "--"; // XXX
            } else if (TextUtils.isEmpty(label)) {
                sectionIndex = "";
            } else {
                sectionIndex = label.substring(0, 1).toUpperCase();
            }

            if (lastSectionIndex == null ||
                    !TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }

            offset++;
        }

        mAllPackagesAdapter.setEntries(entries, sections, positions);
    }

    private void rebuild() {
        mSession.rebuild(mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private class AllPackagesAdapter extends BaseAdapter
            implements SectionIndexer {

        private final LayoutInflater mInflater;
        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();
        private String[] mSections;
        private int[] mPositions;

        public AllPackagesAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            mActivityFilter = new ActivityFilter(context.getPackageManager());
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ApplicationsState.AppEntry entry = mEntries.get(position);
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder(mInflater.inflate(
                        R.layout.long_screen_list_item, parent, false));
                holder.state.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    final ApplicationsState.AppEntry appEntry =
                            (ApplicationsState.AppEntry) buttonView.getTag();

                    if (isChecked) {
                        mLongScreen.addApp(appEntry.info.packageName);
                    } else {
                        mLongScreen.removeApp(appEntry.info.packageName);
                    }
                });
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (entry == null) {
                return holder.rootView;
            }

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);
            holder.state.setTag(entry);
            holder.state.setChecked(mLongScreen.shouldForceLongScreen(entry.info.packageName));
            return holder.rootView;
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries,
                List<String> sections, List<Integer> positions) {
            mEntries = entries;
            mSections = sections.toArray(new String[0]);
            mPositions = new int[positions.size()];
            for (int i = 0; i < positions.size(); i++) {
                mPositions[i] = positions.get(i);
            }
            notifyDataSetChanged();
        }

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
    }

    private static class ViewHolder {
        private final TextView title;
        private final ImageView icon;
        private final Switch state;
        private final View rootView;

        private ViewHolder(View view) {
            this.title = view.findViewById(R.id.app_name);
            this.icon = view.findViewById(R.id.app_icon);
            this.state = view.findViewById(R.id.state);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> mLauncherResolveInfoList = new ArrayList<>();

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i,
                    PackageManager.ResolveInfoFlags.of(0));

            synchronized (mLauncherResolveInfoList) {
                mLauncherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    mLauncherResolveInfoList.add(ri.activityInfo.packageName);
                }
            }
        }

        @Override
        public void init() {}

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean show = !mAllPackagesAdapter.mEntries.contains(entry.info.packageName);
            if (show) {
                synchronized (mLauncherResolveInfoList) {
                    show = mLauncherResolveInfoList.contains(entry.info.packageName) &&
                            entry.info.targetSdkVersion < Build.VERSION_CODES.O;
                }
            }
            return show;
        }
    }
}
