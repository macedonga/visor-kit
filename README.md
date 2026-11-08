# Visor Development Kit

Build your own apps and tools for Meta's smart glasses.

The Visor Development Kit (VDK) lets an Android app, or a script on your computer, use Ray-Ban Meta glasses, the Meta Ray-Ban
Display and the Meta Neural Band through [Visor](https://github.com/macedonga/visor), the open Android app that
runs them. It needs no Meta account, no Meta AI app and no developer review, and it sends nothing anywhere.

> **Status:** in development. Protocol 1 and the 0.1 libraries are being tried on hardware and are not on Maven
> Central yet.

## What you can do

| | |
|---|---|
| **Glasses** | Read the link, battery, model and firmware, and whether the glasses are worn, folded or recording, as it changes. See every pair set up in Visor, with its name and looks, and switch the active one. |
| **Camera** | Take a photo and get it in the phone's gallery. Start and stop videos and hyperlapses. Stream the camera live, up to 1080p over Wi-Fi Direct, with the microphones. |
| **Captures** | List what is on the glasses, import it over Wi-Fi Direct, and follow each import. |
| **Neural Band** | Receive every gesture: finger, action and the band's own reading of it. Make the band vibrate. |
| **Display** | Show layouts with DAT's elements (text, images, icons, buttons, video) and get the clicks; put a script on the teleprompter, and notifications on the Display. |
| **Voice** | Answer the questions the wearer asks that name your app, in the glasses' own voice, or start your app by a phrase. Turn what the glasses hear into text, live. |
| **Assistant** | Give the assistant's model your app's tools, which it calls while it answers the wearer. Read every question and answer. |
| **Settings** | Read every setting the glasses keep, and change the everyday ones. |
| **Without the glasses** | `visor fake` runs a fake Visor on your computer, with a Display simulator in the browser; `visor record` and `visor replay` play back what the wearer did. |

Programs and AI agents can reach the glasses over the network too, through JSON-RPC or MCP, with a token the
person makes for them. Coming next: JavaScript for the Display's web apps. See the
[roadmap](docs/overview.md#roadmap).

## How it works

Visor is the phone side of the glasses: it holds the Bluetooth and Wi-Fi link and speaks their protocol. The VDK
asks Visor to do things, and Visor asks the person first.

```
your app       --(VDK library, binder)--+
                                        +--> Visor --(Bluetooth, Wi-Fi Direct)--> glasses, Neural Band
your computer  --(adb, JSON-RPC)--------+
```

The person grants each app its **scopes** on Visor's consent screen, and can take them back at any time from
Visor's settings. Nothing else needs turning on.

## Quick start

1. Install Visor on the phone and set up the glasses.
2. Add the library:

   ```kotlin
   dependencies {
       implementation("com.macedonga.visor:kit:0.1.0")
   }
   ```

3. Connect, ask for scopes, and use the glasses:

   ```kotlin
   val visor = Visor.connect(context)

   val ask = registerForActivityResult(RequestScopes()) { held -> /* scopes the app holds now */ }
   ask.launch(setOf(Scope.CAMERA, Scope.BAND))

   val photo = visor.camera.takePhoto()        // photo.uri is in the gallery
   visor.band.gestures.collect { gesture ->
       if (gesture.derived == GestureAction.DOUBLE_TAP) visor.band.buzz()
   }
   ```

The [getting started guide](docs/getting-started.md) goes through it step by step.

## Test from your computer

The `visor` command reaches Visor on the phone through adb, over USB or wireless debugging, once Developer mode
is on in Visor. You can call any method, watch any stream, see your app's traffic, and stand in for the glasses or
the band:

```
visor status                              # Visor, the glasses, the band, the apps that use the VDK
visor trace                               # every call and event between your app and Visor
visor simulate band.gestures              # no band needed: type "index double_tap", your app gets it
visor grant com.example.app camera band   # skip the consent screen while you develop
```

For unit tests, `FakeVisor` runs Visor inside your app's process. Both are in [Testing](docs/testing.md).

## Documentation

- [Overview](docs/overview.md): what the VDK is, how it compares with Meta's toolkit, and the roadmap
- [Getting started](docs/getting-started.md)
- [Scopes and consent](docs/scopes-and-consent.md)
- [The connection](docs/connection.md)
- [Glasses](docs/glasses.md), [Camera and captures](docs/camera-and-captures.md), [Neural Band](docs/neural-band.md),
  [Display](docs/display.md), [Voice and the assistant](docs/voice-and-assistant.md)
- [Errors](docs/errors.md)
- [Testing](docs/testing.md)
- [JSON-RPC](docs/json-rpc.md): the VDK from any language
- [The network door](docs/network.md): WebSocket, MCP for AI agents, Python and TypeScript
- [Compatibility](docs/compatibility.md)

## Modules

| Artifact | What it is |
|---|---|
| `com.macedonga.visor:kit` | The Android library. |
| `com.macedonga.visor:kit-testing` | `FakeVisor`, for tests. |
| `com.macedonga.visor:kit-fake` | `FakeVisor` in plain Kotlin, for JVM tests and tools. |
| `com.macedonga.visor:kit-server` | The server engine Visor and FakeVisor share. |
| `com.macedonga.visor:kit-protocol` | The wire contract, for JVM tools and for Visor itself. |
| `visor` | The command for your computer, on the [releases](https://github.com/macedonga/visor-kit/releases) page. `visor fake` runs FakeVisor for building without a phone. |
| `visor-kit` for Python and TypeScript | Clients for other languages, in [`clients/`](clients). |

## Compatibility

An app built against any VDK release keeps working with every later Visor, and a newer app works with an older
Visor as far as that Visor goes. The build enforces this against the last release. See
[Compatibility](docs/compatibility.md).

## License

Apache 2.0. Visor itself is AGPL-3.0; the VDK is separate so that any app can use it.
