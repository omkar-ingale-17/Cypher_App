package com.cypher.assistant.features.youtube.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.ui.theme.CypherCyan
import com.cypher.assistant.ui.theme.CypherGreen
import com.cypher.assistant.ui.theme.CypherSurface
import com.cypher.assistant.ui.theme.CypherSurfaceVariant

@Composable
fun YouTubeScreenDialog(
    onDismiss: () -> Unit,
    viewModel: YouTubeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = CypherSurface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFFFF0000), CypherCyan))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "YouTube",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "YouTube App Control",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Installed Application Voice Controller",
                                style = MaterialTheme.typography.labelSmall,
                                color = CypherCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Card
                    item {
                        StatusCard(
                            isInstalled = uiState.isYouTubeInstalled,
                            isAccessibility = uiState.isAccessibilityEnabled,
                            packageName = uiState.installedPackage,
                            onOpenAccessibility = viewModel::openAccessibilitySettings
                        )
                    }

                    // Search Card
                    item {
                        SearchCard(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onSearch = { query ->
                                viewModel.executeCommand(YouTubeCommand.Search(query))
                            },
                            onPlay = { query ->
                                viewModel.executeCommand(YouTubeCommand.PlaySearch(query))
                            }
                        )
                    }

                    // Quick Actions
                    item {
                        QuickActionsSection(
                            onAction = { cmd -> viewModel.executeCommand(cmd) }
                        )
                    }

                    // Media Controls
                    item {
                        MediaControlsSection(
                            onAction = { cmd -> viewModel.executeCommand(cmd) }
                        )
                    }

                    // Voice Commands Reference
                    item {
                        VoiceCommandsReference()
                    }
                }

                // Last Action Toast / Message
                if (!uiState.lastActionMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CypherSurfaceVariant)
                            .border(1.dp, CypherCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.lastActionMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CypherCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    isInstalled: Boolean,
    isAccessibility: Boolean,
    packageName: String?,
    onOpenAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // YouTube App status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YouTube Application",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isInstalled) CypherGreen else Color(0xFFFF5252))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInstalled) "Available (${packageName ?: "installed"})" else "Not Installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInstalled) CypherGreen else Color(0xFFFF5252)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accessibility Control",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibility) CypherGreen else Color(0xFFFFB300))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAccessibility) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAccessibility) CypherGreen else Color(0xFFFFB300)
                    )
                }
            }

            if (!isAccessibility) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenAccessibility,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CypherCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CypherCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable Cypher Accessibility in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = CypherCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SEARCH & PLAY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search YouTube or video title...", color = Color.White.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CypherCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSearch(query) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CypherSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CypherCyan.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CypherCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search", color = CypherCyan)
                }

                Button(
                    onClick = { onPlay(query) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsSection(
    onAction: (YouTubeCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip("🏠 Home") { onAction(YouTubeCommand.OpenHome) }
                ActionChip("⚡ Shorts") { onAction(YouTubeCommand.OpenShorts) }
                ActionChip("🔔 Subscriptions") { onAction(YouTubeCommand.OpenSubscriptions) }
                ActionChip("🕒 History") { onAction(YouTubeCommand.OpenHistory) }
                ActionChip("👤 My Channel") { onAction(YouTubeCommand.OpenChannel) }
                ActionChip("👍 Like") { onAction(YouTubeCommand.Like) }
                ActionChip("👎 Dislike") { onAction(YouTubeCommand.Dislike) }
                ActionChip("🔴 Subscribe") { onAction(YouTubeCommand.Subscribe) }
                ActionChip("💬 Comments") { onAction(YouTubeCommand.OpenComments) }
                ActionChip("📜 Description") { onAction(YouTubeCommand.OpenDescription) }
                ActionChip("⬇️ Scroll Down") { onAction(YouTubeCommand.ScrollDown) }
                ActionChip("⬆️ Scroll Up") { onAction(YouTubeCommand.ScrollUp) }
            }
        }
    }
}

@Composable
private fun MediaControlsSection(
    onAction: (YouTubeCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MEDIA & VOLUME CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MediaButton("⏮", "Prev") { onAction(YouTubeCommand.Previous) }
                MediaButton("⏸", "Pause") { onAction(YouTubeCommand.Pause) }
                MediaButton("▶", "Play") { onAction(YouTubeCommand.Resume) }
                MediaButton("⏭", "Next") { onAction(YouTubeCommand.Next) }
                MediaButton("🔇", "Mute") { onAction(YouTubeCommand.Mute) }
                MediaButton("🔊", "Vol +") { onAction(YouTubeCommand.VolumeUp) }
                MediaButton("🔉", "Vol -") { onAction(YouTubeCommand.VolumeDown) }
            }
        }
    }
}

@Composable
private fun VoiceCommandsReference() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CypherSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "VOICE COMMANDS CHEATSHEET",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val commands = listOf(
                "\"Cypher, open YouTube\"",
                "\"Cypher, search Python tutorials on YouTube\"",
                "\"Cypher, play Naruto opening\"",
                "\"Cypher, play the first video\"",
                "\"Cypher, pause / resume / next / previous\"",
                "\"Cypher, like this video / dislike this video\"",
                "\"Cypher, subscribe / unsubscribe\"",
                "\"Cypher, open comments / close comments\"",
                "\"Cypher, open description / show more\"",
                "\"Cypher, scroll down / scroll up\"",
                "\"Cypher, open Shorts / open subscriptions / open history\""
            )

            commands.forEach { cmd ->
                Text(
                    text = cmd,
                    style = MaterialTheme.typography.bodySmall,
                    color = CypherCyan.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CypherSurface)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MediaButton(
    iconText: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CypherSurface)
                .border(1.dp, CypherCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconText, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}
