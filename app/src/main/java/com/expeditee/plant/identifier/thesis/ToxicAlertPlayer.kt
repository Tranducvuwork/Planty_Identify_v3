package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import com.expeditee.plant.identifier.R

object ToxicAlertPlayer {
    private const val TAG = "ToxicAlertPlayer"
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, url: String?) {
        stop()
        if (playLocalRaw(context.applicationContext)) return
        if (!url.isNullOrBlank()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(url)
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { _, _, _ ->
                        playFallbackTone()
                        true
                    }
                    prepareAsync()
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "remote sound failed: ${e.message}")
            }
        }
        playFallbackTone()
    }

    private fun playLocalRaw(context: Context): Boolean {
        return try {
            mediaPlayer = MediaPlayer.create(context, R.raw.alert_toxic)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setOnCompletionListener { stop() }
                start()
            }
            mediaPlayer != null
        } catch (e: Exception) {
            Log.w(TAG, "local sound failed: ${e.message}")
            false
        }
    }

    fun playQuiet(context: Context) {
        stop()
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 280)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    tone.release()
                } catch (_: Exception) {
                }
            }, 400)
        } catch (e: Exception) {
            Log.w(TAG, "quiet tone failed: ${e.message}")
        }
    }

    fun playIdentifyCue(context: Context, toxic: Boolean, medicinal: Boolean) {
        if (toxic) play(context, null) else if (medicinal) playQuiet(context)
    }

    fun stop() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    private fun playFallbackTone() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    tone.release()
                } catch (_: Exception) {
                }
            }, 1500)
        } catch (e: Exception) {
            Log.w(TAG, "tone failed: ${e.message}")
        }
    }
}
