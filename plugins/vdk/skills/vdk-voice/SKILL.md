---
name: vdk-voice
description: Voice: questions that name the app, starting the app by a phrase, replies, and transcripts
---

# Voice

- `visor.voice.requests(listOf("the garage"))` brings the wearer's questions that hold one of the app's phrases while
  collected; answer each with `visor.voice.reply(request, text)` within about 20 seconds.
- To start the app by voice, declare an activity with action `com.macedonga.visor.kit.action.VOICE` and the
  meta-data `com.macedonga.visor.kit.VOICE_PHRASES` (a string array); read the question with
  `VoiceLaunch.request(intent)`.
- `visor.voice.transcripts(TranscriptRequest { partials = true })` turns what the glasses hear into text; partial
  transcripts replace each other until the finished one.

Details: `read_vdk_doc` with `voice-and-assistant`.
