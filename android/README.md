# kit: the VDK for Android

The Android library of the [Visor Development Kit](../README.md). Your app binds to Visor's VDK service and uses
the glasses, the Neural Band and the Display through one `Visor` object, with the scopes the person grants it.

| Artifact | Needs |
|---|---|
| `com.macedonga.visor:kit` | Android 8.0 (API 26) or later in your app; Visor on the phone |

```kotlin
dependencies {
    implementation("com.macedonga.visor:kit:0.1.1")
}
```

The library declares Visor's package in `<queries>`, so your manifest needs nothing.

## Use it

```kotlin
val visor = Visor.connect(context)

val ask = registerForActivityResult(RequestScopes()) { held -> }
ask.launch(setOf(Scope.STATE, Scope.CAMERA, Scope.DISPLAY))

visor.glasses.status.collect { status -> batteryView.text = "${status.battery}%" }
val photo = visor.camera.takePhoto()
visor.display.cards.set("pasta", "Kitchen timer", LayoutIconName.CLOCK) {
    header(LayoutIconName.CLOCK, "Kitchen timer")
    stat("4:00", "Pasta")
}
```

Every call is a `suspend fun` and every stream a cold `Flow`. A failure is a `KitException` with a `code`, and a
`reason` when the person can change something.

## What is inside

| Part | What it does |
|---|---|
| `Visor` | The connection: `connect`, `state`, `supports`, `scopes`, `close`. |
| `visor.glasses` | Status, pairs, touches and presses, motion. |
| `visor.camera`, `visor.captures` | Photos, videos, the live camera, imports. |
| `visor.band` | Gestures, status, motion on the wrist, vibration. |
| `visor.display` | The teleprompter, layouts, Briefing cards. |
| `visor.voice`, `visor.assistant`, `visor.notify` | Questions that name the app, speech, transcripts, tools for the model, conversations, notifications. |
| `visor.settings` | The glasses' settings. |
| `RequestScopes` | The activity result contract that opens Visor's consent screen. |
| `VoiceLaunch` | Reads the question that started your app by voice. |
| `LiveVideoDecoder`, `LiveAudioDecoder` | Decode the live camera onto a `Surface` and into PCM. |
| `image(bitmap)` | A photo from your app inside a layout or a card. |

## Read more

[Getting started](https://visor.macedonga.com/developers/docs/getting-started/), then the page of each capability in [the docs](https://visor.macedonga.com/developers/docs/).
For tests, add [`kit-testing`](../testing).
