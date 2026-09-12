PART5 = '''
// ─────────────────────────────────────────────────────────────
// User Name Onboarding Dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun UserNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CypherCardBg,
        title = {
            Text(
                text       = "IDENTIFY YOURSELF",
                color      = CypherCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text     = "How should Cypher address you in conversation?",
                    color    = CypherTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value         = nameText,
                    onValueChange = { nameText = it },
                    placeholder   = { Text("e.g. Commander, Alex", color = CypherTextMuted) },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = CypherCyan,
                        unfocusedBorderColor = CypherTextMuted,
                        focusedTextColor     = CypherTextPrimary,
                        unfocusedTextColor   = CypherTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText.trim().ifBlank { "Commander" }) },
                colors  = ButtonDefaults.buttonColors(containerColor = CypherCyan)
            ) {
                Text("Confirm", color = CypherBgDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip", color = CypherTextMuted)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// Voice Settings Bottom Sheet
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSettingsBottomSheet(
    prefs: UserPreferences,
    availableVoices: List<VoiceInfo>,
    availableLanguages: List<Locale>,
    onDismiss: () -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onLanguageSelected: (Locale) -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var speechRate by remember { mutableFloatStateOf(prefs.ttsSpeechRate) }
    var pitch      by remember { mutableFloatStateOf(prefs.ttsPitch) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CypherCardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(CypherTextMuted, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text          = "VOICE ENGINE SETTINGS",
                color         = CypherCyan,
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Wake Word Toggle
            SettingsToggleRow(
                label       = "Wake Word Detection",
                description = "Respond to \"Cypher\" hands-free",
                checked     = prefs.wakeWordEnabled,
                onChecked   = onToggleWakeWord
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Continuous Listening Toggle
            SettingsToggleRow(
                label       = "Continuous Listening",
                description = "Background service listens for wake phrase",
                checked     = prefs.continuousListening,
                onChecked   = onToggleContinuous
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Speech Rate
            Text(text = "Speech Rate: %", color = CypherTextSecondary, fontSize = 13.sp)
            Slider(
                value        = speechRate,
                onValueChange = { speechRate = it; onSpeechRateChanged(it) },
                valueRange   = 0.5f..2.0f,
                steps        = 14,
                colors       = SliderDefaults.colors(thumbColor = CypherCyan, activeTrackColor = CypherCyan, inactiveTrackColor = CypherBgDark)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pitch
            Text(text = "Voice Pitch: %", color = CypherTextSecondary, fontSize = 13.sp)
            Slider(
                value        = pitch,
                onValueChange = { pitch = it; onPitchChanged(it) },
                valueRange   = 0.5f..2.0f,
                steps        = 14,
                colors       = SliderDefaults.colors(thumbColor = CypherPurple, activeTrackColor = CypherPurple, inactiveTrackColor = CypherBgDark)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Language picker
            if (availableLanguages.isNotEmpty()) {
                Text(text = "Language", color = CypherTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val currentLocale = if (prefs.selectedLanguageTag == "default") Locale.getDefault()
                                    else Locale.forLanguageTag(prefs.selectedLanguageTag)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableLanguages.take(4).forEach { locale ->
                        val isSelected = locale.toLanguageTag() == currentLocale.toLanguageTag()
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) CypherCyan.copy(alpha = 0.2f) else CypherBgDark, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) CypherCyan else CypherTextMuted.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { onLanguageSelected(locale) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text       = locale.toLanguageTag(),
                                color      = if (isSelected) CypherCyan else CypherTextSecondary,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice selector
            if (availableVoices.isNotEmpty()) {
                Text(text = "TTS Voice", color = CypherTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableVoices.take(6).forEach { voice ->
                        val isSelected = voice.name == prefs.selectedVoiceName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) CypherCyan.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isSelected) CypherCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onVoiceSelected(voice) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = voice.displayName, color = CypherTextPrimary, fontSize = 13.sp)
                                Text(
                                    text     = " \u2022 ",
                                    color    = CypherTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            if (voice.requiresNetwork) {
                                Text(text = "NETWORK", color = CypherOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable Settings Toggle Row
// ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = CypherTextPrimary, fontSize = 14.sp)
            Text(description, color = CypherTextMuted, fontSize = 11.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(
                checkedThumbColor = CypherCyan,
                checkedTrackColor = CypherBlue
            )
        )
    }
}
'''

with open("app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt", "a", encoding="utf-8") as f:
    f.write(PART5)
print("Part5 written:", len(PART5), "chars")
