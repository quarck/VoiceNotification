package org.qrck.voicenotifications

import android.content.Context


class BTCarModeStorage(private val ctx: Context) : PersistentStorageBase(ctx, PREFS_NAME) {

    private var triggerDevicesRaw by StringProperty("", "A")

    var triggerDevices: Set<String>
            get() = triggerDevicesRaw.split(',').toSet()
        set(value) {
            triggerDevicesRaw = value.joinToString (",")
        }

    companion object {
        const val PREFS_NAME: String = "bt_mode_state"
    }
}

val Context.btCarModeSettings: BTCarModeStorage
    get() = BTCarModeStorage(this)

