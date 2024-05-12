/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.MainSwitchPreference
import kotlin.properties.Delegates
import kotlinx.coroutines.launch
import lineageos.accessories.AccessoriesInterface
import lineageos.accessories.AccessoryInfo
import lineageos.accessories.AccessoryStatus
import lineageos.accessories.AccessoryType
import lineageos.accessories.BatteryInfo
import lineageos.accessories.IAccessoriesCallback
import lineageos.accessories.IAccessory
import lineageos.accessories.IAccessoryCallback
import org.lineageos.lineageparts.R
import org.lineageos.lineageparts.SettingsPreferenceFragment

class AccessorySettings : SettingsPreferenceFragment() {
    // Settings
    private val enabledMainSwitchPreference
        get() = findPreference<MainSwitchPreference>("enabled")!!
    private val namePreference
        get() = findPreference<Preference>("name")!!
    private val typePreference
        get() = findPreference<Preference>("type")!!
    private val statusPreference
        get() = findPreference<Preference>("status")!!
    private val batteryInfoPreference
        get() = findPreference<Preference>("battery_info")!!
    private val idPreference
        get() = findPreference<FooterPreference>("id")!!

    private val accessoriesInterface by lazy {
        AccessoriesInterface.getInstance(requireContext())
    }

    private val accessoriesCallback = object : IAccessoriesCallback.Stub() {
        override fun onAccessoryAdded(accessory: IAccessory?) {}

        override fun onAccessoryRemoved(accessory: IAccessory?) {
            accessory?.let {
                if (it == this@AccessorySettings.accessory) {
                    lifecycleScope.launch {
                        finishFragment()
                    }
                }
            }
        }
    }

    private val accessoryCallback = object : IAccessoryCallback.Stub() {
        override fun onAccessoryInfoUpdated(accessoryInfo: AccessoryInfo?) {
            accessoryInfo?.also {
                this@AccessorySettings.accessoryInfo = it

                view?.also { v ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        

                        namePreference.summary = it.displayName

                        typePreference.setSummary(
                            when (it.type) {
                                AccessoryType.KEYBOARD -> R.string.accessories_accessory_type_keyboard
                                AccessoryType.ACTIVE_PEN ->
                                    R.string.accessories_accessory_type_active_pen
                                else -> R.string.accessories_accessory_type_unknown
                            }
                        )
                        typePreference.setIcon(
                            when (it.type) {
                                AccessoryType.KEYBOARD -> R.drawable.ic_keyboard
                                AccessoryType.ACTIVE_PEN -> R.drawable.ic_ink_pen
                                else -> R.drawable.ic_question_mark
                            }
                        )

                        idPreference.isVisible = it.displayId.isNotEmpty()
                        idPreference.summary = resources.getString(
                            R.string.accessories_accessory_id_title,
                            it.displayId,
                        )
                    }
                }
            } ?: throw IllegalStateException("AccessoryInfo is null")
        }

        override fun onAccessoryStatusUpdated(status: Byte) {
            this@AccessorySettings.accessoryStatus = status

            view?.also { v ->
                viewLifecycleOwner.lifecycleScope.launch {
                    enabledMainSwitchPreference.isEnabled = status in listOf(
                        AccessoryStatus.DISABLED,
                        AccessoryStatus.ENABLED,
                    )

                    enabledMainSwitchPreference.removeOnSwitchChangeListener(
                        enabledOnCheckedChangeListener
                    )
                    enabledMainSwitchPreference.isChecked = status == AccessoryStatus.ENABLED
                    enabledMainSwitchPreference.addOnSwitchChangeListener(
                        enabledOnCheckedChangeListener
                    )

                    statusPreference.setSummary(
                        when (accessoryStatus) {
                            AccessoryStatus.DISCONNECTED ->
                                R.string.accessories_accessory_status_disconnected
                            AccessoryStatus.CHARGING -> R.string.accessories_accessory_status_charging
                            AccessoryStatus.DISABLED -> R.string.accessories_accessory_status_disabled
                            AccessoryStatus.ENABLED -> R.string.accessories_accessory_status_enabled
                            else -> R.string.accessories_accessory_status_unknown
                        }
                    )
                }
            }
        }

        override fun onBatteryInfoUpdated(batteryInfo: BatteryInfo?) {
            this@AccessorySettings.batteryInfo = batteryInfo

            view?.also { v ->
                viewLifecycleOwner.lifecycleScope.launch {
                    batteryInfo?.also {
                        batteryInfoPreference.summary = BatteryUtils.formatBatteryInfo(
                            requireContext(), it
                        )
                        batteryInfoPreference.setIcon(BatteryUtils.getDrawableResIdForStatus(it))
                    }

                    batteryInfoPreference.isVisible = batteryInfo != null
                }
            }
        }
    }

    // Arguments
    private val accessoryId by lazy {
        requireArguments().getString(KEY_ACCESSORY_ID)!!
    }

    private lateinit var accessory: IAccessory

    private lateinit var accessoryInfo: AccessoryInfo
    private var accessoryStatus by Delegates.notNull<Byte>()
    private var batteryInfo: BatteryInfo? = null

    private val enabledOnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Disable the switch, the AIDL will re-enable it with onAccessoryStatusUpdated()
            enabledMainSwitchPreference.isEnabled = false

            try {
                accessory.setEnabled(isChecked)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    R.string.accessories_accessory_set_enabled_error,
                    Toast.LENGTH_SHORT
                ).show()

                enabledMainSwitchPreference.isEnabled = true
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.accessory_settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accessories = accessoriesInterface.getAccessories()
        accessory = accessories.firstOrNull { it -> it.accessoryInfo.id == accessoryId } ?: run {
            finishFragment()
            return
        }

        enabledMainSwitchPreference.addOnSwitchChangeListener(enabledOnCheckedChangeListener)

        // Hide the battery info preference until we get the first update
        batteryInfoPreference.isVisible = false

        accessoriesInterface.registerCallback(accessoriesCallback)

        accessory.registerCallback(accessoryCallback)
    }

    override fun onDestroyView() {
        accessory.unregisterCallback(accessoryCallback)

        accessoriesInterface.unregisterCallback(accessoriesCallback)

        super.onDestroyView()
    }

    companion object {
        private val KEY_ACCESSORY_ID = "accessoryId"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param accessoryId The [IAccessory] to display's ID
         */
        fun createArgs(
            accessoryId: String,
        ) = bundleOf(
            KEY_ACCESSORY_ID to accessoryId,
        )
    }
}
