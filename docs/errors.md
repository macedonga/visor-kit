# Errors

Every failed call, and every stream that ends badly, throws a `KitException` (in JSON-RPC, an error whose `data`
is a `Failure`). Act on `code`. `message` is for logs: its wording changes between releases.

```kotlin
try {
    visor.camera.takePhoto()
} catch (e: KitException) {
    Log.w(TAG, "photo failed: ${e.code} ${e.reason}: ${e.message}")
}
```

| Code | Meaning | What to do |
|---|---|---|
| `scope_denied` | The app does not hold the scope in `scope`. | Ask for it with `RequestScopes`. |
| `unknown_method` | Visor does not know the method or topic. | Check the name. From the library this cannot happen: it raises `visor_too_old` first. |
| `invalid_params` | The params do not fit the method. | Fix the call. |
| `unsupported` | These glasses cannot do it: the prompter without a display, the band without a band. | Check `GlassesStatus.display`, or `BandStatus.paired`, before offering the feature. |
| `not_connected` | The glasses, or the band, are not connected and did not connect in time. | Tell the person to put the glasses on, or take them out of the case. |
| `busy` | Something else holds what this needs, for example a video is recording. | Try again later. |
| `refused` | The glasses or the band turned the request down. | Usually nothing; see the message. |
| `timeout` | The glasses did not answer in time. | Try again; if it keeps happening, the glasses may need a restart. |
| `failed` | Anything else. `reason` can say more, for example why an import failed. | See the method's page. |
| `visor_missing` | Visor is not installed. Raised by the library. | Point the person to Visor. |
| `visor_too_old` | Visor is older than this call needs. Raised by the library. | Ask the person to update Visor, or check `visor.supports(...)` first. |
| `visor_gone` | The connection to Visor broke during the call, for example because Visor restarted. Raised by the library. | Call again; the connection comes back on its own. |
| `closed` | Your app closed the connection while the call ran. Raised by the library. | Nothing. |

Some failures carry a `reason` that says what the person can change:

| Reason | With | What to tell the person |
|---|---|---|
| `not_set_up` | `unsupported` | Set the band up in Visor's band settings: only the Display holds it now. |
| `not_shared` | `unsupported` | Turn on Visor > Settings > Neural Band > Share with the phone, for `band.motion`. |
| `not_allowed` | `refused` | Allow the web app in Visor > Settings > Web apps > the app > Visor access. |

Visor adds codes and reasons over time. Treat a code you do not know as `failed`.
