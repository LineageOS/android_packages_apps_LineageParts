/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *               2017 The LineageOS Project
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

package org.lineageos.lineageparts.input;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;

import org.lineageos.internal.util.PowerMenuConstants;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

import lineageos.providers.LineageSettings;

import static org.lineageos.internal.util.PowerMenuConstants.*;

public class PowerMenuActions extends SettingsPreferenceFragment {
    final static String TAG = "PowerMenuActions";

    private CheckBoxPreference mScreenshotPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mUsersPref;
    private CheckBoxPreference mBugReportPref;
    private CheckBoxPreference mLockDownPref;
    private CheckBoxPreference mEmergencyPref;

    Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;
    private ArrayList<String> mLocalUserConfig = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);
        mContext = getActivity().getApplicationContext();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mUserManager = UserManager.get(mContext);

        for (String action : PowerMenuConstants.getAllActions()) {
            if (action.equals(GLOBAL_ACTION_KEY_SCREENSHOT)) {
                mScreenshotPref = findPreference(GLOBAL_ACTION_KEY_SCREENSHOT);
            } else if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
            } else if (action.equals(GLOBAL_ACTION_KEY_USERS)) {
                mUsersPref = findPreference(GLOBAL_ACTION_KEY_USERS);
            } else if (action.equals(GLOBAL_ACTION_KEY_BUGREPORT)) {
                mBugReportPref = findPreference(GLOBAL_ACTION_KEY_BUGREPORT);
            } else if (action.equals(GLOBAL_ACTION_KEY_LOCKDOWN)) {
                mLockDownPref = findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
            } else if (action.equals(GLOBAL_ACTION_KEY_EMERGENCY)) {
                mEmergencyPref = findPreference(GLOBAL_ACTION_KEY_EMERGENCY);
            }
        }

        mLocalUserConfig = getUserConfig(mContext);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mScreenshotPref != null) {
            mScreenshotPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SCREENSHOT));
        }

        if (mAirplanePref != null) {
            mAirplanePref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_AIRPLANE));
        }

        if (mUsersPref != null) {
            if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
                getPreferenceScreen().removePreference(findPreference(GLOBAL_ACTION_KEY_USERS));
                mUsersPref = null;
            } else {
                List<UserInfo> users = mUserManager.getUsers();
                boolean enabled = (users.size() > 1);
                mUsersPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_USERS) && enabled);
                mUsersPref.setEnabled(enabled);
            }
        }

        if (mBugReportPref != null) {
            mBugReportPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_BUGREPORT));
        }

        if (mEmergencyPref != null) {
            mEmergencyPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_EMERGENCY));
        }

        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;

        if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_SCREENSHOT);

        } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mUsersPref) {
            value = mUsersPref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mBugReportPref) {
            value = mBugReportPref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_BUGREPORT);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Global.BUGREPORT_IN_POWER_MENU, value ? 1 : 0);

        } else if (preference == mLockDownPref) {
            value = mLockDownPref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_LOCKDOWN);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKDOWN_IN_POWER_MENU, value ? 1 : 0);

        } else if (preference == mEmergencyPref) {
            value = mEmergencyPref.isChecked();
            updateUserConfig(mContext, mLocalUserConfig, value, GLOBAL_ACTION_KEY_EMERGENCY);
        }

        else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private boolean settingsArrayContains(String preference) {
        return mLocalUserConfig.contains(preference);
    }

    private static void updateUserConfig(Context context, ArrayList<String> localUserConfig, boolean enabled, String action) {
        if (enabled) {
            if (!localUserConfig.contains(action)) {
                localUserConfig.add(action);
            }
        } else {
            if (localUserConfig.contains(action)) {
                localUserConfig.remove(action);
            }
        }
        saveUserConfig(context, localUserConfig);
    }

    private void updatePreferences() {
        UserInfo currentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        boolean developmentSettings = Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        boolean bugReport = Settings.Global.getInt(
                getContentResolver(), Settings.Global.BUGREPORT_IN_POWER_MENU, 0) == 1;
        boolean isPrimaryUser = currentUser == null || currentUser.isPrimary();
        if (mBugReportPref != null) {
            mBugReportPref.setEnabled(developmentSettings && isPrimaryUser);
            if (!developmentSettings) {
                mBugReportPref.setChecked(false);
                mBugReportPref.setSummary(R.string.power_menu_bug_report_devoptions_unavailable);
            }
            else if (!isPrimaryUser) {
                mBugReportPref.setChecked(false);
                mBugReportPref.setSummary(R.string.power_menu_bug_report_unavailable_for_user);
            }
            else {
                mBugReportPref.setChecked(bugReport);
                mBugReportPref.setSummary(null);
            }
        }

        boolean isKeyguardSecure = mLockPatternUtils.isSecure(UserHandle.myUserId());
        boolean lockdown = Settings.Global.getInt(
                getContentResolver(), Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) == 1;
        if (mLockDownPref != null) {
            mLockDownPref.setEnabled(isKeyguardSecure);
            if (isKeyguardSecure) {
                mLockDownPref.setChecked(lockdown);
                mLockDownPref.setSummary(null);
            } else {
                mLockDownPref.setChecked(false);
                mLockDownPref.setSummary(R.string.power_menu_lockdown_unavailable);
            }
        }
    }

    private static ArrayList<String> getUserConfig(Context context) {
        ArrayList<String> mLocalUserConfig = new ArrayList<String>();
        String[] actions;
        String savedActions = LineageSettings.Secure.getStringForUser(context.getContentResolver(),
                LineageSettings.Secure.POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);

        if (savedActions == null) {
            actions = context.getResources().getStringArray(
                    com.android.internal.R.array.config_globalActionsList);
        } else {
            actions = savedActions.split("\\|");
        }

        for (String action : actions) {
            mLocalUserConfig.add(action);
        }

        return mLocalUserConfig;
    }

    private static void saveUserConfig(Context context, ArrayList<String> localUserConfig) {
        StringBuilder s = new StringBuilder();

        ArrayList<String> setactions = new ArrayList<String>();
        for (String action : PowerMenuConstants.getAllActions()) {
            if (localUserConfig.contains(action)) {
                setactions.add(action);
            } else {
                continue;
            }
        }

        for (int i = 0; i < setactions.size(); i++) {
            s.append(setactions.get(i).toString());
            if (i != setactions.size() - 1) {
                s.append("|");
            }
        }

        LineageSettings.Secure.putStringForUser(context.getContentResolver(),
                LineageSettings.Secure.POWER_MENU_ACTIONS, s.toString(), UserHandle.USER_CURRENT);
    }

    public static class PowerMenuActionsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (lineageos.content.Intent.ACTION_UPDATE_POWER_MENU_BUGREPORT.equals(action)) {
                updateUserConfig(context, getUserConfig(context), Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.BUGREPORT_IN_POWER_MENU, 0) == 1, GLOBAL_ACTION_KEY_BUGREPORT);
            }
            else if (lineageos.content.Intent.ACTION_UPDATE_POWER_MENU_LOCKDOWN.equals(action)) {
                updateUserConfig(context, getUserConfig(context), Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) == 1, GLOBAL_ACTION_KEY_LOCKDOWN);
            }
        }
    }
}
