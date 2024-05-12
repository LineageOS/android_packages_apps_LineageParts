/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.Preference
import kotlin.reflect.cast
import kotlin.reflect.safeCast
import lineageos.accessories.AccessoriesInterface
import lineageos.accessories.IAccessoriesCallback
import lineageos.accessories.IAccessory
import org.lineageos.lineageparts.PartsActivity
import org.lineageos.lineageparts.SettingsPreferenceFragment
import org.lineageos.lineageparts.R

class AccessoriesSettings : SettingsPreferenceFragment() {
    private val partsActivity
        get() = PartsActivity::class.cast(requireActivity())

    private val accessoriesInterface by lazy {
        AccessoriesInterface.getInstance(requireContext())
    }

    private val accessoriesCallback = object : IAccessoriesCallback.Stub() {
        override fun onAccessoryAdded(accessory: IAccessory?) {
            accessory?.let {
                val accessoryInfo = it.accessoryInfo
                val accessoryId = accessoryInfo.id

                getAccessoryPreferenceForId(accessoryId)?.also {
                    Log.w(LOG_TAG, "Accessory $accessoryId already added")
                } ?: run {
                    val preference = AccessoryItemPreference(requireContext(), it, accessoryId)
                    preferenceScreen.addPreference(preference)
                }
            }
        }

        override fun onAccessoryRemoved(accessory: IAccessory?) {
            accessory?.let {
                val accessoryId = it.accessoryInfo.id

                getAccessoryPreferenceForId(accessoryId)?.let { accessoryItemPreference ->
                    preferenceScreen.removePreference(accessoryItemPreference)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.empty_preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        activity.actionBar?.setTitle(R.string.accessories_title)

        accessoriesInterface.registerCallback(accessoriesCallback)
    }

    override fun onDestroyView() {
        accessoriesInterface.unregisterCallback(accessoriesCallback)

        preferenceScreen?.removeAll()

        super.onDestroyView()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        AccessoryItemPreference::class.safeCast(preference)?.let {
            partsActivity.switchToFragment(
                AccessorySettings(),
                AccessorySettings.createArgs(it.accessoryId),
                0,
                null,
                true,
            )

            return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    private fun getAccessoryPreferenceForId(accessoryId: String) =
        preferenceScreen?.findPreference<AccessoryItemPreference>(accessoryId)

    companion object {
        private const val LOG_TAG = "AccessoriesSettings"
    }
}
