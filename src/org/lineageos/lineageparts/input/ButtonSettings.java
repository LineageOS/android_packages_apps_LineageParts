/*
 * Copyright (C) 2016 The CyanogenMod project
 *               2017-2020 The LineageOS project
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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;
import org.lineageos.lineageparts.utils.TelephonyUtils;
import org.lineageos.internal.util.ScreenType;

import static org.lineageos.internal.util.DeviceKeysConstants.*;

import java.util.List;
import java.util.Set;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

public class ButtonSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {
    private static final String TAG = "SystemSettings";

    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_BACK_WAKE_SCREEN = "back_wake_screen";
    private static final String KEY_CAMERA_LAUNCH = "camera_launch";
    private static final String KEY_CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";
    private static final String KEY_CAMERA_WAKE_SCREEN = "camera_wake_screen";
    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_HOME_WAKE_SCREEN = "home_wake_screen";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_MENU_WAKE_SCREEN = "menu_wake_screen";
    private static final String KEY_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String KEY_ASSIST_WAKE_SCREEN = "assist_wake_screen";
    private static final String KEY_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String KEY_APP_SWITCH_WAKE_SCREEN = "app_switch_wake_screen";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_PANEL_ON_LEFT = "volume_panel_on_left";
    private static final String KEY_VOLUME_WAKE_SCREEN = "volume_wake_screen";
    private static final String KEY_VOLUME_ANSWER_CALL = "volume_answer_call";
    private static final String KEY_DISABLE_NAV_KEYS = "disable_nav_keys";
    private static final String KEY_NAVIGATION_ARROW_KEYS = "navigation_bar_menu_arrow_keys";
    private static final String KEY_NAVIGATION_HOME_LONG_PRESS = "navigation_home_long_press";
    private static final String KEY_NAVIGATION_HOME_DOUBLE_TAP = "navigation_home_double_tap";
    private static final String KEY_NAVIGATION_APP_SWITCH_LONG_PRESS =
            "navigation_app_switch_long_press";
    private static final String KEY_EDGE_LONG_SWIPE = "navigation_bar_edge_long_swipe";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_TORCH_LONG_PRESS_POWER_GESTURE =
            "torch_long_press_power_gesture";
    private static final String KEY_TORCH_LONG_PRESS_POWER_TIMEOUT =
            "torch_long_press_power_timeout";
    private static final String KEY_CLICK_PARTIAL_SCREENSHOT =
            "click_partial_screenshot";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_NAVBAR = "navigation_bar_category";
    private static final String CATEGORY_EXTRAS = "extras_category";

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private SwitchPreference mCameraWakeScreen;
    private SwitchPreference mCameraSleepOnRelease;
    private SwitchPreference mCameraLaunch;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mVolumeWakeScreen;
    private SwitchPreference mVolumeMusicControls;
    private SwitchPreference mSwapVolumeButtons;
    private SwitchPreference mVolumePanelOnLeft;
    private SwitchPreference mDisableNavigationKeys;
    private SwitchPreference mNavigationArrowKeys;
    private ListPreference mNavigationHomeLongPressAction;
    private ListPreference mNavigationHomeDoubleTapAction;
    private ListPreference mNavigationAppSwitchLongPressAction;
    private ListPreference mEdgeLongSwipeAction;
    private SwitchPreference mPowerEndCall;
    private SwitchPreference mHomeAnswerCall;
    private SwitchPreference mTorchLongPressPowerGesture;
    private ListPreference mTorchLongPressPowerTimeout;

    private PreferenceCategory mNavigationPreferencesCat;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final boolean hasPowerKey = DeviceUtils.hasPowerKey();
        final boolean hasHomeKey = DeviceUtils.hasHomeKey(getActivity());
        final boolean hasBackKey = DeviceUtils.hasBackKey(getActivity());
        final boolean hasMenuKey = DeviceUtils.hasMenuKey(getActivity());
        final boolean hasAssistKey = DeviceUtils.hasAssistKey(getActivity());
        final boolean hasAppSwitchKey = DeviceUtils.hasAppSwitchKey(getActivity());
        final boolean hasCameraKey = DeviceUtils.hasCameraKey(getActivity());
        final boolean hasVolumeKeys = DeviceUtils.hasVolumeKeys(getActivity());

        final boolean showHomeWake = DeviceUtils.canWakeUsingHomeKey(getActivity());
        final boolean showBackWake = DeviceUtils.canWakeUsingBackKey(getActivity());
        final boolean showMenuWake = DeviceUtils.canWakeUsingMenuKey(getActivity());
        final boolean showAssistWake = DeviceUtils.canWakeUsingAssistKey(getActivity());
        final boolean showAppSwitchWake = DeviceUtils.canWakeUsingAppSwitchKey(getActivity());
        final boolean showCameraWake = DeviceUtils.canWakeUsingCameraKey(getActivity());
        final boolean showVolumeWake = DeviceUtils.canWakeUsingVolumeKeys(getActivity());

        boolean hasAnyBindableKey = false;
        final PreferenceCategory powerCategory = prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory homeCategory = prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory = prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory = prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory volumeCategory = prefScreen.findPreference(CATEGORY_VOLUME);
        final PreferenceCategory cameraCategory = prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory extrasCategory = prefScreen.findPreference(CATEGORY_EXTRAS);

        // Power button ends calls.
        mPowerEndCall = findPreference(KEY_POWER_END_CALL);

        // Long press power while display is off to activate torchlight
        mTorchLongPressPowerGesture = findPreference(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
        final int torchLongPressPowerTimeout = LineageSettings.System.getInt(resolver,
                LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, 0);
        mTorchLongPressPowerTimeout = initList(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT,
                torchLongPressPowerTimeout);

        // Home button answers calls.
        mHomeAnswerCall = findPreference(KEY_HOME_ANSWER_CALL);

        mHandler = new Handler();

        // Force Navigation bar related options
        mDisableNavigationKeys = findPreference(KEY_DISABLE_NAV_KEYS);

        mNavigationPreferencesCat = findPreference(CATEGORY_NAVBAR);

        Action defaultHomeLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnHomeBehavior));
        Action defaultHomeDoubleTapAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_doubleTapOnHomeBehavior));
        Action defaultAppSwitchLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnAppSwitchBehavior));
        Action homeLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                defaultHomeLongPressAction);
        Action homeDoubleTapAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                defaultHomeDoubleTapAction);
        Action appSwitchLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultAppSwitchLongPressAction);
        Action edgeLongSwipeAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_EDGE_LONG_SWIPE_ACTION,
                Action.NOTHING);

        // Navigation bar arrow keys while typing
        mNavigationArrowKeys = findPreference(KEY_NAVIGATION_ARROW_KEYS);

        // Navigation bar home long press
        mNavigationHomeLongPressAction = initList(KEY_NAVIGATION_HOME_LONG_PRESS,
                homeLongPressAction);

        // Navigation bar home double tap
        mNavigationHomeDoubleTapAction = initList(KEY_NAVIGATION_HOME_DOUBLE_TAP,
                homeDoubleTapAction);

        // Navigation bar app switch long press
        mNavigationAppSwitchLongPressAction = initList(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS,
                appSwitchLongPressAction);

        // Edge long swipe gesture
        mEdgeLongSwipeAction = initList(KEY_EDGE_LONG_SWIPE, edgeLongSwipeAction);

        // Hardware key disabler
        if (isKeyDisablerSupported(getActivity())) {
            // Remove keys that can be provided by the navbar
            updateDisableNavkeysOption();
            mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }
        updateDisableNavkeysCategories(mDisableNavigationKeys.isChecked(), /* force */ true);

        if (hasPowerKey) {
            if (!TelephonyUtils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
            if (!DeviceUtils.deviceSupportsFlashLight(getActivity())) {
                powerCategory.removePreference(mTorchLongPressPowerGesture);
                powerCategory.removePreference(mTorchLongPressPowerTimeout);
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        if (hasHomeKey) {
            if (!showHomeWake) {
                homeCategory.removePreference(findPreference(KEY_HOME_WAKE_SCREEN));
            }

            if (!TelephonyUtils.isVoiceCapable(getActivity())) {
                homeCategory.removePreference(mHomeAnswerCall);
                mHomeAnswerCall = null;
            }

            mHomeLongPressAction = initList(KEY_HOME_LONG_PRESS, homeLongPressAction);
            mHomeDoubleTapAction = initList(KEY_HOME_DOUBLE_TAP, homeDoubleTapAction);
            if (mDisableNavigationKeys.isChecked()) {
                mHomeLongPressAction.setEnabled(false);
                mHomeDoubleTapAction.setEnabled(false);
            }

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(homeCategory);
        }

        if (hasBackKey) {
            if (!showBackWake) {
                backCategory.removePreference(findPreference(KEY_BACK_WAKE_SCREEN));
                prefScreen.removePreference(backCategory);
            }
        } else {
            prefScreen.removePreference(backCategory);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                menuCategory.removePreference(findPreference(KEY_MENU_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    LineageSettings.System.KEY_MENU_ACTION, Action.MENU);
            mMenuPressAction = initList(KEY_MENU_PRESS, pressAction);

            Action longPressAction = Action.fromSettings(resolver,
                        LineageSettings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? Action.NOTHING : Action.APP_SWITCH);
            mMenuLongPressAction = initList(KEY_MENU_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(menuCategory);
        }

        if (hasAssistKey) {
            if (!showAssistWake) {
                assistCategory.removePreference(findPreference(KEY_ASSIST_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    LineageSettings.System.KEY_ASSIST_ACTION, Action.SEARCH);
            mAssistPressAction = initList(KEY_ASSIST_PRESS, pressAction);

            Action longPressAction = Action.fromSettings(resolver,
                    LineageSettings.System.KEY_ASSIST_LONG_PRESS_ACTION, Action.VOICE_SEARCH);
            mAssistLongPressAction = initList(KEY_ASSIST_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(assistCategory);
        }

        if (hasAppSwitchKey) {
            if (!showAppSwitchWake) {
                appSwitchCategory.removePreference(findPreference(KEY_APP_SWITCH_WAKE_SCREEN));
            }

            Action pressAction = Action.fromSettings(resolver,
                    LineageSettings.System.KEY_APP_SWITCH_ACTION, Action.APP_SWITCH);
            mAppSwitchPressAction = initList(KEY_APP_SWITCH_PRESS, pressAction);

            mAppSwitchLongPressAction = initList(KEY_APP_SWITCH_LONG_PRESS, appSwitchLongPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(appSwitchCategory);
        }

        if (hasCameraKey) {
            mCameraWakeScreen = findPreference(KEY_CAMERA_WAKE_SCREEN);
            mCameraSleepOnRelease = findPreference(KEY_CAMERA_SLEEP_ON_RELEASE);
            mCameraLaunch = findPreference(KEY_CAMERA_LAUNCH);

            if (!showCameraWake) {
                prefScreen.removePreference(mCameraWakeScreen);
            }
            // Only show 'Camera sleep on release' if the device has a focus key
            if (res.getBoolean(org.lineageos.platform.internal.R.bool.config_singleStageCameraKey)) {
                prefScreen.removePreference(mCameraSleepOnRelease);
            }
        } else {
            prefScreen.removePreference(cameraCategory);
        }

        if (hasVolumeKeys) {
            if (!showVolumeWake) {
                volumeCategory.removePreference(findPreference(KEY_VOLUME_WAKE_SCREEN));
            }

            if (!TelephonyUtils.isVoiceCapable(getActivity())) {
                volumeCategory.removePreference(findPreference(KEY_VOLUME_ANSWER_CALL));
            }

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = LineageSettings.System.getInt(getContentResolver(),
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            if (mSwapVolumeButtons != null) {
                mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
            }

            final boolean volumePanelOnLeft = LineageSettings.Secure.getIntForUser(
                    getContentResolver(), LineageSettings.Secure.VOLUME_PANEL_ON_LEFT, 0,
                    UserHandle.USER_CURRENT) != 0;
            mVolumePanelOnLeft = prefScreen.findPreference(KEY_VOLUME_PANEL_ON_LEFT);
            if (mVolumePanelOnLeft != null) {
                mVolumePanelOnLeft.setChecked(volumePanelOnLeft);
            }
        } else {
            prefScreen.removePreference(volumeCategory);
            extrasCategory.removePreference(findPreference(KEY_CLICK_PARTIAL_SCREENSHOT));
        }

        // Only show the navigation bar category on devices that have a navigation bar
        // or support disabling the hardware keys
        if (!hasNavigationBar() && !isKeyDisablerSupported(getActivity())) {
            prefScreen.removePreference(mNavigationPreferencesCat);
        }

        final ButtonBacklightBrightness backlight = findPreference(KEY_BUTTON_BACKLIGHT);
        if (!backlight.isButtonSupported(getActivity())
                && !backlight.isKeyboardSupported(getActivity())) {
            prefScreen.removePreference(backlight);
        }

        if (mCameraWakeScreen != null) {
            if (mCameraSleepOnRelease != null && !res.getBoolean(
                    org.lineageos.platform.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setDependency(KEY_CAMERA_WAKE_SCREEN);
            }
        }

        mVolumeWakeScreen = findPreference(KEY_VOLUME_WAKE_SCREEN);
        mVolumeMusicControls = findPreference(KEY_VOLUME_MUSIC_CONTROLS);

        if (mVolumeWakeScreen != null) {
            if (mVolumeMusicControls != null) {
                mVolumeMusicControls.setDependency(KEY_VOLUME_WAKE_SCREEN);
                mVolumeWakeScreen.setDisableDependentsState(true);
            }
        }

        // Override key actions on Go devices in order to hide any unsupported features
        if (ActivityManager.isLowRamDeviceStatic()) {
            String[] actionEntriesGo = res.getStringArray(R.array.hardware_keys_action_entries_go);
            String[] actionValuesGo = res.getStringArray(R.array.hardware_keys_action_values_go);

            if (hasHomeKey) {
                mHomeLongPressAction.setEntries(actionEntriesGo);
                mHomeLongPressAction.setEntryValues(actionValuesGo);

                mHomeDoubleTapAction.setEntries(actionEntriesGo);
                mHomeDoubleTapAction.setEntryValues(actionValuesGo);
            }

            if (hasMenuKey) {
                mMenuPressAction.setEntries(actionEntriesGo);
                mMenuPressAction.setEntryValues(actionValuesGo);

                mMenuLongPressAction.setEntries(actionEntriesGo);
                mMenuLongPressAction.setEntryValues(actionValuesGo);
            }

            if (hasAssistKey) {
                mAssistPressAction.setEntries(actionEntriesGo);
                mAssistPressAction.setEntryValues(actionValuesGo);

                mAssistLongPressAction.setEntries(actionEntriesGo);
                mAssistLongPressAction.setEntryValues(actionValuesGo);
            }

            if (hasAppSwitchKey) {
                mAppSwitchPressAction.setEntries(actionEntriesGo);
                mAppSwitchPressAction.setEntryValues(actionValuesGo);

                mAppSwitchLongPressAction.setEntries(actionEntriesGo);
                mAppSwitchLongPressAction.setEntryValues(actionValuesGo);
            }

            mNavigationHomeLongPressAction.setEntries(actionEntriesGo);
            mNavigationHomeLongPressAction.setEntryValues(actionValuesGo);

            mNavigationHomeDoubleTapAction.setEntries(actionEntriesGo);
            mNavigationHomeDoubleTapAction.setEntryValues(actionValuesGo);

            mNavigationAppSwitchLongPressAction.setEntries(actionEntriesGo);
            mNavigationAppSwitchLongPressAction.setEntryValues(actionValuesGo);

            mEdgeLongSwipeAction.setEntries(actionEntriesGo);
            mEdgeLongSwipeAction.setEntryValues(actionValuesGo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = LineageSettings.Secure.getInt(getContentResolver(),
                    LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }
    }

    private ListPreference initList(String key, Action value) {
        return initList(key, value.ordinal());
    }

    private ListPreference initList(String key, int value) {
        ListPreference list = getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        LineageSettings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleSystemListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomeLongPressAction ||
                preference == mNavigationHomeLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mHomeDoubleTapAction ||
                preference == mNavigationHomeDoubleTapAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mMenuPressAction) {
            handleListChange(mMenuPressAction, newValue,
                    LineageSettings.System.KEY_MENU_ACTION);
            return true;
        } else if (preference == mMenuLongPressAction) {
            handleListChange(mMenuLongPressAction, newValue,
                    LineageSettings.System.KEY_MENU_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAssistPressAction) {
            handleListChange(mAssistPressAction, newValue,
                    LineageSettings.System.KEY_ASSIST_ACTION);
            return true;
        } else if (preference == mAssistLongPressAction) {
            handleListChange(mAssistLongPressAction, newValue,
                    LineageSettings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            handleListChange(mAppSwitchPressAction, newValue,
                    LineageSettings.System.KEY_APP_SWITCH_ACTION);
            return true;
        } else if (preference == mAppSwitchLongPressAction ||
                preference == mNavigationAppSwitchLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mVolumeKeyCursorControl) {
            handleSystemListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mTorchLongPressPowerTimeout) {
            handleListChange(mTorchLongPressPowerTimeout, newValue,
                    LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT);
            return true;
        } else if (preference == mEdgeLongSwipeAction) {
            handleListChange(mEdgeLongSwipeAction, newValue,
                    LineageSettings.System.KEY_EDGE_LONG_SWIPE_ACTION);
            return true;
        }
        return false;
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        LineageSettings.System.putIntForUser(context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void updateDisableNavkeysOption() {
        boolean enabled = LineageSettings.System.getIntForUser(getActivity().getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;

        mDisableNavigationKeys.setChecked(enabled);
    }

    private void updateDisableNavkeysCategories(boolean navbarEnabled, boolean force) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Disable hw-key options if they're disabled */
        final PreferenceCategory homeCategory =
                prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory =
                prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory =
                prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!navbarEnabled);
            backlight.updateSummary();
        }

        /* Toggle hardkey control availability depending on navbar state */
        if (mNavigationPreferencesCat != null) {
            if (force || navbarEnabled) {
                if (DeviceUtils.isEdgeToEdgeEnabled(getContext())) {
                    mNavigationPreferencesCat.addPreference(mEdgeLongSwipeAction);

                    mNavigationPreferencesCat.removePreference(mNavigationArrowKeys);
                    mNavigationPreferencesCat.removePreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.removePreference(mNavigationHomeDoubleTapAction);
                    mNavigationPreferencesCat.removePreference(mNavigationAppSwitchLongPressAction);
                } else if (DeviceUtils.isSwipeUpEnabled(getContext())) {
                    mNavigationPreferencesCat.addPreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeDoubleTapAction);

                    mNavigationPreferencesCat.removePreference(mNavigationAppSwitchLongPressAction);
                    mNavigationPreferencesCat.removePreference(mEdgeLongSwipeAction);
                } else {
                    mNavigationPreferencesCat.addPreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeDoubleTapAction);
                    mNavigationPreferencesCat.addPreference(mNavigationAppSwitchLongPressAction);

                    mNavigationPreferencesCat.removePreference(mEdgeLongSwipeAction);
                }
            }
        }
        if (homeCategory != null) {
            if (mHomeAnswerCall != null) {
                mHomeAnswerCall.setEnabled(!navbarEnabled);
            }
            if (mHomeLongPressAction != null) {
                mHomeLongPressAction.setEnabled(!navbarEnabled);
            }
            if (mHomeDoubleTapAction != null) {
                mHomeDoubleTapAction.setEnabled(!navbarEnabled);
            }
        }
        if (backCategory != null) {
            backCategory.setEnabled(!navbarEnabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!navbarEnabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!navbarEnabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!navbarEnabled);
        }
    }

    private static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar(Display.DEFAULT_DISPLAY);
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        return hasNavigationBar;
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    public static void restoreKeyDisabler(Context context) {
        if (!isKeyDisablerSupported(context)) {
            return;
        }

        boolean enabled = LineageSettings.System.getIntForUser(context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;

        writeDisableNavkeysOption(context, enabled);
    }


    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value;

            if (mSwapVolumeButtons.isChecked()) {
                /* The native inputflinger service uses the same logic of:
                 *   1 - the volume rocker is on one the sides, relative to the natural
                 *       orientation of the display (true for all phones and most tablets)
                 *   2 - the volume rocker is on the top or bottom, relative to the
                 *       natural orientation of the display (true for some tablets)
                 */
                value = getResources().getInteger(
                        R.integer.config_volumeRockerVsDisplayOrientation);
            } else {
                /* Disable the re-orient functionality */
                value = 0;
            }
            LineageSettings.System.putInt(getActivity().getContentResolver(),
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mVolumePanelOnLeft) {
            LineageSettings.Secure.putIntForUser(getActivity().getContentResolver(),
                    LineageSettings.Secure.VOLUME_PANEL_ON_LEFT,
                    mVolumePanelOnLeft.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mDisableNavigationKeys) {
            mDisableNavigationKeys.setEnabled(false);
            mNavigationPreferencesCat.setEnabled(false);
            writeDisableNavkeysOption(getActivity(), mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption();
            updateDisableNavkeysCategories(true, false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDisableNavigationKeys.setEnabled(true);
                    mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
                    updateDisableNavkeysCategories(mDisableNavigationKeys.isChecked(), false);
                }
            }, 1000);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        LineageSettings.Secure.putInt(getContentResolver(),
                LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<String>();

            if (!TelephonyUtils.isVoiceCapable(context)) {
                result.add(KEY_POWER_END_CALL);
                result.add(KEY_HOME_ANSWER_CALL);
                result.add(KEY_VOLUME_ANSWER_CALL);
            }

            if (!DeviceUtils.hasBackKey(context)) {
                result.add(CATEGORY_BACK);
                result.add(KEY_BACK_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingHomeKey(context)) {
                result.add(KEY_BACK_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasHomeKey(context)) {
                result.add(CATEGORY_HOME);
                result.add(KEY_HOME_LONG_PRESS);
                result.add(KEY_HOME_DOUBLE_TAP);
                result.add(KEY_HOME_ANSWER_CALL);
                result.add(KEY_HOME_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingHomeKey(context)) {
                result.add(KEY_HOME_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasMenuKey(context)) {
                result.add(CATEGORY_MENU);
                result.add(KEY_MENU_PRESS);
                result.add(KEY_MENU_LONG_PRESS);
                result.add(KEY_MENU_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingMenuKey(context)) {
                result.add(KEY_MENU_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasAssistKey(context)) {
                result.add(CATEGORY_ASSIST);
                result.add(KEY_ASSIST_PRESS);
                result.add(KEY_ASSIST_LONG_PRESS);
                result.add(KEY_ASSIST_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingAssistKey(context)) {
                result.add(KEY_ASSIST_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasAppSwitchKey(context)) {
                result.add(CATEGORY_APPSWITCH);
                result.add(KEY_APP_SWITCH_PRESS);
                result.add(KEY_APP_SWITCH_LONG_PRESS);
                result.add(KEY_APP_SWITCH_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingAppSwitchKey(context)) {
                result.add(KEY_APP_SWITCH_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasCameraKey(context)) {
                result.add(CATEGORY_CAMERA);
                result.add(KEY_CAMERA_LAUNCH);
                result.add(KEY_CAMERA_SLEEP_ON_RELEASE);
                result.add(KEY_CAMERA_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingCameraKey(context)) {
                result.add(KEY_CAMERA_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasVolumeKeys(context)) {
                result.add(CATEGORY_VOLUME);
                result.add(KEY_SWAP_VOLUME_BUTTONS);
                result.add(KEY_VOLUME_ANSWER_CALL);
                result.add(KEY_VOLUME_KEY_CURSOR_CONTROL);
                result.add(KEY_VOLUME_MUSIC_CONTROLS);
                result.add(KEY_VOLUME_PANEL_ON_LEFT);
                result.add(KEY_VOLUME_WAKE_SCREEN);
                result.add(KEY_CLICK_PARTIAL_SCREENSHOT);
            } else if (!DeviceUtils.canWakeUsingVolumeKeys(context)) {
                result.add(KEY_VOLUME_WAKE_SCREEN);
            }

            if (!DeviceUtils.deviceSupportsFlashLight(context)) {
                result.add(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
                result.add(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT);
            }

            if (!isKeyDisablerSupported(context)) {
                result.add(KEY_DISABLE_NAV_KEYS);
            }

            if (!ButtonBacklightBrightness.isButtonSupported(context)
                    && !ButtonBacklightBrightness.isKeyboardSupported(context)) {
                result.add(KEY_BUTTON_BACKLIGHT);
            }

            if (hasNavigationBar()) {
                if (DeviceUtils.isEdgeToEdgeEnabled(context)) {
                    result.add(KEY_NAVIGATION_ARROW_KEYS);
                    result.add(KEY_NAVIGATION_HOME_LONG_PRESS);
                    result.add(KEY_NAVIGATION_HOME_DOUBLE_TAP);
                    result.add(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS);
                } else if (DeviceUtils.isSwipeUpEnabled(context)) {
                    result.add(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS);
                    result.add(KEY_EDGE_LONG_SWIPE);
                } else {
                    result.add(KEY_EDGE_LONG_SWIPE);
                }
            }
            return result;
        }
    };
}
