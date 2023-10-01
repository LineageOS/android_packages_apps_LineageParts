/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.profiles.actions.item.Item;

import java.util.List;

public class ItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener {
    private final LayoutInflater mInflater;
    private final List<Item> mItems;
    private final OnItemClickListener mItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Item item, int position);
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public ItemListAdapter(Context context, List<Item> items, OnItemClickListener clickListener) {
        super();
        mInflater = LayoutInflater.from(context);
        mItems = items;
        mItemClickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;
        if (viewType == VIEW_TYPE_HEADER) {
            holder = new HeaderViewHolder(
                    mInflater.inflate(R.layout.profiles_header, parent, false));
        } else {
            final View view = mInflater.inflate(
                    androidx.preference.R.layout.preference_material, parent, false);
            holder = new ItemViewHolder(view);
        }
        holder.itemView.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Item item = mItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else {
            ((ItemViewHolder) holder).bind(item);
        }
        holder.itemView.setTag(holder);
    }

    @Override
    public void onClick(View view) {
        RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) view.getTag();
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            mItemClickListener.onItemClick(mItems.get(position), position);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;

        private HeaderViewHolder(View view) {
            super(view);
            mTitleView = view.findViewById(android.R.id.title);
        }

        private void bind(Item item) {
            mTitleView.setText(item.getTitle(itemView.getContext()));
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final TextView mSummaryView;

        private ItemViewHolder(View view) {
            super(view);
            mTitleView = view.findViewById(android.R.id.title);
            mSummaryView = view.findViewById(android.R.id.summary);
        }

        private void bind(Item item) {
            String title = item.getTitle(itemView.getContext());
            String summary = item.getSummary(itemView.getContext());
            boolean enabled = item.isEnabled(itemView.getContext());

            mTitleView.setText(title);
            mTitleView.setVisibility(title != null ? View.VISIBLE : View.GONE);
            mTitleView.setEnabled(enabled);
            mSummaryView.setText(summary);
            mSummaryView.setVisibility(summary != null ? View.VISIBLE : View.GONE);
            mSummaryView.setEnabled(enabled);
        }
    }
}
