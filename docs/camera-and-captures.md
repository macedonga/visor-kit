# Camera and captures

Scope: `camera`. All glasses.

The glasses keep their photos and videos until a phone imports them. Visor imports over Wi-Fi Direct, so the
phone's Wi-Fi must be on; it falls back to Bluetooth only when Wi-Fi Direct fails. The glasses' capture light
turns on for every photo and video, as it does for their own button.

## Photos

```kotlin
val photo = visor.camera.takePhoto()                 // returns once it is in the gallery
val onGlasses = visor.camera.takePhoto(import = false) // returns once the glasses have it
```

The result is a `Capture`. With an import, `uri` is the photo's `content://` address in the phone's gallery, and
Visor grants your app read access to it: your app needs no media permission to show it.

A photo fails with:

| Code | Why |
|---|---|
| `not_connected` | The glasses are not connected and did not connect within 20 seconds. |
| `busy` | The glasses are recording a video. |
| `timeout` | The glasses did not take the photo, or it did not show up on them. |
| `failed` | The import failed. `reason` says why: see Imports below. |

The glasses take a photo for the phone only while a switch of theirs is on. Visor turns it on while any app holds
the `camera` scope. The first time, it can take a minute while Visor reads the glasses'
configuration, and a photo asked for meanwhile fails with `timeout`.

## Videos

```kotlin
visor.camera.startVideo()                      // returns once the glasses record
visor.camera.startVideo(VideoKind.HYPERLAPSE)
visor.camera.stopVideo()                       // returns once they stopped
```

The glasses end a video on their own after their maximum length. The new video then appears on
`captures.available`, like one taken with the button.

## Captures

```kotlin
val waiting = visor.captures.list()            // on the glasses, not on the phone yet
val video = visor.captures.import(waiting.first().id)
```

A `Capture`:

| Field | Type | Meaning |
|---|---|---|
| `id` | `String` | The capture's id on the glasses. `import` takes it. |
| `type` | `CaptureType` | `photo`, `live` (a photo with the clip around it) or `video`. |
| `time` | `Long?` | When the glasses took it, in milliseconds since 1970. |
| `size` | `Long?` | Bytes on the glasses. |
| `uri` | `String?` | The address in the gallery, once imported. |

### Imports

`import` returns at once when the phone already has the capture. Otherwise it returns when the capture is in the
gallery, and fails with `failed` and one of these as `reason`:

| Reason | Meaning |
|---|---|
| `not_found` | The glasses have no capture with that id. |
| `wifi_off` | The phone's Wi-Fi is off. |
| `wifi_stuck` | Android's Wi-Fi Direct stopped answering. Only turning Wi-Fi off and on helps. |
| `wifi_failed`, `glasses_did_not_join`, `no_wifi_session` | Wi-Fi Direct did not come up. |
| `not_allowed` | Visor lacks Android's Nearby devices permission. |
| `cut_off` | The glasses went away halfway, for example into the closed case. |
| `damaged` | A file failed its checksum. |
| `not_saved` | The gallery did not take the file. |
| `failed` | Anything else. |

The glasses send whatever else waits in their queue with an import. Each of those reaches the gallery too and
comes on `imported`.

### Streams

```kotlin
visor.captures.available.collect { }  // each new capture on the glasses
visor.captures.progress.collect { }   // CaptureProgress(id, received, size), at most once per percent
visor.captures.imported.collect { }   // each capture that reaches the gallery
visor.captures.failed.collect { }     // CaptureFailure(id, failure)
```

`progress`, `imported` and `failed` cover every import, also those Visor starts on its own.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `camera.photo` | `import` (boolean, default true) | a `Capture` |
| Method | `camera.video.start` | `kind` (`video` or `hyperlapse`, default `video`) | `{}` |
| Method | `camera.video.stop` | none | `{}` |
| Method | `captures.list` | none | a list of `Capture` |
| Method | `captures.import` | `id` | a `Capture` |
| Topic | `captures.available` | none | `Capture` |
| Topic | `captures.progress` | none | `CaptureProgress` |
| Topic | `captures.imported` | none | `Capture` |
| Topic | `captures.failed` | none | `CaptureFailure` |
