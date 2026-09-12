PART4 = '''
// ─────────────────────────────────────────────────────────────
// Status Card
// ─────────────────────────────────────────────────────────────
@Composable
private fun CypherStatusCard(
    voiceState: VoiceState,
    liveTranscript: String,
    lastResponse: String,
    statusMessage: String
) {
    val statusColor = when (voiceState) {
        VoiceState.IDLE       -> CypherCyan
        VoiceState.LISTENING  -> CypherCyan
        VoiceState.PROCESSING -> CypherOrange
        VoiceState.SPEAKING   -> CypherGreen
        VoiceState.ERROR      -> CypherRed
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors   = CardDefaults.cardColors(containerColor = CypherCardBg),
        shape    = RoundedCornerShape(16.dp),
        border   = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CypherCyan.copy(alpha = 0.3f), CypherBlue.copy(alpha = 0.1f)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = statusMessage,
                color         = statusColor,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            val displayText = when {
                liveTranscript.isNotBlank() -> "\"\""
                lastResponse.isNotBlank()   -> lastResponse
                else                        -> "Ready for command"
            }

            Text(
                text       = displayText,
                color      = if (liveTranscript.isNotBlank()) CypherTextPrimary else CypherTextSecondary,
                fontSize   = 14.sp,
                fontWeight = if (liveTranscript.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                textAlign  = TextAlign.Center,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Command History Card
// ─────────────────────────────────────────────────────────────
@Composable
private fun CommandHistoryCard(record: CommandHistoryEntity) {
    val df = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val time = remember(record.timestampMs) { df.format(Date(record.timestampMs)) }
    val intentLabel = record.intentType.name.replace("_", " ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CypherCardBg),
        shape    = RoundedCornerShape(12.dp),
        border   = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CypherCyan.copy(alpha = 0.15f), Color.Transparent))
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = intentLabel, color = CypherCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = time, color = CypherTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    if (!record.success) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "FAILED", color = CypherRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = record.rawText, color = CypherTextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (record.responseMessage.isNotBlank() && record.responseMessage != record.rawText) {
                    Text(
                        text     = "\u2192 ",
                        color    = CypherGreen,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Error Banner
// ─────────────────────────────────────────────────────────────
@Composable
private fun CypherErrorBanner(errorMessage: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors   = CardDefaults.cardColors(containerColor = CypherRed.copy(alpha = 0.15f)),
        shape    = RoundedCornerShape(10.dp),
        border   = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CypherRed, CypherPink))
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = errorMessage, color = CypherTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = CypherRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
'''

with open("app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt", "a", encoding="utf-8") as f:
    f.write(PART4)
print("Part4 written:", len(PART4), "chars")
