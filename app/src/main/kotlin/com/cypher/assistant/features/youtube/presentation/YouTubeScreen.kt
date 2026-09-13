package com.cypher.assistant.features.youtube.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cypher.assistant.ui.theme.*

private val YouTubeRed = Color(0xFFFF0000)
private val YouTubeCardBg = Color(0xFF1E1E1E)
private val YouTubeCardBorder = Color(0xFF2E2E2E)
private val CypherTextSecondary = CypherOnSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeScreen(
    onDismiss: () -> Unit,
    viewModel: YouTubeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YouTubeRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "YOUTUBE CONTROL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openYouTube() }) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open App",
                            tint = CypherCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CypherSurface
                )
            )
        },
        containerColor = CypherBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search YouTube...", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CypherCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.executeSearch()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Search", tint = CypherCyan)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.executeSearch()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CypherCyan,
                    unfocusedBorderColor = CypherOutline,
                    focusedContainerColor = CypherSurface,
                    unfocusedContainerColor = CypherSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Status message / feedback
            AnimatedVisibility(
                visible = uiState !is YouTubeUiState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState) {
                            is YouTubeUiState.Success -> CypherGreen.copy(alpha = 0.15f)
                            is YouTubeUiState.Error -> Color.Red.copy(alpha = 0.15f)
                            else -> CypherCyan.copy(alpha = 0.15f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (val state = uiState) {
                            is YouTubeUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CypherCyan
                                )
                                Text("Executing...", color = Color.White, fontSize = 13.sp)
                            }
                            is YouTubeUiState.Success -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CypherGreen, modifier = Modifier.size(16.dp))
                                Text(state.message, color = Color.White, fontSize = 13.sp)
                            }
                            is YouTubeUiState.Error -> {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Text(state.errorMessage, color = Color.White, fontSize = 13.sp)
                            }
                            YouTubeUiState.Idle -> {}
                        }
                    }
                }
            }

            // Quick Nav Destinations
            Text(
                text = "NAVIGATE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CypherTextSecondary,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickNavChip(
                    title = "Home",
                    icon = Icons.Default.Home,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.openHome() }
                )
                QuickNavChip(
                    title = "Shorts",
                    icon = Icons.Default.Bolt,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.openShorts() }
                )
                QuickNavChip(
                    title = "Subs",
                    icon = Icons.Default.Subscriptions,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.openSubscriptions() }
                )
                QuickNavChip(
                    title = "History",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.openHistory() }
                )
            }

            // Playback Controls
            Text(
                text = "MEDIA PLAYBACK",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CypherTextSecondary,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = YouTubeCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, YouTubeCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaControlButton(
                        icon = Icons.Default.SkipPrevious,
                        label = "Previous",
                        onClick = { viewModel.previous() }
                    )
                    MediaControlButton(
                        icon = Icons.Default.PlayArrow,
                        label = "Resume",
                        accentColor = CypherCyan,
                        onClick = { viewModel.resume() }
                    )
                    MediaControlButton(
                        icon = Icons.Default.Pause,
                        label = "Pause",
                        accentColor = CypherCyan,
                        onClick = { viewModel.pause() }
                    )
                    MediaControlButton(
                        icon = Icons.Default.Stop,
                        label = "Stop",
                        onClick = { viewModel.stop() }
                    )
                    MediaControlButton(
                        icon = Icons.Default.SkipNext,
                        label = "Next",
                        onClick = { viewModel.next() }
                    )
                }
            }

            // Volume Controls
            Text(
                text = "VOLUME & AUDIO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CypherTextSecondary,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VolumeButton(
                    icon = Icons.Default.VolumeUp,
                    label = "Vol Up",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.volumeUp() }
                )
                VolumeButton(
                    icon = Icons.Default.VolumeDown,
                    label = "Vol Down",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.volumeDown() }
                )
                VolumeButton(
                    icon = Icons.Default.VolumeOff,
                    label = "Mute",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.mute() }
                )
                VolumeButton(
                    icon = Icons.Default.VolumeMute,
                    label = "Unmute",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.unmute() }
                )
            }

            // Voice Command Cheatsheet
            Text(
                text = "VOICE COMMANDS EXAMPLES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CypherTextSecondary,
                letterSpacing = 1.sp
            )

            val commands = listOf(
                "Play Bohemian Rhapsody on YouTube" to "Searches and opens song",
                "Search YouTube for Android tutorials" to "Opens YouTube search",
                "Open YouTube Shorts" to "Launches Shorts feed",
                "Pause YouTube / Resume YouTube" to "Controls media playback",
                "Next video / Skip video" to "Skips to next track",
                "Increase YouTube volume / Mute YouTube" to "Adjusts playback audio"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                commands.forEach { (cmd, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CypherSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cmd,
                                color = CypherCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = desc,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNavChip(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CypherOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CypherCyan, modifier = Modifier.size(20.dp))
            Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MediaControlButton(
    icon: ImageVector,
    label: String,
    accentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CypherSurface)
                .border(1.dp, CypherOutline, CircleShape)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Text(text = label, color = CypherTextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun VolumeButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CypherSurface,
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CypherOutline),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CypherCyan, modifier = Modifier.size(18.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
