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
> Visor shows layouts through Meta's toolkit app on the Display, as Meta's Device Access Toolkit does. The first
> time an app shows one, Visor asks the person once to set the glasses up for apps, and installs the toolkit app.
> A video plays alone on the screen: the glasses fetch its URL through the phone's internet, so a short clip can
> take a few seconds to start.

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
| Column, row | `column { }`, `row { }` | `gap`; `padding`, or `paddingTop`, `paddingBottom`, `paddingStart`, `paddingEnd`, in pixels; `align` along the box: `start`, `center`, `end`, `space_between`; `crossAlign` across it: `start`, `center`, `end`, `stretch`; `reverse` to lay the children out from the end; `wrap`; `card` to draw it on a card; `background`, a colour behind its children; `height` in pixels; `id` to make the whole box a click target |
| Text | `text("...") { }` | `style`: `heading` (40 px, bold), `title` (24 px, bold, the first line of Meta's Briefing cards), `body` (28 px, the default), `meta` (22 px); `color` (below; `secondary` is the default for `meta`); `maxLines`, cut with an ellipsis; `align`: `start`, `center`, `end` |
| Image | `image(url) { }`, `image(bitmap) { }` | An `https://` URL, which the glasses fetch through the phone; a `data:` URI; or on Android a `Bitmap`, sent inside the layout. Visor scales a `data:` image larger than the Display's 600 px a side. `size`: `fill` (the width it gets, the default) or `icon` (a 32 px square); `corners`: `none`, `small`, `medium`; `height` in pixels |
| Icon | `icon(LayoutIconName.HEART) { }` | One of the Display's own icons (below); `style`: `filled` (the default) or `outline`; `color`, white unless set; `size` in pixels, 32 unless set |
| Button | `button(id, label) { }` | `style`: `primary` (the default), `secondary`, `outline`; `icon`; `primary = true` for the button the Display focuses first |
| Button bar | `buttons { button(...) }` | The Display's own bar of buttons; `align`: `start`, `center`, `end` |
| Pages | `pager { page { }; page { } }` | Pages side by side that the wearer swipes through, with dots under them, as Meta's Weather and Stocks details: each page a card of the Display's own material, inset 24 px, and padded 24 px unless it sets its padding |
| Chart | `chart(values) { }` | A line through the values (the default, from the lowest to the highest, with a wash under it) or `style = LayoutStyle.BARS` (bars from zero); `color`, blue unless set; `height` in pixels, 120 unless set. Visor draws it on the phone at the width it shows |
| Video | `video(url) { }` | MP4 over `https://`, at most 400 pixels a side and 70,000 pixels in all; it fills the layout alone, as on DAT; `controls` to show play and pause |

Every element also takes `grow` and `shrink`, its share of the free space along its box, and `alignSelf`, its own
place across the box. `pinTop`, `pinBottom`, `pinStart` and `pinEnd` pin an element that many pixels in from its
box's edges, inside the box's padding, over the box's other children: the big weather icon in the corner of Meta's
Weather card is one.

A colour is `LayoutColor.PRIMARY` (white), `SECONDARY` (light grey), `GREEN` (the green of a rise on Meta's
Stocks card), `RED`, `BLUE`, `TRACK` (the grey under a progress bar), or any colour as `LayoutColor.rgb(0xFFC83D)`,
which is `"#FFC83D"` on the wire. A Visor that does not know a colour uses the default.

Icons are the Display's own, by name. `LayoutIconName.dat` holds the names DAT has, `LayoutIconName.briefing` the
glyphs of Meta's Briefing cards (`SMALL_TRIANGLE_UP`, `SMALL_TRIANGLE_DOWN`, `CHART_LINE`, `TROPHY` and more), and
`LayoutIconName.weather` the 14 weather illustrations of Meta's Weather card (`WEATHER_SUNNY`, `WEATHER_RAINY`,
`WEATHER_NIGHT_CLEAR` and the rest), which are in colour and keep it unless a `color` is set; draw them at 48 px or
more. Any other name from Meta's wearables icon set works too: a Display that lacks it fetches it from Meta.

On the Display the three button styles look alike: Meta's renderer keeps only an icon's outline from them. A
button bar shows its labels while they fit, and only its icons when they do not.

To see a layout without the glasses, open the Display simulator that `visor fake` serves: see
[Testing](testing.md#the-display-simulator).

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

## Cards on the Briefing

The Display's home has a Briefing panel with Meta's glanceable cards: reminders, weather, sports and stocks. An app
can keep cards of its own there, after Meta's, and give each one a detail that a click opens, full screen, as
Meta's cards open theirs. A card stays, whether or not the app runs, until the app removes it, so the wearer sees
it whenever they look at the Briefing. Meta's toolkit has nothing like it.

```kotlin
visor.display.cards.set("pasta", "Kitchen timer", LayoutIconName.CLOCK) {
    header(LayoutIconName.CLOCK, "Kitchen timer")
    stat("4:00", "Pasta")
    progress(0.6)
    detail {
        header("Pasta")
        field("Started", "19:42")
        field("Ends", "19:52")
        chart(listOf(100.0, 96.0, 90.0, 82.0)) { style = LayoutStyle.BARS }
    }
}
visor.display.cards.remove("pasta")
```

A card is a layout, drawn in the Briefing's own card style and padded 24 px as Meta's cards are unless it sets its
padding. Setting a card with the same id replaces it; the Display shows the change the next time it asks, within
about a minute. The name and the icon show in the Display's list of Briefing cards, where the wearer can turn a card
off. On the card itself buttons and videos do nothing: a click on the card opens its detail.

A card and its detail take at most 256 KiB as JSON (`BriefingCard.MAX_BYTES`), images inside them included; past
that `set` fails with `invalid_params`, saying the size. A few photos from `image(bitmap)` fit; for more, give
`https://` URLs, which cost only their length. A chart costs nothing here: Visor draws it.

`detail { }` is the screen a click opens, padded as a card. `pages { page { }; page { } }` makes the detail a set of
pages the wearer swipes through, as Meta's Weather (now, the next hours, the week) and Stocks (one page a stock).

### Building blocks

The pieces of Meta's own cards, sized and coloured as the Display draws them:

| Block | What it draws |
|---|---|
| `header(icon, "Title")`, `header("Title")` | The first line: a 24 px icon beside a bold 24 px title |
| `stat("18°")`, `stat("18°", "Low 9°")` | A figure in large type, and a grey caption under it |
| `change("+1.8%", rising = true)` | A green triangle up and green text, or a red triangle down and red text |
| `field("Wind", "12 km/h")` | A grey label and its value on one line, the value at the end |
| `progress(0.6)`, `progress(0.6, color)` | A bar filled that far, from 0 to 1 |
| `chart(values)` | A line or bars (see the layout elements above) |

They are rows and texts of the layout above, so they mix with anything else. A card that looks like Meta's Weather:

```kotlin
visor.display.cards.set("weather", "Lake weather", LayoutIconName.CLOUD) {
    header(LayoutIconName.CLOUD, "Lake weather")
    stat("18°", "Low 9°")
    text("Partly cloudy") { style = LayoutStyle.META; maxLines = 1 }
    icon(LayoutIconName.WEATHER_PARTLY_CLOUDY) { size = 104; pinTop = 0; pinEnd = 0 }
    pages {
        page {
            header("Next hours")
            chart(listOf(18.0, 19.0, 18.0, 16.0, 14.0, 13.0))
        }
        page {
            header("This week")
            field("Mon", "9° to 21°")
            field("Tue", "11° to 23°")
        }
    }
}
```

### Live cards

A card can follow the app's state: `keep` sets the card again for each new state of a `Flow`, as long as it runs.

```kotlin
scope.launch {
    visor.display.cards.keep("pasta", "Kitchen timer", LayoutIconName.CLOCK, timer.remaining) { left ->
        header(LayoutIconName.CLOCK, "Kitchen timer")
        stat("%d:%02d".format(left.inWholeMinutes, left.inWholeSeconds % 60), "Pasta")
        progress(1 - left / total)
    }
}
```

States closer than a second apart are merged into the newest, and a state that draws the same card sends nothing.
While an app has cards on it, the Display asks for the Briefing every 15 seconds as the wearer looks at it, so a
live card keeps up within seconds. When `keep` ends, the card stays as its last state drew it, until `remove`.

### From Python, TypeScript and JSON-RPC

The Python and TypeScript clients have the same pieces, building the JSON the Kotlin builder makes:

```python
from visor_kit.layout import card, header, stat, change, page, chart

await visor.call("display.cards.set", card(
    "stocks", "My stocks",
    header("My stocks", icon="chart_line"),
    stat("27,012.40"),
    change("+481.50 (+1.8%)", rising=True),
    icon="chart_line",
    pages=[page(chart([26.1, 26.4, 26.9, 27.0]))],
))
```

```ts
import { card, header, stat, change, page, chart } from "visor-kit/layout";

await visor.call("display.cards.set", card("stocks", "My stocks", [
  header("My stocks", "chart_line"),
  stat("27,012.40"),
  change("+481.50 (+1.8%)", true),
], { icon: "chart_line", pages: [page([chart([26.1, 26.4, 26.9, 27.0])])] }));
```

On the wire a card is `{"id", "name", "icon", "layout": {"root": ...}, "detail": {"root": ...}}`, each element
`{"type": "text", "text": "...", "style": "title", ...}` with the field names of the tables above.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Method | `display.prompter.show` | `text`, `style` (a `PrompterStyle`, optional) | `{}` |
| Method | `display.prompter.close` | none | `{}` |
| Method | `display.layout.show` | `root`: a `LayoutNode` tree | `{}` |
| Method | `display.layout.close` | none | `{}` |
| Method | `display.cards.set` | `id`, `name`, `icon` (default `bell`), `layout`, `detail` (a layout, optional) | `{}` |
| Method | `display.cards.remove` | `id` | `{}` |
| Topic | `display.layout.clicks` | none | `LayoutClick`: `id`, `time` |
| Topic | `display.layout.state` | none | `LayoutState`: `showing`, `reason`, `time` |
| Topic | `display.video` | none | `LayoutVideoEvent`: `state`, `error`, `time` |

```
visor call display.prompter.show '{"text":"Hello","style":{"paged":true}}'
```

More of the Display (cards on its home, web apps that reach the VDK) is on the [roadmap](overview.md#roadmap).
