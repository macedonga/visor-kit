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

## The camera live

```kotlin
val config = LiveConfig { quality = LiveQuality.HIGH; frameRate = 30; audio = true }
LiveVideoDecoder(surfaceView.holder.surface).use { video ->
    visor.camera.live(config).flowOn(Dispatchers.IO).collect { sample -> video.decode(sample) }
}
```

`live` streams what the glasses' camera sees, from Visor's Live view over Wi-Fi Direct, so the phone's Wi-Fi must
be on. Collecting starts the Live, and the glasses' capture light stays on while it runs. When the Live already
runs, for Visor's own Live view or another app, your app joins it as it is. When the last app stops collecting,
the Live stops, unless the person started it in Visor.

Each `LiveSample` is a `header` and its `data`:

| Field | Type | Meaning |
|---|---|---|
| `kind` | `LiveKind` | `video` or `audio`. |
| `codec` | `String` | The MIME type a `MediaCodec` takes: `video/hevc` or `video/avc` in Annex-B, `audio/mp4a-latm` as raw AAC-LC. |
| `ptsUs` | `Long` | The glasses' presentation time, in microseconds. |
| `config` | `Boolean` | True when `data` sets up the decoder: the video's parameter sets, or the audio's AudioSpecificConfig. |
| `key` | `Boolean` | True for a video keyframe. |
| `width`, `height` | `Int?` | On a video config: the picture's size. |
| `sampleRate`, `channels` | `Int?` | On an audio config. |

Each kind starts with its config, and the video then starts on a keyframe. A new config can come mid-stream, when
the Live restarts. Visor sends the video and the audio as the glasses encode them, so you can save them, send them
on, or decode them:

- `LiveVideoDecoder(surface)` draws each picture on a `Surface` as soon as it is decoded.
- `LiveVideoDecoder { image, ptsUs -> }` hands each picture over as a YUV_420_888 `android.media.Image`, valid until
  the block returns: for ML Kit, OpenCV or your own model.
- `LiveAudioDecoder { samples, sampleRate, channels, ptsUs -> }` hands the audio over as 16-bit PCM, interleaved.

Feed every sample to each decoder you use, on one thread; each skips the other kind. When frames are lost on the
way, Visor skips ahead to the next keyframe, so a slow app sees a pause instead of a broken picture.

`LiveConfig`:

| Field | Values | Default |
|---|---|---|
| `quality` | `low` (360 x 640), `medium` (504 x 896), `high` (720 x 1280), `full_hd` (1080 x 1920) | `medium` |
| `frameRate` | 2, 7, 15, 24 or 30 | 24 |
| `audio` | true for the glasses' microphones too | false |

The config applies only when your app starts the Live. Visor waits for an app that reads slowly, and ends the
stream of an app that stops reading for 5 seconds, so that a frozen app cannot keep the glasses filming.

The flow fails with:

| Code | Why |
|---|---|
| `not_connected` | The glasses are not connected. |
| `invalid_params` | The quality or the frame rate is not one of the above. |
| `timeout` | The Live did not start within 90 seconds. |
| `failed` | The Live did not start or ended. `reason` says why, as below. |
| `visor_gone` | Visor restarted. The stream does not come back by itself: collect again. |

| Reason | Meaning |
|---|---|
| `stopped` | The person stopped the Live in Visor. |
| `in_case` | The glasses charge in their case, where they keep the camera off. |
| `wifi_off` | The phone's Wi-Fi is off. |
| `wifi_direct_denied` | Visor lacks Android's Nearby devices permission. |
| `wifi_busy` | An import holds the Wi-Fi link to the glasses. |
| `vpn` | A VPN on the phone keeps Visor off the glasses' network. |
| `no_key` | Visor has no key for these glasses on this phone. |
| `join_failed`, `session_failed` | The Wi-Fi link to the glasses did not come up, or the glasses opened no session on it. |
| `no_video` | The Live started but no video came. |
| `gave_up` | The video stopped and restarting it failed. |

### A photo while live

```kotlin
val photo = visor.camera.takeLivePhoto()
val heic = context.contentResolver.openInputStream(Uri.parse(photo.uri))!!.use { it.readBytes() }
```

`takeLivePhoto` takes a photo at the glasses' photo size without stopping the stream. The result is a HEIC that your
app may read from `uri` for 10 minutes. It fails with `busy` when the Live is not running, and `timeout` when the
glasses send no photo. Over adb or the network door, `data` holds the photo in Base64 instead of `uri`.

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

Over JSON-RPC a feed is subscribed like a topic: see [Feeds](json-rpc.md#feeds).

| | Name | Params | Answer |
|---|---|---|---|
| Method | `camera.photo` | `import` (boolean, default true) | a `Capture` |
| Method | `camera.video.start` | `kind` (`video` or `hyperlapse`, default `video`) | `{}` |
| Method | `camera.video.stop` | none | `{}` |
| Feed | `camera.live` | `quality`, `frameRate`, `audio` | records of `LiveHeader` and bytes |
| Method | `camera.live.photo` | none | a `LivePhoto`: `mime`, and `data` in Base64 |
| Method | `captures.list` | none | a list of `Capture` |
| Method | `captures.import` | `id` | a `Capture` |
| Topic | `captures.available` | none | `Capture` |
| Topic | `captures.progress` | none | `CaptureProgress` |
| Topic | `captures.imported` | none | `Capture` |
| Topic | `captures.failed` | none | `CaptureFailure` |
