---
name: vdk-display
description: Layouts on the Meta Ray-Ban Display: elements, clicks, back, video, the simulator and the in-app preview
---

# The Display

- Build a whole screen with `Layout { }`: `column`, `row`, `text`, `image`, `icon(LayoutIconName.X)`,
  `button(id, label)`, `buttons { }` (a button bar) and `video(url)`, which fills a layout alone.
- Show it with `visor.display.layout.show(layout)`; a new show replaces it. `close()` empties it.
- Clicks come on `visor.display.layout.clicks` by element `id`. `visor.display.layout.state` says when the
  layout went, with `reason` `back` when the wearer went back. `visor.display.layout.video` follows a video.
- See it without glasses: `visor fake`, then open `http://127.0.0.1:7333/display`. In an app,
  `fake.display.preview(context)` from `kit-testing` draws the same.

Details: `read_vdk_doc` with `display`, and `testing` for the simulator.
