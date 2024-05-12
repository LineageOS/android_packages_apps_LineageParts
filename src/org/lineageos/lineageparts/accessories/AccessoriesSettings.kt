/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.util.Log
import android.view.View
import lineageos.accessories.AccessoriesInterface
import lineageos.accessories.IAccessoriesCallback
import lineageos.accessories.IAccessory
import org.lineageos.lineageparts.SettingsPreferenceFragment
import org.lineageos.lineageparts.search.Searchable
import org.lineageos.lineageparts.R

class AccessoriesSettings : SettingsPreferenceFragment(), Searchable {
    private val accessoriesInterface by lazy {
        AccessoriesInterface.getInstance(requireContext())
    }

    private val accessoriesCallback = object : IAccessoriesCallback.Stub() {
        override fun onAccessoryAdded(accessory: IAccessory?) {
            accessory?.let {
                val accessoryInfo = it.accessoryInfo
                val accessoryId = accessoryInfo.id

                preferenceScreen?.let { preferenceScreen ->
                    preferenceScreen.findPreference<AccessoryItemPreference>(accessoryId)?.also {
                        Log.w(LOG_TAG, "Accessory $accessoryId already added")
                    } ?: run {
                        val preference = AccessoryItemPreference(requireContext(), it, accessoryId)
                        preferenceScreen.addPreference(preference)
                    }
                }
            }
        }

        override fun onAccessoryRemoved(accessory: IAccessory?) {
            accessory?.let {
                val id = it.accessoryInfo.id

                preferenceScreen?.removePreferenceRecursively(id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        addPreferencesFromResource(R.xml.empty_preferences)
        activity.actionBar?.setTitle(R.string.accessories_title)

        accessoriesInterface.registerCallback(accessoriesCallback)
    }

    override fun onDestroyView() {
        accessoriesInterface.unregisterCallback(accessoriesCallback)

        preferenceScreen?.removeAll()

        super.onDestroyView()
    }

    companion object {
        private const val LOG_TAG = "AccessoriesSettings"
    }
}
