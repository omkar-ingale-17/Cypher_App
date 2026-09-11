package com.cypher.assistant.services.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cypher.assistant.MainActivity
import com.cypher.assistant.core.voice.VoiceEngine
import com.cypher.assistant.core.voice.VoiceState
import com.cypher.assistant.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "VoiceAssistantService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "cypher_voice_channel"

/**
 * Foreground Service hosting continuous background voice activation where Android rules permit.
 *
 * ## Android Background Execution Rules & Reality:
 * - Starting in Android 14 (API 34), microphone usage in the background requires a foreground
 *   service explicitly typed with `FOREGROUND_SERVICE_MICROPHONE` and a visible ongoing notification.
 * - Android actively restricts background audio recording:
 *     1. If another app requests AUDIOFOCUS_GAIN (e.g., incoming phone call, camera video recording),
 *        the system pauses or mutes background recording.
 *     2. Doze mode and OEM battery optimizations will throttle background loops unless the app
 *        is whitelisted by the user in battery optimization settings.
 * - This service adheres to these platform constraints by properly managing audio focus,
 *   handling interruptions gracefully, and releasing audio resources immediately on service stop.
 */
@AndroidEntryPoint
class VoiceAssistantService : Service(), AudioManager.OnAudioFocusChangeListener {

    @Inject
    lateinit var voiceEngine: VoiceEngine

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "VoiceAssistantService created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        observeVoiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_LISTENING -> {
                voiceEngine.stopListening()
            }
            ACTION_START_LISTENING -> {
                requestAudioFocusAndListen()
            }
            else -> {
                startForegroundWithNotification()
                requestAudioFocusAndListen()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(voiceStateText = "Listening for 'Cypher'…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun requestAudioFocusAndListen() {
        serviceScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            val focusGranted = requestAudioFocus()
            if (focusGranted) {
                voiceEngine.startListening(
                    continuous = prefs.continuousListening,
                    requireWakePhrase = prefs.wakeWordEnabled
                )
            } else {
                Log.w(TAG, "Audio focus request rejected by system")
            }
        }
    }

    private fun observeVoiceState() {
        serviceScope.launch {
            voiceEngine.voiceState.collect { state ->
                val stateDesc = when (state) {
                    VoiceState.IDLE -> "Standby — Say 'Cypher'"
                    VoiceState.LISTENING -> "Listening for speech…"
                    VoiceState.PROCESSING -> "Processing command…"
                    VoiceState.SPEAKING -> "Speaking response…"
                    VoiceState.ERROR -> "Microphone error"
                }
                updateNotification(stateDesc)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(this)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost — pausing voice listening")
                voiceEngine.stopListening()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus regained — resuming voice listening")
                requestAudioFocusAndListen()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cypher Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status when Cypher voice assistant is listening in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(voiceStateText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }.let {
            PendingIntent.getService(this, 1, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cypher Voice Assistant")
            .setContentText(voiceStateText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(stateText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(stateText))
    }

    private fun stopForegroundService() {
        abandonAudioFocus()
        voiceEngine.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "VoiceAssistantService destroyed")
        abandonAudioFocus()
        voiceEngine.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_LISTENING = "com.cypher.assistant.action.START_LISTENING"
        const val ACTION_PAUSE_LISTENING = "com.cypher.assistant.action.PAUSE_LISTENING"
        const val ACTION_STOP_LISTENING = "com.cypher.assistant.action.STOP_LISTENING"
        const val ACTION_STOP_SERVICE = "com.cypher.assistant.action.STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
