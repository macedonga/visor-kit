# Glasses

Scope: `state`. All glasses.

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

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `glasses.status` | none | a `GlassesStatus` |
| Topic | `glasses.status` | none | a `GlassesStatus` now, then on each change |

```
$ vdk call glasses.status
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
