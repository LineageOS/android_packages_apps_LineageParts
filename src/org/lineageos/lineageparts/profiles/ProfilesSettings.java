/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.MainSwitchBar;

import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import java.util.UUID;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;
import lineageos.providers.LineageSettings;

public class ProfilesSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "ProfilesSettings";

    public static final String EXTRA_PROFILE = "Profile";
    public static final String EXTRA_NEW_PROFILE = "new_profile_mode";

    private static final int MENU_RESET = Menu.FIRST;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private MainSwitchBar mProfileEnabler;
    private ProfileManager mProfileManager;

    private boolean mEnabled;

    ViewGroup mContainer;

    static Bundle mSavedState;

    public ProfilesSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(ProfileManager.PROFILES_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ProfileManager.PROFILES_STATE_CHANGED_ACTION.equals(action)) {
                    updateProfilesEnabledState();
                }
            }
        };

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.profiles_settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        FrameLayout frameLayout = new FrameLayout(getActivity());
        mContainer = frameLayout;
        frameLayout.addView(view);
        return frameLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.empty_textview, (ViewGroup) view, true);

        TextView emptyTextView = v.findViewById(R.id.empty);
        setEmptyView(emptyTextView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mProfileManager = ProfileManager.getInstance(getActivity());
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);

        setDivider(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mFilter);
        updateProfilesEnabledState();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        final PartsActivity activity = (PartsActivity) getActivity();
        mProfileEnabler = activity.getMainSwitchBar();
        mProfileEnabler.getSwitch().setOnCheckedChangeListener((buttonView, isChecked) -> {
            LineageSettings.System.putInt(activity.getContentResolver(),
                    LineageSettings.System.SYSTEM_PROFILES_ENABLED, isChecked ? 1 : 0);
        });
        mProfileEnabler.setTitle(getString(R.string.profiles_settings_enable_title));
        mProfileEnabler.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setAlphabeticShortcut('r')
                .setEnabled(mEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProfile() {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_NEW_PROFILE, true);
        args.putParcelable(EXTRA_PROFILE, new Profile(getString(R.string.new_profile_name)));

        PartsActivity pa = (PartsActivity) getActivity();
        pa.startPreferencePanel(SetupTriggersFragment.class.getCanonicalName(), args,
                0, null, this, 0);
    }

    private void resetAll() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_reset_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.profile_reset_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mProfileManager.resetAll();
                        mProfileManager.setActiveProfile(
                                mProfileManager.getActiveProfile().getUuid());
                        dialog.dismiss();
                        refreshList();

                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateProfilesEnabledState() {
        FragmentActivity activity = getActivity();

        mEnabled = LineageSettings.System.getInt(activity.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;
        mProfileEnabler.setChecked(mEnabled);
        activity.invalidateOptionsMenu();

        if (!mEnabled) {
            getPreferenceScreen().removeAll(); // empty it
        } else {
            refreshList();
        }

        onSettingsChanged(null);
    }

    public void refreshList() {
        PreferenceScreen plist = getPreferenceScreen();
        plist.removeAll();

        // Get active profile, if null
        Profile prof = mProfileManager.getActiveProfile();
        String selectedKey = prof != null ? prof.getUuid().toString() : null;

        for (Profile profile : mProfileManager.getProfiles()) {
            Bundle args = new Bundle();
            args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
            args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);

            ProfilesPreference ppref = new ProfilesPreference(this, args);
            ppref.setKey(profile.getUuid().toString());
            ppref.setTitle(profile.getName());
            ppref.setPersistent(false);
            ppref.setOnPreferenceChangeListener(this);
            ppref.setSelectable(true);
            ppref.setEnabled(true);

            if (TextUtils.equals(selectedKey, ppref.getKey())) {
                ppref.setChecked(true);
            }

            plist.addPreference(ppref);
        }

        // Add pref to create new profile
        Preference preference = new Preference(getContext());
        preference.setIcon(R.drawable.ic_add_24dp);
        preference.setTitle(R.string.profiles_create_new);
        preference.setSelectable(true);
        preference.setEnabled(true);
        preference.setOnPreferenceClickListener((pref) -> {
            addProfile();
            return true;
        });

        plist.addPreference(preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue instanceof String) {
            setSelectedProfile((String) newValue);
            refreshList();
        }
        return true;
    }

    private void setSelectedProfile(String key) {
        try {
            UUID selectedUuid = UUID.fromString(key);
            mProfileManager.setActiveProfile(selectedUuid);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        ProfileManager pm = ProfileManager.getInstance(context);
        if (!pm.isProfilesEnabled()) {
            return context.getString(R.string.profile_settings_summary_off);
        }

        Profile p = pm.getActiveProfile();
        return p != null ? p.getName() : null;
    };
}
