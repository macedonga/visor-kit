# Settings

The glasses keep about 370 settings: the ones the Meta AI app shows, and many it does not. Visor reads them all
when the glasses connect, and an app with the `settings` scope can read them and change the everyday ones.

```kotlin
val volume = visor.settings.get("notification_volume")
visor.settings.set("notification_volume", 100)
visor.settings.set("head_nod_controls_enabled", true)
visor.settings.list("Camera").forEach { println("${it.label}: ${it.value}") }
visor.settings.changes.collect { setting -> }   // each change, whoever made it
```

## A setting

A `GlassesSetting`:

| Field | Type | Meaning |
|---|---|---|
| `key` | `String` | The glasses' own name, such as `wakeword_enabled`. Stable across releases. |
| `kind` | `SettingKind` | `bool`, `int`, `float`, `string`, or `other` for an encoded value. |
| `value` | `String?` | The value in force, as text: what was set, else the firmware's default. Null when Visor knows neither, and for passwords. |
| `default` | `String?` | The firmware's default. |
| `label` | `String?` | What the Meta AI app calls it, in English. Null for a setting it does not show. |
| `group` | `String?` | Visor's group, such as `Camera`, `Sound` or `Meta AI`. |
| `options` | `List<SettingOption>` | For a choice, each value with its label. |
| `range` | `SettingRange?` | For a number, its `min`, `max`, `step` and `unit`. |
| `writable` | `Boolean` | Whether apps may change it. |
| `pending` | `Boolean` | True from a write until the glasses report the setting again. |

Values are text in the form the kind gives: `"true"`, `"3"`, `"1.5"`, or one of the options' values.
`accepts(value)` says whether a value fits the setting's kind, options and range.

## Changing a setting

`set` sends the value and returns the setting with `pending` true. The glasses take a moment, and Visor reads the
setting back: `changes` then brings it with the new value and `pending` false. If the glasses keep the old value,
`changes` brings the old value.

| Failure | Code and reason |
|---|---|
| The key is not a setting Visor knows | `invalid_params`, `unknown_key` |
| The value does not fit | `invalid_params`, `invalid_value` |
| Apps may not change it | `refused`, `read_only` |
| The glasses are not connected | `not_connected` |

Apps may change the settings the Meta AI app shows as a switch, a choice, a slider, a number, a text or a
language. Visor keeps the rest to itself and the person: updates, resets, heat protection, developer and logging
switches, and every encoded setting. Passwords are never shown.

## Over JSON-RPC

```
visor call settings.get '{"key":"wakeword_enabled"}'
visor call settings.set '{"key":"notification_volume","value":"100"}'
visor call settings.list '{"group":"Camera"}'
visor watch settings.changes '{"keys":["notification_volume"]}'
```

On `visor fake`, five settings stand in for the glasses' own: `wakeword_enabled`, `head_nod_controls_enabled`,
`notification_volume`, `resolution_and_framerate`, and `cloud_ota_enabled`, which apps may only read.
`FakeVisor.settings.change(key, value)` changes one as the wearer would.
