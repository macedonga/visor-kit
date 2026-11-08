# Changelog

## Unreleased

### Added

- `kit-protocol`: the wire contract, protocol version 1, with its OpenRPC document.
- `kit`: `Visor.connect`, the glasses' status, photos and videos, the captures on the glasses and their import,
  the Neural Band's status, gestures and vibration, and the Display's teleprompter. `RequestScopes` asks the
  person for scopes on Visor's consent screen.
- `glasses.inputs`, with the `inputs` scope: each touch of the touchpad and each press of a button, and on the
  Display DAT's navigation (nav, select, back, drags) from the touchpad and the band, with a sources filter and
  `keepBack`.
- `glasses.motion`, with the `motion` scope: the accelerometer, gyroscope, magnetometer and orientation at 5 to 60
  samples a second, as Meta's toolkit gives them. FakeVisor serves it; Visor does not yet.
- `display.layout.show`, `display.layout.close` and `display.layout.clicks`: layouts with DAT's elements (columns
  and rows, text, images, the Display's own icons, buttons, button bars and a video) and their styles, with clicks
  back. `display.layout.state` says when the layout goes and why, and `display.video` follows the video. FakeVisor
  serves them; Visor does not yet.
- `VoiceLaunch`: an app that is not running is started by a phrase its activity declares, as DAT's voice
  invocations start one, and answers with `voice.reply`.
- `voice.transcripts`, with the `microphone` scope: what the glasses hear, as text, from the phone's recogniser,
  with its confidence, and the text so far while the wearer speaks when the app asks for partials.
  The client binds with `BIND_INCLUDE_CAPABILITIES`, so Visor may record while the app is on screen.
- Feeds, streams of binary records, over a socket for apps and as Base64 events over JSON-RPC. `camera.live`
  streams the glasses' camera, encoded, with their microphones when asked; `LiveVideoDecoder` and
  `LiveAudioDecoder` decode it to a Surface, to images or to PCM. `camera.live.photo` takes a photo meanwhile.
- `kit-testing`: `FakeVisor`, Visor in the app's own process, with `FakeLive` for the live camera.
- The `visor` command: call any method, watch any topic, trace an app's traffic, simulate the band or the glasses
  and inject events, and grant scopes, from a computer through adb. `visor live` saves or plays the camera live.
- `visor fake`: FakeVisor on a computer, on the network door's WebSocket and MCP endpoints and for `visor --fake`,
  with a console that plays the glasses. The Python client's `connect_fake()` and the TypeScript client's
  `Visor.connectFake()` reach it.
- `kit-fake`: FakeVisor in plain Kotlin, and `kit-server`: the server engine, both for the JVM.
- `settings.list`, `settings.get`, `settings.set` and `settings.changes`, with the `settings` scope: the glasses'
  settings with their labels, choices and ranges, and changes to the everyday ones.
- `assistant.turns` and `assistant.history`, with the `conversations` scope: what the wearer asks the assistant
  and its answers, as they come and from the history Visor keeps on the phone.
- Python client: `open()` subscribes before the app acts and returns a `Stream`. Python samples in
  `samples/python`: a prompter pager, an assistant journal and a settings tool.
- `samples/web`: a console for the glasses in one page, on JSON-RPC over WebSocket.
- `visor record` saves topics' events as JSON lines, and `visor replay` plays them into the apps at their pace,
  on a phone or on `visor fake`. `FakeVisor.replay` plays them in JVM tests.
