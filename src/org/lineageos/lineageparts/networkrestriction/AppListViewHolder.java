/*
 * Copyright (C) 2018-2020 The LineageOS Project
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
package org.lineageos.lineageparts.networkrestriction;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.lineageos.lineageparts.R;

import java.util.EnumSet;

/**
 * Implementation of a view holder for the network restriction manager app list.
 */
public class AppListViewHolder extends RecyclerView.ViewHolder implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    /**
     * Views for displaying the app data and policy information.
     */
    public ImageView appIcon;
    public TextView appName;
    public CheckBox networkAccessCheckBox;
    public CheckBox wifiCheckBox;
    public CheckBox cellCheckBox;
    public CheckBox vpnCheckBox;
    public CheckBox backgroundDataCheckBox;
    public CheckBox dataSaverRestrictionCheckBox;

    /**
     * Info about the app of this view holder.
     */
    public AppInfo appInfo;
    
    private static boolean showVpn = false;
    private static boolean showBackgroundData = false;
    private static boolean showDataSaver = false;

    /**
     * Listener for a checkbox change.
     */
    private AppListCheckedChangeListener mCheckListener;

    /**
     * Listener for a click on the app name or app icon.
     */
    private AppListItemClickedListener mClickedListener;

    /**
     * Constructor.
     * @param rootView The root view of the layout.
     */
    public AppListViewHolder(View rootView) {
        super(rootView);

        appIcon = (ImageView) rootView.findViewById(R.id.app_icon);
        appName = (TextView) rootView.findViewById(R.id.app_name);
        networkAccessCheckBox = (CheckBox) rootView.findViewById(R.id.network_access_check_box);
        wifiCheckBox = (CheckBox) rootView.findViewById(R.id.wifi_check_box);
        cellCheckBox = (CheckBox) rootView.findViewById(R.id.cell_check_box);
        vpnCheckBox = (CheckBox) rootView.findViewById(R.id.vpn_check_box);
        backgroundDataCheckBox = (CheckBox) rootView.findViewById(R.id.background_data_check_box);
        dataSaverRestrictionCheckBox = (CheckBox) rootView.findViewById(
                R.id.unrestricted_data_saver_check_box);

        appIcon.setOnClickListener(this);
        appName.setOnClickListener(this);

        networkAccessCheckBox.setOnCheckedChangeListener(this);
        wifiCheckBox.setOnCheckedChangeListener(this);
        cellCheckBox.setOnCheckedChangeListener(this);
        vpnCheckBox.setOnCheckedChangeListener(this);
        backgroundDataCheckBox.setOnCheckedChangeListener(this);
        dataSaverRestrictionCheckBox.setOnCheckedChangeListener(this);
    }
    
    /**
     * Sets the visibility of the background data checkboxes.
     * @param visible true makes the checkboxes visible.
     */
    public static void setVpnVisible(boolean visible)
    {
    	showVpn = visible;
    }
    
    /**
     * Sets the visibility of the background data checkboxes.
     * @param visible true makes the checkboxes visible.
     */
    public static void setBackgroundDataVisible(boolean visible)
    {
    	showBackgroundData = visible;
    }
    
    /**
     * Sets the visibility of the data saver checkboxes.
     * @param visible true makes the checkboxes visible.
     */
    public static void setDataSaverVisible(boolean visible)
    {
    	showDataSaver = visible;
    }

    /**
     * Binds an new AppInfo to the view holder.
     * @param info AppInfo which should be bound to this view holder.
     */
    public void bindData(AppInfo info) {
        appInfo = info;

        appIcon.setImageDrawable(appInfo.getIcon());
        appName.setText(appInfo.getName());

        // Set the checkboxes to checked when internet access is granted or when
        // the policy can't be changed.
        
        // Network Access CheckBox
        networkAccessCheckBox.setChecked(appInfo.isNetworkAccessible());
        
        // Wifi CheckBox
        wifiCheckBox.setEnabled(appInfo.isNetworkAccessible());
        wifiCheckBox.setChecked(appInfo.isWifiEnabled() &&
            wifiCheckBox.isEnabled());
        
        // Cell CheckBox
        cellCheckBox.setEnabled(appInfo.isNetworkAccessible());
        cellCheckBox.setChecked(appInfo.isCellEnabled() &&
            cellCheckBox.isEnabled());
        
        // VPN CheckBox
        if(showVpn)
            vpnCheckBox.setVisibility(View.VISIBLE);
	    else
	        vpnCheckBox.setVisibility(View.GONE);
        
        vpnCheckBox.setEnabled(appInfo.isNetworkAccessible());
        vpnCheckBox.setChecked(appInfo.isVpnEnabled() &&
            cellCheckBox.isEnabled());
        
        // Background Data CheckBox
        if(showBackgroundData)
	        backgroundDataCheckBox.setVisibility(View.VISIBLE);
	    else
	        backgroundDataCheckBox.setVisibility(View.GONE);
	        
        backgroundDataCheckBox.setEnabled(appInfo.isCellEnabled() &&
            appInfo.isNetworkAccessible());
        backgroundDataCheckBox.setChecked(
        	backgroundDataCheckBox.isEnabled() &&
        	appInfo.isBackgroundDataEnabled());
        	
        // Data Saver CheckBox
        if(showDataSaver)
	        dataSaverRestrictionCheckBox.setVisibility(View.VISIBLE);
	    else
	        dataSaverRestrictionCheckBox.setVisibility(View.GONE);
        	
        dataSaverRestrictionCheckBox.setEnabled(
        	appInfo.isCellEnabled() &&
		    appInfo.isBackgroundDataEnabled() &&
		    appInfo.isNetworkAccessible());
        dataSaverRestrictionCheckBox.setChecked(
        	dataSaverRestrictionCheckBox.isEnabled() &&
        	appInfo.isDataSaverRestrictionEnabled());
    }

    /**
     * Sets the checked change listener.
     * @param listener The new checked change listener.
     */
    public void setOnAppListCheckedChangeListener(AppListCheckedChangeListener listener) {
        mCheckListener = listener;
    }

    /**
     * Sets the item clicked listener.
     * @param listener The new item clicked listener.
     */
    public void setOnAppListItemClickedListener(AppListItemClickedListener listener) {
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
            if (buttonView == networkAccessCheckBox) {
                appInfo.setNetworkAccessible(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.NETWORKACCESS));
                                       
            } else if (buttonView == wifiCheckBox) {
                appInfo.setWifiEnabled(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.WIFI));
                        
            } else if (buttonView == cellCheckBox) {
                appInfo.setCellEnabled(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.CELL));
                               
            } else if (buttonView == vpnCheckBox) {
                appInfo.setVpnEnabled(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.VPN));
                    
            } else if (buttonView == backgroundDataCheckBox) {
                appInfo.setBackgroundDataEnabled(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.BACKGROUNDDATA));
                        
            } else if (buttonView == dataSaverRestrictionCheckBox) {
                appInfo.setDataSaverRestrictionEnabled(isChecked);
                bindData(appInfo);
                mCheckListener.onAppListCheckedChange(appInfo,
                    EnumSet.of(AppListChangeFlag.DATASAVERRESTRICTION));
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
