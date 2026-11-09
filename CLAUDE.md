# Visor Development Kit (VDK)

The developer kit for Visor, the Android app that runs Meta's glasses. Apps and computers use the glasses through
Visor: their state, camera and captures, the Neural Band, and the Display. Visor itself lives in `../visor`
(github.com/macedonga/visor, AGPL); this repo is Apache-2.0 so apps can link it. Visor serves the VDK from 3.0.0.

## Modules

- `protocol` (`kit-protocol`, JVM): the wire contract. Every method, topic, feed, type and error code, in
  `Methods`, `Topics`, `Feeds`, `Shell` and `JsonRpc`. Both sides compile against it.
- `server` (`kit-server`, JVM): the server engine Visor and FakeVisor share: `Dispatcher`, `Routes`, `Gate`,
  `JsonRpcSession` and `McpAdapter`. `KitException` lives in `kit-protocol`.
- `android` (`kit`, AAR): the client apps use (`Visor`, `RequestScopes`), the frozen AIDL, and the Android doors
  of the server (`server/`: `KitServer` for binder, `ShellServer` for adb).
- `fake` (`kit-fake`, JVM): `FakeVisor` and its parts, in plain Kotlin.
- `testing` (`kit-testing`, AAR): `FakeVisor.connect()`, the fake in the app's own process, for tests.
- `tools/cli` (not on Maven): the `visor` command. It reaches Visor through `adb forward` to the abstract socket
  `visor-kit`, which only the shell uid may use, and speaks JSON-RPC lines. `visor fake` serves `kit-fake` on
  /rpc, /mcp and a line socket for `visor --fake`, with a console that plays the glasses.
- `tools/contract` (not published): writes `protocol/api/openrpc.json` and checks it against the last release.
- `clients/python`, `clients/typescript`: JSON-RPC clients, over adb or the network door, on PyPI and npm.

Apps need only the scopes the person grants on the consent screen. Visor's Developer mode opens nothing for apps:
it only opens the adb socket, for the computer-side tools.

Maven group `com.macedonga.visor`, namespace `com.macedonga` proven by a TXT record on macedonga.com.

## Compatibility rules

An app built against any released VDK must keep working with any later Visor, and a later app with an earlier
Visor. Either side of a connection can be the older one. The checks below run in `./gradlew check` and in CI;
a rule they cannot see is still a rule.

The wire, checked by `:contract:checkContract` against the last `v*` tag:

1. A released method, topic, feed or type is never removed or renamed, and keeps its scope, its `since` and its
   result, event or header.
2. A released field keeps its name, its type, and whether it is required.
3. A new field is optional and has a default, so a peer that does not know it still works.
4. A set of values that can grow (scopes, error codes, kinds, gesture actions) is a string value class with the
   known values in its companion. Never an enum: an older app must not crash on a newer value.
5. Anything new raises `KitProtocol.VERSION` by one, and a new method, topic or feed has `since` equal to it.
6. Meaning is part of the contract. A method keeps doing what it did; new behavior is a new method or an
   optional field that asks for it.
7. A new failure uses an existing `ErrorCode` when one fits. Clients treat unknown codes as `failed`.

Binder:

8. The AIDL in `android/src/main/aidl` is frozen. Transaction codes never change; a new method takes the next
   free code. Messages stay JSON strings, and a feed's bytes go over a socket the app hands over, framed as
   `KitProtocol.FEED_END` says, so the AIDL needs no other type.

Kotlin API, checked by `checkAbi` (the dump in each module's `api/`) and `checkAbiCompatibility` (against the last
`v*` tag):

9. Explicit API mode. Every change to public API shows in `api/*.api`: run `./gradlew updateAbi`, read the diff,
   commit it. A line that disappears since the last release fails the build.
10. No data classes in public API. Public value types use `@Poko`, so a new field breaks no `copy` or `componentN`.
11. No default arguments in public functions or constructors: add an overload. Only `@KitInternal` constructors
    have defaults, which is why the ABI checks may skip their bridges.
12. Constructors of wire types are `@KitInternal`. A type that apps build gets a `Builder` and a factory function,
    as `PrompterStyle` does.
13. Removing public API takes three releases: deprecated in a minor (warning), error in the next minor, gone in
    the next major.
14. No new public sealed type or enum unless the set is closed by nature.

Visor's side, in the Visor repo:

15. Visor answers every released method for good. When a firmware update or Meta takes a capability away, the
    method fails with `unsupported` or `refused`. It never hangs and never pretends to succeed: every wait on the
    glasses has a timeout.
16. Developer mode's intents (`com.macedonga.visor.action.*`) are public API too, under the same rules.

## Docs

The docs live only in the site repo, visor-site (`src/content/docs`, served at visor.macedonga.com/developers/docs),
next to the protocol pages (`src/content/protocol`). This repo has no docs folder: a change to the VDK updates the
page there, and links here point at the site. The `visor` command bundles the docs from that folder, so a build
needs visor-site checked out beside visor-kit, or `-PvdkDocs=<path>`.

User docs (the READMEs, the site's docs) say what each feature does, never whether it was tried on the glasses.

## Working on it

- A change starts in `protocol`, then the client, then FakeVisor, then Visor.
- `./gradlew check` runs every guard. After a deliberate change: `./gradlew updateAbi :contract:updateContract`.
- Visor builds the VDK from source through `includeBuild("../visor-kit")` while this folder sits next to it.
- On the phone: `./gradlew :cli:installDist`, then `tools/cli/build/install/visor/bin/visor status`.
- No test suites, as in Visor: the glasses, FakeVisor and the `visor` command are the check.
- Short comments that say why. One name for one thing. Versions only in `gradle/libs.versions.toml`, stable and at
  least two weeks old.
- The release key and tokens never go in the repo.

## Releases

1. Move `## Unreleased` in `CHANGELOG.md` under `## X.Y.Z` (Added, Changed, Fixed).
2. Tag `vX.Y.Z` and push it. `release.yml` runs every check against the tag before it, puts the `visor` command's
   zip on the GitHub release, publishes the libraries to Maven Central, signed, the TypeScript client to npm and
   the Python client to PyPI, and a copy of the libraries and the npm package on GitHub Packages. npm and PyPI trust
   the workflow as their publisher; Maven Central takes the `MAVEN_CENTRAL_*` and `SIGNING_KEY*` secrets.
3. A run by hand with a released tag publishes what is missing for it, with the workflow from main; a run with no
   tag is a dry run.
4. Bump the VDK in Visor's `gradle/libs.versions.toml` and release Visor.

Library versions are semver. `KitProtocol.VERSION` counts separately and only goes up.

## Meta's DAT

Every capability of Meta's Wearables Device Access Toolkit has a VDK equivalent, mapped in the docs'
`porting-from-dat.md`. DAT's docs: https://wearables.developer.meta.com/docs/develop/dat/getting-started-toolkit

| DAT | VDK |
|---|---|
| `Wearables`, registration, permissions | `Visor.connect`, `RequestScopes` |
| `DeviceSession` and its states | `VisorState` |
| `DeviceSelector` | `glasses.pairs`, `glasses.select` |
| `Device`: battery, charging, don, hinge, link, thermal, display | `glasses.status` |
| Camera stream: quality, frame rate, HEVC or decoded | `camera.live`, `LiveVideoDecoder` |
| Stream audio (PCM) | `camera.live` audio, `LiveAudioDecoder` |
| Photo during a stream; `PhotoResolution`, `PhotoQuality` | `camera.live.photo` (HEIC, full size) |
| Speech: on-device transcription | `voice.transcripts` |
| Voice invocations (`LaunchApp`) | `voice.requests`, `VoiceLaunch` |
| Motion: accelerometer, gyroscope, magnetometer, orientation | `glasses.motion` |
| Inputs: nav, select, back, button, capture, drag | `glasses.inputs`, `band.gestures` |
| Display: FlexBox, Text, Image, Button, Icon, Video, taps back | `display.layout.*` |
| `MockDeviceKit` | `FakeVisor`; a new capability gets its fake part too |

Beyond DAT: Briefing cards (`display.cards.*`), `voice.speak`, `settings.*`, `assistant.*`, `notify.post`, the
teleprompter (`display.prompter.*`), webhooks, the network door and MCP, `visor fake` and its simulator, the band
shared with the phone and `band.motion`, and the VDK in the Display's web apps with their WebMCP tools.
