# Porting from Meta's DAT

This page maps each part of Meta's Wearables Device Access Toolkit for Android (DAT 1.0.1) to the Visor
Development Kit. It is for an app that already works with DAT and should also work, or only work, with Visor.

The two kits differ in shape more than in reach:

- **No registration.** DAT registers the app through the Meta AI app, and Meta approves some features per app.
  With the VDK, the person grants scopes on Visor's consent screen, and that is all.
- **One connection, no capabilities to attach.** DAT creates a `DeviceSession`, starts it, and attaches a camera,
  a display or inputs to it. The VDK has one `Visor` connection; each capability is a call or a flow. Collecting a
  flow starts it, and cancelling it stops it.
- **Exceptions, not `DatResult`.** Every VDK call fails with a `KitException` whose `code` is an `ErrorCode`.
- **The active pair.** DAT selects a device per session. The VDK acts on the pair Visor connects to; an app can
  change that pair for everyone with `glasses.select`.

## Setup

| DAT | VDK |
|---|---|
| `com.meta.wearable:mwdat-core`, `mwdat-camera`, `mwdat-inputs`, `mwdat-speech`, ... | `com.macedonga.visor:kit`, one artifact |
| `com.meta.wearable:mwdat-mockdevice` | `com.macedonga.visor:kit-testing` |
| Manifest `com.meta.wearable.mwdat.APPLICATION_ID`, `CLIENT_TOKEN` | Nothing |
| `ANALYTICS_OPT_OUT`, `CRASH_REPORTING_OPT_OUT` | Nothing: the VDK sends nothing anywhere |
| `BLUETOOTH_CONNECT` permission, and the URL scheme for registration | Nothing: Visor holds the Bluetooth link |
| `Wearables.initialize(context)` | `Visor.connect(context)`, which suspends until Visor answers |
| `Wearables.startRegistration(activity)`, `registrationState` | `RequestScopes()`, an activity result contract |
| `Wearables.RequestPermissionContract()`, `Permission.CAMERA` | `RequestScopes()` with `Scope.CAMERA` |
| `Permission.MICROPHONE` | `Scope.MICROPHONE` for transcripts; the camera's live audio needs `Scope.CAMERA` only |
| `Wearables.checkPermissionStatus(permission)` | `visor.scopes()` |
| `Wearables.openFirmwareUpdate(activity)` | No equivalent |

```kotlin
// DAT
Wearables.initialize(context)
val launcher = registerForActivityResult(Wearables.RequestPermissionContract()) { result -> }
launcher.launch(Permission.CAMERA)

// VDK
val visor = Visor.connect(context)
val ask = registerForActivityResult(RequestScopes()) { held -> }
ask.launch(setOf(Scope.CAMERA))
```

## Devices and sessions

| DAT | VDK |
|---|---|
| `Wearables.devices`, `Wearables.devicesMetadata[id]` | `visor.glasses.pairs`, scope `pairs` |
| `DeviceSession.deviceInfo` | `visor.glasses.status`, scope `state` |
| `AutoDeviceSelector()` | The active pair, which every call acts on |
| `SpecificDeviceSelector(id)` | `visor.glasses.select(serial)`: it changes Visor's active pair for every app |
| `Wearables.createSession(selector)`, `session.start()` | `Visor.connect(context)` |
| `session.stop()` | `visor.close()` |
| `DeviceSessionState` `STARTING`, `STARTED`, `STOPPED` | `VisorState.Connecting`, `Open`, `Closed` |
| `session.errors` | Each call's or flow's `KitException` |
| `session.addCamera(...)`, `addInputs`, `addSpeech`, ... | Nothing: call or collect what you need |

`Device` fields:

| DAT `Device` | VDK |
|---|---|
| `name` | `GlassesPair.name`, `GlassesStatus.name` |
| `deviceType.isDisplayCapable`, `isDisplayCapable()` | `display` |
| `linkState`: `CONNECTING`, `CONNECTED`, `DISCONNECTED` | `link`: `connecting`, `connected`, `disconnected` |
| `batteryLevel` | `battery`, null while unknown instead of 0 |
| `chargingState` | `charging`, a `Boolean?` |
| `donState`: `DONNED`, `DOFFED`, `UNKNOWN` | `worn`: true, false, null |
| `hingeState` | `folded` |
| `thermalLevel`: `NONE`, `LIGHT`, ..., `SHUTDOWN` | `thermal`: `normal`, `light`, ..., `shutdown`, an open set; and `throttling` |
| `compatibility` | Nothing: Visor deals with the glasses' firmware |
| | `model`, `serial`, `firmware`, `recording`, `caseBattery` |

## Camera

| DAT | VDK |
|---|---|
| `session.addCamera(StreamConfiguration(...))`, `camera.stream.start()` | `visor.camera.live(LiveConfig { ... })`, collected |
| `VideoQuality.LOW`, `MEDIUM`, `HIGH` | `LiveQuality.LOW`, `MEDIUM`, `HIGH`, and `FULL_HD` (1080 x 1920) |
| `frameRate`: 2, 7, 15, 24, 30 | `frameRate`: the same |
| `compressVideo = true`: HEVC | The default: each `LiveSample` is HEVC or H.264, Annex-B |
| `compressVideo = false`: decoded frames | `LiveVideoDecoder { image, ptsUs -> }`: each picture as a YUV_420_888 `Image` |
| | `LiveVideoDecoder(surface)`: straight onto a `Surface` |
| `VideoFrame.buffer`, `width`, `height`, `presentationTimeUs` | `Image.planes`, `width`, `height`, and `ptsUs` |
| `VideoFrame.isCodecConfig` | `LiveHeader.config` |
| `stream.state`: `STREAMING` | The flow sends samples |
| `stream.errorStream`, `StreamError` | The flow fails with a `KitException`; `reason` says why |
| `camera.stop()` | Cancel the collection |

```kotlin
// DAT
val camera = session.addCamera(StreamConfiguration(videoQuality = VideoQuality.HIGH, frameRate = 30)).getOrThrow()
camera.stream.start()
camera.stream.videoStream.collect { frame -> analyse(frame.buffer, frame.width, frame.height) }

// VDK
LiveVideoDecoder { image, _ -> analyse(image) }.use { video ->
    visor.camera.live(LiveConfig { quality = LiveQuality.HIGH; frameRate = 30 })
        .flowOn(Dispatchers.IO)
        .collect { video.decode(it) }
}
```

Differences to know:

- DAT streams over Bluetooth; the VDK streams over Wi-Fi Direct, so the phone's Wi-Fi must be on. It reaches 1080p.
- A tap on the touchpad pauses DAT's stream. The VDK's stream does not pause; the person stops the Live in Visor.
- When the Live already runs, for Visor's own view or another app, your app joins it as it is, whatever it asked.

### Photos

| DAT | VDK |
|---|---|
| `stream.capturePhoto()` while streaming | `visor.camera.takeLivePhoto()` while `live` is collected |
| `PhotoData.HEIC(data)` | `LivePhoto.uri`, a HEIC your app may read for 10 minutes |
| `PhotoData.Bitmap(bitmap)` | Decode the HEIC with `ImageDecoder` |
| `camera.photo.capturePhoto(PhotoResolution.FULL, PhotoQuality.HIGH)` | `visor.camera.takePhoto()`: the glasses' own photo at full size, in the gallery |
| `photo.transferProgressStream` | `visor.captures.progress` |
| | `visor.camera.startVideo()`, `stopVideo()`, and every capture on the glasses with `visor.captures` |

### Audio

| DAT | VDK |
|---|---|
| `StreamConfiguration(audioCodec = AudioCodec.PCM(AudioSampleRate.RATE_16000, 1))` | `LiveConfig { audio = true }` |
| `stream.audioStream`, `AudioFrame.buffer` | `LiveAudioDecoder { samples, sampleRate, channels, ptsUs -> }` |

The glasses encode their microphones as 48 kHz stereo AAC, and `LiveAudioDecoder` hands that over as 16-bit PCM.
Resample it if your code expects 16 kHz mono.

## Speech

| DAT | VDK |
|---|---|
| `session.addSpeech()`, `speech.start()` | `visor.voice.transcripts(TranscriptRequest { language = "en-US" })`, collected |
| `speech.transcriptions`: `TranscriptionResult(text, isFinal, confidence)` | `Transcript(text, language, time)`, one finished utterance each |
| `speech.locale` | `TranscriptRequest.language`, or the phone's language |
| `speech.stop()` | Cancel the collection |
| `SpeechError.UNAVAILABLE` | `unsupported` with `no_recognizer` |

The VDK sends only finished utterances, without a confidence. The phone's speech recogniser makes the text from
the glasses' microphone as a Bluetooth headset, so music pauses while transcripts run.

## Voice invocations

| DAT | VDK |
|---|---|
| `Wearables.startVoiceInvocationsStream(selector)` | `visor.voice.requests(phrases)`, collected |
| `LaunchApp` | `VoiceRequest(id, text, phrase, intent)` |
| `invocation.responseHandle.sendSuccess(...)` | `visor.voice.reply(request, text)`: the glasses say `text` |
| `isVoiceInvocationsIntent(intent)` | Nothing: the request comes to the running flow |

DAT opens the app when the wearer names it. The VDK sends the question itself to the app whose phrase it holds,
and the app answers in the glasses' voice. Beyond DAT, `visor.assistant.tools(...)` gives the assistant's model
tools to call while it answers.

## Inputs

| DAT | VDK |
|---|---|
| `session.addInputs(InputsConfiguration(sources))` | `visor.glasses.inputs`, collected, scope `inputs` |
| `InputEvent.Select` | `touchpad` `tap` |
| `InputEvent.Nav(direction)` | `touchpad` `swipe_forward` or `swipe_backward` |
| `InputEvent.Drag(...)` | `touchpad` `slide_forward` or `slide_backward`, between `touch` and `release` |
| `InputEvent.Capture(CapturePressType.SHORT_PRESS)`, `HOLD`, `DOUBLE_PRESS` | `capture_button` `press`, `long_press`, `double_press` |
| `InputEvent.Button(ButtonType.ACTION)` | `action_button` `press`, `release`, `long_release` |
| `InputEvent.Back` | No touchpad gesture means back on these glasses: pick one, such as `double_tap` |
| `InputSource.CAPTOUCH`, `CAPTURE_BUTTON`, `ACTION_BUTTON` | `InputSource.TOUCHPAD`, `CAPTURE_BUTTON`, `ACTION_BUTTON` |
| `consumeBack = true` | Not possible: the glasses still act on every input |
| Neural Band sources | `visor.band.gestures`, scope `band`: every gesture, not only navigation |

## Motion

| DAT | VDK |
|---|---|
| `session.addMotion(MotionConfiguration(samplingRate))` and `motion.samples` | `visor.glasses.motion(rateHz)`, scope `motion` |
| `MotionSamplingRate` 5, 10, 15, 24, 30, 60 Hz | `rateHz`, the same six values |
| `MotionSample` (timestamp, accelerometer, gyroscope, magnetometer, orientation, source) | `MotionSample` with the same fields and units |
| `MotionSource.GLASSES`, `NEURAL_BAND` | `MotionSource.GLASSES`, `BAND` |
| `MotionError.SENSOR_UNAVAILABLE` | the stream ends with `unsupported` |

> Visor does not serve motion yet: it will once it can open a session with the Display's toolkit app, as it will
> for layouts. FakeVisor serves it now: `fake.glasses.motion(...)`.

## Display

| DAT | VDK |
|---|---|
| `display.sendContent { flexBox { ... } }` | `visor.display.layout.show(Layout { ... })` |
| `flexBox(direction = Direction.COLUMN)`, `ROW` | `column { }`, `row { }` |
| `text(..., style = TextStyle.HEADING)` | `text("...") { style = LayoutStyle.HEADING }` |
| `image(uri = ...)`, `image(bitmap = ...)` | `image(url)`: an `https://` URL or a `data:` URI |
| `button(label, onClick = ...)` | `button(id, label)`, and its clicks on `visor.display.layout.clicks` |
| `display.clearDisplay()` | `visor.display.layout.close()` |
| `icon(...)`, `video(...)` | Not yet |

Visor does not show layouts yet: it will once it can open a session with the Display's toolkit app, as DAT does.
FakeVisor serves them already. Until then the VDK shows text on the teleprompter (`visor.display.prompter`) and
notifications (`visor.notify`).

## Testing

| DAT `MockDeviceKit` | `FakeVisor` |
|---|---|
| `MockDeviceKit.getInstance(context).enable()` | `val fake = FakeVisor()`, then `fake.connect()`, in a plain JVM test |
| `pairGlasses(GlassesModel.META_RAYBAN_DISPLAY)` | `fake.glasses.display = true`; more pairs with `fake.pairs.add(...)` |
| `powerOn()`, `powerOff()` | `fake.glasses.connected` |
| `don()`, `doff()` | `fake.glasses.worn` |
| `fold()`, `unfold()` | `fake.glasses.folded` |
| `setBatteryLevel`, `setChargingState`, `setThermalLevel` | `fake.glasses.battery`, `charging`, `thermal` |
| `permissions.set(Permission.CAMERA, PermissionStatus.Denied)` | `fake.granted = fake.granted - Scope.CAMERA` |
| `services.camera.setCameraFeed(uri)` | `fake.live.sendVideoConfig(...)`, `sendVideo(...)` |
| `services.captouch.tap()`, `services.input.select(...)` | `fake.glasses.input(InputSource.TOUCHPAD, InputAction.TAP)` |
| `services.speech.simulateTranscription(text, ...)` | `fake.voice.say(text)` |
| `services.voiceInvocation.simulateLaunchAppAction()` | `fake.voice.ask(question)` |
| `startTestServer(port)` | `visor simulate` and `visor inject` from a computer, against the real Visor |

See [Testing](testing.md) for the rest.

## Errors

| DAT | VDK `ErrorCode` |
|---|---|
| `PermissionStatus.Denied`, `PermissionError` | `scope_denied`, with the missing `scope` |
| `DeviceSessionError.NO_ELIGIBLE_DEVICE`, `DEVICE_DISCONNECTED` | `not_connected` |
| `CaptureError.NotStreaming` | `busy` |
| `StreamError.TIMEOUT` | `timeout` |
| `StreamError.CRITICAL_STREAM_ERROR` | `failed`, with a `reason` |
| `DeviceSessionError.INSUFFICIENT_SDK_VERSION` | `visor_too_old` |

The [Errors](errors.md) page lists every code.
