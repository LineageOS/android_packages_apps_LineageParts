/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2018,2020-2021 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.contributors;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.SearchIndexableRaw;
import org.lineageos.lineageparts.search.Searchable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContributorsCloudFragment extends Fragment implements SearchView.OnQueryTextListener,
        SearchView.OnCloseListener, MenuItem.OnActionExpandListener, Searchable {

    private static final String TAG = "ContributorsCloud";

    private static final String DB_NAME = "contributors.db";

    private static final String STATE_SELECTED_CONTRIBUTOR = "state_selected_contributor";

    private ContributorsCloudViewController mViewController;
    private ImageView mImageView;
    private View mLoadingView;
    private View mFailedView;
    private ListView mSearchResults;
    private ContributorsAdapter mSearchAdapter;

    private SQLiteDatabase mDatabase;

    private int mTotalContributors;
    private int mTotalCommits;
    private long mLastUpdate;

    private int mSelectedContributor = -1;
    private String mContributorName;
    private String mContributorNick;
    private int mContributorCommits;

    private MenuItem mSearchMenuItem;
    private MenuItem mContributorInfoMenuItem;
    private MenuItem mContributionsInfoMenuItem;
    private SearchView mSearchView;

    private Handler mHandler;

    private static final String KEY_PREFIX = "contributor_";

    private static class ViewInfo {
        Bitmap mBitmap;
        float mFocusX;
        float mFocusY;
    }

    private static class ContributorsDataHolder {
        int mId;
        String mLabel;
    }

    private static class ContributorsViewHolder {
        TextView mLabel;
    }

    private static class ContributorsAdapter extends ArrayAdapter<ContributorsDataHolder> {

        public ContributorsAdapter(Context context) {
            super(context, R.id.contributor_name, new ArrayList<ContributorsDataHolder>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater li = LayoutInflater.from(getContext());
                convertView = li.inflate(R.layout.contributors_search_result, null);
                ContributorsViewHolder viewHolder = new ContributorsViewHolder();
                viewHolder.mLabel = convertView.findViewById(R.id.contributor_name);
                convertView.setTag(viewHolder);
            }

            ContributorsDataHolder dataHolder = getItem(position);

            ContributorsViewHolder viewHolder = (ContributorsViewHolder) convertView.getTag();
            viewHolder.mLabel.setText(dataHolder.mLabel);

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private class ContributorCloudLoaderTask extends AsyncTask<Void, Void, Boolean> {
        private ViewInfo mViewInfo;
        private final boolean mNotify;
        private final boolean mNavigate;

        public ContributorCloudLoaderTask(boolean notify, boolean navigate) {
            mNotify = notify;
            mNavigate = navigate;
        }

        @Override
        protected void onPreExecute() {
            mLoadingView.setAlpha(1f);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                loadContributorsInfo(getActivity());
                loadUserInfo(getActivity());
                mViewInfo = generateViewInfo(getActivity(), mSelectedContributor);
                if (mViewInfo != null && mViewInfo.mBitmap != null) {
                    return Boolean.TRUE;
                }

            } catch (Exception ex) {
                Log.e(TAG, "Failed to generate cloud bitmap", ex);
            }
            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mImageView.setImageBitmap(mViewInfo.mBitmap);
                mViewController.update();
                if (mNotify) {
                    if (mNavigate) {
                        onLoadCloudDataSuccess(mViewInfo.mFocusX, mViewInfo.mFocusY);
                    } else {
                        onLoadCloudDataSuccess(-1, -1);
                    }
                }
            } else {
                mImageView.setImageBitmap(null);
                mViewController.update();
                if (mViewInfo != null && mViewInfo.mBitmap != null) {
                    mViewInfo.mBitmap.recycle();
                }
                if (mNotify) {
                    onLoadCloudDataFailed();
                }
            }
        }

        @Override
        protected void onCancelled() {
            onLoadCloudDataFailed();
        }
    }

    public ContributorsCloudFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mSelectedContributor = savedInstanceState.getInt(STATE_SELECTED_CONTRIBUTOR, -1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && mDatabase.isOpen()) {
            try {
                mDatabase.close();
            } catch (SQLException ex) {
                // Ignore
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_CONTRIBUTOR, mSelectedContributor);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Remove all previous menus
        int count = menu.size();
        for (int i = 0; i < count; i++) {
            menu.removeItem(menu.getItem(i).getItemId());
        }

        inflater.inflate(R.menu.contributors_menu, menu);

        mSearchMenuItem = menu.findItem(R.id.contributors_search);
        mContributorInfoMenuItem = menu.findItem(R.id.contributor_info);
        mContributionsInfoMenuItem = menu.findItem(R.id.contributions_info);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchMenuItem.setOnActionExpandListener(this);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);

        showMenuItems(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.contributors_search:
                mSearchView.setQuery("", false);
                mSelectedContributor = -1;

                // Load the data from the database and fill the image
                ContributorCloudLoaderTask task = new ContributorCloudLoaderTask(false, false);
                task.execute();
                break;

            case R.id.contributor_info:
                showUserInfo(getActivity());
                break;

            case R.id.contributions_info:
                showContributorsInfo(getActivity());
                break;

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View v = inflater.inflate(R.layout.contributors_view, container, false);

        mLoadingView= v.findViewById(R.id.contributors_cloud_loading);
        mFailedView= v.findViewById(R.id.contributors_cloud_failed);
        mImageView = v.findViewById(R.id.contributors_cloud_image);
        mViewController = new ContributorsCloudViewController(mImageView);
        mViewController.setMaximumScale(20f);
        mViewController.setMediumScale(7f);

        mSearchResults = v.findViewById(R.id.contributors_cloud_search_results);
        mSearchAdapter = new ContributorsAdapter(getActivity());
        mSearchResults.setAdapter(mSearchAdapter);
        mSearchResults.setOnItemClickListener((parent, view, position, id) -> {
            ContributorsDataHolder contributor =
                    (ContributorsDataHolder) parent.getItemAtPosition(position);
            onContributorSelected(contributor);
        });

        // Load the data from the database and fill the image
        ContributorCloudLoaderTask task = new ContributorCloudLoaderTask(true, false);
        task.execute();

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null) {
            String c = args.getString(PartsActivity.EXTRA_FRAGMENT_ARG_KEY);
            if (c != null && c.startsWith(KEY_PREFIX)) {
                onContributorSelected(Integer.parseInt(c.substring(KEY_PREFIX.length())));
                args.remove(PartsActivity.EXTRA_FRAGMENT_ARG_KEY);
            }
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            animateFadeOutFadeIn(mImageView, mSearchResults);
            mContributorInfoMenuItem.setVisible(false);
            mContributionsInfoMenuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            animateFadeOutFadeIn(mSearchResults, mImageView);
            if (mSelectedContributor != -1) {
                mContributorInfoMenuItem.setVisible(true);
            }
            mContributionsInfoMenuItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onClose() {
        animateFadeOutFadeIn(mSearchResults, mImageView);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        List<ContributorsDataHolder> contributors = new ArrayList<>();
        if (!TextUtils.isEmpty(newText) || newText.length() >= 3) {
            contributors.addAll(performFilter(getActivity(), newText));
        }
        mSearchAdapter.clear();
        mSearchAdapter.addAll(contributors);
        mSearchAdapter.notifyDataSetChanged();
        return true;
    }

    private void showMenuItems(boolean visible) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.setVisible(visible);
        }
        if (mContributorInfoMenuItem != null) {
            mContributorInfoMenuItem.setVisible(mSelectedContributor != -1 && visible);
        }
        if (mContributionsInfoMenuItem != null) {
            mContributionsInfoMenuItem.setVisible(visible);
        }
        if (!visible) {
            mSearchView.setQuery("", false);
            if (mSearchMenuItem != null) {
                mSearchMenuItem.collapseActionView();
            }
        }
    }

    private void onLoadCloudDataSuccess(float focusX, float focusY) {
        animateFadeOutFadeIn(mLoadingView.getVisibility() == View.VISIBLE
                ? mLoadingView : mSearchResults, mImageView);
        showMenuItems(true);

        // Navigate to contributor?
        if (focusX != -1 && focusY != -1) {
            mViewController.setZoomTransitionDuration(2500);
            mViewController.setScale(10, focusX, focusY, true);
            mHandler.postDelayed(() -> mViewController.setZoomTransitionDuration(-1), 2500);
        }
    }

    private void onLoadCloudDataFailed() {
        // Show the cloud not loaded message
        animateFadeOutFadeIn(mLoadingView.getVisibility() == View.VISIBLE
                ? mLoadingView : (mImageView.getVisibility() == View.VISIBLE)
                        ? mImageView : mSearchResults, mFailedView);
        showMenuItems(false);
    }

    private void animateFadeOutFadeIn(final View src, final View dst) {
        if (dst.getVisibility() != View.VISIBLE || dst.getAlpha() != 1f) {
            AnimatorSet set = new AnimatorSet();
            set.playSequentially(
                    ObjectAnimator.ofFloat(src, "alpha", 0f),
                    ObjectAnimator.ofFloat(dst, "alpha", 1f));
            set.setInterpolator(new LinearInterpolator());
            set.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    src.setAlpha(1f);
                    dst.setAlpha(0f);
                    src.setVisibility(View.VISIBLE);
                    dst.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    src.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            set.setDuration(250);
            set.start();
        } else {
            src.setAlpha(1f);
            src.setVisibility(View.GONE);
        }
    }

    private synchronized ViewInfo generateViewInfo(Context context, int selectedId) {
        Bitmap bitmap = null;
        float focusX = -1, focusY = -1;
        final Resources res = context.getResources();

        // Open the database
        SQLiteDatabase db = getDatabase(context, true);
        if (db == null) {
            // We don't have a valid database reference
            return null;
        }

        // Extract original image size
        Cursor c = db.rawQuery("select value from info where key = ?;", new String[]{"orig_size"});
        if (c == null || !c.moveToFirst()) {
            // We don't have a valid cursor reference
            return null;
        }
        int osize = c.getInt(0);
        c.close();

        // Query the metadata table to extract all the commits information
        c = db.rawQuery("select id, name, x, y, r, fs from metadata;", null);
        if (c == null) {
            // We don't have a valid cursor reference
            return null;
        }
        try {
            TypedValue colorAccent = new TypedValue();
            context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                    colorAccent, true);
            int colorForeground = res.getColor(colorAccent.resourceId, context.getTheme());
            int colorSelected = res.getColor(R.color.contributors_cloud_selected_color,
                    context.getTheme());
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

            // Create a bitmap large enough to hold the cloud (use large bitmap when available)
            int bsize = hasLargeHeap() ? 2048 : 1024;
            bitmap = Bitmap.createBitmap(bsize, bsize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Draw every contributor name
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow("id"));

                String name = c.getString(c.getColumnIndexOrThrow("name"));
                float x = translate(c.getFloat(c.getColumnIndexOrThrow("x")), osize, bsize);
                float y = translate(c.getFloat(c.getColumnIndexOrThrow("y")), osize, bsize);
                int r = c.getInt(c.getColumnIndexOrThrow("r"));
                float fs = translate(c.getFloat(c.getColumnIndexOrThrow("fs")), osize, bsize);
                if (id < 0) {
                    y -= translate(fs, osize, bsize);
                }

                // Choose the correct paint
                paint.setColor(selectedId == id ? colorSelected : colorForeground);
                paint.setTextSize(fs);

                // Check text rotation
                float w = 0f, h = 0f;
                if (selectedId == id || r != 0) {
                    Rect bounds = new Rect();
                    paint.getTextBounds(name, 0, name.length(), bounds);
                    h = bounds.height();
                }
                if (selectedId == id || r == -1) {
                    w = paint.measureText(name);
                }
                if (r == 0) {
                    // Horizontal
                    canvas.drawText(name, x, y, paint);
                } else {
                    canvas.save();
                    if (r == -1) {
                        // Vertical (-90 rotation)
                        canvas.translate(h, w - h);
                        canvas.rotate(-90, x, y);
                    } else {
                        // Vertical (+90 rotation)
                        canvas.translate(h/2, -h);
                        canvas.rotate(90, x, y);
                    }
                    canvas.drawText(name, x, y, paint);
                    canvas.restore();
                }

                // Calculate focus
                if (selectedId == id) {
                    int iw = mImageView.getWidth();
                    int ih = mImageView.getHeight();
                    int cx = iw / 2;
                    int cy = ih / 2;
                    int cbx = bsize / 2;
                    int cby = bsize / 2;
                    float cw;
                    float ch;
                    if (r == 0) {
                        cw = translate(w, bsize, Math.min(iw, ih)) / 2;
                        ch = translate(h, bsize, Math.min(iw, ih)) / 2;
                    } else {
                        cw = translate(h, bsize, Math.min(iw, ih)) / 2;
                        ch = translate(w, bsize, Math.min(iw, ih)) / 2;
                    }

                    focusX = cx + translate(x - cbx, bsize, iw) + cw;
                    focusY = cy + translate(y - cby, bsize, ih) + ch;
                }
            }

        } finally {
            c.close();
        }

        // Return the bitmap
        ViewInfo viewInfo = new ViewInfo();
        viewInfo.mBitmap = bitmap;
        viewInfo.mFocusX = focusX;
        viewInfo.mFocusY = focusY;
        return viewInfo;
    }

    private synchronized SQLiteDatabase getDatabase(Context context, boolean retryCopyIfOpenFails) {
        if (mDatabase == null) {
            File dbPath = context.getDatabasePath(DB_NAME);
            try {
                mDatabase = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(),
                        null, SQLiteDatabase.OPEN_READONLY);
                if (mDatabase == null) {
                    Log.e(TAG, "Cannot open cloud database: " + DB_NAME + ". db == null");
                    return null;
                }
                return mDatabase;

            } catch (SQLException ex) {
                Log.e(TAG, "Cannot open cloud database: " + DB_NAME, ex);
                if (mDatabase != null && mDatabase.isOpen()) {
                    try {
                        mDatabase.close();
                    } catch (SQLException ex2) {
                        // Ignore
                    }
                }

                if (retryCopyIfOpenFails) {
                    extractContributorsCloudDatabase(context);
                    mDatabase = getDatabase(context, false);
                }
            }

            // We don't have a valid connection
            return null;
        }
        return mDatabase;
    }

    private void loadContributorsInfo(Context context) {
        mTotalContributors = -1;
        mTotalCommits = -1;
        mLastUpdate = -1;

        // Open the database
        SQLiteDatabase db = getDatabase(context, true);
        if (db == null) {
            // We don't have a valid database reference
            return;
        }

        // Total contributors
        Cursor c = db.rawQuery("select count(*) from metadata where id > 0;", null);
        if (c == null || !c.moveToFirst()) {
            // We don't have a valid cursor reference
            return;
        }
        mTotalContributors = c.getInt(0);
        c.close();

        // Total commits
        c = db.rawQuery("select sum(commits) from metadata where id > 0;", null);
        if (c == null || !c.moveToFirst()) {
            // We don't have a valid cursor reference
            return;
        }
        mTotalCommits = c.getInt(0);
        c.close();

        // Last update
        c = db.rawQuery("select value from info where key = ?;", new String[]{"date"});
        if (c == null || !c.moveToFirst()) {
            // We don't have a valid cursor reference
            return;
        }
        mLastUpdate = c.getLong(0);
        c.close();
    }

    private void loadUserInfo(Context context) {
        // Open the database
        SQLiteDatabase db = getDatabase(context, true);
        if (db == null) {
            // We don't have a valid database reference
            return;
        }

        // Total contributors
        String[] args = new String[]{String.valueOf(mSelectedContributor)};
        Cursor c = db.rawQuery("select m1.name, m1.username, m1.commits " +
                               "from metadata as m1 where m1.id = ?;", args);
        if (c == null || !c.moveToFirst()) {
            // We don't have a valid cursor reference
            return;
        }
        mContributorName = c.getString(0);
        mContributorNick = c.getString(1);
        mContributorCommits = c.getInt(2);
        c.close();
    }

    private void showUserInfo(Context context) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        String name = mContributorName != null ? mContributorName : "-";
        String nick = mContributorNick != null ? mContributorNick : "-";
        String commits = mContributorName != null ? nf.format(mContributorCommits) : "-";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.contributor_info_menu);
        builder.setMessage(Html.fromHtml(getString(R.string.contributor_info_msg,
                name, nick, commits), Html.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showContributorsInfo(Context context) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        java.text.DateFormat df = DateFormat.getLongDateFormat(context);
        java.text.DateFormat tf = DateFormat.getTimeFormat(context);
        String totalContributors = mTotalContributors != -1
                ? nf.format(mTotalContributors) : "-";
        String totalCommits = mTotalCommits != -1
                ? nf.format(mTotalCommits) : "-";
        String lastUpdate = mLastUpdate != -1
                ? df.format(mLastUpdate) + " " + tf.format(mLastUpdate) : "-";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.contributions_info_menu);
        builder.setMessage(Html.fromHtml(getString(R.string.contributions_info_msg,
                totalContributors, totalCommits, lastUpdate), Html.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private List<ContributorsDataHolder> performFilter(Context context, String query) {
        // Open the database
        SQLiteDatabase db = getDatabase(context, false);
        if (db == null) {
            // We don't have a valid database reference
            return new ArrayList<>();
        }

        // Total contributors
        String[] args = new String[]{query.replaceAll("\\|", "")};
        Cursor c = db.rawQuery("select id, name || case when username is null then '' " +
                "else ' <'||username||'>' end contributor from metadata where lower(filter) like " +
                "lower('%' || ? || '%') and id > 0 order by commits desc", args);
        if (c == null) {
            // We don't have a valid cursor reference
            return new ArrayList<>();
        }
        List<ContributorsDataHolder> results = new ArrayList<>();
        while (c.moveToNext()) {
            ContributorsDataHolder result = new ContributorsDataHolder();
            result.mId = c.getInt(0);
            result.mLabel = c.getString(1);
            results.add(result);
        }
        c.close();
        return results;
    }

    private void onContributorSelected(ContributorsDataHolder contributor) {
        onContributorSelected(contributor.mId);
    }

    private void onContributorSelected(int contributorId) {
        mSelectedContributor = contributorId;
        ContributorCloudLoaderTask task = new ContributorCloudLoaderTask(true, true);
        task.execute();
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }
    }

    private boolean hasLargeHeap() {
        ActivityManager am = getActivity().getSystemService(ActivityManager.class);
        return am.getMemoryClass() >= 96;
    }

    private float translate(float v, int ssize, int dsize) {
        return (v * dsize) / ssize;
    }


    public static void extractContributorsCloudDatabase(Context context) {
        final int BUFFER = 1024;
        InputStream is = null;
        OutputStream os;
        File databasePath = context.getDatabasePath(DB_NAME);
        try {
            databasePath.getParentFile().mkdir();
            is = context.getResources().getAssets().open(DB_NAME, AssetManager.ACCESS_BUFFER);
            os = new FileOutputStream(databasePath);
            int read = -1;
            byte[] data = new byte[BUFFER];
            while ((read = is.read(data, 0, BUFFER)) != -1) {
                os.write(data, 0, read);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract contributors database");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context) {

                    // Index the top 100 contributors, for fun :)
                    File dbPath = context.getDatabasePath(DB_NAME);
                    SQLiteDatabase db;
                    try {
                        db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(),
                                null, SQLiteDatabase.OPEN_READONLY);
                        if (db == null) {
                            Log.e(TAG, "Cannot open cloud database: " + DB_NAME + ". db == null");
                            return null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        return null;
                    }

                    List<SearchIndexableRaw> result = new ArrayList<>();
                    try (Cursor c = db.rawQuery(
                            "select id, username from metadata order by commits desc limit 100;",
                            null)) {
                        while (c.moveToNext()) {
                            SearchIndexableRaw raw = new SearchIndexableRaw(context);
                            raw.key = KEY_PREFIX + c.getString(0);
                            raw.rank = 10;
                            raw.title = c.getString(1);
                            result.add(raw);
                        }
                    }
                    db.close();

                    return result;
                }
    };
}
