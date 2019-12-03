/*
 * Copyright (C) 2018-2019 The LineageOS Project
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
package org.lineageos.lineageparts.firewall;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import java.util.EnumSet;
import java.util.List;

/**
 * Implementation for a SettingsPreferenceFragment, which shows a overview over all
 * network policies.
 */
public class FirewallPreferences extends SettingsPreferenceFragment implements
        LoaderManager.LoaderCallbacks<List<FirewallAppInfo>>,
        FirewallAppListCheckedChangeListener,
        FirewallAppListItemClickedListener {

    /**
     * String constants.
     */
    public static final String TAG = "FirewallPreferences";
    public static final String SHOW_SYSTEM_APPS_PREF = "showSystemApps";

    /**
     * Reference to the network policy manager.
     */
    private NetworkPolicyManager mNetworkPolicyManager;

    /**
     * Reference to the shared preferences.
     */
    private SharedPreferences mSharedPreferences;

    /**
     * Reference to the app list that shows the network policies for the apps.
     */
    private FirewallAppList mAppList;

    /**
     * Reference to a TextView for displaying error messages e.g. "no apps to show".
     */
    private TextView mErrorTextView;

    /**
     * Reference to a LinearLayout that contains a progressbar and a "loading" text view.
     */
    private LinearLayout mLoadingContainer;

    /**
     * Is true, when the app list should show system apps.
     */
    private boolean mShowSystemApps;

    /**
     * Called when the layout should be inflated.
     * @param inflater Inflater for inflating the layout.
     * @param container Parent in which the layout should be inflated.
     * @param savedInstanceState Last saved instance (not used).
     * @return The inflated View.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mNetworkPolicyManager = NetworkPolicyManager.from(getContext());
        mSharedPreferences = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        mShowSystemApps = mSharedPreferences.getBoolean(SHOW_SYSTEM_APPS_PREF, false);
        return inflater.inflate(R.layout.firewall_preferences, container, false);
    }

    /**
     * Called when the activity was created.
     * @param savedInstanceState Last saved instance (not used).
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAppList = (FirewallAppList) getActivity().findViewById(R.id.apps_list);
        mAppList.setOnAppListCheckedChangeListener(this);
        mAppList.setOnAppListItemClickedListener(this);

        mErrorTextView = (TextView) getActivity().findViewById(R.id.error);

        mLoadingContainer = (LinearLayout) getActivity().findViewById(R.id.loading_container);

        mErrorTextView.setVisibility(View.GONE);
    }

    /**
     * Called when the options menu should be created.
     * @param menu
     * @param inflater Inflater for inflating the menu layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.firewall_menu, menu);

        MenuItem blockNewAppsItem = menu.findItem(R.id.firewall_block_new_apps);
        MenuItem showSystemAppsItem = menu.findItem(R.id.firewall_show_system_apps);

        blockNewAppsItem.setChecked(mNetworkPolicyManager.getRestrictNewApps());
        showSystemAppsItem.setChecked(mShowSystemApps);
    }

    /**
     * Called when the execution of the app is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();

        // rebuild the list; the user might have changed settings inbetween
        reloadAppList();
    }

    /**
     * Called when the execution of the app is paused.
     */
    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SHOW_SYSTEM_APPS_PREF, mShowSystemApps);
        editor.commit();
    }

    /**
     * Called when the user clicked on a item of the options menu.
     * @param item Item on which the user clicked.
     * @return Returnvalue of the superclass implementation.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }

        switch (id) {
            case R.id.firewall_block_new_apps:
                mNetworkPolicyManager.setRestrictNewApps(item.isChecked());
                break;
            case R.id.firewall_show_system_apps:
                mShowSystemApps = item.isChecked();
                reloadAppList();
                break;
            case R.id.firewall_block_all_apps:
                setAllNetworkPoliciesEnabled(false);
                break;
            case R.id.firewall_unblock_all_apps:
                setAllNetworkPoliciesEnabled(true);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the loader object should be created.
     * @param id Identifier for the loader object (not used).
     * @param args Arguments for the loader object (not used).
     * @return The created loader object.
     */
    @Override
    public Loader<List<FirewallAppInfo>> onCreateLoader(int id, Bundle args) {
        return new FirewallAppInfoLoader(getContext());
    }

    /**
     * Called when the loader has finished loading the app data.
     * @param loader Reference to the loader that has loaded the app data.
     * @param apps List of the FirewallAppInfo loaded.
     */
    @Override
    public void onLoadFinished(Loader<List<FirewallAppInfo>> loader, List<FirewallAppInfo> apps) {
        mLoadingContainer.setVisibility(View.GONE);

        mAppList.removeAllAppInfo();

        for (FirewallAppInfo appInfo : apps) {
            if (mShowSystemApps || !appInfo.isSystemApp()) {
                final int policy = mNetworkPolicyManager.getUidPolicy(appInfo.getUid());

                appInfo.setWifiEnabled((policy & NetworkPolicyManager.POLICY_REJECT_ON_WLAN) == 0);
                appInfo.setCellEnabled((policy & NetworkPolicyManager.POLICY_REJECT_ON_DATA) == 0);
                appInfo.setVpnEnabled((policy & NetworkPolicyManager.POLICY_REJECT_ON_VPN) == 0);
                mAppList.addAppInfo(appInfo);
            }
        }

        mAppList.notifyDataSetChanged();

        if (mAppList.getAppInfoCount() <= 0) {
            mErrorTextView.setVisibility(View.VISIBLE);
        } else {
            mAppList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called, when a reset of the loader is requested.
     * @param loader Loader which should be reseted.
     */
    @Override
    public void onLoaderReset(Loader<List<FirewallAppInfo>> loader) {
        // Hide app list an show progress bar.
        mLoadingContainer.setVisibility(View.VISIBLE);
        mAppList.setVisibility(View.GONE);
    }

    /**
     * Restarts the loading procedure of the app info.
     */
    public void reloadAppList() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * This method is called, when the user changes a checkbox state.
     * @param appInfo FirewallAppInfo of the app the checkbox belongs to.
     * @param changeFlags Flags for determining, which checkbox was changed.
     */
    @Override
    public void onAppListCheckedChange(FirewallAppInfo appInfo,
                                       EnumSet<FirewallAppListChangeFlag> changeFlags) {
        setNetworkPolicy(appInfo);
    }

    /**
     * This method is called, when the user clicks on the app name or app icon.
     * @param appInfo FirewallAppInfo of the clicked item.
     */
    @Override
    public void onAppListItemClicked(FirewallAppInfo appInfo) {
        final Intent i = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
                Uri.fromParts("package", appInfo.getPackageName(), null));
        getActivity().startActivityForResult(i, 0);
    }

    /**
     * Updates the network policies according to the FirewallAppInfo.
     * @param appInfo FirewallAppInfo that identifies the app and has the network info.
     */
    public void setNetworkPolicy(FirewallAppInfo appInfo) {
        int oldPolicy = mNetworkPolicyManager.getUidPolicy(appInfo.getUid());
        int newPolicy = oldPolicy;

        if (appInfo.isWifiEnabled()) {
            newPolicy &= ~NetworkPolicyManager.POLICY_REJECT_ON_WLAN;
        } else {
            newPolicy |= NetworkPolicyManager.POLICY_REJECT_ON_WLAN;
        }

        if (appInfo.isCellEnabled()) {
            newPolicy &= ~NetworkPolicyManager.POLICY_REJECT_ON_DATA;
        } else {
            newPolicy |= NetworkPolicyManager.POLICY_REJECT_ON_DATA;
        }

        if (appInfo.isVpnEnabled()) {
            newPolicy &= ~NetworkPolicyManager.POLICY_REJECT_ON_VPN;
        } else {
            newPolicy |= NetworkPolicyManager.POLICY_REJECT_ON_VPN;
        }

        // Prevent unnecessary calls to setUidPolicy()
        if(newPolicy != oldPolicy) {
            try {
                mNetworkPolicyManager.setUidPolicy(appInfo.getUid(), newPolicy);
            } catch (Exception e) {
                Log.e(TAG, "Could't set the network policy for: " + appInfo.getPackageName(), e);
            }
        }
    }

    /**
     * Grants or restricts networkpolicies of all apps.
     * @param enabled True, when all apps should get network access.
     */
    public void setAllNetworkPoliciesEnabled(boolean enabled) {
        for (int i = 0; i < mAppList.getAppInfoCount(); ++i) {
            FirewallAppInfo appInfo = mAppList.getAppInfoAt(i);

            appInfo.setWifiEnabled(enabled);
            appInfo.setCellEnabled(enabled);
            appInfo.setVpnEnabled(enabled);

            setNetworkPolicy(appInfo);
        }

        mAppList.notifyDataSetChanged();
    }
}
