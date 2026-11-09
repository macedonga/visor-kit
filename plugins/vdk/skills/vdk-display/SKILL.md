---
name: vdk-display
description: Layouts, Briefing cards and web apps on the Meta Ray-Ban Display: elements, blocks, clicks, back, video, live cards, connectGlasses, the simulator and the in-app preview
---

# The Display

- Build a whole screen with `Layout { }`: `column`, `row`, `text`, `image` (a URL, a `data:` URI, or a `Bitmap`
  on Android), `icon(LayoutIconName.X)`, `button(id, label)`, `buttons { }` (a button bar), `pager { page { } }`,
  `chart(values)` and `video(url)`, which fills a layout alone. Text takes `style` (heading, title, body, meta),
  `color` (`LayoutColor.rgb(0xRRGGBB)` too), `maxLines`, `align`; any element takes `pinTop`/`pinEnd`/...
- Blocks drawn as Meta's own cards: `header(icon, title)`, `stat(value, caption)`, `change(text, rising)`,
  `field(label, value)`, `progress(fraction)`.
- Show it with `visor.display.layout.show(layout)`; a new show replaces it. `close()` empties it.
- Clicks come on `visor.display.layout.clicks` by element `id`. `visor.display.layout.state` says when the
  layout went, with `reason` `back` when the wearer went back. `visor.display.layout.video` follows a video.
- Briefing cards stay on the Display's home: `visor.display.cards.set(id, name, icon) { ...; detail { } }` or
  `pages { page { } }`, `cards.keep(id, name, icon, flow) { state -> }` for live ones, `cards.remove(id)`. Clicks
  in a detail come on `visor.display.cards.clicks` with `card` and `id`; a `toast` on the element confirms one. At
  most 256 KiB a card.
- See it without glasses: `visor fake`, then open `http://127.0.0.1:7333/display` (Screen: the Briefing for
  cards); `visor --fake card card.json`. In an app, `fake.display.preview(context)` and
  `fake.display.briefingPreview(context)` from `kit-testing` draw the same.

- A web app on the Display uses the VDK from JavaScript: `if (onGlasses()) visor = await Visor.connectGlasses()`
  (TypeScript client). It fails with `refused`, reason `not_allowed`, until the person allows the app in Visor >
  Settings > Web apps > the app > Visor access; show them that path. Elsewhere, `Visor.connectFake()`.

Details: `read_vdk_doc` with `display`, `briefing-cards` and `web-apps`, and `testing` for the simulator.
