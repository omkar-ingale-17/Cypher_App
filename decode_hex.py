open(r'app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt', 'wb').write(bytes.fromhex(open('hex.txt').read().replace('\n','').replace('\r','').strip()))
