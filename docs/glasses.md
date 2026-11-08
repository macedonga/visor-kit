# Glasses

Visor can have several pairs set up, and connects to one at a time: the **active** pair. Every method acts on the
active pair. The status needs the `state` scope; seeing every pair and choosing the active one needs `pairs`.

## Status

```kotlin
visor.glasses.status.collect { status -> }   // now, then each change
val now = visor.glasses.status.first()        // once
```

A `GlassesStatus`:

| Field | Type | Meaning |
|---|---|---|
| `paired` | `Boolean` | False when Visor has no glasses set up. Every other field is then empty. |
| `link` | `Link` | `disconnected`, `connecting` or `connected`. |
| `model` | `String?` | The glasses' own model name, to show to people. |
| `name` | `String?` | The name the person gave the pair. |
| `serial` | `String?` | |
| `firmware` | `String?` | The firmware build. |
| `display` | `Boolean?` | True for glasses with a display. Null until Visor has seen the glasses once. |
| `battery` | `Int?` | 0 to 100. |
| `charging` | `Boolean?` | |
| `worn` | `Boolean?` | True while the glasses sit on a face. |
| `folded` | `Boolean?` | True while the temples are folded. |
| `recording` | `Boolean?` | True while the glasses record a video. |
| `caseBattery` | `Int?` | The charging case's battery, 0 to 100, while the glasses sit in the case. |

The values come from the glasses, so most are null while they are not connected. Tell models apart by `display`,
never by `model`, whose wording can change.

## Your pairs

Scope: `pairs`.

```kotlin
visor.glasses.pairs.collect { pairs ->
    pairs.forEach { println("${it.name ?: it.look?.styleName} ${if (it.active) "(active)" else ""}") }
}
visor.glasses.select("2Y0YBYPJ8F007Q")
```

`pairs` sends every pair set up in Visor now, then each time one is added, renamed, removed or made active.
`select` makes another pair the active one: Visor lets go of the pair connected now and connects to the new one, as
when the person picks it on Visor's home. It returns once Visor has switched, before the pair is connected, so watch
`status` for the link. It fails with `busy` while a Live view or an import holds the glasses, and with
`invalid_params` and reason `not_found` for a serial Visor has not set up.

A `GlassesPair`:

| Field | Type | Meaning |
|---|---|---|
| `serial` | `String` | |
| `name` | `String?` | The name the person gave the pair. |
| `active` | `Boolean` | True for the pair Visor connects to now. |
| `link` | `Link` | The active pair's link; the others read `disconnected`. |
| `product` | `Int?` | Meta's product id of the model, such as 266 for Ray-Ban Meta (Gen 2). |
| `display` | `Boolean?` | True for glasses with a display. |
| `look` | `GlassesLook?` | Frame, colour and lenses from Meta's record of the serial: `style`, `styleName`, `color`, `colorName`, `lens`, `lensName`, `size`. |
| `battery` | `Int?` | The battery when Visor last heard from the pair. |
| `caseBattery` | `Int?` | The case's battery at that time. |
| `lastSeen` | `Long?` | When Visor last heard from the pair, in milliseconds since 1970. |
| `band` | `String?` | The serial of the Neural Band paired with it. |

## Touches and presses

Scope: `inputs`. All glasses.

```kotlin
visor.glasses.inputs.collect { input ->
    if (input.source == InputSource.TOUCHPAD && input.action == InputAction.SWIPE_FORWARD) nextPage()
}
```

Each `GlassesInput` is one touch of the touchpad or one press of a button, as the glasses report it to the phone.
The glasses still act on it as they always do: a tap still pauses the music, and the capture button still takes a
photo. The VDK cannot keep an input from them, except back on the Display (below).

| `source` | Actions |
|---|---|
| `touchpad` | `tap`, `double_tap`, `triple_tap`, `tap_hold`, `swipe_forward`, `swipe_backward`, `slide_forward`, `slide_backward`, `touch`, `release`, `palm_tap`, `palm_hold` |
| `capture_button` | `press`, `release`, `long_press`, `very_long_press`, `very_very_long_press`, `extremely_long_press`, `very_very_long_release`, `double_press` |
| `action_button` | `press`, `release`, `long_release` |
| `touchpad`, `band` on the Display | `nav_up`, `nav_down`, `nav_left`, `nav_right`, `select`, `back`, `drag_start`, `drag_move`, `drag_end` |

A swipe goes `forward` towards the lenses. `touch` and `release` tell when a finger rests on the touchpad and when
it leaves; `slide_*` is a finger that moves while it rests there. `time` is when Visor heard the input. Which
actions a pair reports depends on its model and firmware: treat an action you do not know as unknown, and a source
you do not know too.

On the Display the touchpad and the band also navigate, as in DAT: `nav_*`, `select` and `back`, and drags with
`x`, `y`, `dx` and `dy` in the glasses' own units. Ask for some sources only, and keep back for your app, with an
`InputsRequest`:

```kotlin
visor.glasses.inputs(InputsRequest { sources = listOf(InputSource.BAND); keepBack = true }).collect { input -> }
```

`keepBack` works on the Display only: back then comes to the app as `back`, and the Display stays on its screen.

## Motion

```kotlin
visor.glasses.motion.collect { sample -> }          // 30 samples a second
visor.glasses.motion(rateHz = 60).collect { sample -> }
```

> On the glasses, Visor reads motion from their own sensor service, with nothing on the screen and no toolkit app.
> The phone fuses the accelerometer, gyroscope and magnetometer into `orientation`, which settles within two
> seconds of the start. `source` is `glasses`: the band's motion comes only from FakeVisor.

Motion needs the `motion` scope. The rate is one of 5, 10, 15, 24, 30 or 60 samples a second. A `MotionSample`:

| Field | Type | Meaning |
|---|---|---|
| `time` | `Long` | When the sensors read it, in nanoseconds on the glasses' clock. Only differences mean something. |
| `accelerometer` | `Vector3?` | Acceleration, gravity included, in m/s². |
| `gyroscope` | `Vector3?` | Rotation speed, in rad/s. |
| `magnetometer` | `Vector3?` | The magnetic field, in µT. |
| `orientation` | `Quaternion?` | The sensors combined, as a unit quaternion. |
| `source` | `MotionSource` | `glasses` or `band`. |

The axes are Android's, in the sensor's frame. A sensor the glasses did not read for a sample is null.

In a test, wait for `fake.glasses.motionRate` to be set, then send readings with `fake.glasses.motion(ax, ay, az)`
or a whole `MotionSample` with `fake.glasses.motion(sample)`.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `glasses.status` | none | a `GlassesStatus` |
| Topic | `glasses.status` | none | a `GlassesStatus` now, then on each change |
| Method | `glasses.pairs` | none | a list of `GlassesPair` |
| Topic | `glasses.pairs` | none | the list now, then on each change |
| Method | `glasses.select` | `serial` | the new active `GlassesPair` |
| Topic | `glasses.inputs` | `sources`, `keepBack` (an `InputsRequest`, optional) | a `GlassesInput` for each touch or press |
| Topic | `glasses.motion` | `rateHz` (default 30) | a `MotionSample` for each reading |

```
$ visor call glasses.status
{
    "paired": true,
    "link": "connected",
    "serial": "...",
    "display": false,
    "battery": 82,
    "charging": false,
    "worn": true,
    "folded": false,
    "recording": false
}
```
