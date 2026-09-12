PART3 = '''
// ─────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────
@Composable
private fun CypherTopBar(
    userName: String,
    voiceState: VoiceState,
    continuousActive: Boolean,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CypherCardBg, CircleShape)
                    .border(1.dp, CypherCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = CypherCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text          = "CYPHER AI",
                    color         = CypherTextPrimary,
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text       = if (userName.isNotBlank()) "User: " else "Tap to set name",
                    color      = CypherCyan,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (continuousActive) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(CypherGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, CypherGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("LIVE", color = CypherGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
            IconButton(
                onClick  = onSettingsClick,
                modifier = Modifier.size(38.dp).background(CypherCardBg, CircleShape).border(1.dp, CypherTextMuted.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CypherCyan, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Glowing Orb
// ─────────────────────────────────────────────────────────────
@Composable
private fun CypherGlowingOrb(
    voiceState: VoiceState,
    audioLevel: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val isActive = voiceState == VoiceState.LISTENING
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = if (isActive) 1.22f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation    = tween(durationMillis = if (isActive) 700 else 2200, easing = FastOutSlowInEasing),
            repeatMode   = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val primaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.IDLE       -> CypherCyan
            VoiceState.LISTENING  -> CypherCyan
            VoiceState.PROCESSING -> CypherOrange
            VoiceState.SPEAKING   -> CypherGreen
            VoiceState.ERROR      -> CypherRed
        },
        animationSpec = tween(400), label = "OrbColor"
    )
    val secondaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.IDLE       -> CypherBlue
            VoiceState.LISTENING  -> CypherBlue
            VoiceState.PROCESSING -> CypherPink
            VoiceState.SPEAKING   -> CypherCyan
            VoiceState.ERROR      -> CypherOrange
        },
        animationSpec = tween(400), label = "OrbSecondary"
    )

    val dynamicScale = (1.0f + (audioLevel.coerceIn(0f, 100f) / 100f) * 0.35f) * pulseScale

    Box(
        modifier = Modifier
            .size(190.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient outer glow
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(dynamicScale)
                .blur(32.dp)
                .background(
                    brush = Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.5f), Color.Transparent)),
                    shape = CircleShape
                )
        )
        // Mid halo ring
        Box(
            modifier = Modifier
                .size(135.dp)
                .scale(pulseScale)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    shape = CircleShape
                )
        )
        // Core orb
        Box(
            modifier = Modifier
                .size(105.dp)
                .background(
                    brush = Brush.radialGradient(listOf(primaryColor, secondaryColor, CypherBgDark)),
                    shape = CircleShape
                )
                .border(2.dp, primaryColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (voiceState) {
                    VoiceState.PROCESSING -> Icons.Default.Refresh
                    VoiceState.SPEAKING   -> Icons.Default.VolumeUp
                    VoiceState.ERROR      -> Icons.Default.MicOff
                    else                  -> Icons.Default.Mic
                },
                contentDescription = "Orb",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}
'''

with open("app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt", "a", encoding="utf-8") as f:
    f.write(PART3)
print("Part3 written:", len(PART3), "chars")
