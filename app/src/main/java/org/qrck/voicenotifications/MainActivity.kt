package org.qrck.voicenotifications

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun launchNotificationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    fun onLaunchService(view: android.view.View) {
        launchNotificationSettings()
    }

    fun onLaunchBTConfig(view: android.view.View) {
        val intent = Intent(this, BluetoothDevicesActivity::class.java)
        startActivity(intent)
    }
}