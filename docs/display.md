# Display

Scope: `display`. The Meta Ray-Ban Display. On glasses without a display every call fails with `unsupported`.

## Teleprompter

The Display has a teleprompter: one script at a time, which it scrolls or shows a page at a time.

```kotlin
visor.display.prompter.show("Slide 3: the numbers")
visor.display.prompter.show(script, PrompterStyle { paged = true; sizeSp = 40.0 })
visor.display.prompter.close()
```

A second `show` replaces the text on the Display. `show` returns once the Display opened the teleprompter, and
fails with `refused` when the Display turns it down, `timeout` when it does not answer, and `not_connected`.

`PrompterStyle`:

| Field | Default | Meaning |
|---|---|---|
| `sizeSp` | 35 | The text size on the Display. |
| `speed` | 32 | The scroll speed, in the Display's own unit. 32 is its default. |
| `paged` | false | A page at a time instead of scrolling. |
| `followVoice` | false | Scroll as the wearer reads aloud, with the Display's own speech recognition. |

The wearer can close the teleprompter on the Display; the VDK does not hear about it. Captions or translation
opening on the Display also take its place.

## Layouts

> [!NOTE]
> Visor does not show layouts yet: it will once it can open a session with the Display's toolkit app, the way Meta's
> Device Access Toolkit does. FakeVisor serves them now, so an app can be built and tested against them.

```kotlin
visor.display.layout.show(Layout {
    text("Garage") { style = LayoutStyle.HEADING }
    row {
        gap = 16
        button("open", "Open")
        button("close", "Close") { style = LayoutStyle.SECONDARY }
    }
})
visor.display.layout.clicks.collect { click -> if (click.id == "open") openGarage() }
```

A layout fills the 600 by 600 screen, and a new `show` replaces it. Its root is a column; boxes nest:

| Element | Builder | Fields |
|---|---|---|
| Column, row | `column { }`, `row { }` | `gap`, `padding` in pixels; `align` along the box and `crossAlign` across it: `start`, `center`, `end`, `space_between`; `card` to draw it on a card; `id` to make the whole box a click target |
| Text | `text("...") { }` | `style`: `heading` (40 px, bold), `body` (28 px, the default), `meta` (24 px, secondary colour) |
| Image | `image(url) { }` | An `https://` URL or a `data:` URI; `height` in pixels; `rounded` |
| Button | `button(id, label) { }` | `style`: `primary` (filled, the default) or `secondary` (outlined) |

`clicks` brings a `LayoutClick` with the `id` of each button or box the wearer clicks, to the app that showed the
layout only. `close()` empties the screen.

In a test, `fake.display.layout` holds the layout shown and `fake.display.click(id)` clicks an element.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `display.prompter.show` | `text`, `style` (a `PrompterStyle`, optional) | `{}` |
| Method | `display.prompter.close` | none | `{}` |
| Method | `display.layout.show` | `root`: a `LayoutNode` tree | `{}` |
| Method | `display.layout.close` | none | `{}` |
| Topic | `display.layout.clicks` | none | `LayoutClick`: `id`, `time` |

```
visor call display.prompter.show '{"text":"Hello","style":{"paged":true}}'
```

More of the Display (cards on its home, web apps that reach the VDK) is on the [roadmap](overview.md#roadmap).
