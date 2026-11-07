# visor-kit for Python

The Visor Development Kit (VDK) from Python, over adb or Visor's network door.

```
pip install ./clients/python            # adb only
pip install "./clients/python[network]"  # the network door too
```

```python
import asyncio
import visor_kit

async def main():
    async with await visor_kit.connect_adb() as visor:
        print(await visor.call("glasses.status"))
        async for gesture in visor.subscribe("band.gestures"):
            print(gesture["finger"], gesture["derived"])

asyncio.run(main())
```

- `connect_adb(serial=None)` needs adb and Developer mode on in Visor; it holds every scope.
- `connect_network("ws://127.0.0.1:7333/rpc", token)` needs a client made in Visor's Advanced settings, under
  Network access, and holds that client's scopes.
- `call(method, params)` returns the result; `subscribe(topic, params)` yields events; `feed(name, params)`
  yields a feed's records as `(header, payload)`, the payload as bytes. All raise `VisorError`, whose `code` is
  the kit's error code.

Method and topic names, params and results are in [the JSON-RPC docs](../../docs/json-rpc.md) and
[`openrpc.json`](../../protocol/api/openrpc.json).
