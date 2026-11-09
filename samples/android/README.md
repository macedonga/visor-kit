# VDK Lab

The Android sample: every part of the VDK an app reaches, one tab each, on whatever glasses Visor runs.

| Tab | What it uses |
|---|---|
| Glasses | `glasses.status`, `glasses.pairs` and `glasses.select`, `glasses.inputs` |
| Motion | `glasses.motion` at each rate: the head's pitch, roll and heading from `orientation`, and the three sensors |
| Camera | `camera.photo`, `camera.video.start` and `stop`, `camera.live` with its sound, `camera.live.photo`, and the captures: `list`, `import`, `available`, `progress`, `imported`, `failed` |
| Voice | `voice.transcripts`, `voice.speak`, `voice.requests` and `voice.reply`, and an activity started by voice |
| Assistant | `assistant.tools` and `assistant.tools.answer`, `assistant.turns`, `assistant.history` |
| Settings | `settings.list`, `settings.set`, `settings.changes` |
| Display and band | `display.prompter`, `display.layout` with its clicks and state, the Briefing cards of `Cards.kt`, `notify.post` and `remove`, `band.status`, `band.buzz`, `band.gestures`, `band.motion`. Only for glasses with a display. |
| Log | Each call and stream, with what Visor answered |

```
./gradlew :sample:installDebug
```

Open it and grant its scopes on Visor's consent screen. Run `visor trace` on your computer alongside to see each
exchange with Visor.

The Motion tab turns `orientation` into the head's angles with the sensor axes of Ray-Ban Meta (Gen 2): x down, y
backward, z to the left. **Set straight ahead** takes the pose now as level and as heading 0°.

With the app closed, "Hey Meta, ask VDK lab for the battery" starts `VoiceActivity`, which answers. Its phrases are
in `res/values/arrays.xml`.

The Briefing card recipes of [Briefing cards](https://visor.macedonga.com/developers/docs/briefing-cards/) are in
`Cards.kt`: a weather card, a stocks watchlist, and a kitchen timer with buttons in its detail.
