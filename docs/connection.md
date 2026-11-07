# The connection

A `Visor` object is one connection to Visor. It follows Visor through restarts on its own, and tells you where
it stands through `state`.

## Connecting

```kotlin
val visor = Visor.connect(context)
```

`connect` binds to Visor's VDK service, says hello, and returns once Visor answered. It fails with a
`KitException`:

| Code | Why |
|---|---|
| `visor_missing` | Visor is not installed. |
| `visor_too_old` | This Visor has no VDK service. |

It suspends while Visor's process starts. To give up after a while, wrap it: `withTimeout(10.seconds) {
Visor.connect(context) }`.

Keep one `Visor` for as long as you use the glasses, for example in a ViewModel or a service, and call `close()`
when you are done. It uses the application context, so it holds no activity.

## State

```kotlin
visor.state.collect { state ->
    when (state) {
        VisorState.Connecting -> ...       // binding again after Visor restarted
        is VisorState.Open -> ...          // state.welcome: Visor's version, its methods, your scopes
        is VisorState.Closed -> ...        // over for good; state.error says why, null when you closed it
    }
}
```

- **Connecting:** Visor restarted, or was updated, and Android is bringing it back. Calls made now wait for it.
  Streams pick up again by themselves once it is back; events sent in between are lost.
- **Open:** Visor answers. `welcome` lists the methods and topics this Visor has and the scopes your app holds.
- **Closed:** you closed the connection, or Visor turned it away, for example because Visor was
  uninstalled. Connect again for a new one.

## Calls and streams

Every call is a suspending function, safe from the main thread, and fails with a `KitException`. Cancelling the
coroutine cancels the call in Visor too.

Every stream is a cold `Flow`: collecting it subscribes, and stopping the collection unsubscribes. Streams of
state, like `glasses.status`, send the current value first. A stream ends with a `KitException` when Visor ends
it, for example when a scope is taken back.

## Versions

Visor and your app can be different ages. At hello, each side says which protocol version it speaks, and Visor
lists what it answers. Ask before using something new:

```kotlin
if (visor.supports(Methods.DISPLAY_PROMPTER_SHOW)) { ... }
```

A call that the installed Visor does not have fails with `visor_too_old` before it is sent. See
[Compatibility](compatibility.md).

## Threads

The library runs nothing on the main thread. Answers from Visor arrive on binder threads and resume your
coroutines in their own context.
