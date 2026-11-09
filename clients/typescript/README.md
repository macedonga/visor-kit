# visor-kit for TypeScript

The Visor Development Kit (VDK) from TypeScript and JavaScript: Ray-Ban Meta glasses, the Meta Ray-Ban Display and
the Meta Neural Band through [Visor](https://github.com/macedonga/visor). It runs in browsers, in Node 22 or later,
and in web apps on the Meta Ray-Ban Display.

```
npm install visor-kit
```

From a clone of this repo: `npm install ./clients/typescript`, after `npm run build` there.

## Connect

```ts
import { Visor, onGlasses } from "visor-kit";

const visor =
  onGlasses() ? await Visor.connectGlasses()                     // a web app on the Display
  : token     ? await Visor.connect("ws://192.168.1.20:7333/rpc", token)   // Visor's network door
  :             await Visor.connectFake();                       // `visor fake` on this computer
```

- `connect(url, token)` reaches Visor's network door, with a client the person made in Visor's Advanced settings,
  under Network access. The client holds the scopes the person gave it.
- `connectGlasses()` reaches Visor from a web app on the Display, through the Display's browser. The person allows
  the app in Visor, Settings > Web apps. It rejects with `refused` and reason `not_allowed` until they do.
- `connectFake()` reaches the FakeVisor that `visor fake` runs, for building without a phone.

## Use it

```ts
console.log(await visor.call("glasses.status"));

const gestures = visor.subscribe("band.gestures", {}, (g) => console.log(g.finger, g.derived));
gestures.close();

if (visor.supports("band.motion")) {
  visor.subscribe("band.motion", { rateHz: 30 }, (sample) => draw(sample.orientation));
}
```

- `call(method, params)` resolves with the result, or rejects with a `VisorError` whose `code` and `reason` are the
  VDK's, such as `scope_denied` or `not_connected`.
- `subscribe(topic, params, onEvent, onEnd)` calls `onEvent` for each event until `close()` or the stream ends.
- `feed(name, params, onRecord, onEnd)` brings a feed's records, such as `camera.live`, as a header and bytes.
- `supports(name)` says whether this Visor answers a method, topic or feed.
- `welcome` holds Visor's answer to the hello: its version, what it answers, and the scopes this client holds.

Every method and topic of the VDK works this way, by its name. Each one is in the
[docs](https://github.com/macedonga/visor-kit/tree/main/docs), with its params and answers.

## Layouts and Briefing cards

`visor-kit/layout` builds layouts and Briefing cards for the Display, as the Kotlin builder does:

```ts
import { card, header, stat, change, page, chart, buttons, button } from "visor-kit/layout";

await visor.call("display.cards.set", card("stocks", "My stocks", [
  header("My stocks", "chart_line"),
  stat("27,012.40"),
  change("+1.8%", true),
], { icon: "chart_line", pages: [page([chart([26.1, 26.4, 26.7, 27.0])])] }));
```

It has every element (`column`, `row`, `text`, `image`, `icon`, `button`, `buttons`, `video`, `pager`, `chart`) and
the building blocks `header`, `stat`, `change`, `field` and `progress`. See
[Briefing cards](https://visor.macedonga.com/developers/docs/briefing-cards/).

## License

Apache 2.0.
