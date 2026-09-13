package com.cypher.assistant.services.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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

/**
 * Foreground Service hosting the hands-free Cypher Voice Assistant engine.
 *
 * Responsibilities:
 *  - Holds PARTIAL_WAKE_LOCK to prevent CPU sleep while listening.
 *  - Handles SCREEN_OFF, SCREEN_ON, USER_PRESENT broadcasts to maintain active listening.
 *  - Manages audio focus transitions.
 *  - Handles clean termination on onTaskRemoved() / onDestroy() without resurrection.
 */
@AndroidEntryPoint
class VoiceAssistantService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val TAG = "CYPHER_SERVICE"
        private const val CHANNEL_ID = "cypher_voice_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.cypher.assistant.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.cypher.assistant.action.STOP_SERVICE"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var serviceStopped: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_SERVICE: Failed to start VoiceAssistantService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_SERVICE: Failed to stop VoiceAssistantService", e)
            }
        }
    }

    @Inject
    lateinit var voiceEngine: VoiceEngine

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CYPHER_SERVICE: SERVICE_CREATED")
        serviceStopped = false
        isRunning = true
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Acquire partial wake lock to keep CPU awake for background listening
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Cypher::VoiceAssistantWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12h safety max
            }
            Log.d(TAG, "CYPHER_SERVICE: Partial wake lock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_SERVICE: Could not acquire wake lock", e)
        }

        createNotificationChannel()
        startForegroundWithNotification()
        registerScreenStateReceiver()
        observeVoiceState()
    }

    private fun registerScreenStateReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (serviceStopped) return
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.i(TAG, "CYPHER_SERVICE: SCREEN_OFF -> Voice assistant maintains active background listening")
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.i(TAG, "CYPHER_SERVICE: SCREEN_ON -> Verifying voice engine status")
                        voiceEngine.ensureListeningActive()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        Log.i(TAG, "CYPHER_SERVICE: USER_PRESENT -> Device unlocked, verifying voice engine status")
                        voiceEngine.ensureListeningActive()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenStateReceiver() {
        try {
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_SERVICE: Error unregistering screenReceiver", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "CYPHER_SERVICE: SERVICE_STARTED (action=${intent?.action})")

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                shutdownVoiceAssistant()
                return START_NOT_STICKY
            }
            else -> {
                requestAudioFocusAndListen()
            }
        }

        // Return START_NOT_STICKY so service does not resurrect if killed by system
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "CYPHER_SERVICE: TASK_REMOVED -> User removed Cypher from Recents, shutting down permanently")
        shutdownVoiceAssistant()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(voiceStateText = "Listening for 'Cypher', 'Jan', 'Jaan', 'Baby'...")
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
        if (serviceStopped) return
        serviceScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            if (!prefs.wakeWordEnabled && !prefs.continuousListening) {
                Log.d(TAG, "CYPHER_VOICE: Wake word disabled in preferences")
                return@launch
            }

            val focusGranted = requestAudioFocus()
            if (focusGranted && !serviceStopped) {
                Log.d(TAG, "CYPHER_VOICE: Starting voice service listening loop")
                voiceEngine.startListening(requireWakePhrase = prefs.wakeWordEnabled)
            } else {
                Log.w(TAG, "CYPHER_VOICE: Audio focus request rejected by system")
            }
        }
    }

    private fun observeVoiceState() {
        serviceScope.launch {
            voiceEngine.voiceState.collect { state ->
                if (serviceStopped) return@collect
                val stateDesc = when (state) {
                    VoiceState.IDLE -> "Standby - Say 'Cypher', 'Jan', 'Jaan', or 'Baby'"
                    VoiceState.LISTENING_FOR_WAKE_WORD -> "Listening for 'Cypher', 'Jan', 'Jaan', 'Baby'..."
                    VoiceState.WAKE_WORD_DETECTED -> "Wake word detected!"
                    VoiceState.LISTENING_FOR_COMMAND -> "Listening for command..."
                    VoiceState.PROCESSING -> "Processing command..."
                    VoiceState.SPEAKING -> "Speaking response..."
                    VoiceState.ERROR -> "Say 'Cypher' to activate"
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

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
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
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(this)
            }
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Error abandoning audio focus", e)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (serviceStopped) return
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "CYPHER_VOICE: Audio focus permanently lost")
                voiceEngine.stopListening()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "CYPHER_VOICE: Transient audio focus loss (code=$focusChange) -> ignored to preserve listening loop")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "CYPHER_VOICE: Audio focus regained -> verifying voice listening")
                if (!serviceStopped) requestAudioFocusAndListen()
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
            .setContentTitle("Cypher AI Voice Assistant")
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
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(stateText))
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Error updating notification", e)
        }
    }

    private fun shutdownVoiceAssistant() {
        if (serviceStopped) return
        serviceStopped = true
        isRunning = false

        Log.i(TAG, "CYPHER_SERVICE: Shutting down VoiceAssistantService completely")

        unregisterScreenStateReceiver()

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}

        abandonAudioFocus()
        voiceEngine.shutdown()
        serviceScope.cancel()

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Error removing foreground notification", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "CYPHER_SERVICE: SERVICE_DESTROYED")
        shutdownVoiceAssistant()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
