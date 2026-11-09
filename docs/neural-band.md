# Neural Band

Scope: `band`. The Meta Ray-Ban Display with a paired Meta Neural Band. Without a band, `band.status` says
`paired: false`, and the gestures and the buzz fail with `unsupported`.

A band the person paired to the Display with Meta's app shows in `band.status` with the battery the Display reports,
and `held: false`. Its gestures reach apps all the same. The buzz needs the phone to **borrow** the band, which it
can do only once the person sets the band up in Visor's band settings; until then the buzz fails with
`unsupported` and the reason `not_set_up`.

The band talks to the Display, which uses its gestures to move around. Visor reads them through a session with the
Display, so the band stays with the Display: the Display acts on each gesture as usual, and the app gets it too.

## Sharing the band with the phone

The person can share a band set up in Visor: **Settings > Neural Band > Share with the phone**. The band then stays
connected to the phone, and the Display reaches it through the phone, as it does on Meta's relay lease. The Display
still gets every gesture and acts on it. For apps:

- `gestures` come straight from the band, without a session with the Display, with the band's own `action` (`press`,
  `release`, `tap`, `click`, a swipe) and what it made of them in `derived`.
- `buzz` goes out at once. It does not take the band from the Display.
- `status.shared` is true, and `status.link` stays `connected`.

Nothing changes in your code: the same calls work either way, and `shared` only tells you which way they go.

Sharing turns itself off when it does not work: when the phone cannot reach the band, or when the Display does not
reach the band through the phone. The band then goes back to the Display, and Visor's band settings say why.

## Gestures

```kotlin
visor.band.gestures.collect { gesture ->
    when {
        gesture.finger == Finger.INDEX && gesture.derived == GestureAction.DOUBLE_TAP -> next()
        gesture.finger == Finger.MIDDLE && gesture.derived == GestureAction.HOLD -> back()
    }
}
```

Collecting `gestures` opens a session with the Display, which shows nothing on its screen. The first time, Visor
asks the person once to set the glasses up for apps. The session needs the glasses worn, and the Display ends it
when another app comes to the front; Visor opens it again while your app collects. Collect only while your app
needs the gestures, for example while one screen is in front.

A `Gesture`:

| Field | Type | Meaning |
|---|---|---|
| `finger` | `Finger` | `thumb`, `index` or `middle`. |
| `action` | `GestureAction` | What the finger did: `tap`, `double_tap`, `triple_tap`, `hold`, or a thumb swipe `up`, `down`, `left`, `right`. |
| `derived` | `GestureAction?` | What the band made of it. On the glasses it is the same as `action`. |
| `time` | `Long` | When the phone got it, in milliseconds since 1970. |

The Display tells taps apart before it forwards them: a double tap comes as one `double_tap`, after its first
`tap`. A middle-finger tap is the Display's back, so the Display goes back while your app gets the tap. Sets like
`GestureAction` grow over time; treat values you do not know as nothing to act on.

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
| `borrowed` | `Boolean` | True while the phone holds the band. |
| `held` | `Boolean?` | True when Visor holds the band's keys, so the phone can borrow it. False for a band only the Display holds. |
| `shared` | `Boolean` | True while the person shares the band with the phone: gestures come straight from it and a buzz goes out at once. |

## Motion

```kotlin
visor.band.motion.collect { sample -> }               // 30 samples a second
visor.band.motion(rateHz = 60).collect { sample -> }
```

The band's own sensors on the wrist, in the same `MotionSample` as [the glasses' motion](glasses.md#motion), with
`source` `band`: `accelerometer` in m/s², `gyroscope` in rad/s, `magnetometer` as the band reports it, and the
band's own fused `orientation`. The band sends each sensor on its own, so a sample carries the newest reading of
each; `time` is when the phone got it, in nanoseconds, and only the differences between samples mean something. Use it to point
with the wrist, to know when the wearer raises the arm, or to count repetitions.

It needs the `motion` scope and a band the person shares with the phone (see above). Check `status.shared` before
you offer it. Without sharing, or when sharing stops while you read, it ends with `unsupported` and the reason
`not_shared`, whose message names the setting: reading the band's sensors otherwise would take the band from the
Display.
In a test, set `fake.band.shared = true`, wait for `fake.band.motionRate`, then send readings with
`fake.band.motion(ax, ay, az)` or a whole `MotionSample`.

## Vibration

```kotlin
visor.band.buzz()                             // 400 ms at full strength: the wearer notices it
visor.band.buzz(80.milliseconds, 0.5f)        // 10 ms to 1 s, strength above 0 up to 1
```

A buzz borrows the band for a moment, unless the band is shared: then it goes out at once. It fails with
`not_connected` when the band's link does not come up in time, and with `refused` when the band turns it down.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `band.status` | none | a `BandStatus` |
| Method | `band.buzz` | `durationMs` (10 to 1000, default 400), `strength` (1 to 255, default 255) | `{}` |
| Topic | `band.status` | none | `BandStatus` now, then on each change |
| Topic | `band.gestures` | none | `Gesture` |
| Topic | `band.motion` | `rateHz` (5 to 60, default 30) | a `MotionSample` with `source` `band` |

Without a band, `visor simulate band.gestures` makes Visor send only the gestures you type, and borrows nothing.
See [Testing](testing.md).
