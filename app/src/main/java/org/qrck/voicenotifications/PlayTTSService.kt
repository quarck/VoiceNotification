package org.qrck.voicenotifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.IBinder
import android.widget.EditText

import android.speech.tts.TextToSpeech
import java.util.*

class TTSSpeaker(val service: PlayTTSService)
    :  android.speech.tts.UtteranceProgressListener()
    , TextToSpeech.OnInitListener
{
    var ttsObject: TextToSpeech? = null
    var speechQueue = mutableListOf<String>()

    private val russianLetters = setOf(
        'а', 'б', 'в', 'г', 'д',
        'е', 'ё', 'ж', 'з', 'и',
        'й', 'к', 'л', 'м', 'н',
        'о', 'п', 'р', 'с', 'т',
        'у', 'ф', 'х', 'ц', 'ч',
        'ш', 'щ', 'ъ', 'ы', 'ь',
        'э', 'ю', 'я',
    )

    private fun isRussianText(s: String): Boolean {
        var cntTotalLetters = 0
        var cntRussianLetters = 0

        for (c in s.lowercase()) {
            if (russianLetters.contains(c)) {
                cntRussianLetters++
            }
            cntTotalLetters++
        }
        return cntRussianLetters >= 0.5 * cntTotalLetters
    }

    fun playNotification(ctx: Context, app: String, title: String, text: String) {
        synchronized(this) {
            if (app.isNotBlank())
                speechQueue.add("$app: ")
            if (title.isNotBlank())
                speechQueue.add(title)
            if (text.isNotBlank())
                speechQueue.add(text)

            if (ttsObject == null)
                ttsObject = TextToSpeech(ctx, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.ERROR)
            return

        synchronized(this) {
            ttsObject?.setOnUtteranceProgressListener(this)

            val attribBuilder =
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_SYSTEM)

            ttsObject?.setAudioAttributes(attribBuilder.build())
        }
        playOne()
    }

    fun playOne() {
        synchronized(this) {
            if (speechQueue.isEmpty()) {
                ttsObject?.stop()
                ttsObject?.shutdown()
                service.stopForeground(true)
                ttsObject = null
                return
            }

            val text = speechQueue[0]
            speechQueue.removeAt(0)

            if (!isRussianText(text)) {
                ttsObject?.language = Locale("en")
            } else {
                ttsObject?.language = Locale("ru")
            }

            ttsObject?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                System.currentTimeMillis().toString()
            )
        }
    }

    override fun onStart(utteranceId: String?) {
    }

    override fun onDone(utteranceId: String?) {
        playOne()
    }

    override fun onError(utteranceId: String?) {
        synchronized(this) {
            service.stopForeground(true)

            ttsObject?.stop()
            ttsObject?.shutdown()
            ttsObject = null
        }
    }

}

class PlayTTSService : Service() {

    var speaker = TTSSpeaker(this)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val NOTIFICATION_CHANNEL = "channel0"

        val mgr = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                "Main",
                NotificationManager.IMPORTANCE_MIN
            )

        channel.description = "Default channel"
        channel.setShowBadge(false)
        channel.enableVibration(false)

        channel.setBypassDnd(false)

        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        channel.importance = NotificationManager.IMPORTANCE_MIN

        mgr.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder =
            Notification.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("TTS Notify")
                .setContentText("Playing notification text")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())

        val notification = builder.build()

        startForeground(1, notification)

        playSound(
            intent?.getStringExtra("app"),
            intent?.getStringExtra("title"),
            intent?.getStringExtra("text")
        )

        return START_NOT_STICKY
    }

    private fun playSound(app: String?, title: String?, text: String?) {
        speaker.playNotification(this, app ?: "", title ?: "", text ?: "")
    }
}