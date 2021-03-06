package org.qrck.voicenotifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        updateView()

        // wherever we open this view - we stop playing the TTS
        PlayTTSService.stopActiveTTS(this)
    }

    private fun updateView() {
        val cancelMuteBtn = findViewById<Button>(R.id.buttonCancelMute)
        val mutedForMins = (PersistentState(this).muteUntil - System.currentTimeMillis()) / 60_000.0
        if ( mutedForMins > 0.0) {
            cancelMuteBtn.visibility = View.VISIBLE
            cancelMuteBtn.text = "Cancel mute (${mutedForMins.roundToInt()}mins)"
        } else {
            cancelMuteBtn.visibility = View.GONE
        }
    }

    fun onLaunchService(view: android.view.View) =
        startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    private fun muteFor(durationMillis: Long) {
        val state = PersistentState(this)
        state.muteUntil = System.currentTimeMillis() + durationMillis
        updateView()
    }

    fun mute8Hours(view: android.view.View) = muteFor(8 * 3600 * 1000L)

    fun muteOneHour(view: android.view.View) = muteFor(3600 * 1000L)

    fun mute30Mins(view: android.view.View) = muteFor(1800 * 1000L)

    fun mute15Mins(view: android.view.View) = muteFor(900 * 1000L)

    fun cancelMute(view: android.view.View) = muteFor(-1)

    fun onLaunchSettings(view: android.view.View) =
        startActivity(Intent(this, SettingsActivity::class.java))

    fun onLaunchDevices(view: android.view.View) =
        startActivity(Intent(this, BluetoothDevicesActivity::class.java))

    fun onLaunchApps(view: android.view.View) =
        startActivity(Intent(this, AppListActivity::class.java))
}