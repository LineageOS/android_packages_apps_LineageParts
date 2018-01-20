/*
 * Copyright (C) 2018 The LineageOS Project
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
package org.lineageos.lineageparts.style;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.style.models.Accent;
import org.lineageos.lineageparts.style.models.Style;
import org.lineageos.lineageparts.style.util.AccentAdapter;
import org.lineageos.lineageparts.style.util.AccentUtils;
import org.lineageos.lineageparts.style.util.OverlayManager;
import org.lineageos.lineageparts.style.util.UIUtils;

import java.util.List;

import lineageos.providers.LineageSettings;

public class StylePreferences extends SettingsPreferenceFragment {
    private static final String TAG = "StylePreferences";
    private static final String DARK_THEME = "org.lineageos.overlay.dark";

    private Preference mStylePref;
    private Preference mAccentPref;

    private OverlayManager mOverlayManager;
    private List<Accent> mAccents;

    private boolean isDarkOn;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        addPreferencesFromResource(R.xml.style_preferences);

        mOverlayManager = new OverlayManager(getContext());
        mAccents = AccentUtils.getAccents(getContext(), mOverlayManager.isEnabled(DARK_THEME));

        mStylePref = findPreference("style_global");
        mStylePref.setOnPreferenceChangeListener(this::onStyleChange);
        setupStylePref();

        mAccentPref = findPreference("style_accent");
        mAccentPref.setOnPreferenceClickListener(this::onAccentClick);
        setupAccentPref();

        Preference automagic = findPreference("style_automagic");
        automagic.setOnPreferenceClickListener(p -> onAutomagicClick());
    }

    private boolean onAccentClick(Preference preference) {
        mAccents = AccentUtils.getAccents(getContext(), mOverlayManager.isEnabled(DARK_THEME));

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.style_accent_title)
                .setAdapter(new AccentAdapter(mAccents, getContext()),
                        (dialog, i) -> onAccentSelected(mAccents.get(i)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        return true;
    }

    private void setupAccentPref() {
        String currentAccent = LineageSettings.System.getString(getContext().getContentResolver(),
                LineageSettings.System.BERRY_CURRENT_ACCENT);
        try {
            updateAccentPref(AccentUtils.getAccent(getContext(), currentAccent,
                    mOverlayManager.isEnabled(DARK_THEME)));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, currentAccent + ": package not found.");
        }
    }

    private void onAccentSelected(Accent accent) {
        String previousAccent = LineageSettings.System.getString(getContext().getContentResolver(),
                LineageSettings.System.BERRY_CURRENT_ACCENT);

        if (!TextUtils.isEmpty(previousAccent)) {
            // Disable previous theme
            mOverlayManager.setEnabled(previousAccent, false);
        }

        LineageSettings.System.putString(getContext().getContentResolver(),
                LineageSettings.System.BERRY_CURRENT_ACCENT, accent.getPackageName());

        if (!TextUtils.isEmpty(accent.getPackageName())) {
            // Enable new theme
            mOverlayManager.setEnabled(accent.getPackageName(), true);
        }
        updateAccentPref(accent);
    }

    private void updateAccentPref(Accent accent) {
        int size = getResources().getDimensionPixelSize(R.dimen.style_accent_icon);

        mAccentPref.setSummary(accent.getName());
        mAccentPref.setIcon(UIUtils.getAccentBitmap(getResources(), size, accent.getColor()));
    }

    private boolean onAutomagicClick() {
        if (!hasStoragePermission()) {
            Toast.makeText(getContext(), getString(R.string.style_permission_error),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        Bitmap bitmap = getWallpaperBitmap();
        if (bitmap == null) {
            return false;
        }

        Accent[] accentsArray = new Accent[mAccents.size()];
        mAccents.toArray(accentsArray);

        Palette palette = Palette.from(bitmap).generate();
        new AutomagicTask(palette, this::onAutomagicCompleted).execute(accentsArray);

        return true;
    }

    private void onAutomagicCompleted(Style style) {
        String styleType = getString(style.isLight() ?
                R.string.style_global_entry_light : R.string.style_global_entry_dark).toLowerCase();
        String accentName = style.getAccent().getName().toLowerCase();
        String message = getString(R.string.style_automagic_dialog_content, styleType, accentName);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.style_automagic_title)
                .setMessage(message)
                .setPositiveButton(R.string.style_automagic_dialog_positive,
                        (dialog, i) -> applyStyle(style))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupStylePref() {
        setStyleIcon(LineageSettings.System.getInt(getContext().getContentResolver(), 
                LineageSettings.System.BERRY_GLOBAL_STYLE, 0));
    }

    private void applyStyle(Style style) {
        LineageSettings.System.putInt(getContext().getContentResolver(),
            LineageSettings.System.BERRY_GLOBAL_STYLE, style.isLight() ? 2 : 3);

        onStyleChange(mStylePref, style.isLight() ? "2" : "3");
        onAccentSelected(style.getAccent());
    }

    private boolean onStyleChange(Preference preference, Object newValue) {
        if (!(newValue instanceof String)) {
            return false;
        }

        Integer value = Integer.valueOf((String) newValue);
        LineageSettings.System.putInt(getContext().getContentResolver(),
                LineageSettings.System.BERRY_GLOBAL_STYLE, value);

        setStyleIcon(value);
        Log.d(TAG, "Current mode is: " + value);
        return true;
    }

    private void setStyleIcon(int value) {
        int icon;
        switch (value) {
            case 1:
                icon = R.drawable.ic_style_time;
                break;
            case 2:
                icon = R.drawable.ic_style_light;
                break;
            case 3:
                icon = R.drawable.ic_style_dark;
                break;
            default:
                icon = R.drawable.ic_style_auto;
                break;
        }

        mStylePref.setIcon(icon);
    }

    @Nullable
    private Bitmap getWallpaperBitmap() {
        WallpaperManager manager = WallpaperManager.getInstance(getContext());
        Drawable wallpaper = manager.getDrawable();

        if (wallpaper == null) {
            return null;
        }

        if (wallpaper instanceof BitmapDrawable) {
            return ((BitmapDrawable) wallpaper).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(wallpaper.getIntrinsicWidth(),
                wallpaper.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        wallpaper.setBounds(0, 0 , canvas.getWidth(), canvas.getHeight());
        wallpaper.draw(canvas);
        return bitmap;
    }

    private boolean hasStoragePermission() {
        Activity activity = getActivity();
        return activity != null &&
                activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    private static final class AutomagicTask extends AsyncTask<Accent, Void, Style> {
        private static final int COLOR_DEFAULT = Color.BLACK;

        private final Palette mPalette;
        private final Callback mCallback;

        AutomagicTask(Palette palette, Callback callback) {
            mPalette = palette;
            mCallback = callback;
        }

        @NonNull
        @Override
        public Style doInBackground(Accent... accents) {
            int wallpaperColor = mPalette.getVibrantColor(COLOR_DEFAULT);

            // If vibrant color extraction failed, let's try muted color
            if (wallpaperColor == COLOR_DEFAULT) {
                wallpaperColor = mPalette.getMutedColor(COLOR_DEFAULT);
            }

            Accent bestAccent = getBestAccent(accents, wallpaperColor);
            boolean isLight = UIUtils.isColorLight(wallpaperColor);

            return new Style(bestAccent, isLight);
        }

        @Override
        public void onPostExecute(Style style) {
            mCallback.onDone(style);
        }

        private Accent getBestAccent(Accent[] accents, int wallpaperColor) {
            int bestIndex = 0;
            double minDiff = Double.MAX_VALUE;

            for (int i = 0; i < accents.length; i++) {
                double diff = diff(accents[i].getColor(), wallpaperColor);
                if (diff < minDiff) {
                    bestIndex = i;
                    minDiff = diff;
                }
            }

            return accents[bestIndex];
        }

        private double diff(@ColorInt int accent, @ColorInt int wallpaper) {
            return Math.sqrt(Math.pow(Color.red(accent) - Color.red(wallpaper), 2) +
                    Math.pow(Color.green(accent) - Color.green(wallpaper), 2) +
                    Math.pow(Color.blue(accent) - Color.blue(wallpaper), 2));
        }
    }

    private interface Callback {
        void onDone(Style style);
    }
}