# kit-protocol: the wire contract

Every method, topic, feed, type and error code of the VDK, in plain Kotlin, with the OpenRPC document that
describes them for any language. The Android library, the server, Visor and FakeVisor all build on it, so they can
never disagree about a field.

| Artifact | Needs |
|---|---|
| `com.macedonga.visor:kit-protocol` | Java 17 or later; kotlinx.serialization |

```kotlin
dependencies {
    implementation("com.macedonga.visor:kit-protocol:0.1.0")
}
```

## What is inside

| Part | What it does |
|---|---|
| `Methods`, `Topics`, `Feeds` | Each one with its name, scope, the protocol version that added it, and its params and answer types. |
| The types | `GlassesStatus`, `Gesture`, `MotionSample`, `Layout`, `BriefingCard` and every other value on the wire. |
| `Scope`, `ErrorCode` | The scopes a person grants and the codes a failure carries. |
| `KitProtocol.VERSION` | The protocol version: 1. |
| `KitJson` | The JSON settings the wire uses. |
| [`api/openrpc.json`](api/openrpc.json) | The whole contract as OpenRPC, for any language and for code generators. |

## The rules it keeps

A released method, topic, field or type is never removed or renamed. A new field is optional, with a default. Sets
that grow, such as scopes, error codes and gesture actions, are string value classes, so an older app reads a newer
value as unknown instead of failing. The build checks all of it against the last release: see
[Compatibility](../docs/compatibility.md).
