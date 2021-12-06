package org.qrck.voicenotifications

import android.bluetooth.*
import android.content.Context

data class BTDeviceSummary(val name: String, val address: String, val currentlyConnected: Boolean)


class BTDeviceManager(val ctx: Context){

    private val adapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val manager: BluetoothManager? by lazy { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    public val storage: BTCarModeStorage by lazy { ctx.btCarModeSettings }

    public val pairedDevices: List<BTDeviceSummary>?
        get() = adapter
                ?.bondedDevices
                ?.map { BTDeviceSummary(it.name, it.address, isDeviceConnected(it))}
                ?.toList()

    public fun isDeviceConnected(device: BluetoothDevice) =
            manager?.getConnectionState(device, android.bluetooth.BluetoothProfile.GATT) ==
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED

    public fun isDeviceConnected(address: String): Boolean =
            adapter?.getRemoteDevice(address)?.let { isDeviceConnected(it) } ?: false

    public fun isDeviceConnected(dev: BTDeviceSummary): Boolean =
            adapter?.getRemoteDevice(dev.address)?.let { isDeviceConnected(it) } ?: false

    public val anyTriggerDevicesConnected: Boolean
        get() = storage.triggerDevices.any { isDeviceConnected(it) }
}