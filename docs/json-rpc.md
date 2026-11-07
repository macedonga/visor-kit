# JSON-RPC

The VDK speaks JSON-RPC 2.0, so any language can use it, through two doors: Visor's adb socket, described here,
and [the network door](network.md), a WebSocket with the same messages.

## Reaching the socket

Visor listens on the abstract Unix socket `visor-kit` while Developer mode is on, and lets in only adb's shell
user. Forward a port to it:

```
adb forward tcp:7333 localabstract:visor-kit
```

Then connect to `127.0.0.1:7333`. Remove the forward when done: `adb forward --remove tcp:7333`. If nothing
answers and the connection closes at once, Visor is not running or its Developer mode is off. Developer mode opens
only this socket: apps never need it.

The shell holds every scope, and also has the `shell.*` methods: `shell.apps`, `shell.grant`, `shell.revoke`,
`shell.simulate`, `shell.inject`, and the topic `shell.trace`.

## Framing

One JSON object per line, UTF-8, ending in `\n`, both ways. Requests carry an `id` you choose; answers carry the
same `id`. Several requests can be in flight at once.

```
-> {"jsonrpc":"2.0","id":1,"method":"kit.hello","params":{"protocol":1,"kit":"my-script 1.0"}}
<- {"jsonrpc":"2.0","id":1,"result":{"protocol":1,"visor":"2.4.0","granted":["state","camera","band","display"],"methods":[...],"topics":[...]}}
-> {"jsonrpc":"2.0","id":2,"method":"glasses.status"}
<- {"jsonrpc":"2.0","id":2,"result":{"paired":true,"link":"connected","battery":82,"worn":true}}
```

Say `kit.hello` first. Params are by name, as an object; a method without params takes `{}` or none.

## Errors

```
<- {"jsonrpc":"2.0","id":3,"error":{"code":-32000,"message":"The glasses are not connected.","data":{"code":"not_connected","message":"The glasses are not connected."}}}
```

`error.data` is the VDK's `Failure`, with `code`, `message`, and sometimes `scope` and `reason`; see
[Errors](errors.md). `error.code` is `-32601` for an unknown method, `-32602` for params that do not fit,
`-32700` for a line that is not JSON-RPC, and `-32000` for every other failure.

## Streams

JSON-RPC has no streams, so the VDK adds three methods and two notifications:

| | Name | Params | Answer |
|---|---|---|---|
| Method | `kit.subscribe` | `topic`, `params` (optional) | `{"subscription": ID}`, where ID is the request's own id |
| Method | `kit.unsubscribe` | `subscription` | `{}` |
| Method | `kit.cancel` | `id` of a running call | `{}` |
| Notification | `kit.event` | `subscription`, `event` | |
| Notification | `kit.ended` | `subscription`, `failure` (absent when the stream simply ended) | |

```
-> {"jsonrpc":"2.0","id":4,"method":"kit.subscribe","params":{"topic":"band.gestures"}}
<- {"jsonrpc":"2.0","id":4,"result":{"subscription":4}}
<- {"jsonrpc":"2.0","method":"kit.event","params":{"subscription":4,"event":{"finger":"index","action":"tap","derived":"tap","time":1793111111111}}}
```

The answer to `kit.subscribe` always comes before the subscription's first event.

## The contract

[`protocol/api/openrpc.json`](../protocol/api/openrpc.json) is an [OpenRPC](https://open-rpc.org) document of
every method, its params and its answer, with every type under `components.schemas`. The VDK's own additions:

- `x-scope`: the scope a method or topic needs.
- `x-since`: the protocol version that added it.
- `x-shell`: answered only to the adb shell.
- `x-topics`: the topics, each with its `params` and its `event`.

Generate a client from it, or read it as the reference. It is written by the build from the Kotlin contract, so
it is always exact.

## A client in Python

```python
import json, socket, subprocess

subprocess.run(["adb", "forward", "tcp:7333", "localabstract:visor-kit"], check=True)
sock = socket.create_connection(("127.0.0.1", 7333))
lines = sock.makefile("r", encoding="utf-8")

def call(id, method, params=None):
    sock.sendall((json.dumps({"jsonrpc": "2.0", "id": id, "method": method, "params": params or {}}) + "\n").encode())
    while True:
        message = json.loads(lines.readline())
        if message.get("id") == id:
            return message

call(1, "kit.hello", {"protocol": 1, "kit": "python-example"})
print(call(2, "glasses.status")["result"])
call(3, "kit.subscribe", {"topic": "band.gestures"})
for line in lines:
    message = json.loads(line)
    if message.get("method") == "kit.event":
        print(message["params"]["event"])
```
