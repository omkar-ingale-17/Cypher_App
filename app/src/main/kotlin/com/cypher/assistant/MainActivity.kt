package com.cypher.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cypher.assistant.features.voice.VoiceScreen
import com.cypher.assistant.ui.theme.CypherTheme
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

/**
 * Single-activity host for all Compose navigation.
 * Keeps a weak reference to allow graceful background minimization (moveTaskToBack)
 * without stopping the foreground VoiceAssistantService.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private var activeActivity: WeakReference<MainActivity>? = null

        /**
         * Minimizes the Cypher activity safely by sending its task to the background.
         * The VoiceAssistantService continues listening uninterrupted.
         */
        fun minimizeActivity(): Boolean {
            return activeActivity?.get()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.moveTaskToBack(true)
                } else {
                    false
                }
            } ?: false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeActivity = WeakReference(this)
        enableEdgeToEdge()
        setContent {
            CypherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeActivity = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeActivity?.get() == this) {
            activeActivity = null
        }
    }
}
