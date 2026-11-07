# Scopes and consent

An app can use only what the person allowed. Visor checks every call and every stream against the app's
**scopes**, which the person grants on Visor's consent screen. Nothing else is needed: no setting to turn on, no
Developer mode.

## The scopes

| Scope | Lets the app | Methods and topics |
|---|---|---|
| `state` | See whether the glasses are connected, worn and charged | `glasses.status` |
| `camera` | Take photos and videos, and copy them to the phone | `camera.*`, `captures.*` |
| `band` | Read the Neural Band's gestures and make it vibrate | `band.*` |
| `display` | Show text on the Display | `display.*` |

`kit.hello` needs no scope. Later releases add scopes; an app built against an older kit never sees them unless
it asks for them by id.

## Asking

`RequestScopes` opens Visor's consent screen over your app. The screen names your app, as the launcher shows it,
and lists only the scopes it does not hold yet. The person allows all of them or none.

```kotlin
val ask = registerForActivityResult(RequestScopes()) { held -> }
ask.launch(setOf(Scope.CAMERA, Scope.BAND))
```

The result is every scope your app holds once the person is done, also when they just go back. A scope this
Visor does not know is never granted, so it is missing from the result.

Launching throws `ActivityNotFoundException` when Visor is not installed. Check `Visor.isInstalled(context)`
first.

## When a scope is missing

A call that needs a scope the app does not hold fails with `scope_denied`, and `KitException.scope` names the
scope:

```kotlin
catch (e: KitException) {
    if (e.code == ErrorCode.SCOPE_DENIED) ask.launch(setOf(e.scope!!))
}
```

A stream started without its scope ends at once with the same failure. Collect it again after the person allows
it.

## Taking scopes back

The person sees every app that holds scopes in Visor, under Settings, Advanced settings, Apps with access, and can
remove an app's access there. When that happens:

- the app's open streams end with `scope_denied`;
- its next calls fail the same way;
- calls already running finish.

## Who an app is

Visor learns which app calls from Android, by the calling uid, and ties each grant to the app's package and to the
certificate it is signed with. If a different app is later installed under the same package name with another
certificate, it holds nothing. Your debug and release builds are signed with different certificates, so each one
asks for its own grant.

## While you develop

`visor grant` and `visor revoke` set an app's scopes from your computer, without the consent screen:

```
visor grant com.example.app camera band
visor revoke com.example.app band
visor apps
```

See [Testing](testing.md).
