# Briefing cards

Scope: `display`. The Meta Ray-Ban Display.

The Display's home has a Briefing panel: Meta's glanceable cards for the weather, sports, stocks and reminders. An
app can keep cards of its own there, after Meta's, in the same look. Each card can open a detail, full screen, with
pages the wearer swipes through and buttons that reach the app. A card stays until the app removes it, whether or
not the app runs, so a delivery, a timer or a score is a glance away all day. Meta's toolkit has nothing like it.

This page is the guide, with recipes. The reference (every element, field and colour) is in
[Display](display.md#cards-on-the-briefing).

## Design for a glance

The wearer looks at a card for a second, through a screen that adds light to the world:

- Start with `header(icon, "Title")`, as Meta's cards do: it tells the wearer what the card is before they read it.
- One figure in large type (`stat`), and at most two lines under it. Cut long text with `maxLines = 1`.
- Put the rest in the detail: `detail { }` for one screen, `pages { }` for several, a page a topic.
- Black is clear on the Display, and the card's own background is the panel's. Use colour for meaning only:
  `change(..., rising = ...)` is green or red, `LayoutColor.SECONDARY` for what matters less.
- A weather illustration (`LayoutIconName.WEATHER_*`) needs 48 px or more; the glyphs are sharp at 20 to 32 px. A
  big illustration pinned in the top corner (`pinTop = 0; pinEnd = 0`), as on Meta's Weather card, leaves the left
  for text.
- Give the card a short `name` and an icon: the wearer sees them in the Display's list of Briefing cards, where they
  can turn a card off.

## Recipes

The recipes are in the Android sample (`samples/android`, `Cards.kt`), behind its Briefing buttons.

### Weather, as Meta's

A card with today's figure and an illustration, and three pages behind it: now, the next hours, the week.

```kotlin
val card = BriefingCard("weather", "Lake weather", LayoutIconName.CLOUD) {
    header(LayoutIconName.CLOUD, "Lake weather")
    stat("18°", "Low 9°")
    text("Partly cloudy") { style = LayoutStyle.META; maxLines = 1 }
    icon(LayoutIconName.WEATHER_PARTLY_CLOUDY) { size = 96; pinTop = 0; pinEnd = 0 }
    pages {
        page {
            gap = 8
            text("Brugnera") { style = LayoutStyle.META }
            stat("18°", "High 21°, low 9°")
            text("Partly cloudy, clearing tonight")
        }
        page {
            gap = 16
            header("Next hours")
            row {
                align = LayoutAlign.SPACE_BETWEEN
                for ((hour, sky, temperature) in hours) {
                    column {
                        gap = 8
                        text(hour) { style = LayoutStyle.META; align = LayoutAlign.CENTER }
                        icon(sky) { size = 48; alignSelf = LayoutAlign.CENTER }
                        text("$temperature°") { align = LayoutAlign.CENTER }
                    }
                }
            }
            chart(hours.map { it.third.toDouble() }) { height = 90 }
        }
        page {
            gap = 14
            header("This week")
            for ((day, low, high) in week) {
                row {
                    gap = 12
                    crossAlign = LayoutAlign.CENTER
                    text(day) { style = LayoutStyle.META }
                    text("$low°") { style = LayoutStyle.META }
                    // The day's range on the week's, 5 to 25 degrees.
                    row {
                        grow = 1.0
                        height = 8
                        crossAlign = LayoutAlign.STRETCH
                        background = LayoutColor.TRACK
                        row { grow = (low - 5) / 20.0 }
                        row { grow = (high - low) / 20.0; background = LayoutColor.BLUE }
                        row { grow = (25 - high) / 20.0 }
                    }
                    text("$high°")
                }
            }
        }
    }
}
visor.display.cards.set(card)
```

The week's bars are rows with a `background`: any proportion is three rows that share a track by `grow`.

### A stocks watchlist

A line a stock on the card, and a page a stock behind it, with the week as a line in green or red.

```kotlin
BriefingCard("stocks", "My stocks", LayoutIconName.CHART_LINE) {
    header(LayoutIconName.CHART_LINE, "My stocks")
    for (stock in stocks) {
        row {
            align = LayoutAlign.SPACE_BETWEEN
            crossAlign = LayoutAlign.CENTER
            text(stock.name) { maxLines = 1; shrink = 1.0 }
            change("%+.1f%%".format(stock.percent), rising = stock.percent >= 0)
        }
    }
    pages {
        for (stock in stocks) {
            page {
                gap = 12
                header(stock.name)
                stat(stock.price)
                change("%+.1f%%".format(stock.percent), rising = stock.percent >= 0)
                chart(stock.week) { height = 80; color = if (stock.percent >= 0) LayoutColor.GREEN else LayoutColor.RED }
                field("52 week high", stock.high)
            }
        }
    }
}
```

### A live kitchen timer, with buttons

`keep` follows a `Flow`: here the time left, ticking each second. The detail has buttons, whose clicks come back on
`clicks`, and a `toast` that tells the wearer at once.

```kotlin
suspend fun kitchenTimer(visor: Visor, total: Duration): Unit = coroutineScope {
    val left = MutableStateFlow(total)
    val ticking = launch {
        while (left.value > Duration.ZERO) {
            delay(1.seconds)
            left.update { (it - 1.seconds).coerceAtLeast(Duration.ZERO) }
        }
    }
    val clicks = launch {
        visor.display.cards.clicks.collect { click ->
            if (click.card != "pasta") return@collect
            when (click.id) {
                "add" -> left.update { it + 1.minutes }
                "stop" -> ticking.cancel()
            }
        }
    }
    val card = launch {
        visor.display.cards.keep("pasta", "Kitchen timer", LayoutIconName.CLOCK, left) { remaining ->
            header(LayoutIconName.CLOCK, "Kitchen timer")
            stat("%d:%02d".format(remaining.inWholeMinutes, remaining.inWholeSeconds % 60), "Pasta")
            progress(1 - remaining / total)
            detail {
                header("Pasta")
                field("Left", "%d:%02d".format(remaining.inWholeMinutes, remaining.inWholeSeconds % 60))
                buttons {
                    button("add", "+1 minute") { icon = LayoutIconName.PLUS; toast = "One more minute" }
                    button("stop", "Stop") { toast = "Stopped" }
                }
            }
        }
    }
    ticking.join()
    clicks.cancel()
    card.cancel()
    visor.display.cards.remove("pasta")
}
```

The Display asks for the Briefing every 15 seconds while it shows it, so the countdown moves in steps there. An open
detail keeps what it showed until the wearer opens it again.

### A photo from the app

`image(bitmap)` on Android sends a photo inside the card, scaled to the Display:

```kotlin
BriefingCard("parcel", "Parcel", LayoutIconName.CART) {
    header(LayoutIconName.CART, "Parcel")
    text("At your door by 14:00")
    detail {
        image(doorstepPhoto) { height = 240; corners = LayoutCorners.MEDIUM }
        field("Courier", "GLS")
        progress(0.8)
    }
}
```

A card and its detail take at most 256 KiB (`BriefingCard.MAX_BYTES`). A few photos fit; for more, give `https://`
URLs, which the glasses fetch through the phone.

## From Python, TypeScript and JSON-RPC

The Python and TypeScript clients have the same pieces. The stocks card, from Python:

```python
from visor_kit.layout import card, header, row, text, change, page, stat, chart

stocks = [("NAS100/USD", "27,012.40", 1.8, [26.1, 26.4, 26.7, 27.0])]
await visor.call("display.cards.set", card(
    "stocks", "My stocks",
    header("My stocks", icon="chart_line"),
    *[row(text(name, max_lines=1, shrink=1.0), change("%+.1f%%" % pct, rising=pct >= 0),
          align="space_between", cross_align="center") for name, _, pct, _ in stocks],
    icon="chart_line",
    pages=[page(header(name), stat(price), chart(week, height=80)) for name, price, _, week in stocks],
))
async for click in visor.subscribe("display.cards.clicks"):
    print(click["card"], click["id"])
```

From TypeScript:

```ts
import { card, header, stat, change, page, chart } from "visor-kit/layout";

await visor.call("display.cards.set", card("stocks", "My stocks", [
  header("My stocks", "chart_line"),
  stat("27,012.40"),
  change("+1.8%", true),
], { icon: "chart_line", pages: [page([chart([26.1, 26.4, 26.7, 27.0])])] }));
```

On the wire, from any language or the `visor` command (`visor card card.json`):

```json
{
  "id": "stocks",
  "name": "My stocks",
  "icon": "chart_line",
  "layout": {"root": {"type": "column", "children": [
    {"type": "text", "text": "My stocks", "style": "title", "maxLines": 1},
    {"type": "text", "text": "27,012.40", "style": "heading"}
  ]}},
  "detail": {"root": {"type": "column", "children": [
    {"type": "pager", "children": [
      {"type": "column", "children": [{"type": "chart", "values": [26.1, 26.4, 26.7, 27.0], "height": 80}]}
    ]}
  ]}}
}
```

## Testing a card

- `visor fake`, then the Display simulator's Briefing screen: the cards in the panel's look, their details, pages and
  buttons. `visor --fake card card.json` puts a card there from a file.
- In a JVM test, `fake.display.cards` holds the app's cards and `fake.display.clickCard("pasta", "stop")` clicks.
- In the app, `fake.display.briefingPreview(context)` from `kit-testing` is a view of the Briefing.
- On the glasses: open the Briefing on the Display's home. A new card shows within 15 seconds.

## How it behaves

- `set` with the same id replaces the card. The card stays, across Visor restarts, until `remove`.
- Visor serves the cards after Meta's own, and the wearer can turn any of them off in the Display's list.
- On the card itself buttons and videos do nothing: a click on the card opens its detail.
- Clicks in a detail reach only the app that set the card. An element's `toast` shows once Visor has the click.
- A card and its detail take at most 256 KiB; `set` fails with `invalid_params`, saying the size, past that.
