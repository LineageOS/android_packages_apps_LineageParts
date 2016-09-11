/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.cyanogenmod.cmparts;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.cyanogenmod.cmparts.profiles.NFCProfileTagCallback;
import org.cyanogenmod.internal.cmparts.IPartsCatalog;
import org.cyanogenmod.internal.cmparts.PartInfo;

public class PartsActivity extends Activity {

    private static final String TAG = "PartsActivity";

    // Parts mode
    public static final String ACTION_PART = "org.cyanogenmod.cmparts.PART";
    public static final String EXTRA_PART = "part";

    // Settings compatibility
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";

    private IPartsCatalog mCatalog;

    private NFCProfileTagCallback mNfcProfileCallback;

    private ActionBar mActionBar;
    private SwitchBar mSwitchBar;
    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        connectCatalog();

        setContentView(R.layout.cmparts);

        PartInfo info = null;
        String action = getIntent().getAction();
        String partExtra = getIntent().getStringExtra(EXTRA_PART);
        String fragmentClass = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        String component = getIntent().getComponent().getClassName();
        Bundle initialArgs = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        Log.d(TAG, "Launched with: " + getIntent().toString() + " action: " +
                getIntent().getAction() + " component: " + component +
                " part: " + partExtra + " fragment: " + fragmentClass);

        if (!ACTION_PART.equals(action) && component == null) {
            throw new UnsupportedOperationException("Unknown action: " + getIntent().getAction());
        }

        if (fragmentClass == null) {
            if (partExtra != null) {
                // Parts mode
                info = PartsCatalog.getPartInfo(getResources(), partExtra);
            } else {
                // Alias mode
                info = PartsCatalog.getPartInfoForClass(getResources(),
                        getIntent().getComponent().getClassName());
            }
            if (info == null) {
                throw new UnsupportedOperationException("Unable to get part info: " + getIntent().toString());
            }
            fragmentClass = info.getFragmentClass();
        }

        if (fragmentClass == null) {
            throw new UnsupportedOperationException("Unable to get fragment class: " + getIntent().toString());
        }

        setTitleFromIntent(getIntent(), info);

        switchToFragment(fragmentClass, initialArgs, mInitialTitleResId, mInitialTitle);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectCatalog();
    }

    private void connectCatalog() {
        Intent i = new Intent(this, PartsCatalog.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void disconnectCatalog() {
        unbindService(mConnection);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCatalog = IPartsCatalog.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCatalog = null;
        }
    };

    public void setNfcProfileCallback(NFCProfileTagCallback callback) {
        mNfcProfileCallback = callback;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (mNfcProfileCallback != null) {
                mNfcProfileCallback.onTagRead(detectedTag);
            }
            return;
        }
        super.onNewIntent(intent);
    }

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
                                     CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                // There not much we can do in that case
                title = "";
            }
        }

        Intent intent = new Intent(ACTION_PART);
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentClass);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, titleRes);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleText);

        if (resultTo == null) {
            startActivity(intent);
        } else {
            startActivityForResult(intent, resultRequestCode);
        }
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    public void switchToFragment(String fragmentClass, Bundle args, int titleRes, CharSequence titleText) {
        Log.d(TAG, "Launching fragment: " + fragmentClass);

        Fragment fragment = Fragment.instantiate(this, fragmentClass);
        if (fragment == null) {
            Log.e(TAG, "Invalid fragment! " + fragmentClass);
            return;
        }
        fragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (titleRes > 0) {
            transaction.setBreadCrumbTitle(titleRes);
        } else if (titleText != null) {
            transaction.setBreadCrumbTitle(titleText);
        }

        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();

        refreshBars();
    }


    public Button getBackButton() {
        return (Button) findViewById(R.id.back_button);
    }

    public Button getNextButton() {
        return (Button) findViewById(R.id.next_button);
    }

    public void showButtonBar(boolean show) {
        findViewById(R.id.button_bar).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void refreshBars() {
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        }
    }

    private void setTitleFromPart(PartInfo part) {
        mInitialTitleResId = 0;
        mInitialTitle = part.getTitle();
        setTitle(mInitialTitle);
    }

    private void setTitleFromIntent(Intent intent, PartInfo part) {
        if (part != null) {
            mInitialTitleResId = -1;
            mInitialTitle = part.getTitle();
            setTitle(mInitialTitle);
        } else {
            final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
            if (initialTitleResId > 0) {
                mInitialTitle = null;
                mInitialTitleResId = initialTitleResId;
                setTitle(mInitialTitleResId);
            } else {
                mInitialTitleResId = -1;
                final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
                mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
                setTitle(mInitialTitle);
            }
        }
    }
}

