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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.Switch;

import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.widget.SwitchBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lineageos.power.PerformanceManager;
import lineageos.providers.LineageSettings;

public class AppPerfProfileSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener {

    private static final String TAG = "AppPerfProfileSettings";

    private LoadAppTask mTask = null;

    private ListView mAppsList;
    private View mLoadingContainer;
    private AppPerfProfileListAdapter mAppsListAdapter;
    private SwitchBar mSwitchBar;

    private SharedPreferences mPreferences;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private static PerformanceManager mPerformanceManager;
    private boolean mAppPerfProfilesEnabled;

    private boolean isAppPerfProfilesEnabled(ContentResolver cr) {
        return LineageSettings.Secure.getInt(cr,
                LineageSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED, 1) == 1;
    }

    public AppPerfProfileSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        mFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        mFilter.addDataScheme("package");

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED) || 
                        action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    scheduleAppsLoad();
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPerformanceManager = PerformanceManager.getInstance(getActivity());
        mPreferences = getActivity().getSharedPreferences("app_perf_profile_settings", Activity.MODE_PRIVATE);
        mAppPerfProfilesEnabled = isAppPerfProfilesEnabled(getActivity().getContentResolver());
        mAppsListAdapter = new AppPerfProfileListAdapter(getContext(), mPerformanceManager);

        getActivity().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.app_profile_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwitchBar = ((PartsActivity) getActivity()).getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.setChecked(mAppPerfProfilesEnabled);
        mSwitchBar.show();

        mLoadingContainer = view.findViewById(R.id.loading_container);

        mAppsList = (ListView) view.findViewById(R.id.app_list_view);
        mAppsList.setAdapter(mAppsListAdapter);

        // load apps
        scheduleAppsLoad();
    }

    @Override
    public void onDestroyView() {
        if (mTask != null) {
            mTask.cancel(true);
        }
        if (mSwitchBar != null) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        if (mAppsList != null) {
            mAppsList.setAdapter(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.app_perf_profile_menu, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_system_apps:
                final String prefName = "show_system_apps";
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                scheduleAppsLoad();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Handle a switch change
        LineageSettings.Secure.putInt(getActivity().getContentResolver(),
                LineageSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED, isChecked ? 1 : 0);
    }

    public void showProgressBar(boolean flag) {
        if (flag) {
            mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.fade_in));
            mAppsList.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.fade_out));

            mLoadingContainer.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
        } else {
            mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.fade_out));
            mAppsList.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.fade_in));

            mLoadingContainer.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
        }
    }

    private void scheduleAppsLoad() {
        showProgressBar(true);
        if (mTask != null) {
            mTask.cancel(true);
        }
        mTask = new LoadAppTask(this, shouldShowSystemApps());
        mTask.execute();
    }

    public void handlerAppList(List<AppInfo> list) {
        final ArrayList<String> sections = new ArrayList<String>();
        final ArrayList<Integer> positions = new ArrayList<Integer>();
        String lastSectionIndex = null;
        int offset = 0;

        for (int i = 0; i < list.size(); i++) {
            final AppInfo info = list.get(i);
            final String label = (String) info.mAppLabel;
            final String sectionIndex;

            if (TextUtils.isEmpty(label)) {
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

        mAppsListAdapter.setAppInfoList(list, sections, positions);
        showProgressBar(false);
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    // holder for package data passed into the adapter
    public static final class AppInfo {
        String mAppLabel;
        String mPackageName;
        Drawable mIcon;
    }

    /**
     * An asynchronous task to load labels and icons of the installed applications.
     */
    private static class LoadAppTask extends AsyncTask<Void, Void, List<AppInfo>> {

        private AppPerfProfileSettings mActivity;
        private PackageManager mPm;
        private boolean mShowSystemApps;
        private static final String[] BLACKLISTED_PACKAGES = {
                "com.android.systemui"
        };

        LoadAppTask(AppPerfProfileSettings activity, boolean showSystemApps) {
            mActivity = activity;
            mPm = mActivity.getPackageManager();
            mShowSystemApps = showSystemApps;
        }

        private boolean isBlacklisted(String packageName) {
            for (String pkg : BLACKLISTED_PACKAGES) {
                if (pkg.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> appInfoList = new ArrayList<>();
            List<PackageInfo> packages = mPm.getInstalledPackages(0);

            for (PackageInfo info : packages) {
                final ApplicationInfo appInfo = info.applicationInfo;

                // skip all system apps if they shall not be included
                if ((!mShowSystemApps && (appInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) != 0)
                        || mPm.getLaunchIntentForPackage(info.packageName) == null && (appInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) != 0
                        || isBlacklisted(appInfo.packageName)) {
                    continue;
                }

                AppInfo app = new AppInfo();
                app.mAppLabel = appInfo.loadLabel(mPm).toString();
                app.mPackageName = info.packageName;
                try {
                    app.mIcon = mPm.getApplicationIcon(app.mPackageName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                appInfoList.add(app);
            }
            // sort the apps by their enabled state, then by title
            Collections.sort(appInfoList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo lhs, AppInfo rhs) {
                    return lhs.mAppLabel.compareToIgnoreCase(rhs.mAppLabel);
                }
            });
            return appInfoList;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfoList) {
            super.onPostExecute(appInfoList);
            mActivity.handlerAppList(appInfoList);
        }
    }
}
