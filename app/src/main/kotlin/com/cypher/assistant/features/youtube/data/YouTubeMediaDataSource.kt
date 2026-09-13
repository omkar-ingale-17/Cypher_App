package com.cypher.assistant.features.youtube.data

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media playback and system volume controller using official Android public AudioManager APIs.
 */
@Singleton
class YouTubeMediaDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun play(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)

    fun pause(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)

    fun resume(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)

    fun stop(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)

    fun next(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)

    fun previous(): Boolean = sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun increaseVolume(): Boolean {
        val am = audioManager ?: return false
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun decreaseVolume(): Boolean {
        val am = audioManager ?: return false
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun mute(): Boolean {
        val am = audioManager ?: return false
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun unmute(): Boolean {
        val am = audioManager ?: return false
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    private fun sendMediaKeyEvent(keyCode: Int): Boolean {
        val am = audioManager ?: return false
        val eventTime = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
        return true
    }
}
