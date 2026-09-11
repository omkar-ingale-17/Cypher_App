package com.cypher.assistant.features.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cypher.assistant.core.voice.VoiceState
import com.cypher.assistant.core.voice.tts.VoiceInfo
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import com.cypher.assistant.ui.theme.CypherCyan
import com.cypher.assistant.ui.theme.CypherGreen
import com.cypher.assistant.ui.theme.CypherPurple
import com.cypher.assistant.ui.theme.CypherRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicTapped(hasMicrophonePermission = true)
        } else {
            viewModel.onMicTapped(hasMicrophonePermission = false)
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onMicTapped(hasMicrophonePermission = true)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090B10),
                        Color(0xFF0F131D),
                        Color(0xFF07080C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            TopBar(
                voiceState = uiState.voiceState,
                onSettingsClick = { viewModel.openSettingsSheet() },
                onClearHistoryClick = { viewModel.clearHistory() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Interactive Glowing Cypher Orb ───────────────────────────────
            CypherGlowingOrb(
                voiceState = uiState.voiceState,
                rmsLevel = uiState.rmsLevel,
                onClick = { handleMicClick() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Status & Live Transcript Card ─────────────────────────────────
            StatusTranscriptCard(
                voiceState = uiState.voiceState,
                statusMessage = uiState.statusMessage,
                liveTranscript = uiState.liveTranscript,
                lastResponse = uiState.lastResponse
            )

            // ── Error Alert Banner ────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    ErrorBanner(
                        message = error,
                        onDismiss = { viewModel.dismissError() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Command History ────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                CommandHistoryList(
                    history = commandHistory,
                    onItemClick = { item -> viewModel.onTextCommandSubmitted(item.rawText) }
                )
            }

            // ── Bottom Manual Text Input Bar ──────────────────────────────────
            BottomTextInputBar(
                onSend = { text -> viewModel.onTextCommandSubmitted(text) }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Voice & Language Preferences Bottom Sheet ─────────────────────────
        if (uiState.isSettingsSheetOpen) {
            VoiceSettingsBottomSheet(
                uiState = uiState,
                onDismiss = { viewModel.closeSettingsSheet() },
                onLanguageSelected = { viewModel.onLanguageSelected(it) },
                onVoiceSelected = { viewModel.onVoiceSelected(it) },
                onSpeechRateChanged = { viewModel.onSpeechRateChanged(it) },
                onPitchChanged = { viewModel.onPitchChanged(it) },
                onToggleContinuous = { viewModel.onToggleContinuousListening(it) },
                onToggleWakeWord = { viewModel.onToggleWakeWord(it) }
            )
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    voiceState: VoiceState,
    onSettingsClick: () -> Unit,
    onClearHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(stateColor(voiceState))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CYPHER",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = voiceState.name,
                color = stateColor(voiceState),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        Row {
            IconButton(onClick = onClearHistoryClick) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Voice Settings",
                    tint = CypherCyan
                )
            }
        }
    }
}

// ── Animated Cypher Orb ──────────────────────────────────────────────────────

@Composable
private fun CypherGlowingOrb(
    voiceState: VoiceState,
    rmsLevel: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val basePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val targetColor = stateColor(voiceState)
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "OrbColor")

    val dynamicScale = when (voiceState) {
        VoiceState.LISTENING -> 1f + (rmsLevel * 0.4f) + (basePulse - 1f)
        VoiceState.PROCESSING -> basePulse
        VoiceState.SPEAKING -> 1.05f + (basePulse - 1f) * 0.5f
        else -> 1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Outer glow ripple
        if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(dynamicScale * 1.25f)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = 0.15f))
            )
        }

        // Middle aura ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(dynamicScale * 1.1f)
                .clip(CircleShape)
                .background(animatedColor.copy(alpha = 0.25f))
        )

        // Core Glowing Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .scale(dynamicScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor,
                            animatedColor.copy(alpha = 0.6f),
                            Color(0xFF0F1522)
                        )
                    )
                )
                .border(2.dp, animatedColor.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                imageVector = when (voiceState) {
                    VoiceState.SPEAKING -> Icons.Default.RecordVoiceOver
                    VoiceState.PROCESSING -> Icons.Default.GraphicEq
                    VoiceState.ERROR -> Icons.Default.Warning
                    else -> Icons.Default.Mic
                },
                contentDescription = "Microphone Orb",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

// ── Status & Transcript Card ─────────────────────────────────────────────────

@Composable
private fun StatusTranscriptCard(
    voiceState: VoiceState,
    statusMessage: String,
    liveTranscript: String,
    lastResponse: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(16.dp)),
        color = Color(0xFF121722).copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusMessage,
                color = stateColor(voiceState),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            if (liveTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"$liveTranscript\"",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            if (lastResponse.isNotBlank() && voiceState == VoiceState.IDLE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lastResponse,
                    color = CypherCyan.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Error Banner ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CypherRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        color = CypherRed.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CypherRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Command History List ─────────────────────────────────────────────────────

@Composable
private fun CommandHistoryList(
    history: List<CommandHistoryEntity>,
    onItemClick: (CommandHistoryEntity) -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No voice commands yet.\nTap the glowing orb and say \"Cypher, open YouTube\".",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { item ->
                CommandHistoryCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun CommandHistoryCard(
    item: CommandHistoryEntity,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeStr = formatter.format(Date(item.timestampMs))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF1B2230), RoundedCornerShape(12.dp)),
        color = Color(0xFF0F131C)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (item.success) CypherGreen else CypherRed)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.rawText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.responseMessage.isNotBlank()) {
                    Text(
                        text = item.responseMessage,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeStr,
                color = Color(0xFF6B7A99),
                fontSize = 11.sp
            )
        }
    }
}

// ── Bottom Text Input Bar ────────────────────────────────────────────────────

@Composable
private fun BottomTextInputBar(
    onSend: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Type command or query…", color = Color(0xFF5A667A), fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CypherCyan,
                unfocusedBorderColor = Color(0xFF222B3D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E121A),
                unfocusedContainerColor = Color(0xFF0E121A)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (textInput.isNotBlank()) {
                        onSend(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (textInput.isNotBlank()) {
                    onSend(textInput)
                    textInput = ""
                    keyboardController?.hide()
                }
            },
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(CypherCyan)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Command",
                tint = Color(0xFF0A0D14),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Voice & Language Settings Bottom Sheet ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSettingsBottomSheet(
    uiState: VoiceUiState,
    onDismiss: () -> Unit,
    onLanguageSelected: (Locale) -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10141E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Voice & Engine Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Wake Word & Continuous Listening Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wake Word (\"Cypher\")", color = Color.White, fontSize = 14.sp)
                    Text("Activates listening when phrase is spoken", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = uiState.userPreferences.wakeWordEnabled,
                    onCheckedChange = onToggleWakeWord,
                    colors = SwitchDefaults.colors(checkedThumbColor = CypherCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Continuous Listening Mode", color = Color.White, fontSize = 14.sp)
                    Text("Keeps microphone active (Foreground Service)", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = uiState.userPreferences.continuousListening,
                    onCheckedChange = onToggleContinuous,
                    colors = SwitchDefaults.colors(checkedThumbColor = CypherCyan)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector
            Text("Language / Locale", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            val languages = listOf(
                Locale.ENGLISH,
                Locale.US,
                Locale.UK,
                Locale.FRENCH,
                Locale.GERMAN,
                Locale.forLanguageTag("es-ES"),
                Locale.forLanguageTag("hi-IN")
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { loc ->
                    val isSelected = uiState.userPreferences.selectedLanguageTag == loc.toLanguageTag() ||
                            (uiState.userPreferences.selectedLanguageTag == "default" && loc == Locale.ENGLISH)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(loc) },
                        label = { Text(loc.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CypherCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TTS Voice Picker
            Text("Text-to-Speech Voice", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            if (uiState.availableVoices.isEmpty()) {
                Text("Using system default voice", color = Color.Gray, fontSize = 12.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableVoices.take(6)) { voice ->
                        val isSelected = uiState.userPreferences.selectedVoiceName == voice.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { onVoiceSelected(voice) },
                            label = { Text(voice.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CypherPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speech Rate Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speech Rate", color = Color.White, fontSize = 13.sp)
                Text(String.format(Locale.US, "%.1fx", uiState.userPreferences.ttsSpeechRate), color = CypherCyan, fontSize = 13.sp)
            }
            Slider(
                value = uiState.userPreferences.ttsSpeechRate,
                onValueChange = onSpeechRateChanged,
                valueRange = 0.5f..2.0f,
                steps = 6,
                colors = SliderDefaults.colors(thumbColor = CypherCyan, activeTrackColor = CypherCyan)
            )

            // Speech Pitch Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speech Pitch", color = Color.White, fontSize = 13.sp)
                Text(String.format(Locale.US, "%.1fx", uiState.userPreferences.ttsPitch), color = CypherCyan, fontSize = 13.sp)
            }
            Slider(
                value = uiState.userPreferences.ttsPitch,
                onValueChange = onPitchChanged,
                valueRange = 0.5f..2.0f,
                steps = 6,
                colors = SliderDefaults.colors(thumbColor = CypherCyan, activeTrackColor = CypherCyan)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun stateColor(state: VoiceState): Color = when (state) {
    VoiceState.IDLE -> CypherCyan
    VoiceState.LISTENING -> Color(0xFF00B0FF)
    VoiceState.PROCESSING -> CypherPurple
    VoiceState.SPEAKING -> CypherGreen
    VoiceState.ERROR -> CypherRed
}
