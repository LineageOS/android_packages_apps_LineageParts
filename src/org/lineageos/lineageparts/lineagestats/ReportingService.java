/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.lineagestats;

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "LineageStats";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = getSystemService(JobScheduler.class);

        Context context = getApplicationContext();

        String deviceId = Utilities.getUniqueID(context);
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(context);
        String deviceCarrier = Utilities.getCarrier(context);
        String deviceCarrierId = Utilities.getCarrierId(context);

        final int lineageOldJobId = AnonymousStats.getLastJobId(context);
        final int lineageOrgJobId = AnonymousStats.getNextJobId(context);

        if (DEBUG) Log.d(TAG, "scheduling job id: " + lineageOrgJobId);

        PersistableBundle lineageBundle = new PersistableBundle();
        lineageBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        lineageBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        lineageBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        lineageBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        lineageBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        lineageBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);
        lineageBundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        // set job types
        lineageBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_LINEAGEORG);

        // schedule lineage stats upload
        js.schedule(new JobInfo.Builder(lineageOrgJobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(lineageBundle)
                .setPersisted(true)
                .build());

        // cancel old job in case it didn't run yet
        js.cancel(lineageOldJobId);

        // reschedule
        AnonymousStats.updateLastSynced(this);
        ReportingServiceManager.setAlarm(this);
    }
}
