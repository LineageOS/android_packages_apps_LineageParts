/*
 * Copyright (C) 2018-2021 The LineageOS Project
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
package org.lineageos.lineageparts.networkrestriction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivitySettingsManager;
import android.net.NetworkPolicyManager;
import android.util.Log;

import java.util.Set;

/**
 * Implementation for a BroadcastReceiver which sets the permissions after the
 * installation of an app.
 */
public class AppInstalledReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            Log.i(NetworkRestrictionPreferences.TAG, "PACKAGE_ADDED received: " + packageName);
        
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                NetworkRestrictionPreferences.TAG, Context.MODE_PRIVATE);
        
            boolean blockNewApps = sharedPrefs.getBoolean(
                NetworkRestrictionPreferences.SHOW_BLOCK_NEW_APPS_PREF, false);  
            
            if(blockNewApps && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
            {
                PackageManager pkgMgr = context.getPackageManager();
                NetworkPolicyManager netMgr = NetworkPolicyManager.from(context);
                    
                try {
                    int uid = pkgMgr.getPackageUid(packageName, 0);
                    
                    netMgr.setUidPolicy(uid,
                        NetworkPolicyManager.POLICY_REJECT_WIFI |
                        NetworkPolicyManager.POLICY_REJECT_CELLULAR |
                        NetworkPolicyManager.POLICY_REJECT_VPN);
                    setRestrictedNetworking(context, uid);
                } catch (Exception e) {
                    Log.e(NetworkRestrictionPreferences.TAG, 
                        "Could't set the network policy for: " + packageName, e);
                }
            }
        }
    }

    /**
     * Sets the app as network restricted
     * @param context Context.
     * @param uid uid of the app.
     */
    private void setRestrictedNetworking(Context context, int uid) {
        // To restrict a uid, we have to remove it from the list
        Set<Integer> uids =
                ConnectivitySettingsManager.getUidsAllowedOnRestrictedNetworks(context);
        uids.remove(uid);
        ConnectivitySettingsManager.setUidsAllowedOnRestrictedNetworks(context, uids);
    }
}
