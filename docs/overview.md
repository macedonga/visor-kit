# Overview

The VDK is how other software uses Meta's glasses through Visor. Visor is an Android app that replaces the
Meta AI app as the phone side of Ray-Ban Meta glasses, the Meta Ray-Ban Display and the Meta Neural Band: it
holds their Bluetooth and Wi-Fi links and speaks their protocol. The VDK gives that link to your code.

## Who it is for

- **Android apps** that want the glasses' camera, their status, the band's gestures or the Display.
- **Scripts and tools on a computer**: automations, prototypes, a test harness, an AI agent that drives the
  glasses. They reach Visor through adb today, and through a network API later.
- **Visor's own features**, which can be built and tried outside Visor first.

## How it works

```
your app       --(VDK library, binder)--+
                                        +--> Visor --(Bluetooth, Wi-Fi Direct)--> glasses, Neural Band
your computer  --(adb, JSON-RPC)--------+
```

Everything the VDK does is a **method** (a request with one answer, like `camera.photo`) or a **topic** (a
stream of events, like `band.gestures`). Both have a name, a scope, and the protocol version that added them.
The [vdk-protocol](../protocol) module defines all of them, and
[`protocol/api/openrpc.json`](../protocol/api/openrpc.json) describes them for any language.

There are two ways in today:

- **The Android library** (`com.macedonga.visor:vdk`). Your app binds to Visor's VDK service. Visor knows which
  app is calling from Android itself, by its package and its signing certificate, so nothing can pretend to be
  your app.
- **The adb socket.** Visor listens on a local socket that only adb's shell user may use. The `vdk` command on
  your computer, or any program that speaks [JSON-RPC](json-rpc.md), reaches it with `adb forward`.

The person stays in charge:

- **Scopes** say what an app may do. The person grants them on Visor's consent screen and can take them back in
  Settings. An app needs nothing else. See [Scopes and consent](scopes-and-consent.md).
- **Developer mode** in Visor opens the adb socket for the computer-side tools. Apps never need it.

The glasses' own safeguards stay in place: the capture light turns on for every photo and video the VDK takes.

## What it can do

| Area | Methods and topics | Glasses |
|---|---|---|
| [Glasses](glasses.md) | `glasses.status` | All |
| [Camera](camera-and-captures.md) | `camera.photo`, `camera.video.start`, `camera.video.stop` | All |
| [Captures](camera-and-captures.md) | `captures.list`, `captures.import`, `captures.available`, `captures.progress`, `captures.imported`, `captures.failed` | All |
| [Neural Band](neural-band.md) | `band.status`, `band.gestures`, `band.buzz` | Meta Ray-Ban Display with a Neural Band |
| [Display](display.md) | `display.prompter.show`, `display.prompter.close` | Meta Ray-Ban Display |

## Compared with Meta's Wearables Device Access Toolkit

Meta's toolkit (DAT) works through the Meta AI app and a Meta-signed program on the glasses. The VDK works through
Visor instead, so the trade-offs differ:

| | Meta's toolkit | VDK |
|---|---|---|
| Needs | The Meta AI app, a Meta account, registration | Visor, and the person's consent |
| Gating | Meta can turn features off per user; several can't be published yet | None |
| Telemetry | Crash reports and analytics on by default | None |
| Camera | A video stream up to 720p over Bluetooth | Photos and videos, imported over Wi-Fi Direct |
| Neural Band | Navigation events only | Every gesture as the band reports it, and vibration |
| Display | Layouts of text, images, buttons and video | The teleprompter today; more on the roadmap |
| From a computer | No | Yes, through adb |

DAT does some things the VDK does not do yet, such as a live camera stream for apps and full layouts on the
Display. Those are on the roadmap below.

## Roadmap

1. **VDK 0.1:** the glasses' status, the camera and captures, the Neural Band, the Display's teleprompter, the
   `visor` command and FakeVisor.
2. **Voice and the assistant:** phrases routed to apps ("Hey Meta, ask My App..."), tools for the assistant's
   model, answers in the glasses' own voice, and notifications on the glasses.
3. **The network:** JSON-RPC over WebSocket and an MCP server on the phone, webhooks, Python and TypeScript
   clients, and raw access to the glasses' services for research.
4. **The Display:** a JavaScript library for the Display's web apps, cards on the Display's home, and the Neural
   Band as an input device without glasses.
5. **Research:** the protocol of Meta's on-glasses toolkit program, for full layouts on the Display.
