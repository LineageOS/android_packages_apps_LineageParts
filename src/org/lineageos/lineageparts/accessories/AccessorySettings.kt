/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
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
            accessoryInfo?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    this@AccessorySettings.accessoryInfo = it

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
        }

        override fun onAccessoryStatusUpdated(status: Byte) {
            this@AccessorySettings.accessoryStatus = status

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
            }
        }

        override fun onBatteryInfoUpdated(batteryInfo: BatteryInfo?) {
            this@AccessorySettings.batteryInfo = batteryInfo

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

    private lateinit var accessory: IAccessory

    private lateinit var accessoryInfo: AccessoryInfo
    private var accessoryStatus by Delegates.notNull<Byte>()
    private var batteryInfo: BatteryInfo? = null

    private val enabledOnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Disable the switch, the AIDL will re-enable it with onAccessoryStatusUpdated()
            enabledMainSwitchPreference.isEnabled = false

            accessory.setEnabled(isChecked)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.accessory_settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enabledMainSwitchPreference.addOnSwitchChangeListener(enabledOnCheckedChangeListener)

        accessoriesInterface.registerCallback(accessoriesCallback)

        accessory.registerCallback(accessoryCallback)
    }

    override fun onDestroyView() {
        accessory.unregisterCallback(accessoryCallback)

        accessoriesInterface.unregisterCallback(accessoriesCallback)

        super.onDestroyView()
    }

    companion object {
        fun create(accessory: IAccessory) = AccessorySettings().apply {
            this.accessory = accessory
        }
    }
}
