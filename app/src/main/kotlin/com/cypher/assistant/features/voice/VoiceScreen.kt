package com.cypher.assistant.features.voice

import com.cypher.assistant.features.applications.presentation.ApplicationScreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cypher.assistant.core.voice.VoiceState
import com.cypher.assistant.core.voice.tts.VoiceInfo
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import com.cypher.assistant.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Neon Cyberpunk Theme Colors
private val CypherBgDark = Color(0xFF070B14)
private val CypherCardBg = Color(0xFF0E1626)
private val CypherCardBorder = Color(0xFF1E2D4A)
private val CypherCyan = Color(0xFF00F0FF)
private val CypherBlue = Color(0xFF0066FF)
private val CypherPurple = Color(0xFF9D00FF)
private val CypherPink = Color(0xFFFF007A)
private val CypherGreen = Color(0xFF00FF88)
private val CypherOrange = Color(0xFFFF8800)
private val CypherRed = Color(0xFFFF2255)
private val CypherTextPrimary = Color(0xFFF0F6FC)
private val CypherTextSecondary = Color(0xFF8B949E)
private val CypherTextMuted = Color(0xFF484F58)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.commandHistory.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startStandbyListening(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.startStandbyListening(context)
        }
    }

    var textInput by remember { mutableStateOf("") }
    var showNameEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CypherBgDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                CypherTopBar(
                    userName = uiState.userPreferences.userName,
                    voiceState = uiState.voiceState,
                    wakeWordEnabled = uiState.userPreferences.wakeWordEnabled,
                    onUserClick = { showNameEditDialog = true },
                    onAppsClick = { viewModel.openApplicationsSheet() },
                    onSettingsClick = { viewModel.openSettingsSheet() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Banner
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    uiState.errorMessage?.let { msg ->
                        CypherErrorBanner(
                            errorMessage = msg,
                            onDismiss = { viewModel.dismissError() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Glowing Visualizer Orb
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CypherGlowingOrb(
                        voiceState = uiState.voiceState,
                        rmsLevel = uiState.rmsLevel
                    )
                }

                // Status and Transcription Box
                CypherStatusCard(
                    statusMessage = uiState.statusMessage,
                    liveTranscript = uiState.liveTranscript,
                    lastResponse = uiState.lastResponse,
                    voiceState = uiState.voiceState
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recent Command History Section
                CommandHistorySection(
                    history = history,
                    onClearHistory = { viewModel.clearHistory() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Text Command Bar
                CypherCommandInputBar(
                    text = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.onTextCommandSubmitted(textInput)
                            textInput = ""
                            focusManager.clearFocus()
                        }
                    }
                )
            }

            // First-launch or Profile Name Dialog
            if (uiState.isOnboardingOpen || showNameEditDialog) {
                UserNameDialog(
                    initialName = if (uiState.userPreferences.userName == "Commander") "" else uiState.userPreferences.userName,
                    isOnboarding = uiState.isOnboardingOpen,
                    onConfirm = { name ->
                        viewModel.onSetUserName(name)
                        showNameEditDialog = false
                    },
                    onDismiss = {
                        if (uiState.isOnboardingOpen) {
                            viewModel.onDismissOnboarding()
                        }
                        showNameEditDialog = false
                    }
                )
            }

            // Application Control Dialog
            if (uiState.isApplicationsSheetOpen) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { viewModel.closeApplicationsSheet() },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    ApplicationScreen(
                        onDismiss = { viewModel.closeApplicationsSheet() }
                    )
                }
            }

            // Voice Settings Bottom Sheet
            if (uiState.isSettingsSheetOpen) {
                VoiceSettingsBottomSheet(
                    preferences = uiState.userPreferences,
                    availableVoices = uiState.availableVoices,
                    availableLanguages = uiState.availableLanguages,
                    onDismiss = { viewModel.closeSettingsSheet() },
                    onLanguageSelected = { viewModel.onLanguageSelected(it) },
                    onVoiceSelected = { viewModel.onVoiceSelected(it) },
                    onSpeechRateChanged = { viewModel.onSpeechRateChanged(it) },
                    onPitchChanged = { viewModel.onPitchChanged(it) },
                    onToggleWakeWord = { viewModel.onToggleWakeWord(it, context) },
                    onToggleContinuous = { viewModel.onToggleContinuousListening(it) },
                    onEditName = {
                        viewModel.closeSettingsSheet()
                        showNameEditDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun CypherTopBar(
    userName: String,
    voiceState: VoiceState,
    wakeWordEnabled: Boolean,
    onUserClick: () -> Unit,
    onAppsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo and Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CypherCyan, CypherBlue, CypherPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text(
                    text = "CYPHER AI",
                    color = CypherTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (wakeWordEnabled) CypherGreen else CypherTextMuted
                            )
                    )
                    Text(
                        text = if (wakeWordEnabled) "WAKE PHRASE ACTIVE" else "WAKE PHRASE PAUSED",
                        color = if (wakeWordEnabled) CypherGreen else CypherTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // User Badge & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onUserClick,
                shape = RoundedCornerShape(20.dp),
                color = CypherCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CypherCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = CypherCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = userName,
                        color = CypherTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onAppsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CypherCardBg)
                    .border(1.dp, CypherCardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Applications",
                    tint = CypherCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CypherCardBg)
                    .border(1.dp, CypherCardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CypherCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CypherGlowingOrb(
    voiceState: VoiceState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic RMS audio reactivity boost
    val audioScale = (1f + (rmsLevel.coerceIn(0f, 10f) / 20f))

    val primaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.IDLE,
            VoiceState.LISTENING_FOR_WAKE_WORD -> CypherCyan
            VoiceState.WAKE_WORD_DETECTED -> CypherGreen
            VoiceState.LISTENING_FOR_COMMAND -> CypherCyan
            VoiceState.PROCESSING -> CypherPurple
            VoiceState.SPEAKING -> CypherGreen
            VoiceState.ERROR -> CypherRed
        },
        animationSpec = tween(400),
        label = "orb_color_primary"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.IDLE,
            VoiceState.LISTENING_FOR_WAKE_WORD -> CypherBlue
            VoiceState.WAKE_WORD_DETECTED -> CypherCyan
            VoiceState.LISTENING_FOR_COMMAND -> CypherPurple
            VoiceState.PROCESSING -> CypherPink
            VoiceState.SPEAKING -> CypherCyan
            VoiceState.ERROR -> CypherOrange
        },
        animationSpec = tween(400),
        label = "orb_color_secondary"
    )

    val isAudioActive = voiceState == VoiceState.LISTENING_FOR_COMMAND ||
            voiceState == VoiceState.LISTENING_FOR_WAKE_WORD

    val currentScale = if (isAudioActive) {
        pulseScale * audioScale
    } else {
        pulseScale
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow Ring 1
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(currentScale * 1.08f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                secondaryColor.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        // Outer Glow Ring 2
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(currentScale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.6f),
                                secondaryColor.copy(alpha = 0.8f),
                                primaryColor.copy(alpha = 0.6f)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
        )

        // Core Glowing Orb
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(if (isAudioActive) audioScale else 1f)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = primaryColor,
                    spotColor = secondaryColor
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.9f),
                            secondaryColor.copy(alpha = 0.7f),
                            CypherCardBg
                        )
                    )
                )
                .border(2.dp, primaryColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (voiceState) {
                VoiceState.IDLE,
                VoiceState.LISTENING_FOR_WAKE_WORD -> Icons.Default.Mic
                VoiceState.WAKE_WORD_DETECTED,
                VoiceState.LISTENING_FOR_COMMAND -> Icons.Default.GraphicEq
                VoiceState.PROCESSING -> Icons.Default.RecordVoiceOver
                VoiceState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                VoiceState.ERROR -> Icons.Default.Warning
            }
            Icon(
                imageVector = icon,
                contentDescription = voiceState.name,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun CypherStatusCard(
    statusMessage: String,
    liveTranscript: String,
    lastResponse: String,
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CypherCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Text
            Text(
                text = statusMessage,
                color = when (voiceState) {
                    VoiceState.LISTENING_FOR_WAKE_WORD -> CypherCyan
                    VoiceState.WAKE_WORD_DETECTED -> CypherGreen
                    VoiceState.LISTENING_FOR_COMMAND -> CypherCyan
                    VoiceState.PROCESSING -> CypherPurple
                    VoiceState.SPEAKING -> CypherGreen
                    VoiceState.ERROR -> CypherRed
                    VoiceState.IDLE -> CypherTextSecondary
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            // Live User Transcript
            if (liveTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CypherBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CypherCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = CypherCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "\"$liveTranscript\"",
                            color = CypherTextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Last Cypher Response
            if (lastResponse.isNotBlank() && voiceState != VoiceState.LISTENING_FOR_COMMAND) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CypherBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CypherGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CypherGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = lastResponse,
                            color = CypherTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandHistorySection(
    history: List<CommandHistoryEntity>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CypherCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ACTIVITY HISTORY",
                        color = CypherTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (history.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CypherCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${history.size}",
                                color = CypherCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = CypherTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity.\nSay \"Cypher\", \"Jan\", \"Jaan\", or \"Baby\" to begin.",
                        color = CypherTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        CommandHistoryItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandHistoryItemRow(
    item: CommandHistoryEntity,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(item.timestampMs) { dateFormat.format(Date(item.timestampMs)) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CypherBgDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.success) CypherCardBorder else CypherRed.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CypherBlue.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.intentType.name,
                            color = CypherCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "\"${item.rawText}\"",
                        color = CypherTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = timeStr,
                    color = CypherTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (item.responseMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.responseMessage,
                    color = if (item.success) CypherTextSecondary else CypherRed,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CypherCommandInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = "Type a command...",
                    color = CypherTextMuted,
                    fontSize = 13.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CypherCardBg,
                unfocusedContainerColor = CypherCardBg,
                focusedBorderColor = CypherCyan,
                unfocusedBorderColor = CypherCardBorder,
                focusedTextColor = CypherTextPrimary,
                unfocusedTextColor = CypherTextPrimary,
                cursorColor = CypherCyan
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank()) {
                        Brush.linearGradient(listOf(CypherCyan, CypherBlue))
                    } else {
                        Brush.linearGradient(listOf(CypherCardBg, CypherCardBg))
                    }
                )
                .border(
                    1.dp,
                    if (text.isNotBlank()) CypherCyan else CypherCardBorder,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Color.White else CypherTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CypherErrorBanner(
    errorMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CypherRed.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CypherRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = CypherRed,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = errorMessage,
                    color = CypherTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = CypherTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UserNameDialog(
    initialName: String,
    isOnboarding: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CypherCardBg,
        title = {
            Text(
                text = if (isOnboarding) "Welcome to Cypher" else "Set Your Name",
                color = CypherTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = if (isOnboarding) {
                        "Cypher is your personal voice assistant. What should Cypher call you?"
                    } else {
                        "Update the name Cypher uses to address you in conversation."
                    },
                    color = CypherTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text("Enter your name...", color = CypherTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CypherBgDark,
                        unfocusedContainerColor = CypherBgDark,
                        focusedBorderColor = CypherCyan,
                        unfocusedBorderColor = CypherCardBorder,
                        focusedTextColor = CypherTextPrimary,
                        unfocusedTextColor = CypherTextPrimary,
                        cursorColor = CypherCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (nameText.isBlank()) "Commander" else nameText.trim()
                    onConfirm(finalName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CypherCyan)
            ) {
                Text(
                    text = if (isOnboarding) "Get Started" else "Save",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (isOnboarding) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Skip", color = CypherTextSecondary)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = CypherTextSecondary)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSettingsBottomSheet(
    preferences: UserPreferences,
    availableVoices: List<VoiceInfo>,
    availableLanguages: List<Locale>,
    onDismiss: () -> Unit,
    onLanguageSelected: (Locale) -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onEditName: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CypherCardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CypherCardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOICE & ASSISTANT SETTINGS",
                    color = CypherCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CypherTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // User Profile Section
            Surface(
                onClick = onEditName,
                shape = RoundedCornerShape(12.dp),
                color = CypherBgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, CypherCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Author / User Name",
                            color = CypherTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = preferences.userName,
                            color = CypherTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = CypherCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Toggles
            SettingsToggleRow(
                title = "Wake Phrases (Cypher, Jaan, Baby)",
                description = "Activate voice assistant by saying 'Cypher', 'Jaan', or 'Baby'",
                checked = preferences.wakeWordEnabled,
                onCheckedChange = onToggleWakeWord
            )

            SettingsToggleRow(
                title = "Continuous Listening",
                description = "Keep listening for subsequent commands after replying",
                checked = preferences.continuousListening,
                onCheckedChange = onToggleContinuous
            )

            // Speech Rate Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speech Rate",
                        color = CypherTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fx", preferences.ttsSpeechRate),
                        color = CypherCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = preferences.ttsSpeechRate,
                    onValueChange = onSpeechRateChanged,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CypherCyan,
                        activeTrackColor = CypherCyan,
                        inactiveTrackColor = CypherCardBorder
                    )
                )
            }

            // Pitch Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Voice Pitch",
                        color = CypherTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fx", preferences.ttsPitch),
                        color = CypherCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = preferences.ttsPitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CypherCyan,
                        activeTrackColor = CypherCyan,
                        inactiveTrackColor = CypherCardBorder
                    )
                )
            }

            // Language Dropdown
            if (availableLanguages.isNotEmpty()) {
                var langExpanded by remember { mutableStateOf(false) }
                val currentLangName = remember(preferences.selectedLanguageTag, availableLanguages) {
                    if (preferences.selectedLanguageTag == "default") {
                        "Default (${Locale.getDefault().displayLanguage})"
                    } else {
                        val loc = Locale.forLanguageTag(preferences.selectedLanguageTag)
                        loc.displayName
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = currentLangName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language", color = CypherTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CypherBgDark,
                            unfocusedContainerColor = CypherBgDark,
                            focusedBorderColor = CypherCyan,
                            unfocusedBorderColor = CypherCardBorder,
                            focusedTextColor = CypherTextPrimary,
                            unfocusedTextColor = CypherTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        for (loc in availableLanguages.take(15)) {
                            DropdownMenuItem(
                                text = { Text(loc.displayName, color = CypherTextPrimary) },
                                onClick = {
                                    onLanguageSelected(loc)
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Voice Selector Dropdown
            if (availableVoices.isNotEmpty()) {
                var voiceExpanded by remember { mutableStateOf(false) }
                val currentVoiceName = preferences.selectedVoiceName.ifBlank { "Default Engine Voice" }

                ExposedDropdownMenuBox(
                    expanded = voiceExpanded,
                    onExpandedChange = { voiceExpanded = !voiceExpanded }
                ) {
                    OutlinedTextField(
                        value = currentVoiceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Voice Profile", color = CypherTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CypherBgDark,
                            unfocusedContainerColor = CypherBgDark,
                            focusedBorderColor = CypherCyan,
                            unfocusedBorderColor = CypherCardBorder,
                            focusedTextColor = CypherTextPrimary,
                            unfocusedTextColor = CypherTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = voiceExpanded,
                        onDismissRequest = { voiceExpanded = false }
                    ) {
                        for (voice in availableVoices.take(12)) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(voice.displayName, color = CypherTextPrimary, fontSize = 13.sp)
                                        Text(
                                            "${voice.locale.displayCountry} * ${if (voice.isFemale == true) "Female" else if (voice.isFemale == false) "Male" else "Neutral"}",
                                            color = CypherTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    onVoiceSelected(voice)
                                    voiceExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CypherTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = CypherTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CypherCyan,
                uncheckedThumbColor = CypherTextMuted,
                uncheckedTrackColor = CypherBgDark,
                uncheckedBorderColor = CypherCardBorder
            )
        )
    }
}
