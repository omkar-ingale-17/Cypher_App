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

/**
 * Single-activity host for all Compose navigation.
 * Future screens are added as Composable destinations - no new Activities needed.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CypherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceScreen()
                }
            }
        }
    }
}
