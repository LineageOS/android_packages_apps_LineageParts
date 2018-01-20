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
import org.lineageos.lineageparts.style.models.StyleStatus;
import org.lineageos.lineageparts.style.util.AccentAdapter;
import org.lineageos.lineageparts.style.util.AccentUtils;
import org.lineageos.lineageparts.style.util.OverlayManager;
import org.lineageos.lineageparts.style.util.UIUtils;

import java.util.List;

import lineageos.providers.LineageSettings;

public class StylePreferences extends SettingsPreferenceFragment {
    private static final String TAG = "StylePreferences";
    private static final int INDEX_WALLPAPER = 0;
    private static final int INDEX_TIME = 1;
    private static final int INDEX_LIGHT = 2;
    private static final int INDEX_DARK = 3;

    private Preference mStylePref;
    private Preference mAccentPref;

    private List<Accent> mAccents;

    private StyleStatus mStyleStatus;

    private byte mOkStatus = 0;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        addPreferencesFromResource(R.xml.style_preferences);


        mStylePref = findPreference("berry_global_style");
        mStylePref.setOnPreferenceChangeListener(this::onStyleChange);
        setupStylePref();

        mAccents = AccentUtils.getAccents(getContext(), mStyleStatus);
        mAccentPref = findPreference("style_accent");
        mAccentPref.setOnPreferenceClickListener(this::onAccentClick);
        setupAccentPref();

        Preference automagic = findPreference("style_automagic");
        automagic.setOnPreferenceClickListener(p -> onAutomagicClick());
    }

    private boolean onAccentClick(Preference preference) {
        mAccents = AccentUtils.getAccents(getContext(), mStyleStatus);

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
            updateAccentPref(AccentUtils.getAccent(getContext(), currentAccent));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, currentAccent + ": package not found.");
        }
    }

    private void onAccentSelected(Accent accent) {
        String previousAccent = LineageSettings.System.getString(getContext().getContentResolver(),
                LineageSettings.System.BERRY_CURRENT_ACCENT);

        OverlayManager om = new OverlayManager(getContext());
        if (!TextUtils.isEmpty(previousAccent)) {
            // Disable previous theme
            om.setEnabled(previousAccent, false);
        }

        LineageSettings.System.putString(getContext().getContentResolver(),
                LineageSettings.System.BERRY_CURRENT_ACCENT, accent.getPackageName());

        if (!TextUtils.isEmpty(accent.getPackageName())) {
            // Enable new theme
            om.setEnabled(accent.getPackageName(), true);
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
                .setNegativeButton(android.R.string.cancel,
                        (dialog, i) -> increaseOkStatus())
                .show();
    }

    private void setupStylePref() {
        int preference = LineageSettings.System.getInt(getContext().getContentResolver(),
                LineageSettings.System.BERRY_GLOBAL_STYLE, INDEX_WALLPAPER);

        setStyleIcon(preference);
        switch (preference) {
            case INDEX_LIGHT:
                mStyleStatus = StyleStatus.LIGHT_ONLY;
                break;
            case INDEX_DARK:
                mStyleStatus = StyleStatus.DARK_ONLY;
                break;
            default:
                mStyleStatus = StyleStatus.DYNAMIC;
                break;
        }
    }

    private void applyStyle(Style style) {
        int value = style.isLight() ? INDEX_LIGHT : INDEX_DARK;
        LineageSettings.System.putInt(getContext().getContentResolver(),
            LineageSettings.System.BERRY_GLOBAL_STYLE, value);

        onStyleChange(mStylePref, value);
        onAccentSelected(style.getAccent());
    }

    private boolean onStyleChange(Preference preference, Object newValue) {
        Integer value;
        if (newValue instanceof String) {
            value = Integer.valueOf((String) newValue);
        } else if (newValue instanceof Integer) {
            value = (Integer) newValue;
        } else {
            return false;
        }

        boolean accentCompatibility = checkAccentCompatibility(value);
        if (!accentCompatibility) {
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.style_global_title)
                .setMessage(R.string.style_accent_configuration_not_supported)
                .setPositiveButton(R.string.style_accent_configuration_positive,
                        (dialog, i) -> onAccentConflict(value))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return false;
        }

        LineageSettings.System.putInt(getContext().getContentResolver(),
                LineageSettings.System.BERRY_GLOBAL_STYLE, value);

        setStyleIcon(value);
        return true;
    }

    private void setStyleIcon(int value) {
        int icon;
        switch (value) {
            case INDEX_TIME:
                icon = R.drawable.ic_style_time;
                break;
            case INDEX_LIGHT:
                icon = R.drawable.ic_style_light;
                break;
            case INDEX_DARK:
                icon = R.drawable.ic_style_dark;
                break;
            default:
                icon = R.drawable.ic_style_auto;
                break;
        }

        mStylePref.setIcon(icon);
    }

    private boolean checkAccentCompatibility(int value) {
        String currentAccentPkg = LineageSettings.System.getString(
                getContext().getContentResolver(), LineageSettings.System.BERRY_CURRENT_ACCENT);
        StyleStatus supportedStatus;
        try {
            supportedStatus = AccentUtils.getAccent(getContext(), currentAccentPkg)
                .getSupportedStatus();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e.getMessage());
            supportedStatus = StyleStatus.DYNAMIC;
        }

        switch (supportedStatus) {
            case LIGHT_ONLY:
                return value == INDEX_LIGHT;
            case DARK_ONLY:
                return value == INDEX_DARK;
            case DYNAMIC:
            default: // Never happens, but compilation fails without this
                return true;
        }
    }

    private void onAccentConflict(int value) {
        StyleStatus proposedStatus;
        switch (value) {
            case INDEX_LIGHT:
                proposedStatus = StyleStatus.LIGHT_ONLY;
                break;
            case INDEX_DARK:
                proposedStatus = StyleStatus.DARK_ONLY;
                break;
            default:
                proposedStatus = StyleStatus.DYNAMIC;
                break;
        }

        // Let the user pick the new accent
        List<Accent> accents = AccentUtils.getAccents(getContext(), proposedStatus);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.style_accent_title)
                .setAdapter(new AccentAdapter(accents, getContext()),
                        (dialog, i) -> {
                            onAccentSelected(accents.get(i));
                            onStyleChange(mStylePref, value);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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

    private void increaseOkStatus() {
        mOkStatus++;
        if (mOkStatus != 2) {
            return;
        }

        mOkStatus = (byte) 0;
        new AlertDialog.Builder(getActivity())
            .setTitle(android.R.string.ok)
            .setPositiveButton(android.R.string.ok, null)
            .show();
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

            boolean isLight = UIUtils.isColorLight(wallpaperColor);
            Accent bestAccent = getBestAccent(accents, wallpaperColor, isLight);

            return new Style(bestAccent, isLight);
        }

        @Override
        public void onPostExecute(Style style) {
            mCallback.onDone(style);
        }

        private Accent getBestAccent(Accent[] accents, int wallpaperColor, boolean isLight) {
            int bestIndex = 0;
            double minDiff = Double.MAX_VALUE;
            StyleStatus targetStatus = isLight ? StyleStatus.LIGHT_ONLY : StyleStatus.DARK_ONLY;

            for (int i = 0; i < accents.length; i++) {
                double diff = diff(accents[i].getColor(), wallpaperColor);
                if (diff < minDiff && AccentUtils.isCompatible(targetStatus, accents[i])) {
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