/*
* Copyright (C) 2015 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.lineageos.lineageparts.widget;

import android.content.Context;
import android.net.Uri;
import android.widget.Switch;

import lineageos.preference.SettingsHelper;
import lineageos.providers.LineageSettings;

public class LineageBaseSystemSettingSwitchBar implements SwitchBar.OnSwitchChangeListener,
        SettingsHelper.OnSettingsChangeListener {

    private Context mContext;
    private SwitchBar mSwitchBar;
    private boolean mListeningToOnSwitchChange = false;

    private boolean mStateMachineEvent;

    private final String mSettingKey;
    private final int mDefaultState;

    private final SwitchBarChangeCallback mCallback;
    public interface SwitchBarChangeCallback {
        public void onEnablerChanged(boolean isEnabled);
    }

    public LineageBaseSystemSettingSwitchBar(Context context, SwitchBar switchBar, String key,
                                      boolean defaultState, SwitchBarChangeCallback callback) {
        mContext = context;
        mSwitchBar = switchBar;
        mSettingKey = key;
        mDefaultState = defaultState ? 1 : 0;
        mCallback = callback;
        setupSwitchBar();
    }

    public void setupSwitchBar() {
        setSwitchState();
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = true;
        }
        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = false;
        }
        mSwitchBar.hide();
    }

    public void resume(Context context) {
        mContext = context;
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            SettingsHelper.get(mContext).startWatching(
                    this, LineageSettings.System.getUriFor(mSettingKey));

            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            SettingsHelper.get(mContext).stopWatching(this);

            mListeningToOnSwitchChange = false;
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchBar.setChecked(checked);
        mStateMachineEvent = false;
        if (mCallback != null) {
            mCallback.onEnablerChanged(checked);
        }
    }

    private void setSwitchState() {
        boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                mSettingKey, mDefaultState) == 1;
        mStateMachineEvent = true;
        setSwitchBarChecked(enabled);
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        // Handle a switch change
        LineageSettings.System.putInt(mContext.getContentResolver(),
                mSettingKey, isChecked ? 1 : 0);

        if (mCallback != null) {
            mCallback.onEnablerChanged(isChecked);
        }
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        setSwitchState();
    }
}
