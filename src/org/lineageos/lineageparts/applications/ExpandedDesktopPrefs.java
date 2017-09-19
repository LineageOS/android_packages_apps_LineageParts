/**
 * Copyright (C) 2015-2016 The CyanogenMod Project
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

package org.lineageos.lineageparts.applications;

import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyControl;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import lineageos.preference.SettingsHelper;

public class ExpandedDesktopPrefs extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, SettingsHelper.OnSettingsChangeListener {

    private static final String KEY_EXPANDED_DESKTOP_OPTIONS = "expanded_desktop_options";
    private static final String KEY_EXPANDED_DESKTOP_STYLE = "expanded_desktop_style";

    private final Uri DEFAULT_WINDOW_POLICY_STYLE =
            Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL_STYLE);

    private ListPreference mExpandedDesktopStylePref;
    private int mExpandedDesktopStyle;

    public static ExpandedDesktopPrefs newInstance() {
        return new ExpandedDesktopPrefs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.expanded_desktop_prefs);

        boolean hasNavigationBar = true;
        try {
            hasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (RemoteException e) {
            // Do nothing
        }

        if (hasNavigationBar) {
            mExpandedDesktopStyle = getExpandedDesktopStyle();
            createPreferences();
        } else {
            removePreferences();
        }

        watch(DEFAULT_WINDOW_POLICY_STYLE);
    }

    private void createPreferences() {
        mExpandedDesktopStylePref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP_STYLE);
        mExpandedDesktopStylePref.setOnPreferenceChangeListener(this);
        updateExpandedDesktopStyle();
    }

    private void removePreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        final PreferenceCategory category =
                (PreferenceCategory) screen.findPreference(KEY_EXPANDED_DESKTOP_OPTIONS);
        screen.removePreference(category);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final int val = Integer.parseInt((String) value);
        WindowManagerPolicyControl.saveStyleToSettings(getActivity(), val);
        return true;
    }

    private void updateExpandedDesktopStyle() {
        if (mExpandedDesktopStylePref == null) {
            return;
        }
        mExpandedDesktopStyle = getExpandedDesktopStyle();
        mExpandedDesktopStylePref.setValueIndex(mExpandedDesktopStyle);
        mExpandedDesktopStylePref.setSummary(getDesktopSummary(mExpandedDesktopStyle));
        // We need to visually show the change
        // TODO: This is hacky, but it works
        writeValue("");
        writeValue("immersive.full=*");
    }

    private int getDesktopSummary(final int state) {
        switch (state) {
            case WindowManagerPolicyControl.ImmersiveDefaultStyles.IMMERSIVE_STATUS:
                return R.string.expanded_desktop_style_hide_status;
            case WindowManagerPolicyControl.ImmersiveDefaultStyles.IMMERSIVE_NAVIGATION:
                return R.string.expanded_desktop_style_hide_navigation;
            case WindowManagerPolicyControl.ImmersiveDefaultStyles.IMMERSIVE_FULL:
            default:
                return R.string.expanded_desktop_style_hide_both;
        }
    }

    private int getExpandedDesktopStyle() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.POLICY_CONTROL_STYLE,
                WindowManagerPolicyControl.ImmersiveDefaultStyles.IMMERSIVE_FULL);
    }

    private void writeValue(final String value) {
        Settings.Global.putString(getContentResolver(), Settings.Global.POLICY_CONTROL, value);
    }

    @Override
    public void onSettingsChanged(Uri settingsUri) {
        super.onSettingsChanged(settingsUri);
        updateExpandedDesktopStyle();
    }
}
