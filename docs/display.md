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

A layout fills the 600 by 600 screen, and a new `show` replaces it in full: there are no partial updates. Its root
is a column; boxes nest. The elements are DAT's, with the same sizes and styles:

| Element | Builder | Fields |
|---|---|---|
| Column, row | `column { }`, `row { }` | `gap`; `padding`, or `paddingTop`, `paddingBottom`, `paddingStart`, `paddingEnd`, in pixels; `align` along the box: `start`, `center`, `end`, `space_between`; `crossAlign` across it: `start`, `center`, `end`, `stretch`; `reverse` to lay the children out from the end; `wrap`; `card` to draw it on a card; `id` to make the whole box a click target |
| Text | `text("...") { }` | `style`: `heading` (40 px, bold), `body` (28 px, the default), `meta` (22 px); `color`: `primary` (white) or `secondary` (grey, the default for `meta`) |
| Image | `image(url) { }` | An `https://` URL or a `data:` URI; `size`: `fill` (the width it gets, the default) or `icon` (a 32 px square); `corners`: `none`, `small`, `medium`; `height` in pixels |
| Icon | `icon(LayoutIconName.HEART) { }` | One of the Display's own icons, by the names DAT has (`LayoutIconName.all`); `style`: `filled` (the default) or `outline` |
| Button | `button(id, label) { }` | `style`: `primary` (the default), `secondary`, `outline`; `icon`; `primary = true` for the button the Display focuses first |
| Button bar | `buttons { button(...) }` | The Display's own bar of buttons; `align`: `start`, `center`, `end` |
| Video | `video(url) { }` | MP4 over `https://`, at most 400 pixels a side and 70,000 pixels in all; it fills the layout alone, as on DAT; `controls` to show play and pause |

Every element also takes `grow` and `shrink`, its share of the free space along its box, and `alignSelf`, its own
place across the box.

```kotlin
visor.display.layout.show(Layout {
    padding = 24
    row {
        crossAlign = LayoutAlign.CENTER
        gap = 12
        icon(LayoutIconName.HOUSE)
        text("Garage") { style = LayoutStyle.HEADING; grow = 1.0 }
    }
    text("Closed since 18:02") { style = LayoutStyle.META }
    buttons {
        button("open", "Open") { icon = LayoutIconName.ARROW_UP_SHALLOW_U; primary = true }
        button("lights", "Lights") { style = LayoutStyle.OUTLINE }
    }
})
```

`clicks` brings a `LayoutClick` with the `id` of each button or box the wearer clicks, to the app that showed the
layout only. `state` says whether the layout shows, and why it went: the wearer went `back` (as on DAT, back ends
the layout), another app `replaced` it, the app `closed` it, or the glasses `failed` to draw it. `video` brings the
video's `starting`, `playing`, `paused`, `ended` and `failed`, with an `error` such as `invalid_url`. `close()` empties
the screen.

The Display dims a layout after 20 seconds and sleeps after 25, as it does any screen. The layout stays until the
wearer goes back.

In a test, `fake.display.layout` holds the layout shown, `fake.display.click(id)` clicks an element,
`fake.display.back()` goes back, and `fake.display.video(LayoutVideoState.ENDED, null)` moves the video along.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `display.prompter.show` | `text`, `style` (a `PrompterStyle`, optional) | `{}` |
| Method | `display.prompter.close` | none | `{}` |
| Method | `display.layout.show` | `root`: a `LayoutNode` tree | `{}` |
| Method | `display.layout.close` | none | `{}` |
| Topic | `display.layout.clicks` | none | `LayoutClick`: `id`, `time` |
| Topic | `display.layout.state` | none | `LayoutState`: `showing`, `reason`, `time` |
| Topic | `display.video` | none | `LayoutVideoEvent`: `state`, `error`, `time` |

```
visor call display.prompter.show '{"text":"Hello","style":{"paged":true}}'
```

More of the Display (cards on its home, web apps that reach the VDK) is on the [roadmap](overview.md#roadmap).
