/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.os.Bundle
import android.view.View
import lineageos.accessories.IAccessory
import org.lineageos.lineageparts.SettingsPreferenceFragment

class AccessorySettings : SettingsPreferenceFragment() {
    private lateinit var accessory: IAccessory

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        activity.actionBar?.title = accessory.accessoryInfo.displayName
    }

    companion object {
        fun create(accessory: IAccessory) = AccessorySettings().apply {
            this.accessory = accessory
        }
    }
}
