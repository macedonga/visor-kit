---
name: vdk-testing
description: Test VDK apps without glasses: FakeVisor, visor fake, the Display simulator, record and replay, recorded feeds
---

# Testing without glasses

- Unit tests: `val fake = FakeVisor()`, `val visor = fake.connect()` (`kit-testing`). Drive it with
  `fake.glasses.*`, `fake.band.gesture(...)`, `fake.voice.say(...)`, `fake.display.click(id)`,
  `fake.display.back()`.
- On a computer: `visor fake` serves JSON-RPC on `ws://127.0.0.1:7333/rpc`, MCP on `/mcp`, and the Display
  simulator on `/display`; its console plays the glasses (type `help`).
- `visor record FILE` saves what the wearer did; `visor replay FILE` plays it back, on a phone or the fake.
- Recorded feeds: `fake.glasses.playMotion(reader, loop)` for DAT's motion CSV, `fake.live.play(...)` for an
  H.264 or H.265 stream.

Details: `read_vdk_doc` with `testing`.
