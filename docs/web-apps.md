# Web apps on the Display

Scope: any. The Meta Ray-Ban Display, with a web app the person added in Visor.

The Display runs web apps in its own browser. A web app reaches the VDK through that browser and the phone's Visor,
with the TypeScript client's `Visor.connectGlasses()`: the same calls, topics and errors as everywhere else, from
JavaScript on the glasses. Meta's toolkit has nothing like it.

```ts
import { Visor, VisorError, onGlasses } from "visor-kit";

async function start() {
  if (!onGlasses()) return Visor.connectFake();   // on a computer, with `visor fake`
  try {
    return await Visor.connectGlasses();
  } catch (e) {
    if (e instanceof VisorError && e.reason === "not_allowed") {
      showMessage("Allow this app on your phone: Visor > Settings > Web apps > this app > Visor access.");
    }
    throw e;
  }
}

const visor = await start();
visor.subscribe("band.gestures", {}, (gesture) => console.log(gesture));
await visor.call("voice.speak", { text: "Hello from the web app" });
```

Build the client from `clients/typescript` and serve `dist/index.js` and `dist/glasses.js` with the page, or bundle
them with the page's own code.

## What the person allows

In Visor, **Settings > Web apps**, the person taps the app and picks what it may do under **Visor access**, one
scope at a time. That turns on **Nearby Bluetooth devices** for the app too, which is how it reaches Visor. The
first time, Visor reads the glasses' configuration before it saves, which can take a minute; the sheet says so while
it waits. The browser asks the wearer once more before the page's first connection.

Removing the app takes its access away.

## When it fails

`connectGlasses()` rejects with a `VisorError`:

| Code | Reason | What happened | What to do |
|---|---|---|---|
| `unsupported` | | The page is not in the Display's browser. | Check `onGlasses()` first, and use `connectFake()` or `connect()` elsewhere. |
| `refused` | `not_allowed` | The glasses do not let this app reach Visor. | Tell the person where to allow it, as in the example. |
| `visor_gone` | | Visor did not answer within `timeoutMs` (10 s by default). | Visor is not running, or the app has no Visor access yet. |

After that, a call the person did not allow fails with `scope_denied`, as anywhere else.

## How it travels

The Display's browser hands a page's `navigator.bluetooth.requestLEScan` to the phone, which runs the scan and
sends back what it hears. Visor reads the scan's filter instead:

- A message to Visor is a scan whose filter holds manufacturer data for company `0xFFFF`, starting with `VDK1`.
- Visor's answers and events come back as advertisements from a device named `Visor`, with the same kind of data.
- One long scan carries everything Visor sends. Each message to Visor is a short scan of its own.

The client does all of this; a page uses `call`, `subscribe` and `feed` as anywhere else. Messages go in order. A
large one, such as a layout with a photo, goes in pieces of 8000 bytes. Each piece is a scan, so a call takes tens
of milliseconds more than over a socket: use `subscribe` rather than polling.

A web app's origin is all Visor knows about it. Every page on that origin gets the same access, so allow only
origins you trust.

## Tools for the assistant

A web app can give the wearer's assistant tools of its own, with the browser's WebMCP API, while it is open:

```js
document.modelContext.registerTool({
  name: "add_to_list",
  description: "Add an item to the shopping list.",
  inputSchema: { type: "object", properties: { item: { type: "string" } }, required: ["item"] },
  execute: async ({ item }) => ({ content: [{ type: "text", text: `Added ${item}.` }] }),
});
```

The person turns this on once, in Visor: **Settings > Web apps > Web app tools for your assistant**. Visor then
hands the tools to the model the person set up in Visor, with your description and schema, and runs the calls the
model makes. Without it, `document.modelContext` is missing: check for it before you register.

## Other parts of the web-app SDK

The Display's browser gives web apps more than the VDK, from the phone Visor runs on:

- `navigator.geolocation`, from the phone's location.
- The Generic Sensor API with `sensorId: 'phone'`: the phone's accelerometer, gyroscope, magnetometer, linear
  acceleration, gravity and orientation, up to 100 readings a second.
- Real Bluetooth scans of the devices around the wearer, for an app with **Nearby Bluetooth devices** on.

The protocol behind each is on the wiki's
[Display web apps](https://github.com/macedonga/visor/wiki/Display-web-apps) page.
