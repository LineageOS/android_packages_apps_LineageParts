/*
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles.triggers;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.profiles.ProfilesSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTriggerListFragment extends Fragment {
    private ProfileManager mProfileManager;
    private Profile mProfile;

    private final List<AbstractTriggerItem> mTriggers = new ArrayList<>();

    private View mEmptyView;
    private RecyclerView mRecyclerView;
    private TriggerAdapter mAdapter;

    private final TriggerAdapter.ItemClickListener mItemClickListener = item -> {
        final Resources res = getResources();
        final String[] possibleEntries = res.getStringArray(getOptionArrayResId());
        final String[] possibleValues = res.getStringArray(getOptionValuesArrayResId());
        final ArrayList<Trigger> triggers = new ArrayList<>();
        final TriggerInfo info = onConvertToTriggerInfo(item);

        for (int i = 0; i < possibleEntries.length; i++) {
            Trigger toAdd = new Trigger();
            toAdd.value = Integer.parseInt(possibleValues[i]);
            toAdd.name = possibleEntries[i];
            if (isTriggerStateSupported(info, toAdd.value)) {
                triggers.add(toAdd);
            }
        }

        final String[] entries = new String[triggers.size()];
        final int[] valueInts = new int[triggers.size()];
        int currentTriggerState = mProfile.getTriggerState(info.type, info.id);
        int currentItem = -1;
        for (int i = 0; i < triggers.size(); i++) {
            Trigger t = triggers.get(i);
            entries[i] = t.name;
            valueInts[i] = t.value;
            if (valueInts[i] == currentTriggerState) {
                currentItem = i;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_trigger_configure)
                .setSingleChoiceItems(entries, currentItem, (dialog, which) -> {
                    mProfile.setTrigger(info.type, info.id, valueInts[which], info.name);
                    mProfileManager.updateProfile(mProfile);
                    updateTriggerList();
                    dialog.dismiss();
                })
                .show();
    };

    protected static Bundle initArgs(Profile profile) {
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileManager = ProfileManager.getInstance(getActivity());
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_trigger_list, container, false);

        ViewStub emptyViewStub = view.findViewById(android.R.id.empty);
        emptyViewStub.setLayoutResource(getEmptyViewLayoutResId());

        mEmptyView = emptyViewStub.inflate();
        mEmptyView.setOnClickListener(v -> startActivity(getEmptyViewClickIntent()));

        mRecyclerView = view.findViewById(android.R.id.list);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new TriggerAdapter(mTriggers, mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTriggerList();
    }

    protected abstract void onLoadTriggers(Profile profile, List<AbstractTriggerItem> triggers);
    protected abstract TriggerInfo onConvertToTriggerInfo(AbstractTriggerItem trigger);
    protected abstract boolean isTriggerStateSupported(TriggerInfo info, int triggerState);
    protected abstract int getEmptyViewLayoutResId();
    protected abstract Intent getEmptyViewClickIntent();
    protected abstract int getOptionArrayResId();
    protected abstract int getOptionValuesArrayResId();

    protected void initTriggerItemFromState(AbstractTriggerItem trigger, int state, int iconResId) {
        final String[] values = getResources().getStringArray(getOptionValuesArrayResId());
        for (int i = 0; i < values.length; i++) {
            if (Integer.parseInt(values[i]) == state) {
                trigger.setSummary(getResources().getStringArray(getOptionArrayResId())[i]);
                break;
            }
        }
        trigger.setTriggerState(state);
        trigger.setIcon(iconResId);
    }

    private void updateTriggerList() {
        mTriggers.clear();
        onLoadTriggers(mProfile, mTriggers);
        mAdapter.notifyDataSetChanged();

        mRecyclerView.setVisibility(mTriggers.isEmpty() ? View.GONE : View.VISIBLE);
        mEmptyView.setVisibility(mTriggers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static class Trigger {
        int value;
        String name;
    }

    protected static class TriggerInfo {
        final String id;
        final String name;
        final int type;

        protected TriggerInfo(int type, String id, String name) {
            this.type = type;
            this.id = id;
            this.name = name;
        }
    }

    private static class TriggerViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final TextView mDescView;
        private final ImageView mIconView;

        public TriggerViewHolder(View view) {
            super(view);
            mTitleView = view.findViewById(R.id.title);
            mDescView = view.findViewById(R.id.desc);
            mIconView = view.findViewById(R.id.icon);
        }

        public void bind(AbstractTriggerItem trigger) {
            mTitleView.setText(trigger.getTitle());
            mDescView.setText(trigger.getSummary());
            mIconView.setImageResource(trigger.getIcon());
        }
    }

    private static class TriggerAdapter extends RecyclerView.Adapter<TriggerViewHolder>
            implements View.OnClickListener {
        interface ItemClickListener {
            void onItemClick(AbstractTriggerItem item);
        }

        private final List<AbstractTriggerItem> mTriggers;
        private final ItemClickListener mItemClickListener;

        public TriggerAdapter(List<AbstractTriggerItem> triggers,
                ItemClickListener itemClickListener) {
            super();
            mTriggers = triggers;
            mItemClickListener = itemClickListener;
        }

        @Override
        public int getItemCount() {
            return mTriggers.size();
        }

        @Override
        public TriggerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            TriggerViewHolder holder = new TriggerViewHolder(
                    inflater.inflate(R.layout.abstract_trigger_row, parent, false));
            holder.itemView.setOnClickListener(this);
            return holder;
        }

        @Override
        public void onBindViewHolder(TriggerViewHolder holder, int position) {
            holder.bind(mTriggers.get(position));
            holder.itemView.setTag(holder);
        }

        @Override
        public void onClick(View view) {
            TriggerViewHolder holder = (TriggerViewHolder) view.getTag();
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                mItemClickListener.onItemClick(mTriggers.get(position));
            }
        }
    }
}
