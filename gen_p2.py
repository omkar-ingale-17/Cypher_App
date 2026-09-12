PART2 = '''
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.userPreferences
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(uiState.isOnboardingOpen) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var hasMicPerm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPerm = isGranted
        if (isGranted) viewModel.onMicTapped(true)
    }

    LaunchedEffect(uiState.isOnboardingOpen) {
        showNameDialog = uiState.isOnboardingOpen
    }

    LaunchedEffect(commandHistory.size) {
        if (commandHistory.isNotEmpty()) listState.animateScrollToItem(0)
    }

    if (showNameDialog) {
        UserNameDialog(
            currentName = prefs.userName,
            onDismiss = { viewModel.onDismissOnboarding() },
            onSave    = { name -> viewModel.onSetUserName(name) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(CypherBgDark),
        containerColor = CypherBgDark,
        topBar = {
            CypherTopBar(
                userName        = prefs.userName,
                voiceState      = uiState.voiceState,
                continuousActive = prefs.continuousListening && prefs.wakeWordEnabled,
                onSettingsClick  = { showSettingsSheet = true },
                onProfileClick   = { showNameDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CypherCyan.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.26f),
                            radius = size.width * 0.75f
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                CypherGlowingOrb(
                    voiceState = uiState.voiceState,
                    audioLevel = uiState.rmsLevel,
                    onClick    = {
                        if (!hasMicPerm) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.onMicTapped(true)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                CypherStatusCard(
                    voiceState     = uiState.voiceState,
                    liveTranscript = uiState.liveTranscript,
                    lastResponse   = uiState.lastResponse,
                    statusMessage  = uiState.statusMessage
                )

                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter   = fadeIn() + slideInVertically(),
                    exit    = fadeOut() + slideOutVertically()
                ) {
                    uiState.errorMessage?.let { err ->
                        CypherErrorBanner(errorMessage = err, onDismiss = { viewModel.dismissError() })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text         = "COMMAND LOG",
                        color        = CypherCyan,
                        fontSize     = 12.sp,
                        fontWeight   = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily   = FontFamily.Monospace
                    )
                    if (commandHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CypherTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (commandHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text       = "Say \"Cypher\" or tap the Orb to begin",
                                color      = CypherTextMuted,
                                fontSize   = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(items = commandHistory, key = { it.id }) { record ->
                                CommandHistoryCard(record = record)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value       = textInput,
                        onValueChange = { textInput = it },
                        modifier    = Modifier.weight(1f).height(52.dp),
                        placeholder = { Text("Type command or question...", color = CypherTextMuted, fontSize = 13.sp) },
                        singleLine  = true,
                        colors      = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = CypherCyan,
                            unfocusedBorderColor = CypherCardBg,
                            focusedContainerColor   = CypherCardBg,
                            unfocusedContainerColor = CypherCardBg,
                            focusedTextColor     = CypherTextPrimary,
                            unfocusedTextColor   = CypherTextPrimary,
                            cursorColor          = CypherCyan
                        ),
                        shape          = RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) { viewModel.onTextCommandSubmitted(textInput); textInput = "" }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick   = { if (textInput.isNotBlank()) { viewModel.onTextCommandSubmitted(textInput); textInput = "" } },
                        modifier  = Modifier.size(48.dp).background(
                            brush = Brush.linearGradient(listOf(CypherCyan, CypherBlue)), shape = CircleShape
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = CypherBgDark, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        VoiceSettingsBottomSheet(
            prefs                   = prefs,
            availableVoices         = uiState.availableVoices,
            availableLanguages      = uiState.availableLanguages,
            onDismiss               = { showSettingsSheet = false },
            onSpeechRateChanged     = viewModel::onSpeechRateChanged,
            onPitchChanged          = viewModel::onPitchChanged,
            onLanguageSelected      = viewModel::onLanguageSelected,
            onVoiceSelected         = viewModel::onVoiceSelected,
            onToggleContinuous      = viewModel::onToggleContinuousListening,
            onToggleWakeWord        = viewModel::onToggleWakeWord
        )
    }
}
'''

with open("app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt", "a", encoding="utf-8") as f:
    f.write(PART2)
print("Part2 written:", len(PART2), "chars")
