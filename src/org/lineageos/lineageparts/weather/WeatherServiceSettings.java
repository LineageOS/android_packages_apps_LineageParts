/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2019 The LineageOS Project
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

package org.lineageos.lineageparts.weather;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lineageos.providers.LineageSettings;
import lineageos.providers.WeatherContract;
import lineageos.weatherservice.WeatherProviderService;

public class WeatherServiceSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private Context mContext;
    private Handler mHandler;
    private static final String TAG = WeatherServiceSettings.class.getSimpleName();

    private static final String PREFERENCE_GENERAL = "weather_general_settings";
    private static final String PREFERENCE_PROVIDERS = "weather_service_providers";
    private static final String PREFERENCE_TEMP_UNIT = "weather_temperature_unit";

    private static final float DISABLED_ALPHA = 0.4f;

    private PreferenceCategory mGeneralSettingsCategory;
    private PreferenceCategory mProvidersCategory;
    private ListPreference mTemperatureUnit;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.weather_settings);

        final PreferenceScreen ps = getPreferenceScreen();
        mGeneralSettingsCategory = ps.findPreference(PREFERENCE_GENERAL);
        mProvidersCategory = ps.findPreference(PREFERENCE_PROVIDERS);
        mTemperatureUnit = ps.findPreference(PREFERENCE_TEMP_UNIT);
        mTemperatureUnit.setOnPreferenceChangeListener(this);

        watch(LineageSettings.Secure.getUriFor(LineageSettings.Secure.WEATHER_PROVIDER_SERVICE));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        registerPackageMonitor();

        mTemperatureUnit.setValue(String.valueOf(getSelectedTemperatureUnit(mContext)));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPackageMonitor();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTemperatureUnit) {
            LineageSettings.Global.putInt(mContext.getContentResolver(),
                    LineageSettings.Global.WEATHER_TEMPERATURE_UNIT,
                    Integer.valueOf((String) newValue));
        }
        return true;
    }

    private void registerPackageMonitor() {
        mPackageMonitor.register(mContext, BackgroundThread.getHandler().getLooper(),
                UserHandle.ALL, true);
    }

    private void launchGetWeatherProviders() {
        startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.weather_settings_provider_url)))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void unregisterPackageMonitor() {
        mPackageMonitor.unregister();
    }

    private PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    };

    private static List<WeatherProviderServiceInfo> getInstalledServices(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(WeatherProviderService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfoList = pm.queryIntentServices(intent,
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        List<WeatherProviderServiceInfo> weatherProviderServiceInfos
                = new ArrayList<>(resolveInfoList.size());
        ComponentName activeService = getEnabledWeatherServiceProvider(context);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo.serviceInfo == null) continue;

            if (resolveInfo.serviceInfo.packageName == null
                    || resolveInfo.serviceInfo.name == null) {
                //Really?
                continue;
            }

            if (!resolveInfo.serviceInfo.permission.equals(
                    lineageos.platform.Manifest.permission.BIND_WEATHER_PROVIDER_SERVICE)) {
                continue;
            }
            WeatherProviderServiceInfo serviceInfo = new WeatherProviderServiceInfo();
            serviceInfo.componentName = new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            serviceInfo.isActive = serviceInfo.componentName.equals(activeService);
            serviceInfo.caption = resolveInfo.loadLabel(pm);
            serviceInfo.icon = resolveInfo.loadIcon(pm);
            serviceInfo.settingsComponentName = getSettingsComponent(pm, resolveInfo);

            weatherProviderServiceInfos.add(serviceInfo);
        }
        return weatherProviderServiceInfos;
    }

    private void updateAdapter() {
        final List<WeatherProviderServiceInfo> weatherProviderServiceInfos =
                getInstalledServices(getContext());

        final PreferenceScreen ps = getPreferenceScreen();
        if (!weatherProviderServiceInfos.isEmpty()) {
            if (ps.findPreference(PREFERENCE_GENERAL) == null) {
                ps.addPreference(mGeneralSettingsCategory);
            }
            if (ps.findPreference(PREFERENCE_PROVIDERS) == null) {
                ps.addPreference(mProvidersCategory);
            }

            mProvidersCategory.removeAll();
            for (WeatherProviderServiceInfo info : weatherProviderServiceInfos) {
                mProvidersCategory.addPreference(new WeatherProviderPreference(mContext, info));
            }

            Preference addServicePreference = new Preference(mContext);
            addServicePreference.setTitle(R.string.weather_settings_add_weather_provider);
            addServicePreference.setIcon(R.drawable.ic_menu_add);
            addServicePreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    launchGetWeatherProviders();
                    return false;
                }
            });
            mProvidersCategory.addPreference(addServicePreference);

        } else {
            ps.removePreference(mGeneralSettingsCategory);
            ps.removePreference(mProvidersCategory);
        }

    }

    /**
     * Gets the currently selected temperature unit.
     * If none is selected yet, returns a unit appropriate for the current locale
     */
    public static int getSelectedTemperatureUnit(Context context) {
        int tempUnit = LineageSettings.Global.getInt(context.getContentResolver(),
                LineageSettings.Global.WEATHER_TEMPERATURE_UNIT, -1);
        if (tempUnit != -1) {
            return tempUnit;
        }

        Locale locale = context.getResources().getConfiguration().locale;
        boolean useFahrenheit = locale.equals(Locale.US)
                || locale.toString().equals("en_BS")  // Bahamas
                || locale.toString().equals("en_BZ")  // Belize
                || locale.toString().equals("en_KY"); // Cayman Islands
        return useFahrenheit
                ? WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT
                : WeatherContract.WeatherColumns.TempUnit.CELSIUS;
    }

    private class WeatherProviderPreference extends Preference {
        private WeatherProviderServiceInfo mInfo;
        private RadioButton mRadioButton;
        private View mSettingsButton;
        private Context mContext;

        public WeatherProviderPreference(Context context, WeatherProviderServiceInfo info) {
            super(context);
            mInfo = info;
            mContext = context;

            setLayoutResource(R.layout.weather_service_provider_info_row);
            setTitle(mInfo.caption);
            setIcon(mInfo.icon);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);

            mRadioButton = (RadioButton) holder.findViewById(R.id.radio);
            mRadioButton.setChecked(mInfo.isActive);
            mRadioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    holder.itemView.onTouchEvent(event);
                    return false;
                }
            });

            boolean showSettings = mInfo.settingsComponentName != null;
            View settingsDivider = holder.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);

            mSettingsButton = holder.findViewById(R.id.settings);
            mSettingsButton.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            mSettingsButton.setAlpha(mInfo.isActive ? 1f : DISABLED_ALPHA);
            mSettingsButton.setEnabled(mInfo.isActive);
            mSettingsButton.setFocusable(mInfo.isActive);
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchSettingsActivity(mInfo);
                }
            });
        }

        @Override
        public void performClick() {
            if (isActiveProvider()) {
                return;
            }
            setActiveWeatherProviderService();
            notifyChanged();
        }

        private boolean isActiveProvider() {
            return mInfo.isActive;
        }

        public void setActiveState(boolean active) {
            mInfo.isActive = active;
            mRadioButton.setChecked(active);

            boolean hasSettings = mInfo.settingsComponentName != null;
            if (hasSettings) {
                mSettingsButton.setAlpha(mInfo.isActive ? 1f : DISABLED_ALPHA);
                mSettingsButton.setEnabled(mInfo.isActive);
                mSettingsButton.setFocusable(mInfo.isActive);
            }
        }

        private void launchSettingsActivity(WeatherProviderServiceInfo info) {
            if (info != null && info.settingsComponentName != null) {
                try {
                    mContext.startActivity(new Intent().setComponent(info.settingsComponentName));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(mContext,
                            R.string.weather_settings_activity_not_found,
                            Toast.LENGTH_LONG)
                            .show();
                    Log.w(TAG, info.settingsComponentName + " not found");
                }
            }
        }

        private void setActiveWeatherProviderService() {
            if (!mInfo.isActive) {
                markAsActiveProvider();
                mInfo.isActive = true;
                LineageSettings.Secure.putString(mContext.getContentResolver(),
                        LineageSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        mInfo.componentName.flattenToString());
            }
        }

        private void markAsActiveProvider() {
            // Check for current active provider
            PreferenceCategory providersCategory = findPreferenceInHierarchy(
                    WeatherServiceSettings.PREFERENCE_PROVIDERS);
            if (providersCategory != null) {
                final int count = providersCategory.getPreferenceCount();
                for (int index = 0; index < count; index++) {
                    Preference p = providersCategory.getPreference(index);
                    if (p instanceof WeatherProviderPreference) {
                        WeatherProviderPreference preference = (WeatherProviderPreference) p;
                        if (preference.isActiveProvider()) {
                            preference.setActiveState(false);
                            break;
                        }
                    }
                }
            }
            // Mark this provider as active
            setActiveState(true);
        }
    }

    private static ComponentName getSettingsComponent(PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null
                || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        XmlResourceParser parser = null;
        Exception caughtException = null;

        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm,
                    WeatherProviderService.SERVICE_META_DATA);
            if (parser == null) {
                Log.w(TAG, "Can't find " + WeatherProviderService.SERVICE_META_DATA + " meta-data");
                return null;
            }
            Resources res =
                    pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"weather-provider-service".equals(nodeName)) {
                Log.w(TAG, "Meta-data does not start with weather-provider-service tag");
                return null;
            }
            //Will use Dream styleable for now, it has the attribute we need
            TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.Dream);
            cn = sa.getString(com.android.internal.R.styleable.Dream_settingsActivity);
            sa.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e) {
            caughtException = e;
        } catch (XmlPullParserException e) {
            caughtException = e;
        } finally {
            if (parser != null) parser.close();
        }
        if (caughtException != null) {
            Log.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName,
                    caughtException);
            return null;
        }
        if (cn != null && cn.indexOf('/') < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        return cn == null ? null : ComponentName.unflattenFromString(cn);
    }

    private static ComponentName getEnabledWeatherServiceProvider(Context context) {
        String activeWeatherServiceProvider = LineageSettings.Secure.getString(
                context.getContentResolver(), LineageSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (activeWeatherServiceProvider == null) return null;
        return ComponentName.unflattenFromString(activeWeatherServiceProvider);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_weather_state, contentRoot, false);
        TextView emptyTextView = (TextView) emptyView.findViewById(R.id.message);
        emptyTextView.setText(R.string.weather_settings_no_services_prompt);

        Button addProviderButton = (Button) emptyView.findViewById(R.id.add_weather_provider);
        addProviderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGetWeatherProviders();
            }
        });

        contentRoot.addView(emptyView);
        setEmptyView(emptyView);
    }

    private static class WeatherProviderServiceInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        ComponentName componentName;
        public ComponentName settingsComponentName;
    }

    public static final SummaryProvider SUMMARY_PROVIDER = new SummaryProvider() {
        @Override
        public String getSummary(Context context, String key) {
            final List<WeatherProviderServiceInfo> infos = getInstalledServices(context);
            if (infos != null && infos.size() > 0) {
                for (WeatherProviderServiceInfo info : infos) {
                    if (info.isActive) {
                        Log.d(TAG, "lineageparts: found active provider: " + info.caption);
                        return String.valueOf(info.caption);
                    }
                }
            }
            return context.getString(R.string.weather_settings_no_services_summary);
        }
    };
}
