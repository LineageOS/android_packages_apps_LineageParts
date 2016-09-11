/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package org.cyanogenmod.cmparts.profiles;

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;

import org.cyanogenmod.cmparts.R;
import org.cyanogenmod.cmparts.SettingsPreferenceFragment;
import org.cyanogenmod.cmparts.PartsActivity;
import org.cyanogenmod.cmparts.profiles.triggers.NfcTriggerFragment;

public class SetupTriggersFragment extends SettingsPreferenceFragment {

    ViewPager mPager;
    Profile mProfile;
    ProfileManager mProfileManager;
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            if (mNewProfileMode) {
                actionBar.setTitle(R.string.profile_setup_setup_triggers_title);
            } else {
                String title = getString(R.string.profile_setup_setup_triggers_title_config,
                        mProfile.getName());
                actionBar.setTitle(title);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager.setCurrentItem(mPreselectedItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_setup_triggers, container, false);

        mPager = (ViewPager) root.findViewById(R.id.view_pager);
        mAdapter = new TriggerPagerAdapter(getActivity(), getChildFragmentManager());

        Bundle profileArgs = new Bundle();
        profileArgs.putParcelable(ProfilesSettings.EXTRA_PROFILE, mProfile);

        final TriggerPagerAdapter.TriggerFragments[] fragments =
                TriggerPagerAdapter.TriggerFragments.values();

        for (final TriggerPagerAdapter.TriggerFragments fragment : fragments) {
            if (fragment.getFragmentClass() == NfcTriggerFragment.class) {
                if (!getActivity().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_NFC)) {
                    // device doesn't have NFC
                    continue;
                }
            }
            mAdapter.add(fragment.getFragmentClass(), profileArgs, fragment.getTitleRes());
        }

        mPager.setAdapter(mAdapter);

        PagerTabStrip tabs = (PagerTabStrip) root.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(R.color.theme_accent);

        // HACK - https://code.google.com/p/android/issues/detail?id=213359
        ((ViewPager.LayoutParams)tabs.getLayoutParams()).isDecor = true;

        if (mNewProfileMode) {
            showButtonBar(true);
            getNextButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle args = new Bundle();
                    args.putParcelable(ProfilesSettings.EXTRA_PROFILE,  mProfile);
                    args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, mNewProfileMode);

                    PartsActivity pa = (PartsActivity) getActivity();
                    pa.startPreferencePanel(SetupActionsFragment.class.getCanonicalName(), args,
                            R.string.profile_profile_manage, null,
                            SetupTriggersFragment.this, REQUEST_SETUP_ACTIONS);
                }
            });

            // back button
            getBackButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finishPreferencePanel(SetupTriggersFragment.this, Activity.RESULT_CANCELED, null);
                }
            });
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
