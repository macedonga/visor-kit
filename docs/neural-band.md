# Neural Band

Scope: `band`. The Meta Ray-Ban Display with a paired Meta Neural Band. Without a band, `band.status` says
`paired: false`, and the gestures and the buzz fail with `unsupported`.

A band the person paired to the Display with Meta's app shows in `band.status` with the battery the Display reports,
and `held: false`. Its gestures reach apps all the same. The buzz needs the phone to **borrow** the band, which it
can do only once the person sets the band up in Visor's band settings; until then the buzz fails with
`unsupported` and the reason `not_set_up`.

The band talks to the Display, which uses its gestures to move around. Visor reads them through a session with the
Display, so the band stays with the Display: the Display acts on each gesture as usual, and the app gets it too.

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
| `link` | `Link` | The phone's own link to the band, which is up only while the phone borrows it for a buzz. |
| `battery` | `Int?` | 0 to 100. |
| `charging` | `Boolean?` | |
| `leftHanded` | `Boolean?` | True when the band is set for the left wrist. |
| `borrowed` | `Boolean` | True while the phone holds the band. |
| `held` | `Boolean?` | True when Visor holds the band's keys, so the phone can borrow it. False for a band only the Display holds. |

## Vibration

```kotlin
visor.band.buzz()                             // 400 ms at full strength: the wearer notices it
visor.band.buzz(80.milliseconds, 0.5f)        // 10 ms to 1 s, strength above 0 up to 1
```

A buzz borrows the band for a moment. It fails with `not_connected` when the band's link does not come up in
time, and with `refused` when the band turns it down.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `band.status` | none | a `BandStatus` |
| Method | `band.buzz` | `durationMs` (10 to 1000, default 400), `strength` (1 to 255, default 255) | `{}` |
| Topic | `band.status` | none | `BandStatus` now, then on each change |
| Topic | `band.gestures` | none | `Gesture` |

Without a band, `visor simulate band.gestures` makes Visor send only the gestures you type, and borrows nothing.
See [Testing](testing.md).
