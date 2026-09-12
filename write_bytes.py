import sys
mode = 'wb' if len(sys.argv) > 2 and sys.argv[2] == 'init' else 'ab'
data = bytes(map(int, sys.argv[1].split()))
with open(r'app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt', mode) as out:
    out.write(data)
print('Wrote', len(data), 'bytes')
