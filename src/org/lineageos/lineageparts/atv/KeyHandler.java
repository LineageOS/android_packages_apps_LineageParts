/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.atv;

import android.app.DreamManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.android.internal.os.DeviceKeyHandler;

import lineageos.providers.LineageSettings;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = KeyHandler.class.getSimpleName();
    private static Map<Integer, String> KEYMAP;

    // User actions, see LineageSettings.System.ATV_SHORTCUT_KEY_ACTIONS
    private static final String ACTION_NONE = "none";
    private static final String ACTION_KEY = "key:";
    private static final String ACTION_APP = "app:";

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRepeatSentKey = this::repeatSentKey;

    // The key sent in place of a held shortcut key, see sendKey. Only used on mHandler.
    private int mShortcutKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private int mSentKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private int mSentRepeatCount;
    private long mSentDownTime;
    private boolean mSentToMediaSession;

    public KeyHandler(Context context) {
        mContext = context;

        int[] keycodes = mContext.getResources().getIntArray(
                org.lineageos.platform.internal.R.array.keyhandler_keycodes);
        String[] packages = mContext.getResources().getStringArray(
                org.lineageos.platform.internal.R.array.keyhandler_packages);

        KEYMAP = IntStream.range(0, keycodes.length).boxed()
                .collect(Collectors.toMap(i -> keycodes[i], i -> packages[i]));
    }

    public KeyEvent handleKeyEvent(KeyEvent event) {
        String targetName = KEYMAP.get(event.getKeyCode());
        if (targetName == null || !hasSetupCompleted()) {
            return event;
        }

        // Keys sent in place of another shortcut key keep their default action, so they can't loop
        String action = isSentByUs(event) ? null : getUserAction(event.getKeyCode());
        if (ACTION_NONE.equals(action)) {
            return null;
        } else if (action != null && action.startsWith(ACTION_KEY)) {
            sendKey(event, Integer.parseInt(action.substring(ACTION_KEY.length())));
            return null;
        } else if (action != null && action.startsWith(ACTION_APP)) {
            targetName = action.substring(ACTION_APP.length());
        }

        // Consume DOWN as well as UP, so apps never see a DOWN without its UP.
        // Keys without a target are consumed too: some (e.g. BUTTON_3/4/5)
        // have a DPAD_CENTER fallback and would otherwise act as OK.
        if (event.getAction() == KeyEvent.ACTION_UP) {
            Intent launchIntent = resolveTarget(targetName);
            if (launchIntent != null) {
                wakeUpIfAsleep();
                mContext.startActivityAsUser(launchIntent, UserHandle.CURRENT);
            }
        }
        return null;
    }

    /**
     * Returns the action set by the user for keyCode, or null for its default action. See
     * {@link LineageSettings.System#ATV_SHORTCUT_KEY_ACTIONS}.
     */
    private String getUserAction(int keyCode) {
        String actions = LineageSettings.System.getStringForUser(mContext.getContentResolver(),
                LineageSettings.System.ATV_SHORTCUT_KEY_ACTIONS, UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(actions)) {
            return null;
        }
        String prefix = keyCode + ":";
        for (String entry : actions.split(",")) {
            if (entry.startsWith(prefix)) {
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    private static boolean isSentByUs(KeyEvent event) {
        return event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD
                && (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0;
    }

    /**
     * Sends keyCode in place of event, from the main thread as this runs in input dispatch.
     *
     * Media keys are sent right away, and only fast forward and rewind repeat while the shortcut
     * key is held, as injected keys never do. While the screen is off or the screensaver runs,
     * they go straight to the media session, so pausing music doesn't light the screen up.
     *
     * Other keys repeat while held, for long presses, and wake the device up first. A tap is only
     * sent on release, so the shortcut key's own UP can't reach the policy in the middle of it,
     * which resets its gesture detector and drops e.g. ASSIST.
     */
    private void sendKey(KeyEvent event, int keyCode) {
        final int shortcutKeyCode = event.getKeyCode();
        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
        mHandler.post(() -> {
            if (down) {
                stopSentKey(false);
                mShortcutKeyCode = shortcutKeyCode;
                mSentKeyCode = keyCode;
                mSentRepeatCount = -1;
                if (KeyEvent.isMediaSessionKey(keyCode)
                        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                    mSentToMediaSession = KeyEvent.isMediaSessionKey(keyCode)
                            && (isScreenDark() || isHandledByMediaSession(keyCode));
                    mSentDownTime = SystemClock.uptimeMillis();
                    mSentRepeatCount = 0;
                    injectSentKey(KeyEvent.ACTION_DOWN, 0, 0, mSentDownTime);
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                            || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                        mHandler.postDelayed(mRepeatSentKey,
                                ViewConfiguration.getKeyRepeatTimeout());
                    }
                } else {
                    mSentToMediaSession = false;
                    wakeUpIfAsleep();
                    mHandler.postDelayed(mRepeatSentKey, ViewConfiguration.getKeyRepeatTimeout());
                }
            } else if (shortcutKeyCode == mShortcutKeyCode) {
                stopSentKey(canceled);
            }
        });
    }

    private void repeatSentKey() {
        long now = SystemClock.uptimeMillis();
        if (++mSentRepeatCount == 0) {
            mSentDownTime = now;
        }
        // The policy checks for long presses before the dispatcher would set the flag
        injectSentKey(KeyEvent.ACTION_DOWN, mSentRepeatCount,
                mSentRepeatCount == 1 ? KeyEvent.FLAG_LONG_PRESS : 0, now);
        mHandler.postDelayed(mRepeatSentKey, ViewConfiguration.getKeyRepeatDelay());
    }

    private void stopSentKey(boolean canceled) {
        if (mSentKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return;
        }
        mHandler.removeCallbacks(mRepeatSentKey);
        long now = SystemClock.uptimeMillis();
        if (mSentRepeatCount >= 0) {
            injectSentKey(KeyEvent.ACTION_UP, 0, canceled ? KeyEvent.FLAG_CANCELED : 0, now);
        } else if (!canceled) {
            mSentDownTime = now;
            injectSentKey(KeyEvent.ACTION_DOWN, 0, 0, now);
            injectSentKey(KeyEvent.ACTION_UP, 0, 0, now);
        }
        mShortcutKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        mSentKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    }

    private void injectSentKey(int action, int repeatCount, int flags, long eventTime) {
        KeyEvent key = new KeyEvent(mSentDownTime, eventTime, action, mSentKeyCode, repeatCount,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM | flags,
                InputDevice.SOURCE_KEYBOARD);
        if (mSentToMediaSession) {
            mContext.getSystemService(AudioManager.class).dispatchMediaKeyEvent(key);
        } else {
            mContext.getSystemService(InputManager.class).injectInputEvent(key,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    /**
     * Whether the playing media session supports the action of keyCode, so it can get the key
     * without the app's window seeing it, e.g. to keep a player's dimmed screen dark.
     */
    private boolean isHandledByMediaSession(int keyCode) {
        List<MediaController> sessions = mContext.getSystemService(MediaSessionManager.class)
                .getActiveSessions(null);
        PlaybackState state = sessions.isEmpty() ? null : sessions.get(0).getPlaybackState();
        if (state == null) {
            return false;
        }
        long actions = switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> PlaybackState.ACTION_PLAY_PAUSE
                    | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE;
            case KeyEvent.KEYCODE_MEDIA_PLAY -> PlaybackState.ACTION_PLAY;
            case KeyEvent.KEYCODE_MEDIA_PAUSE -> PlaybackState.ACTION_PAUSE;
            case KeyEvent.KEYCODE_MEDIA_STOP -> PlaybackState.ACTION_STOP;
            case KeyEvent.KEYCODE_MEDIA_NEXT -> PlaybackState.ACTION_SKIP_TO_NEXT;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS -> PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> PlaybackState.ACTION_FAST_FORWARD;
            case KeyEvent.KEYCODE_MEDIA_REWIND -> PlaybackState.ACTION_REWIND;
            default -> 0;
        };
        return (state.getActions() & actions) != 0;
    }

    private boolean isScreenDark() {
        DreamManager dm = mContext.getSystemService(DreamManager.class);
        return !mContext.getSystemService(PowerManager.class).isInteractive()
                || (dm != null && dm.isDreaming());
    }

    private void wakeUpIfAsleep() {
        PowerManager pm = mContext.getSystemService(PowerManager.class);
        if (!pm.isInteractive()) {
            pm.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_WAKE_KEY,
                    TAG + ":SHORTCUT_KEY");
        }
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TV_USER_SETUP_COMPLETE, 0) != 0;
    }

    private Intent resolveTarget(String targetName) {
        PackageManager pm = mContext.getPackageManager();

        ComponentName component = ComponentName.unflattenFromString(targetName);
        Intent launchIntent = null;

        // Explicit components avoid package visibility and unusual intent-filter issues
        if (component != null) {
            Intent componentIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setComponent(component)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Intent#resolveActivity returns explicit components as is, so ask
            // PackageManager whether the activity actually exists
            if (pm.resolveActivity(componentIntent, 0) != null) {
                launchIntent = componentIntent;
            }
        }

        if (launchIntent == null) {
            // Fall back to the component's package, or look the name up as one
            String packageName = component != null ? component.getPackageName() : targetName;
            launchIntent = pm.getLaunchIntentForPackage(packageName);

            // TV-only apps may expose a leanback launcher activity without a regular one
            if (launchIntent == null) {
                launchIntent = pm.getLeanbackLaunchIntentForPackage(packageName);
            }
        }

        // If it isn't an installed package or component, try as an intent
        if (launchIntent == null) {
            launchIntent = new Intent(targetName);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.putExtra("no_input_mode", true);

            if (launchIntent.resolveActivity(pm) == null) {
                launchIntent = null;
            }
        }

        if (launchIntent == null) {
            Log.w(TAG, "Cannot launch " + targetName + ": package/intent not found.");
        }
        return launchIntent;
    }
}
