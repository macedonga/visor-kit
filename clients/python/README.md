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
- `connect_fake()` reaches the FakeVisor that `visor fake` runs on this computer, for building without a phone.
  It needs the network extra.
- `call(method, params)` returns the result; `subscribe(topic, params)` yields events, subscribing when the
  iteration starts; `open(topic, params)` subscribes first and returns a `Stream` to iterate and `aclose()`, for
  events that follow something you do; `feed(name, params)`
  yields a feed's records as `(header, payload)`, the payload as bytes. All raise `VisorError`, whose `code` is
  the kit's error code.

`visor_kit.layout` builds layouts and Briefing cards for the Display as plain dicts, with the same pieces as the
Kotlin builder: `column`, `row`, `text`, `image`, `icon`, `chart`, `pager`, `page`, and the blocks `header`, `stat`,
`change`, `field` and `progress`. Fields take snake_case: `max_lines=1` is `maxLines`.

```python
from visor_kit.layout import card, header, stat, progress, page, chart, field

await visor.call("display.cards.set", card(
    "pasta", "Kitchen timer",
    header("Kitchen timer", icon="clock"),
    stat("4:00", "Pasta"),
    progress(0.6),
    icon="clock",
    pages=[page(field("Started", "19:42")), page(chart([20, 60, 95, 100]))],
))
```

`image_file(path)` turns a photo into a `data:` URI for `image()`; Visor scales it to the Display.

Method and topic names, params and results are in [the JSON-RPC docs](../../docs/json-rpc.md) and
[`openrpc.json`](../../protocol/api/openrpc.json).
