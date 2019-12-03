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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.lineageparts.R;

/**
 * Implementation for a app list view. It uses a RecyclerView to display the list.
 */
public class FirewallAppList extends LinearLayout {

    /**
     * Reference to the ListAdapter, which holds all list items.
     */
    private FirewallAppListAdapter mListAdapter;

    /**
     * Reference to the RecyclerView, which displays the list.
     */
    private RecyclerView mRecyclerView;

    /**
     * Simple constructor to use when creating a view from code.
     * @param context Reference to a Context.
     */
    public FirewallAppList(Context context) {
        super(context);

        init("");
    }

    /**
     * Constructor that is called when inflating a view from XML.
     * @param context Reference to a Context.
     * @param attrs Attributes set via xml. Only "title" is supported at the moment.
     */
    public FirewallAppList(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.title});

        init(array.getString(0));
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * @param context Reference to a Context.
     * @param attrs Attributes set via xml. Only "title" is supported at the moment.
     * @param defStyleAttr Style or theme attribute.
     */
    public FirewallAppList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.title});

        init(array.getString(0));
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource.
     * @param context Reference to a Context.
     * @param attrs Attributes set via xml. Only "title" is supported at the moment.
     * @param defStyleAttr Style or theme attribute.
     * @param defStyleRes Style resource.
     */
    public FirewallAppList(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.title});

        init(array.getString(0));
    }

    /**
     * Performs the initialisation of the app list.
     * @param title The title which will be shown in the header of the app list.
     */
    private void init(String title) {
        View holder = inflate(getContext(), R.layout.firewall_app_list, this);

        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        titleView.setText(title);

        mRecyclerView = (RecyclerView) holder.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);

        mListAdapter = new FirewallAppListAdapter();
        mRecyclerView.setAdapter(mListAdapter);
    }

    /**
     * Sets a FirewallAppListCheckedChangeListener to the list adapter.
     * @param listener Listener to set.
     */
    public void setOnAppListCheckedChangeListener(FirewallAppListCheckedChangeListener listener) {
        mListAdapter.setOnAppListCheckedChangeListener(listener);
    }

    /**
     * Sets a FirewallAppListItemClickedListener to the list adapter.
     * @param listener Listener to set.
     */
    public void setOnAppListItemClickedListener(FirewallAppListItemClickedListener listener) {
        mListAdapter.setOnAppListItemClickedListener(listener);
    }

    /**
     * Adds the specified app info to the app list.
     * @param appInfo The app info to add to the list.
     */
    public void addAppInfo(FirewallAppInfo appInfo) {
        mListAdapter.addAppInfo(appInfo);
    }

    /**
     * Removes the specified app info from the app list.
     * @param appInfo The app info to remove from the list.
     */
    public void removeAppInfo(FirewallAppInfo appInfo) {
        mListAdapter.removeAppInfo(appInfo);
    }

    /**
     * Clears the app info list.
     */
    public void removeAllAppInfo() {
        mListAdapter.removeAllAppInfo();
    }

    /**
     * Retuns the element count of the app list.
     * @return Number of elements in the list.
     */
    public int getAppInfoCount() {
        return mListAdapter.getItemCount();
    }

    /**
     * Returns the app info at the specified index of the list.
     * @param i Index of the list at which the app info should be returned.
     * @return The requested app info.
     */
    public FirewallAppInfo getAppInfoAt(int i) {
        return mListAdapter.getAppInfoAt(i);
    }

    /**
     * Updates the List. This method must be called after any change of the data set.
     */
    public void notifyDataSetChanged() {
        mListAdapter.notifyDataSetChanged();
    }

}
