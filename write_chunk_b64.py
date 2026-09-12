import sys, base64
target = r'app/src/main/kotlin/com/cypher/assistant/features/voice/VoiceScreen.kt'
mode = 'wb' if len(sys.argv) > 2 and sys.argv[2] == 'init' else 'ab'
decoded = base64.b64decode(sys.argv[1])
with open(target, mode) as out:
    out.write(decoded)
print('Appended', len(decoded), 'bytes to VoiceScreen.kt')
