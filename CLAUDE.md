# Visor Development Kit (VDK)

The developer kit for Visor, the Android app that runs Meta's glasses. Apps and computers use the glasses through
Visor: their state, camera and captures, the Neural Band, and the Display. Visor itself lives in `../visor`
(github.com/macedonga/visor, AGPL); this repo is Apache-2.0 so apps can link it.

Nothing here is public until the whole roadmap below is built: the GitHub repo stays private, nothing goes to Maven
Central, no tags. Then the history is reset to one commit and the documentation moves to visor.macedonga.com.

## Modules

- `protocol` (`kit-protocol`, JVM): the wire contract. Every method, topic, feed, type and error code, in
  `Methods`, `Topics`, `Feeds`, `Shell` and `JsonRpc`. Both sides compile against it.
- `server` (`kit-server`, JVM): the server engine Visor and FakeVisor share: `Dispatcher`, `Routes`, `Gate`,
  `JsonRpcSession` and `McpAdapter`. `KitException` lives in `kit-protocol`.
- `android` (`kit`, AAR): the client apps use (`Visor`, `RequestScopes`), the frozen AIDL, and the Android doors
  of the server (`server/`: `KitServer` for binder, `ShellServer` for adb).
- `fake` (`kit-fake`, JVM): `FakeVisor` and its parts, in plain Kotlin.
- `testing` (`kit-testing`, AAR): `FakeVisor.connect()`, the fake in the app's own process, for tests.
- `tools/cli` (not published): the `visor` command. It reaches Visor through `adb forward` to the abstract socket
  `visor-kit`, which only the shell uid may use, and speaks JSON-RPC lines. `visor fake` serves `kit-fake` on
  /rpc, /mcp and a line socket for `visor --fake`, with a console that plays the glasses.

Apps need only the scopes the person grants on the consent screen. Visor's Developer mode opens nothing for apps:
it only opens the adb socket, for the computer-side tools.
- `tools/contract` (not published): writes `protocol/api/openrpc.json` and checks it against the last release.
- `clients/python`, `clients/typescript` (not published yet): JSON-RPC clients, over adb or the network door.

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

The docs live only in the site repo, visor-site (`src/content/docs`, served at visor.macedonga.com/developers/docs),
next to the protocol pages (`src/content/protocol`). This repo has no docs folder: a change to the VDK updates the
page there, and links here point at the site. The `visor` command bundles the docs from that folder, so a build
needs visor-site checked out beside visor-kit, or `-PvdkDocs=<path>`.

User docs (README, the site's docs, client READMEs) say what each feature does, never whether it was tried on the glasses:
the user, 2026-11-09, "The users do not care about if something has been tried or not". Tried status lives here and
in Visor's memory only.

Publishing: the release workflow publishes Maven Central, the visor command, npm (`visor-kit`, trusted publishing
from the Release environment) and PyPI (`visor-kit`, trusted publishing). The libraries and the npm package also get a copy on GitHub Packages
(`@macedonga/visor-kit` for npm), so the repository's page lists them; PyPI has no such registry on a `v*.*.*` tag. 0.1.0 was tagged on 2026-11-09, at the user's word, when the
repo went public. Maven Central is its own job: it needs the com.macedonga namespace verified and the SIGNING_KEY
secrets (release key 52BDFC0453E2AA07, kept in the user's keys folder), and is re-run alone from the release run.

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
2. Tag `vX.Y.Z` and push it. `release.yml` runs every check against the tag before it, publishes the libraries
   to Maven Central signed, and puts the `visor` command's zip on the GitHub release. Run it by hand for a dry run.
3. Bump the VDK in Visor's `gradle/libs.versions.toml` and release Visor.

Library versions are semver. `KitProtocol.VERSION` counts separately and only goes up.

## Roadmap

Status as of 2026-11-09: phases 1 to 5 are built. On the glasses, tried means on the Meta Ray-Ban Display (A024 phone)
unless said; Visor's own record is visor/memory/vdk.md.

Parity with Meta's DAT (1.0.1) is a goal: every DAT capability gets a kit equivalent, and the docs' `porting-from-dat.md`
maps all of it (written 2026-11-07 from github.com/meta/meta-wearables-dat-android). Status:

| DAT | Kit | Status |
|---|---|---|
| `Wearables`, registration, permissions | `Visor.connect`, `RequestScopes` | done |
| `DeviceSession` and its states | `VisorState` | done |
| `DeviceSelector` | `glasses.pairs`, `glasses.select` | done |
| `Device`: battery, charging, don, hinge, link, thermal, display | `glasses.status` | done |
| Camera stream: quality, frame rate, HEVC or decoded | `camera.live`, `LiveVideoDecoder` | built, not tried on glasses (the camera only with the user's go) |
| Stream audio (PCM) | `camera.live` audio, `LiveAudioDecoder` | built, not tried on glasses |
| Photo during a stream; `PhotoResolution`, `PhotoQuality` | `camera.live.photo` (HEIC, full size) | built, not tried on glasses |
| Speech: on-device transcription | `voice.transcripts` | Ray-Ban Meta: the headset mic into the phone's recogniser, not tried. The Display: the toolkit app's speech, built 2026-11-09, not tried |
| Voice invocations (`LaunchApp`) | `voice.requests`, `VoiceLaunch` | done |
| Motion: accelerometer, gyroscope, magnetometer, orientation | `glasses.motion` | tried: the glasses' data collection daemon (svc1027), nothing on screen |
| Inputs: nav, select, back, button, capture, drag | `glasses.inputs`, `band.gestures` | tried while a layout shows (the toolkit app's input capability); outside layouts, touchpad taps are not found |
| Display: FlexBox, Text, Image, Button, Icon, Video, taps back | `display.layout.*` | tried: through Visor's own toolkit session, videos included |
| `MockDeviceKit` | `FakeVisor` | done; add the new capabilities as they come |

Beyond DAT: `display.cards.*` (Briefing cards; cards, details, pages, colours, images and clicks in a detail tried;
charts, weather illustrations, toasts and live cards not yet),
`voice.speak` (tried), `settings.*` (tried), `assistant.*`, `notify.post`, `display.prompter.*` (tried), webhooks
(tried), the network door and MCP, `visor fake` and its simulator.

0. Measure on the glasses, with the user: touchpad and capture button events on glasses without a display (the
   Gen 2 pair); `assistant.turns` live. Dropped by the user (2026-11-09): touchpad data outside layouts on the
   Display ("what's an actual use case?"). `band.buzz` as it is, borrowing the band, is "useless if it disconnects
   from the glasses": a shared band (below) buzzes without that. Not tried yet: the shared band and `band.motion`.
1. Done: glasses status, camera, captures, band, the Display's prompter; Visor's kit service, consent screen and
   adb socket; the `visor` command; FakeVisor; the sample app.
2. Done: voice requests and replies in the glasses' own voice, tools for the user's model, notifications on the
   Display, lines the glasses say unasked.
3. Done: JSON-RPC over WebSocket, MCP and webhooks on 127.0.0.1:7333 (LAN opt-in) with per-client tokens, the
   raw scope (Developer mode only; 36, 54, 79 and 29 refused), Python and TypeScript clients in `clients/`.
4. The Display: Briefing cards, done. The band shared with the phone, built 2026-11-09, not tried: the phone keeps
   the band and the Display reaches it through Visor's AirShield relay, as on Meta's RELAY lease, so `band.gestures`
   come straight from the band, `band.buzz` goes out at once and `band.motion` reads the wrist. A sharing that does
   not work turns itself off and gives the band back to the Display. Built 2026-11-09, not tried: the VDK in the
   Display's web apps (`Visor.connectGlasses()` in the TypeScript client), over the browser's Bluetooth scans, not
   Visor's tunnel, which pages cannot reach.
5. Done: the toolkit session (svc7009 and svc6500 through Visor's own app link), for DAT's display, input and
   speech capabilities without the Meta AI app.
   Meta's DAT docs: https://wearables.developer.meta.com/docs/develop/dat/getting-started-toolkit
