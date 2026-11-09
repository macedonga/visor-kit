# VDK guidance for coding assistants

Save this as `AGENTS.md` in an app that uses the Visor Development Kit (`visor agents > AGENTS.md`), or point a
rules file at it. It tells a coding assistant how to build on the VDK. For any detail, search the docs: run
`visor docs QUERY`, or add the docs MCP server with `claude mcp add vdk-docs -- visor docs-mcp` and call
`search_vdk_docs` and `read_vdk_doc`.

## What the VDK is

The VDK lets an Android app, or a program on a computer, use Ray-Ban Meta glasses, the Meta Ray-Ban Display and the
Meta Neural Band through Visor, the Android app that runs them. It is an alternative to Meta's Wearables Device
Access Toolkit (DAT): no Meta AI app, no Meta account, no developer centre. Porting from DAT: read
`porting-from-dat`.

- Android: `implementation("com.macedonga.visor:kit:<version>")`, then `val visor = Visor.connect(context)`.
- Tests: `com.macedonga.visor:kit-testing` gives `FakeVisor` and `fake.connect()` in a plain JVM test.
- Other languages: JSON-RPC over adb or Visor's network door; Python `visor_kit` and TypeScript clients.
- A web app on the Display: the TypeScript client's `Visor.connectGlasses()`, guarded by `onGlasses()`. On a
  `refused` with reason `not_allowed`, tell the person to allow the app in Visor > Settings > Web apps.

## Rules

1. Ask for scopes before using them: `registerForActivityResult(RequestScopes())`, then `launch(setOf(...))`. A
   call without its scope fails with `KitException` code `scope_denied`, naming the missing `scope`.
2. Every call is a `suspend fun`; every stream is a cold `Flow` that runs while collected. Collect streams in a
   lifecycle-aware scope and cancel them to stop: there are no start and stop pairs.
3. Handle `KitException` by its `code` (`unsupported`, `not_connected`, `busy`, `invalid_params`, `failed`,
   `scope_denied`) and `reason`. Treat an unknown code as `failed` and unknown enum-like values as unknown: the
   value sets (scopes, input actions, gesture actions, layout types) grow.
4. Layouts for the Display are whole screens: build one with `Layout { }` and show it with
   `visor.display.layout.show(...)`; a new show replaces it. Clicks come on `display.layout.clicks` by the
   element's `id`; `display.layout.state` says when the wearer went back.
   For something the wearer should see all day (a timer, a delivery, a score), prefer a Briefing card:
   `visor.display.cards.set(id, name, icon) { header(...); stat(...); detail { } }`, kept with `cards.keep(id,
   name, icon, flow) { }`, with clicks in its detail on `display.cards.clicks`. A card takes at most 256 KiB.
5. A voice phrase reaches the app while it collects `visor.voice.requests(phrases)`, or starts it when its
   activity declares the phrases (`VoiceLaunch`). Reply within about 20 seconds with `visor.voice.reply`.
6. Do not invent methods. The methods and topics are in `kit-protocol` (`Methods`, `Topics`) and in the docs'
   JSON-RPC tables; `visor methods` lists what a Visor answers.

## Testing without glasses

- `FakeVisor` in unit tests: set `fake.glasses.*`, send `fake.glasses.input(...)`, `fake.band.gesture(...)`,
  `fake.voice.say(...)`, `fake.display.click(id)`, `fake.display.back()`, `fake.display.clickCard(card, id)`.
- `visor fake` on a computer: JSON-RPC on `ws://127.0.0.1:7333/rpc`, MCP on `/mcp`, the Display simulator on
  `http://127.0.0.1:7333/display`, and a console that plays the glasses.
- `visor record FILE` and `visor replay FILE` capture and play back what the wearer did.
- `FakeGlasses.playMotion` plays DAT's motion CSV; `FakeLive.play` plays a recorded H.264 or H.265 stream.
