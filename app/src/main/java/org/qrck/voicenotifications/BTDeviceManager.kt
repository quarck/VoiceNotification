package org.qrck.voicenotifications

import android.bluetooth.*
import android.bluetooth.BluetoothProfile.GATT
import android.content.Context
import android.media.AudioDeviceInfo

import android.media.AudioManager

import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.getSystemService




data class BTDeviceSummary(val name: String, val address: String, val currentlyConnected: Boolean)


class BTDeviceManager(val ctx: Context){

    private val manager: BluetoothManager? by lazy { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    private val adapter: BluetoothAdapter? by lazy { manager?.getAdapter() }

    public val storage: BTCarModeStorage by lazy { ctx.btCarModeSettings }

    public val pairedDevices: List<BTDeviceSummary>?
        get() = adapter
                ?.bondedDevices
                ?.map { BTDeviceSummary(it.name, it.address, isDeviceConnected(it))}
                ?.toList()

    public fun isDeviceConnected(device: BluetoothDevice) =
            manager?.getConnectionState(device, BluetoothProfile.GATT) ==
                    BluetoothProfile.STATE_CONNECTED

    public val anyTriggerDevicesConnected: Boolean
        get() {
            val triggers = storage.triggerDevices
            if (triggers.isEmpty())
                return false

            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                val addr = dev.address
                if (addr.isBlank())
                    continue
                if (addr in triggers)
                    return true
            }

            return false
        }
}