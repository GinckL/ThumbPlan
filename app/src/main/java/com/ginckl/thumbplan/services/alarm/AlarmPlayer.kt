package com.ginckl.thumbplan.services.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File

/** Plays the alarm sound (custom file or system alarm tone) and vibrates, until [stop] is called. */
class AlarmPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun start(customAudioPath: String?, playSound: Boolean, vibrate: Boolean) {
        stop()
        if (playSound) {
            val custom = customAudioPath?.let { File(it) }?.takeIf { it.exists() }?.let { Uri.fromFile(it) }
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = custom?.let { prepare(it) } ?: fallback?.let { prepare(it) }
            player?.start()
        }
        if (vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        }
    }

    private fun prepare(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
        }
    } catch (_: Exception) {
        null
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }
}
