# Changelog

## Unreleased

### Added

- `kit-protocol`: the wire contract, protocol version 1, with its OpenRPC document.
- `kit`: `Visor.connect`, the glasses' status, photos and videos, the captures on the glasses and their import,
  the Neural Band's status, gestures and vibration, and the Display's teleprompter. `RequestScopes` asks the
  person for scopes on Visor's consent screen, where they may allow them only this time.
- `glasses.inputs`, with the `inputs` scope: each touch of the touchpad and each press of a button, and on the
  Display DAT's navigation (nav, select, back, drags) from the touchpad and the band, with a sources filter and
  `keepBack`.
- `glasses.motion`, with the `motion` scope: the accelerometer, gyroscope, magnetometer and orientation at 5 to 60
  samples a second. Visor reads them from the glasses' own sensor service, with nothing on their screen, and fuses
  the orientation on the phone; FakeVisor also serves the band's motion.
- `display.cards.set` and `display.cards.remove`: an app's own cards on the Display home's Briefing, beside Meta's
  weather and sports, kept until the app removes them, each with an optional `detail` that a click opens full
  screen, or pages the wearer swipes through. FakeVisor keeps them in `fake.display.cards`.
- Layouts look like Meta's own cards: the `title` text style, any colour (`LayoutColor.rgb`), `maxLines` and text
  `align`, icon `color` and `size`, box `background` and `height`, `pin*` to place an element over the others, a
  `pager` of pages with dots, and `chart` (a line or bars, drawn by Visor on the phone). The building blocks
  `header`, `stat`, `change`, `field` and `progress`; Meta's Briefing glyphs and its 14 weather illustrations in
  `LayoutIconName`.
- `image(bitmap)` on Android: a photo from the app inside a layout or a card, scaled to the Display. Visor scales
  `data:` images larger than 600 px a side. A card and its detail take at most 256 KiB (`BriefingCard.MAX_BYTES`),
  checked by the client, Visor and FakeVisor alike.
- `display.cards.keep`: a card that follows a `Flow` of the app's state. While an app has cards on the Briefing,
  the Display asks for it every 15 seconds.
- Python `visor_kit.layout` and TypeScript `visor-kit/layout`: layouts and Briefing cards with the Kotlin builder's
  pieces and blocks; `image_file(path)` in Python.
- `display.cards.clicks`: clicks on buttons and boxes in a card's detail reach the card's app; `toast` on the
  element confirms one on the Display. FakeVisor: `fake.display.clickCard(card, id)`.
- The Display simulator shows the Briefing with the app's cards and opens their details; kit-testing has
  `fake.display.briefingPreview(context)`. The `visor` command: `card FILE`, `call METHOD @FILE` and `-` for
  standard input, `--fake=PORT`, and an error that names the port and the cause when no fake answers.
- `kit-testing` serves FakeVisor's imported captures and live photos to the app under test, with the bytes the
  test gives them: `fake.glasses.add(type, bytes, mime)`, `fake.live.photo(bytes, mime)`.
- `voice.speak`, with the `voice` scope: the glasses say a line in their own voice, unasked. On the Display;
  FakeVisor keeps what was said in `fake.voice.spoken`.
- Videos in layouts on the Display, with `display.video`: `starting`, `playing`, `ended`, `failed`.
- `glasses.inputs` on the Display: while a layout shows, `nav_*`, `select`, `back` and drags with `x`, `y`, `dx`
  and `dy`, as DAT gives them.
- `band.gestures` leaves the band on the Display: the gestures come through a session with it, and `derived` carries
  the same gesture as `action`.
- `display.layout.show`, `display.layout.close` and `display.layout.clicks`: layouts with DAT's elements (columns
  and rows, text, images, the Display's own icons, buttons, button bars and a video) and their styles, with clicks
  back. `display.layout.state` says when the layout goes and why, and `display.video` follows the video. Visor
  shows them through Meta's toolkit app on the Display, videos aside; FakeVisor serves everything.
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
- AI tools: `visor docs` searches the docs, `visor docs-mcp` serves them to coding assistants over MCP, `visor
  agents` prints guidance to save as AGENTS.md, a Claude Code plugin with skills, and `docs/llms.txt`.
- `kit-fake`: FakeVisor in plain Kotlin, and `kit-server`: the server engine, both for the JVM.
- FakeVisor plays DAT's motion CSV (`playMotion`) and a recorded H.264 or H.265 stream as the live camera
  (`FakeLive.play`); `visor fake` has `motion-csv` and `camera` for them.
- `settings.list`, `settings.get`, `settings.set` and `settings.changes`, with the `settings` scope: the glasses'
  settings with their labels, choices and ranges, and changes to the everyday ones.
- `assistant.turns` and `assistant.history`, with the `conversations` scope: what the wearer asks the assistant
  and its answers, as they come and from the history Visor keeps on the phone.
- Python client: `open()` subscribes before the app acts and returns a `Stream`. Python samples in
  `samples/python`: a prompter pager, an assistant journal and a settings tool.
- `samples/web`: a console for the glasses in one page, on JSON-RPC over WebSocket.
- `visor record` saves topics' events as JSON lines, and `visor replay` plays them into the apps at their pace,
  on a phone or on `visor fake`. `FakeVisor.replay` plays them in JVM tests.
