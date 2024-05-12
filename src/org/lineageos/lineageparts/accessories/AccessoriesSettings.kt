/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.PreferenceCategory
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

    private var accessoriesPreferenceCategory: PreferenceCategory? = null

    private val accessoriesCallback = object : IAccessoriesCallback.Stub() {
        override fun onAccessoryAdded(accessory: IAccessory?) {
            accessory?.let {
                val accessoryInfo = it.accessoryInfo
                val id = accessoryInfo.id

                accessoriesPreferenceCategory?.let { accessoriesPreferenceCategory ->
                    accessoriesPreferenceCategory.findPreference<AccessoryItemPreference>(id)?.also {
                        Log.w(LOG_TAG, "Accessory $id already added")
                    } ?: run {
                        val preference = AccessoryItemPreference(requireContext()).apply {
                            this@apply.accessory = it
                        }
                        accessoriesPreferenceCategory.addPreference(preference)
                    }
                }
            }
        }

        override fun onAccessoryRemoved(accessory: IAccessory?) {
            accessory?.let {
                val id = it.accessoryInfo.id

                accessoriesPreferenceCategory?.removePreferenceRecursively(id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val context = requireContext()

        addPreferencesFromResource(R.xml.empty_preferences)
        activity.actionBar?.setTitle(R.string.accessories_title)

        val preferenceScreen = preferenceScreen

        accessoriesPreferenceCategory = PreferenceCategory(context).apply {
            setTitle(R.string.accessories_title)
        }.also {
            preferenceScreen.addPreference(it)
        }

        accessoriesInterface.registerCallback(accessoriesCallback)
    }

    override fun onDestroyView() {
        accessoriesInterface.unregisterCallback(accessoriesCallback)
        accessoriesPreferenceCategory?.removeAll()
        accessoriesPreferenceCategory = null

        super.onDestroyView()
    }

    companion object {
        private const val LOG_TAG = "AccessoriesSettings"
    }
}
