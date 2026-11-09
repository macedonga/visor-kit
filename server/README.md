# kit-server: the engine Visor and FakeVisor share

The server side of the VDK protocol: it checks every call against the caller's scopes, runs methods, keeps streams
and feeds, and speaks JSON-RPC and MCP. Visor and FakeVisor both run it, which is why the fake behaves as Visor
does. Apps never need it.

| Artifact | Needs |
|---|---|
| `com.macedonga.visor:kit-server` | Java 17 or later; for hosting the VDK, not for using it |

## What is inside

| Part | What it does |
|---|---|
| `Dispatcher` | Routes calls, subscriptions and feeds to their handlers, checks scopes, and ends streams whose scope is taken back. |
| `Gate` | What the host decides: who calls, which scopes they hold, and what to do when a grant changes. |
| `Routes` | How a host declares its methods, topics and feeds. |
| `JsonRpcSession` | One JSON-RPC connection over any transport: adb, a WebSocket, the Display's browser. |
| `McpAdapter` | The methods a caller may use, as MCP tools for AI agents. |

The binder service (`KitServer`) and the adb socket (`ShellServer`) are in the Android library, as they need
Android.

Its types are marked `@KitInternal`: they can change in any release.
