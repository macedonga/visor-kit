# Changelog

## Unreleased

### Added

- `kit-protocol`: the wire contract, protocol version 1, with its OpenRPC document.
- `kit`: `Visor.connect`, the glasses' status, photos and videos, the captures on the glasses and their import,
  the Neural Band's status, gestures and vibration, and the Display's teleprompter. `RequestScopes` asks the
  person for scopes on Visor's consent screen.
- `kit-testing`: `FakeVisor`, Visor in the app's own process.
- The `visor` command: call any method, watch any topic, trace an app's traffic, simulate the band or the glasses
  and inject events, and grant scopes, from a computer through adb.
