# Neural Band

Scope: `band`. The Meta Ray-Ban Display with a paired Meta Neural Band. Without a band, `band.status` says
`paired: false`, and the gestures and the buzz fail with `unsupported`.

A band the person paired to the Display with Meta's app shows in `band.status` with the battery the Display reports,
and `held: false`. The phone can borrow it only once the person sets it up in Visor's band settings, so until then
the gestures and the buzz fail with `unsupported` and the reason `not_set_up`.

The band talks to the Display, which uses its gestures to move around. To hand the gestures to an app, the phone
**borrows** the band: while it holds the band, the gestures come to the phone and the Display does not get them.
The band goes back to the Display as soon as the phone lets go.

## Gestures

```kotlin
visor.band.gestures.collect { gesture ->
    when {
        gesture.finger == Finger.INDEX && gesture.derived == GestureAction.DOUBLE_TAP -> next()
        gesture.finger == Finger.MIDDLE && gesture.derived == GestureAction.HOLD -> back()
    }
}
```

Collecting `gestures` borrows the band; stopping the collection gives it back once no other app collects. Collect
only while your app needs the gestures, for example while one screen is in front.

A `Gesture`:

| Field | Type | Meaning |
|---|---|---|
| `finger` | `Finger` | `thumb`, `index` or `middle`. |
| `action` | `GestureAction` | What the finger did: `press`, `release`, `tap`, `double_tap`, `click`, or a thumb swipe `up`, `down`, `left`, `right`. |
| `derived` | `GestureAction?` | What the band made of it: `tap`, `double_tap`, `hold`, `release`, or a swipe direction. Null when the band did not say. |
| `time` | `Long` | When the phone got it, in milliseconds since 1970. |

Most apps want `derived`. Each tap comes as `tap`, and the second tap of a double tap comes as `double_tap`. To
tell a single tap from the first half of a double, wait a moment before acting on a `tap`: Visor's own gesture
lessons wait 420 ms. Sets like `GestureAction` grow over time; treat values you do not know as nothing to act
on.

## Status

```kotlin
visor.band.status.collect { status -> }
```

A `BandStatus`:

| Field | Type | Meaning |
|---|---|---|
| `paired` | `Boolean` | False when no band is paired with the glasses. |
| `link` | `Link` | The phone's own link to the band, which is up only while the phone borrows it. |
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
