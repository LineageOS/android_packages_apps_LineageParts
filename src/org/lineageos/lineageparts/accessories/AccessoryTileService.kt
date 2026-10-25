/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.accessories

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import lineageos.accessories.AccessoriesInterface
import lineageos.accessories.AccessoryInfo
import lineageos.accessories.AccessoryStatus
import lineageos.accessories.AccessoryType
import lineageos.accessories.BatteryInfo
import lineageos.accessories.DataLinkType
import lineageos.accessories.IAccessoriesCallback
import lineageos.accessories.IAccessory
import lineageos.accessories.IAccessoryCallback
import lineageos.app.LineageContextConstants
import org.lineageos.lineageparts.utils.GenericUtils

/**
 * Quick settings tile toggling all the accessories of the given type.
 */
abstract class AccessoryTileService(private val accessoryType: Byte) : TileService() {
    private val handler = Handler(Looper.getMainLooper())

    private val accessoriesInterface by lazy {
        AccessoriesInterface.getInstance(this)
    }

    private var listening = false

    // Accessory ID -> accessory, with its latest info and status
    private val accessories = mutableMapOf<String, TrackedAccessory>()

    private inner class TrackedAccessory(val accessory: IAccessory, var info: AccessoryInfo) {
        // Unknown until the first status update
        var status: Byte? = null

        val callback = object : IAccessoryCallback.Stub() {
            override fun onAccessoryInfoUpdated(accessoryInfo: AccessoryInfo?) {
                accessoryInfo?.let {
                    handler.post {
                        info = it
                        updateTile()
                    }
                }
            }

            override fun onAccessoryStatusUpdated(status: Byte) {
                handler.post {
                    this@TrackedAccessory.status = status
                    updateTile()
                }
            }

            override fun onBatteryInfoUpdated(batteryInfo: BatteryInfo?) {}
        }
    }

    private val accessoriesCallback = object : IAccessoriesCallback.Stub() {
        override fun onAccessoryAdded(accessory: IAccessory?) {
            val info = accessory?.accessoryInfo ?: return
            if (info.type != accessoryType) {
                return
            }

            handler.post {
                if (!listening || accessories.containsKey(info.id)) {
                    return@post
                }
                val trackedAccessory = TrackedAccessory(accessory, info)
                accessories[info.id] = trackedAccessory
                accessory.registerCallback(trackedAccessory.callback)
            }
        }

        override fun onAccessoryRemoved(accessory: IAccessory?) {
            handler.post {
                accessories.values.removeIf { it.accessory.asBinder() == accessory?.asBinder() }
                updateTile()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()

        listening = true
        accessoriesInterface.registerCallback(accessoriesCallback)
    }

    override fun onStopListening() {
        listening = false
        accessoriesInterface.unregisterCallback(accessoriesCallback)
        accessories.values.forEach {
            try {
                it.accessory.unregisterCallback(it.callback)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed to unregister callback", e)
            }
        }
        accessories.clear()

        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    private fun toggle() {
        val toggleable = accessories.values.filter {
            it.info.supportsToggling && it.status != null &&
                    it.status != AccessoryStatus.DISCONNECTED
        }
        if (toggleable.isEmpty()) {
            return
        }

        val enabled = toggleable.none { it.status == AccessoryStatus.ENABLED }
        toggleable.forEach {
            try {
                it.accessory.setEnabled(enabled)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to toggle ${it.info.id}", e)
            }
        }

        if (enabled && toggleable.any { usesBluetooth(it.info) } && isBluetoothDisabled()) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
    }

    // A pen only reports its address once attached, so don't rely on the data link alone
    private fun usesBluetooth(info: AccessoryInfo) =
        info.dataLink?.type == DataLinkType.BLUETOOTH || info.type == AccessoryType.ACTIVE_PEN

    private fun isBluetoothDisabled() =
        getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == false

    private fun updateTile() {
        val tile = qsTile ?: return

        // Keep the last state until every accessory reported its status
        if (!listening || accessories.values.any { it.status == null }) {
            return
        }

        val connected = accessories.values.filter { it.status != AccessoryStatus.DISCONNECTED }
        tile.state = when {
            connected.isEmpty() -> Tile.STATE_UNAVAILABLE
            connected.any { it.status == AccessoryStatus.ENABLED } -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    companion object {
        private const val LOG_TAG = "AccessoryTileService"

        /**
         * Show the tiles only for the accessory types the device has.
         */
        @JvmStatic
        fun updateTilesAvailability(context: Context) {
            if (!context.packageManager.hasSystemFeature(
                    LineageContextConstants.Features.ACCESSORIES
                )
            ) {
                return
            }

            val types = try {
                AccessoriesInterface.getInstance(context).accessories.map { it.accessoryInfo.type }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to get the accessories", e)
                return
            }

            // Tiles are never hidden again, an accessory might just not be registered yet
            if (AccessoryType.KEYBOARD in types) {
                GenericUtils.setComponentEnabled(
                    context, KeyboardTileService::class.java.name, true
                )
            }
            if (AccessoryType.ACTIVE_PEN in types) {
                GenericUtils.setComponentEnabled(context, StylusTileService::class.java.name, true)
            }
        }
    }
}

class KeyboardTileService : AccessoryTileService(AccessoryType.KEYBOARD)

class StylusTileService : AccessoryTileService(AccessoryType.ACTIVE_PEN)
