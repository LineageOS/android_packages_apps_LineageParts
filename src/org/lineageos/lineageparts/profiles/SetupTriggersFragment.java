/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.profiles.triggers.NfcTriggerFragment;
import org.lineageos.lineageparts.widget.RtlCompatibleViewPager;
import org.lineageos.lineageparts.widget.SlidingTabLayout;

public class SetupTriggersFragment extends SettingsPreferenceFragment {

    RtlCompatibleViewPager mPager;
    Profile mProfile;
    ProfileManager mProfileManager;
    SlidingTabLayout mTabLayout;
    TriggerPagerAdapter mAdapter;
    boolean mNewProfileMode;
    int mPreselectedItem;

    public static final String EXTRA_INITIAL_PAGE = "current_item";

    private static final int REQUEST_SETUP_ACTIONS = 5;

    public static SetupTriggersFragment newInstance(Profile profile, boolean newProfile) {
        SetupTriggersFragment fragment = new SetupTriggersFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, newProfile);
        fragment.setArguments(args);
        return fragment;
    }

    public SetupTriggersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
            mNewProfileMode = getArguments().getBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
            mPreselectedItem = getArguments().getInt(EXTRA_INITIAL_PAGE, 0);
        }
        mProfileManager = ProfileManager.getInstance(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final PartsActivity activity = (PartsActivity) getActivity();
        if (mNewProfileMode) {
            activity.setTitle(R.string.profiles_create_new);
            activity.getTopIntro().setText(R.string.profile_setup_setup_triggers_title);
        } else {
            activity.setTitle(R.string.profile_profile_manage);
            activity.getTopIntro().setText(getString(
                    R.string.profile_setup_setup_triggers_title_config, mProfile.getName()));
        }
        activity.showTopIntro(true);

        activity.getCollapsingToolbarLayout().measure(
                View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
        activity.getCollapsingToolbarLayout().post(() ->
                mPager.setHeightOffset(mTabLayout.getHeight()));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager.setCurrentItem(mPreselectedItem);
        mTabLayout.setViewPager(mPager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_setup_triggers, container, false);

        mPager = (RtlCompatibleViewPager) root.findViewById(R.id.view_pager);
        mTabLayout = root.findViewById(R.id.sliding_tabs);
        mAdapter = new TriggerPagerAdapter(getActivity(), getChildFragmentManager());

        Bundle profileArgs = new Bundle();
        profileArgs.putParcelable(ProfilesSettings.EXTRA_PROFILE, mProfile);

        final TriggerPagerAdapter.TriggerFragments[] fragments =
                TriggerPagerAdapter.TriggerFragments.values();

        for (final TriggerPagerAdapter.TriggerFragments fragment : fragments) {
            if (fragment.getFragmentClass().equals(NfcTriggerFragment.class)) {
                if (!getActivity().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_NFC)) {
                    // device doesn't have NFC
                    continue;
                }
            }
            mAdapter.add(fragment.getFragmentClass(), profileArgs, fragment.getTitleRes());
        }

        mPager.setAdapter(mAdapter);

        if (mNewProfileMode) {
            showButtonBar(true);
            getNextButton().setOnClickListener(view -> {
                Bundle args = new Bundle();
                args.putParcelable(ProfilesSettings.EXTRA_PROFILE, mProfile);
                args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, mNewProfileMode);

                PartsActivity pa = (PartsActivity) getActivity();
                pa.startPreferencePanel(SetupActionsFragment.class.getCanonicalName(), args,
                        R.string.profile_profile_manage, null,
                        SetupTriggersFragment.this, REQUEST_SETUP_ACTIONS);
            });

            // back button
            getBackButton().setOnClickListener(view ->
                    finishPreferencePanel(SetupTriggersFragment.this, Activity.RESULT_CANCELED,
                    null));
        }
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETUP_ACTIONS) {
            if (resultCode == Activity.RESULT_OK) {
                // exit out of the wizard!
                finishFragment();
            }
        }
    }


}
