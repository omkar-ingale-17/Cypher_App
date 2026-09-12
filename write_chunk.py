import sys
target = r'app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt'
mode = 'w' if len(sys.argv) > 2 and sys.argv[2] == 'init' else 'a'
with open(target, mode, encoding='utf-8') as out:
    out.write(sys.argv[1])
print('Wrote chunk, mode:', mode)
