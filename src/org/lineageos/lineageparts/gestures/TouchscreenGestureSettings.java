/**
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2019 The LineageOS Project
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

package org.lineageos.lineageparts.gestures;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.utils.ResourceUtils;

import vendor.lineage.touch.V1_0.Gesture;
import vendor.lineage.touch.V1_0.ITouchscreenGesture;

import java.lang.System;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class TouchscreenGestureSettings extends SettingsPreferenceFragment {
    private static final String KEY_TOUCHSCREEN_GESTURE = "touchscreen_gesture";
    private static final String TOUCHSCREEN_GESTURE_TITLE = KEY_TOUCHSCREEN_GESTURE + "_%s_title";

    private ArrayList<Gesture> mGestures;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.touchscreen_gesture_settings);

        initTouchscreenGestures();
    }

    private void initTouchscreenGestures() {
        try {
            ITouchscreenGesture touchscreenGesture =
                    ITouchscreenGesture.getService(true /* retry */);
            mGestures = touchscreenGesture.getSupportedGestures();
        } catch (NoSuchElementException | RemoteException e) {
            return;
        }
        final int[] actions = getDefaultGestureActions(getContext(), mGestures);
        for (final Gesture gesture : mGestures) {
            getPreferenceScreen().addPreference(new TouchscreenGesturePreference(
                    getContext(), gesture, actions[gesture.id]));
        }
    }

    private class TouchscreenGesturePreference extends ListPreference {
        private final Context mContext;
        private final Gesture mGesture;

        public TouchscreenGesturePreference(final Context context,
                                            final Gesture gesture,
                                            final int defaultAction) {
            super(context);
            mContext = context;
            mGesture = gesture;

            setKey(buildPreferenceKey(gesture));
            setEntries(R.array.touchscreen_gesture_action_entries);
            setEntryValues(R.array.touchscreen_gesture_action_values);
            setDefaultValue(String.valueOf(defaultAction));
            setIcon(getIconDrawableResourceForAction(defaultAction));

            setSummary("%s");
            setDialogTitle(R.string.touchscreen_gesture_action_dialog_title);
            setTitle(ResourceUtils.getLocalizedString(
                    context.getResources(), gesture.name, TOUCHSCREEN_GESTURE_TITLE));
        }

        @Override
        public boolean callChangeListener(final Object newValue) {
            final int action = Integer.parseInt(String.valueOf(newValue));
            try {
                ITouchscreenGesture touchscreenGesture =
                        ITouchscreenGesture.getService(true /* retry */);
                touchscreenGesture.setGestureEnabled(mGesture, action > 0);
            } catch (NoSuchElementException | RemoteException e) {
                return false;
            }
            return super.callChangeListener(newValue);
        }

        @Override
        protected boolean persistString(String value) {
            if (!super.persistString(value)) {
                return false;
            }
            final int action = Integer.parseInt(String.valueOf(value));
            setIcon(getIconDrawableResourceForAction(action));
            sendUpdateBroadcast(mContext, mGestures);
            return true;
        }

        private int getIconDrawableResourceForAction(final int action) {
            switch (action) {
                case TouchscreenGestureConstants.ACTION_CAMERA:
                    return R.drawable.ic_gesture_action_camera;
                case TouchscreenGestureConstants.ACTION_FLASHLIGHT:
                    return R.drawable.ic_gesture_action_flashlight;
                case TouchscreenGestureConstants.ACTION_BROWSER:
                    return R.drawable.ic_gesture_action_browser;
                case TouchscreenGestureConstants.ACTION_DIALER:
                    return R.drawable.ic_gesture_action_dialer;
                case TouchscreenGestureConstants.ACTION_EMAIL:
                    return R.drawable.ic_gesture_action_email;
                case TouchscreenGestureConstants.ACTION_MESSAGES:
                    return R.drawable.ic_gesture_action_messages;
                case TouchscreenGestureConstants.ACTION_PLAY_PAUSE_MUSIC:
                    return R.drawable.ic_gesture_action_play_pause;
                case TouchscreenGestureConstants.ACTION_PREVIOUS_TRACK:
                    return R.drawable.ic_gesture_action_previous_track;
                case TouchscreenGestureConstants.ACTION_NEXT_TRACK:
                    return R.drawable.ic_gesture_action_next_track;
                case TouchscreenGestureConstants.ACTION_VOLUME_DOWN:
                    return R.drawable.ic_gesture_action_volume_down;
                case TouchscreenGestureConstants.ACTION_VOLUME_UP:
                    return R.drawable.ic_gesture_action_volume_up;
                default:
                    // No gesture action
                    return R.drawable.ic_gesture_action_none;
            }
        }
    }

    public static void restoreTouchscreenGestureStates(final Context context) {
        ITouchscreenGesture touchscreenGesture;
        final ArrayList<Gesture> gestures;
        try {
            touchscreenGesture = ITouchscreenGesture.getService(true /* retry */);
            gestures = touchscreenGesture.getSupportedGestures();
        } catch (NoSuchElementException | RemoteException e) {
            return;
        }

        final int[] actionList = buildActionList(context, gestures);
        for (final Gesture gesture : gestures) {
            try {
                touchscreenGesture.setGestureEnabled(gesture, actionList[gesture.id] > 0);
            } catch (RemoteException e) {
            }
        }

        sendUpdateBroadcast(context, gestures);
    }

    private static int[] getDefaultGestureActions(final Context context,
            final ArrayList<Gesture> gestures) {
        final int[] defaultActions = context.getResources().getIntArray(
                R.array.config_defaultTouchscreenGestureActions);
        if (defaultActions.length >= gestures.size()) {
            return defaultActions;
        }

        final int[] filledDefaultActions = new int[gestures.size()];
        System.arraycopy(defaultActions, 0, filledDefaultActions, 0, defaultActions.length);
        return filledDefaultActions;
    }

    private static int[] buildActionList(final Context context,
            final ArrayList<Gesture> gestures) {
        final int[] result = new int[gestures.size()];
        final int[] defaultActions = getDefaultGestureActions(context, gestures);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (final Gesture gesture : gestures) {
            final String key = buildPreferenceKey(gesture);
            final String defaultValue = String.valueOf(defaultActions[gesture.id]);
            result[gesture.id] = Integer.parseInt(prefs.getString(key, defaultValue));
        }
        return result;
    }

    private static String buildPreferenceKey(final Gesture gesture) {
        return "touchscreen_gesture_" + gesture.id;
    }

    private static void sendUpdateBroadcast(final Context context,
            final ArrayList<Gesture> gestures) {
        final Intent intent = new Intent(TouchscreenGestureConstants.UPDATE_PREFS_ACTION);
        final int[] keycodes = new int[gestures.size()];
        final int[] actions = buildActionList(context, gestures);
        for (final Gesture gesture : gestures) {
            keycodes[gesture.id] = gesture.keycode;
        }
        intent.putExtra(TouchscreenGestureConstants.UPDATE_EXTRA_KEYCODE_MAPPING, keycodes);
        intent.putExtra(TouchscreenGestureConstants.UPDATE_EXTRA_ACTION_MAPPING, actions);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}
