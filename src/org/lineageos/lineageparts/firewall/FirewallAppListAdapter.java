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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lineageos.lineageparts.R;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Implementation of a list adapter for the firewall app list.
 */
public class FirewallAppListAdapter extends RecyclerView.Adapter
        implements FirewallAppListCheckedChangeListener, FirewallAppListItemClickedListener {

    /**
     * List that holds all FirewallAppInfo elements of the list.
     */
    private List<FirewallAppInfo> mAppList;

    /**
     * Listeners for checked change and item clicked events
     */
    private FirewallAppListCheckedChangeListener mCheckListener;
    private FirewallAppListItemClickedListener mClickListener;

    public FirewallAppListAdapter() {
        super();

        mAppList = new ArrayList<FirewallAppInfo>();
    }

    /**
     * Creates a ViewHolder and inflates the layout for it.
     * @param parent Parent ViewGroup for inflating the layout.
     * @param viewType Type of the view that should be expanded. It is the same type as returned
     *                 from getItemViewType().
     * @return The created ViewHolder.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);

        FirewallAppListViewHolder holder = new FirewallAppListViewHolder(view);

        holder.setOnAppListCheckedChangeListener(this);
        holder.setOnAppListItemClickedListener(this);

        return holder;
    }

    /**
     * Binds new data to the ViewHolder.
     * @param holder ViewHolder to which new data should be bound.
     * @param position Index in the app list.
     */
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ((FirewallAppListViewHolder) holder).bindData(mAppList.get(position));
    }

    /**
     * Returns the number of items in the list.
     * @return Number of items in the list.
     */
    @Override
    public int getItemCount() {
        return mAppList.size();
    }

    /**
     * Returns the ViewType for a specific item in the app list.
     * @param position Index in the app list.
     * @return The ViewType of the selected item in the app list.
     */
    @Override
    public int getItemViewType(final int position) {
        return R.layout.firewall_app_list_item;
    }

    /**
     * This method is called, when the user changes a checkbox state.
     * @param appInfo FirewallAppInfo of the app the checkbox belongs to.
     * @param changeFlags Flags for determining, which checkbox was changed.
     */
    @Override
    public void onAppListCheckedChange(FirewallAppInfo appInfo,
                                       EnumSet<FirewallAppListChangeFlag> changeFlags) {
        if (mCheckListener != null) {
            mCheckListener.onAppListCheckedChange(appInfo, changeFlags);
        }
    }

    /**
     * This method is called, when the user clicks on the app name or app icon.
     * @param appInfo FirewallAppInfo of the clicked item.
     */
    @Override
    public void onAppListItemClicked(FirewallAppInfo appInfo) {
        if (mClickListener != null) {
            mClickListener.onAppListItemClicked(appInfo);
        }
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
        mClickListener = listener;
    }

    /**
     * Adds a specified FirewallAppInfo to the app list.
     * @param appInfo The FirewallAppInfo which should be added to the app list.
     */
    public void addAppInfo(FirewallAppInfo appInfo) {
        mAppList.add(appInfo);
    }

    /**
     * Removes a specified FirewallAppInfo from the app list.
     * @param appInfo The FirewallAppInfo which should be removed from the app list.
     */
    public void removeAppInfo(FirewallAppInfo appInfo) {
        mAppList.remove(appInfo);
    }

    /**
     * Clears the app list.
     */
    public void removeAllAppInfo() {
        mAppList.clear();
    }

    /**
     * Returns an element in the app list.
     * @param i Index in the app list.
     * @return The selected element in the app list.
     */
    public FirewallAppInfo getAppInfoAt(int i) {
        return mAppList.get(i);
    }
}