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

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.lineageparts.R;

import java.util.EnumSet;

/**
 * Implementation of a view holder for the firewall app list.
 */
public class FirewallAppListViewHolder extends RecyclerView.ViewHolder implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    /**
     * Views for displaying the app data and policy information.
     */
    public ImageView appIcon;
    public TextView appName;
    public CheckBox wifiCheckBox;
    public CheckBox cellCheckBox;
    public CheckBox vpnCheckBox;

    /**
     * Info about the app of this view holder.
     */
    public FirewallAppInfo appInfo;

    /**
     * Listener for a checkbox change.
     */
    private FirewallAppListCheckedChangeListener mCheckListener;

    /**
     * Listener for a click on the app name or app icon.
     */
    private FirewallAppListItemClickedListener mClickedListener;

    /**
     * Constructor.
     * @param rootView The root view of the layout.
     */
    public FirewallAppListViewHolder(View rootView) {
        super(rootView);

        appIcon = (ImageView) rootView.findViewById(R.id.app_icon);
        appName = (TextView) rootView.findViewById(R.id.app_name);
        wifiCheckBox = (CheckBox) rootView.findViewById(R.id.wifi_check_box);
        cellCheckBox = (CheckBox) rootView.findViewById(R.id.cell_check_box);
        vpnCheckBox = (CheckBox) rootView.findViewById(R.id.vpn_check_box);

        appIcon.setOnClickListener(this);
        appName.setOnClickListener(this);

        wifiCheckBox.setOnCheckedChangeListener(this);
        cellCheckBox.setOnCheckedChangeListener(this);
        vpnCheckBox.setOnCheckedChangeListener(this);
    }

    /**
     * Binds an new FirewallAppInfo to the view holder.
     * @param info FirewallAppInfo which should be bound to this view holder.
     */
    public void bindData(FirewallAppInfo info) {
        appInfo = info;

        appIcon.setImageDrawable(appInfo.getIcon());
        appName.setText(appInfo.getName());

        // Set the checkboxes to checked when internet access is granted
        wifiCheckBox.setChecked(appInfo.isWifiEnabled());
        cellCheckBox.setChecked(appInfo.isCellEnabled());
        vpnCheckBox.setChecked(appInfo.isVpnEnabled());
    }

    /**
     * Sets the checked change listener.
     * @param listener The new checked change listener.
     */
    public void setOnAppListCheckedChangeListener(FirewallAppListCheckedChangeListener listener) {
        mCheckListener = listener;
    }

    /**
     * Sets the item clicked listener.
     * @param listener The new item clicked listener.
     */
    public void setOnAppListItemClickedListener(FirewallAppListItemClickedListener listener) {
        mClickedListener = listener;
    }

    /**
     * This method is called, when the user changes a checkbox state.
     * @param buttonView Reference to the corresponding checkbox.
     * @param isChecked True, when the new state is checked.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mCheckListener != null) {
            if (buttonView == wifiCheckBox) {
                appInfo.setWifiEnabled(isChecked);
                mCheckListener.onAppListCheckedChange(appInfo,
                        EnumSet.of(FirewallAppListChangeFlag.WIFI));
            } else if (buttonView == cellCheckBox) {
                appInfo.setCellEnabled(isChecked);
                mCheckListener.onAppListCheckedChange(appInfo,
                        EnumSet.of(FirewallAppListChangeFlag.CELL));
            } else if (buttonView == vpnCheckBox) {
                appInfo.setVpnEnabled(isChecked);
                mCheckListener.onAppListCheckedChange(appInfo,
                        EnumSet.of(FirewallAppListChangeFlag.VPN));
            }
        }
    }

    /**
     * This method is called, when the user clicks on the app name or app icon.
     * @param v The view the user clicked on. Not used.
     */
    @Override
    public void onClick(View v) {
        if (mClickedListener != null) {
            mClickedListener.onAppListItemClicked(appInfo);
        }
    }
}
