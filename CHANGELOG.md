# Changelog

## Unreleased

### Added

- `kit-protocol`: the wire contract, protocol version 1, with its OpenRPC document.
- `kit`: `Visor.connect`, the glasses' status, photos and videos, the captures on the glasses and their import,
  the Neural Band's status, gestures and vibration, and the Display's teleprompter. `RequestScopes` asks the
  person for scopes on Visor's consent screen.
- `glasses.inputs`, with the `inputs` scope: each touch of the touchpad and each press of a button.
- Feeds, streams of binary records, over a socket for apps and as Base64 events over JSON-RPC. `camera.live`
  streams the glasses' camera, encoded, with their microphones when asked; `LiveVideoDecoder` and
  `LiveAudioDecoder` decode it to a Surface, to images or to PCM. `camera.live.photo` takes a photo meanwhile.
- `kit-testing`: `FakeVisor`, Visor in the app's own process, with `FakeLive` for the live camera.
- The `visor` command: call any method, watch any topic, trace an app's traffic, simulate the band or the glasses
  and inject events, and grant scopes, from a computer through adb. `visor live` saves or plays the camera live.
