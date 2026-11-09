# Neural Band

Scope: `band`, and `motion` for the band's motion. The Meta Ray-Ban Display with a paired Meta Neural Band. Without
a band, `band.status` says `paired: false`, and the gestures, the motion and the buzz fail with `unsupported`.

The band works with apps in one of two ways, which the person picks in Visor. Your code is the same for both.

- **With the Display**, the usual way. The band talks to the Display, which uses its gestures to move around.
  Visor reads them through a session with the Display, so the Display acts on each gesture as usual and the app
  gets it too. A buzz borrows the band from the Display for a moment.
- **Shared with the phone.** The band stays connected to the phone, and the Display reaches it through the phone,
  as Meta's relay lease does. Gestures come straight from the band, a buzz goes out at once, and the band's motion
  sensors are open to apps. The person turns it on in **Visor > Settings > Neural Band > Share with the phone**.
  When sharing does not work, Visor turns it off itself: the band goes back to the Display, and Visor's band
  settings say why.

A band the person paired to the Display with Meta's app shows in `band.status` with the battery the Display reports,
and `held: false`. Its gestures reach apps all the same. The buzz and sharing need Visor to hold the band's keys,
which it does once the person sets the band up in Visor's band settings; until then the buzz fails with
`unsupported` and the reason `not_set_up`.

## Gestures

```kotlin
visor.band.gestures.collect { gesture ->
    when {
        gesture.finger == Finger.INDEX && gesture.derived == GestureAction.DOUBLE_TAP -> next()
        gesture.finger == Finger.MIDDLE && gesture.derived == GestureAction.HOLD -> back()
    }
}
```

With the band on the Display, collecting `gestures` opens a session with the Display, which shows nothing on its
screen. The first time, Visor asks the person once to set the glasses up for apps. The session needs the glasses
worn, and the Display ends it when another app comes to the front; Visor opens it again while your app collects.
Collect only while your app needs the gestures, for example while one screen is in front. With the band shared,
the gestures come straight from it and no session is needed.

A `Gesture`:

| Field | Type | Meaning |
|---|---|---|
| `finger` | `Finger` | `thumb`, `index` or `middle`. |
| `action` | `GestureAction` | What the finger did. With the band on the Display: `tap`, `double_tap`, `triple_tap`, `hold`, or a thumb swipe `up`, `down`, `left`, `right`. With the band shared, the band's own events: `press`, `release`, `tap`, `double_tap`, `click`, or a swipe. |
| `derived` | `GestureAction?` | What the band made of the events: `tap`, `double_tap`, `hold`, `release`, or a swipe. With the band on the Display it is the same as `action`. |
| `time` | `Long` | When the phone got it, in milliseconds since 1970. |

Act on `derived` and your app works either way. The Display tells taps apart before it forwards them: a double
tap comes as one `double_tap`, after its first `tap`. A middle-finger tap is the Display's back, so the Display
goes back while your app gets the tap. Sets like `GestureAction` grow over time; treat values you do not know as
nothing to act on.

## Status

```kotlin
visor.band.status.collect { status -> }
```

A `BandStatus`:

| Field | Type | Meaning |
|---|---|---|
| `paired` | `Boolean` | False when no band is paired with the glasses. |
| `link` | `Link` | The phone's own link to the band: up while the phone borrows it for a buzz, and all along while the band is shared. |
| `battery` | `Int?` | 0 to 100. |
| `charging` | `Boolean?` | |
| `leftHanded` | `Boolean?` | True when the band is set for the left wrist. |
| `borrowed` | `Boolean` | True while the band sends its gestures to the phone's own link: during a lesson in Visor, or while an app reads the gestures of a shared band. |
| `held` | `Boolean?` | True when Visor holds the band's keys, so the phone can buzz and share it. False for a band only the Display holds. |
| `shared` | `Boolean` | True while the person shares the band with the phone. |

## Motion

Scope: `motion`. A band shared with the phone.

```kotlin
if (visor.band.status.first().shared) {
    visor.band.motion.collect { sample -> }                // 30 samples a second
    visor.band.motion(rateHz = 60).collect { sample -> }
}
```

The band's own sensors on the wrist, in the same `MotionSample` as [the glasses' motion](glasses.md#motion), with
`source` `band`: `accelerometer` in m/s², `gyroscope` in rad/s, `magnetometer` as the band reports it, and the
band's own fused `orientation`. The band sends each sensor on its own, so a sample carries the newest reading of
each. `time` is when the phone got it, in nanoseconds; only the differences between samples mean something. Use it
to point with the wrist, to know when the wearer raises the arm, or to count repetitions.

Without sharing, or when sharing stops while you read, the stream ends with `unsupported` and the reason
`not_shared`. Its message names the setting, so you can show it to the person. Reading the band's sensors without
sharing would take the band from the Display.

In a test, set `fake.band.shared = true`, wait for `fake.band.motionRate`, then send readings with
`fake.band.motion(ax, ay, az)` or a whole `MotionSample`.

## Vibration

```kotlin
visor.band.buzz()                             // 400 ms at full strength: the wearer notices it
visor.band.buzz(80.milliseconds, 0.5f)        // 10 ms to 1 s, strength above 0 up to 1
```

With the band on the Display, a buzz borrows the band for a moment and gives it back; with the band shared, it goes
out at once. It fails with `not_connected` when the band's link does not come up in time, and with `refused` when
the band turns it down.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `band.status` | none | a `BandStatus` |
| Method | `band.buzz` | `durationMs` (10 to 1000, default 400), `strength` (1 to 255, default 255) | `{}` |
| Topic | `band.status` | none | `BandStatus` now, then on each change |
| Topic | `band.gestures` | none | `Gesture` |
| Topic | `band.motion` | `rateHz` (5 to 60, default 30) | a `MotionSample` with `source` `band` |

Without a band, `visor simulate band.gestures` makes Visor send only the gestures you type. See
[Testing](testing.md).
