/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.lineageparts;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.animation.*;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.lineageparts.widget.CustomDialogPreference;
import org.lineageos.lineageparts.widget.DialogCreatable;
import org.lineageos.lineageparts.widget.FloatingActionButton;
import org.lineageos.lineageparts.widget.HighlightablePreferenceGroupAdapter;
import org.lineageos.lineageparts.widget.LayoutPreference;

import java.util.Arrays;
import java.util.UUID;

import lineageos.preference.SettingsHelper;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public abstract class SettingsPreferenceFragment extends PreferenceFragment
        implements DialogCreatable, PartsUpdater.Refreshable {

    /**
     * The Help Uri Resource key. This can be passed as an extra argument when creating the
     * Fragment.
     **/
    public static final String HELP_URI_RESOURCE_KEY = "help_uri_resource";

    private static final String TAG = "SettingsPreference";

    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    private SettingsDialogFragment mDialogFragment;

    private String mHelpUri;

    private static final int ORDER_FIRST = -1;
    private static final int ORDER_LAST = Integer.MAX_VALUE -1;

    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;

    private RecyclerView.Adapter mCurrentRootAdapter;
    private boolean mIsDataSetObserverRegistered = false;
    private RecyclerView.AdapterDataObserver mDataSetObserver =
            new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            onDataSetChanged();
        }
    };

    private ViewGroup mPinnedHeaderFrameLayout;
    private FloatingActionButton mFloatingActionButton;
    private ViewGroup mButtonBar;

    private LayoutPreference mHeader;

    private LayoutPreference mFooter;
    private View mEmptyView;
    private LinearLayoutManager mLayoutManager;
    private HighlightablePreferenceGroupAdapter mAdapter;
    private ArrayMap<String, Preference> mPreferenceCache;
    private boolean mAnimationAllowed;

    private final ArraySet<Uri> mTriggerUris = new ArraySet<Uri>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        mPinnedHeaderFrameLayout = (ViewGroup) root.findViewById(R.id.pinned_header);
        mFloatingActionButton = (FloatingActionButton) root.findViewById(R.id.fab);
        mButtonBar = (ViewGroup) root.findViewById(R.id.button_bar);
        return root;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    public FloatingActionButton getFloatingActionButton() {
        return mFloatingActionButton;
    }

    public ViewGroup getButtonBar() {
        return mButtonBar;
    }

    public View setPinnedHeaderView(int layoutResId) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View pinnedHeader =
                inflater.inflate(layoutResId, mPinnedHeaderFrameLayout, false);
        setPinnedHeaderView(pinnedHeader);
        return pinnedHeader;
    }

    public void setPinnedHeaderView(View pinnedHeader) {
        mPinnedHeaderFrameLayout.addView(pinnedHeader);
        mPinnedHeaderFrameLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle args = getArguments();
        if (args != null) {
            mPreferenceKey = args.getString(PartsActivity.EXTRA_FRAGMENT_ARG_KEY);
            highlightPreferenceIfNeeded();
        }
    }

    @Override
    protected void onBindPreferences() {
        registerObserverIfNeeded();
    }

    @Override
    protected void onUnbindPreferences() {
        unregisterObserverIfNeeded();
    }

    @Override
    public void onSettingsChanged(Uri contentUri) {
        PartsUpdater.notifyChanged(getActivity(), getPreferenceScreen().getKey());
    }

    public void showLoadingWhenEmpty() {
        View loading = getView().findViewById(R.id.loading_container);
        setEmptyView(loading);
    }

    public void handleLoadingContainer(View loading, View doneLoading, boolean done,
                                              boolean animate) {
        setViewShown(loading, !done, animate);
        setViewShown(doneLoading, done, animate);
    }

    private void setViewShown(final View view, boolean shown, boolean animate) {
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(),
                    shown ? android.R.anim.fade_in : android.R.anim.fade_out);
            if (shown) {
                view.setVisibility(View.VISIBLE);
            } else {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.INVISIBLE);
                    }
                });
            }
            view.startAnimation(animation);
        } else {
            view.clearAnimation();
            view.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setLoading(boolean loading, boolean animate) {
        View loading_container = getView().findViewById(R.id.loading_container);
        handleLoadingContainer(loading_container, getListView(), !loading, animate);
    }

    public void registerObserverIfNeeded() {
        if (!mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
            }
            mCurrentRootAdapter = getListView().getAdapter();
            mCurrentRootAdapter.registerAdapterDataObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = true;
            onDataSetChanged();
        }
    }

    public void unregisterObserverIfNeeded() {
        if (mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
                mCurrentRootAdapter = null;
            }
            mIsDataSetObserverRegistered = false;
        }
    }

    public void highlightPreferenceIfNeeded() {
        if (!isAdded()) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.requestHighlight(getView(), getListView());
        }
    }

    protected void onDataSetChanged() {
        highlightPreferenceIfNeeded();
        updateEmptyView();
    }

    public LayoutPreference getHeaderView() {
        return mHeader;
    }

    public LayoutPreference getFooterView() {
        return mFooter;
    }

    protected void setHeaderView(int resource) {
        mHeader = new LayoutPreference(getPrefContext(), resource);
        addPreferenceToTop(mHeader);
    }

    protected void setHeaderView(View view) {
        mHeader = new LayoutPreference(getPrefContext(), view);
        addPreferenceToTop(mHeader);
    }

    private void addPreferenceToTop(LayoutPreference preference) {
        preference.setOrder(ORDER_FIRST);
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().addPreference(preference);
        }
    }

    protected void setFooterView(int resource) {
        setFooterView(resource != 0 ? new LayoutPreference(getPrefContext(), resource) : null);
    }

    protected void setFooterView(View v) {
        setFooterView(v != null ? new LayoutPreference(getPrefContext(), v) : null);
    }

    private void setFooterView(LayoutPreference footer) {
        if (getPreferenceScreen() != null && mFooter != null) {
            getPreferenceScreen().removePreference(mFooter);
        }
        if (footer != null) {
            mFooter = footer;
            mFooter.setOrder(ORDER_LAST);
            if (getPreferenceScreen() != null) {
                getPreferenceScreen().addPreference(mFooter);
            }
        } else {
            mFooter = null;
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null && !preferenceScreen.isAttached()) {
            // Without ids generated, the RecyclerView won't animate changes to the preferences.
            preferenceScreen.setShouldUseGeneratedIds(mAnimationAllowed);
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            if (mHeader != null) {
                preferenceScreen.addPreference(mHeader);
            }
            if (mFooter != null) {
                preferenceScreen.addPreference(mFooter);
            }
        }
    }

    private void updateEmptyView() {
        if (mEmptyView == null) return;
        if (getPreferenceScreen() != null) {
            boolean show = (getPreferenceScreen().getPreferenceCount()
                    - (mHeader != null ? 1 : 0)
                    - (mFooter != null ? 1 : 0)) <= 0;
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public void setEmptyView(View v) {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        mEmptyView = v;
        updateEmptyView();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        mLayoutManager = new LinearLayoutManager(getContext());
        return mLayoutManager;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        final Bundle arguments = getArguments();
        mAdapter = new HighlightablePreferenceGroupAdapter(preferenceScreen,
                arguments == null
                        ? null
                        : arguments.getString(PartsActivity.EXTRA_FRAGMENT_ARG_KEY),
                mPreferenceHighlighted);
        return mAdapter;
    }

    protected void setAnimationAllowed(boolean animationAllowed) {
        mAnimationAllowed = animationAllowed;
    }

    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<String, Preference>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    protected Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    protected void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
    }

    protected int getCachedCount() {
        return mPreferenceCache.size();
    }

    private int findListPositionFromKey(PreferenceGroupAdapter adapter, String key) {
        final int count = adapter.getItemCount();
        for (int n = 0; n < count; n++) {
            final Preference preference = adapter.getItem(n);
            final String preferenceKey = preference.getKey();
            if (preferenceKey != null && preferenceKey.equals(key)) {
                return n;
            }
        }
        return -1;
    }

    protected void removePreference(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /*
     * The name is intentionally made different from Activity#finish(), so that
     * users won't misunderstand its meaning.
     */
    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    public final void finishPreferencePanel(Fragment caller, int resultCode, Intent data) {
        ((PartsActivity)getActivity()).finishPreferencePanel(caller, resultCode, data);
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        synchronized (mTriggerUris) {
            SettingsHelper.get(activity).startWatching(this,
                    mTriggerUris.toArray(new Uri[mTriggerUris.size()]));
        }
    }

    @Override
    public void onDetach() {
        if (isRemoving()) {
            if (mDialogFragment != null) {
                mDialogFragment.dismiss();
                mDialogFragment = null;
            }
        }
        synchronized (mTriggerUris) {
            SettingsHelper.get(getActivity()).stopWatching(this);
            mTriggerUris.clear();
        }
        super.onDetach();
    }

    protected void watch(Uri... contentUris) {
        synchronized (mTriggerUris) {
            mTriggerUris.addAll(Arrays.asList(contentUris));
            if (!isDetached()) {
                SettingsHelper.get(getActivity()).startWatching(this,
                        mTriggerUris.toArray(new Uri[mTriggerUris.size()]));
            }
        }
    }

    // Dialog management

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getChildFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }

    protected void removeDialog(int dialogId, boolean stateLossAllowed) {
        if (stateLossAllowed) {
            if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
                getFragmentManager().beginTransaction().remove(mDialogFragment).
                        commitAllowingStateLoss();
            }
            mDialogFragment = null;
        } else {
            removeDialog(dialogId);
        }
    }

    /**
     * Sets the OnCancelListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnCancelListener = listener;
        }
    }

    /**
     * Sets the OnDismissListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnDismissListener = listener;
        }
    }

    public void onDialogShowing() {
        // override in subclass to attach a dismiss listener, for instance
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    public static class SettingsDialogFragment extends DialogFragment {
        private static final String KEY_DIALOG_ID = "key_dialog_id";
        private static final String KEY_PARENT_FRAGMENT_ID = "key_parent_fragment_id";

        private int mDialogId;

        private Fragment mParentFragment;

        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public SettingsDialogFragment() {
            /* do nothing */
        }

        public SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            mDialogId = dialogId;
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }
            mParentFragment = (Fragment) fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (mParentFragment != null) {
                outState.putInt(KEY_DIALOG_ID, mDialogId);
                outState.putInt(KEY_PARENT_FRAGMENT_ID, mParentFragment.getId());
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            if (mParentFragment != null && mParentFragment instanceof SettingsPreferenceFragment) {
                ((SettingsPreferenceFragment) mParentFragment).onDialogShowing();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDialogId = savedInstanceState.getInt(KEY_DIALOG_ID, 0);
                mParentFragment = getParentFragment();
                int mParentFragmentId = savedInstanceState.getInt(KEY_PARENT_FRAGMENT_ID, -1);
                if (mParentFragment == null) {
                    mParentFragment = getFragmentManager().findFragmentById(mParentFragmentId);
                }
                if (!(mParentFragment instanceof DialogCreatable)) {
                    throw new IllegalArgumentException(
                            (mParentFragment != null
                                    ? mParentFragment.getClass().getName()
                                    : mParentFragmentId)
                                    + " must implement "
                                    + DialogCreatable.class.getName());
                }
                // This dialog fragment could be created from non-SettingsPreferenceFragment
                if (mParentFragment instanceof SettingsPreferenceFragment) {
                    // restore mDialogFragment in mParentFragment
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = this;
                }
            }
            return ((DialogCreatable) mParentFragment).onCreateDialog(mDialogId);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (mOnCancelListener != null) {
                mOnCancelListener.onCancel(dialog);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        }

        public int getDialogId() {
            return mDialogId;
        }

        @Override
        public void onDetach() {
            super.onDetach();

            // This dialog fragment could be created from non-SettingsPreferenceFragment
            if (mParentFragment instanceof SettingsPreferenceFragment) {
                // in case the dialog is not explicitly removed by removeDialog()
                if (((SettingsPreferenceFragment) mParentFragment).mDialogFragment == this) {
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = null;
                }
            }
        }
    }

    protected Button getBackButton() {
        return (Button) ((PartsActivity)getActivity()).getBackButton();
    }

    protected Button getNextButton() {
        return (Button) ((PartsActivity)getActivity()).getNextButton();
    }

    protected void showButtonBar(boolean show) {
        ((PartsActivity)getActivity()).showButtonBar(show);
    }

    public void finish() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            activity.finish();
        }
    }

    protected Intent getIntent() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().getIntent();
    }

    protected void setResult(int result, Intent intent) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result, intent);
    }

    protected void setResult(int result) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result);
    }

    public boolean isAvailable() {
        return true;
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    public boolean startFragment(Fragment caller, String fragmentClass, int titleRes,
                                 int requestCode, Bundle extras) {
        final Activity activity = getActivity();
        if (activity instanceof PartsActivity) {
            PartsActivity sa = (PartsActivity) activity;
            sa.startPreferencePanel(fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG,
                    "Parent isn't PartsActivity! (name: " + fragmentClass + ")");
            return false;
        }
    }
}
