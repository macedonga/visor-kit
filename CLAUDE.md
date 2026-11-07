# VDK, the Visor Development Kit

The developer kit for Visor, the Android app that runs Meta's glasses. Apps and computers use the glasses through
Visor: their state, camera and captures, the Neural Band, and the Display. Visor itself lives in `../visor`
(github.com/macedonga/visor, AGPL); this repo is Apache-2.0 so apps can link it.

Nothing here is public until the whole roadmap below is built: the GitHub repo stays private, nothing goes to Maven
Central, no tags. Then the history is reset to one commit and the documentation moves to visor.macedonga.com.

## Modules

- `protocol` (`vdk-protocol`, JVM): the wire contract. Every method, topic, type and error code, in
  `Methods`, `Topics`, `Shell` and `JsonRpc`. Both sides compile against it.
- `android` (`vdk`, AAR): the client apps use (`Visor`, `RequestScopes`), the frozen AIDL, and the server engine
  (`server/`: `Dispatcher`, `VdkServer` for binder, `ShellServer` for adb) that Visor and FakeVisor share.
- `testing` (`vdk-testing`, AAR): `FakeVisor`, Visor in the app's own process, for tests.
- `tools/cli` (not published): the `vdk` command. It reaches Visor through `adb forward` to the abstract socket
  `visor-vdk`, which only the shell uid may use, and speaks JSON-RPC lines.

Apps need only the scopes the person grants on the consent screen. Visor's Developer mode opens nothing for apps:
it only opens the adb socket, for the computer-side tools.
- `tools/contract` (not published): writes `protocol/api/openrpc.json` and checks it against the last release.

Maven group `com.macedonga.visor`, namespace `com.macedonga` proven by a TXT record on macedonga.com.

## Compatibility rules

An app built against any released VDK must keep working with any later Visor, and a later app with an earlier
Visor. Either side of a connection can be the older one. The checks below run in `./gradlew check` and in CI;
a rule they cannot see is still a rule.

The wire, checked by `:contract:checkContract` against the last `v*` tag:

1. A released method, topic or type is never removed or renamed, and keeps its scope, its `since` and its result.
2. A released field keeps its name, its type, and whether it is required.
3. A new field is optional and has a default, so a peer that does not know it still works.
4. A set of values that can grow (scopes, error codes, kinds, gesture actions) is a string value class with the
   known values in its companion. Never an enum: an older app must not crash on a newer value.
5. Anything new raises `VdkProtocol.VERSION` by one, and a new method or topic has `since` equal to it.
6. Meaning is part of the contract. A method keeps doing what it did; new behavior is a new method or an
   optional field that asks for it.
7. A new failure uses an existing `ErrorCode` when one fits. Clients treat unknown codes as `failed`.

Binder:

8. The AIDL in `android/src/main/aidl` is frozen. Transaction codes never change; a new method takes the next
   free code. Messages stay JSON strings, so the AIDL never needs a new type.

Kotlin API, checked by `checkAbi` (the dump in each module's `api/`) and `checkAbiCompatibility` (against the last
`v*` tag):

9. Explicit API mode. Every change to public API shows in `api/*.api`: run `./gradlew updateAbi`, read the diff,
   commit it. A line that disappears since the last release fails the build.
10. No data classes in public API. Public value types use `@Poko`, so a new field breaks no `copy` or `componentN`.
11. No default arguments in public functions or constructors: add an overload. Only `@VdkInternal` constructors
    have defaults, which is why the ABI checks may skip their bridges.
12. Constructors of wire types are `@VdkInternal`. A type that apps build gets a `Builder` and a factory function,
    as `PrompterStyle` does.
13. Removing public API takes three releases: deprecated in a minor (warning), error in the next minor, gone in
    the next major.
14. No new public sealed type or enum unless the set is closed by nature.

Visor's side, in the Visor repo:

15. Visor answers every released method for good. When a firmware update or Meta takes a capability away, the
    method fails with `unsupported` or `refused`. It never hangs and never pretends to succeed: every wait on the
    glasses has a timeout.
16. Developer mode's intents (`com.macedonga.visor.action.*`) are public API too, under the same rules.

## Working on it

- A change starts in `protocol`, then the client, then FakeVisor, then Visor.
- `./gradlew check` runs every guard. After a deliberate change: `./gradlew updateAbi :contract:updateContract`.
- Visor builds the VDK from source through `includeBuild("../visor-kit")` while this folder sits next to it.
- On the phone: `./gradlew :cli:installDist`, then `tools/cli/build/install/vdk/bin/vdk status`.
- No test suites, as in Visor: the glasses, FakeVisor and the `vdk` command are the check.
- Short comments that say why. One name for one thing. Versions only in `gradle/libs.versions.toml`, stable and at
  least two weeks old.
- The release key and tokens never go in the repo.

## Releases

1. Move `## Unreleased` in `CHANGELOG.md` under `## X.Y.Z` (Added, Changed, Fixed).
2. Tag `vX.Y.Z` and push it. `release.yml` runs every check against the tag before it, publishes the libraries
   to Maven Central signed, and puts the `vdk` command's zip on the GitHub release. Run it by hand for a dry run.
3. Bump the VDK in Visor's `gradle/libs.versions.toml` and release Visor.

Library versions are semver. `VdkProtocol.VERSION` counts separately and only goes up.

## Roadmap

0. Measure on the glasses: band gestures while the Display keeps the band (RELAY lease); whether a Display web
   app's requests take the IP tunnel or the svc41111 HTTP relay; a Visor-made svc9002 card; touchpad and capture
   button events on glasses without a display; whether the Display carries a DWA broker.
1. VDK 0.1 (this): glasses status, camera, captures, band, the Display's prompter; Visor's VDK service, consent
   screen and shell socket; the `vdk` command; FakeVisor.
2. Voice and assistant hooks: phrases routed to apps, tools for the user's model, answers in the glasses' own
   voice; notifications on the glasses.
3. The network door: JSON-RPC over WebSocket and an MCP server on 127.0.0.1 (LAN opt-in), webhooks, the raw
   scope (with svc36, change owner and OTA denied), Python and TypeScript clients, samples.
4. The Display: `visor.js` for web apps through Visor's tunnel, Briefing cards, the band without glasses.
5. Research: the DWA broker protocol (services 6500 to 6504), for DAT's display components without Meta AI.
