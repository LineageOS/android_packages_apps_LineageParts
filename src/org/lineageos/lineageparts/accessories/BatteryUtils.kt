/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import lineageos.accessories.BatteryInfo
import lineageos.accessories.BatteryStatus
import org.lineageos.lineageparts.R

object BatteryUtils {
    private const val LOG_TAG = "BatteryUtils"

    private data class BatteryLevel(
        @DrawableRes val notChargingDrawableResId: Int,
        @DrawableRes val chargingDrawableResId: Int,
    )

    /**
     * (percentage / 12.5) to [BatteryLevel]
     */
    private val batteryLevels = arrayOf(
        BatteryLevel(
            R.drawable.ic_battery_0_bar,
            R.drawable.ic_battery_charging_full // Actually an empty one
        ),
        BatteryLevel(
            R.drawable.ic_battery_1_bar,
            R.drawable.ic_battery_charging_20
        ),
        BatteryLevel(
            R.drawable.ic_battery_2_bar,
            R.drawable.ic_battery_charging_30
        ),
        BatteryLevel(
            R.drawable.ic_battery_3_bar,
            R.drawable.ic_battery_charging_50
        ),
        BatteryLevel(
            R.drawable.ic_battery_4_bar,
            R.drawable.ic_battery_charging_60
        ),
        BatteryLevel(
            R.drawable.ic_battery_5_bar,
            R.drawable.ic_battery_charging_80
        ),
        BatteryLevel(
            R.drawable.ic_battery_6_bar,
            R.drawable.ic_battery_charging_90
        ),
        BatteryLevel(
            R.drawable.ic_battery_full,
            R.drawable.ic_battery_full
        ),
    )

    private val stringResIdForStatus = mapOf(
        BatteryStatus.UNKNOWN to R.string.accessories_battery_status_unknown,
        BatteryStatus.CHARGING to R.string.accessories_battery_status_charging,
        BatteryStatus.DISCHARGING to R.string.accessories_battery_status_discharging,
        BatteryStatus.NOT_CHARGING to R.string.accessories_battery_status_not_charging,
    )

    fun formatBatteryInfo(context: Context, batteryInfo: BatteryInfo) = with(context.resources) {
        val batteryStatusStringResId =
            stringResIdForStatus[batteryInfo.status] ?: R.string.accessories_battery_status_unknown

        batteryInfo.levelPercentage.takeIf { it in 0..100 }?.let { level ->
            getString(
                R.string.accessories_accessory_battery_info_summary,
                getString(batteryStatusStringResId),
                level
            )
        } ?: getString(batteryStatusStringResId)
    }

    fun getDrawableResIdForStatus(batteryInfo: BatteryInfo) = when {
        batteryInfo.status != BatteryStatus.UNKNOWN && batteryInfo.levelPercentage in 0..100 ->
            batteryLevels[(batteryInfo.levelPercentage / 100f * 7).toInt()].let {
                when (batteryInfo.status) {
                    BatteryStatus.CHARGING -> it.chargingDrawableResId
                    BatteryStatus.DISCHARGING,
                    BatteryStatus.NOT_CHARGING -> it.notChargingDrawableResId

                    else -> {
                        Log.wtf(LOG_TAG, "Unknown BatteryStatus: $batteryInfo.status")
                        null
                    }
                }
            }

        else -> null
    } ?: R.drawable.ic_battery_unknown
}
