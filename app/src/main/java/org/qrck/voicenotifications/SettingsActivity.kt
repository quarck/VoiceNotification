package org.qrck.voicenotifications

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchEnableSamsungTTS: SwitchMaterial
    private lateinit var switchEnableMediaStream: SwitchMaterial

    private lateinit var settings: PersistentState


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = PersistentState(this)

        switchEnableSamsungTTS = findViewById(R.id.switchEnableSamsungTTS)
        switchEnableMediaStream = findViewById(R.id.switchUseMediaStream)

        switchEnableSamsungTTS.isChecked = settings.enableSamsungTTS
        switchEnableMediaStream.isChecked = settings.enableMediaStream

        switchEnableSamsungTTS.setOnClickListener {
            settings.enableSamsungTTS = switchEnableSamsungTTS.isChecked
        }
        switchEnableMediaStream.setOnClickListener {
            settings.enableMediaStream = switchEnableMediaStream.isChecked
        }

    }
}