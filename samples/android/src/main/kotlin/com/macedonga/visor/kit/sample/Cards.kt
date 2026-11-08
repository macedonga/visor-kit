package com.macedonga.visor.kit.sample

import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.protocol.BriefingCard
import com.macedonga.visor.kit.protocol.LayoutAlign
import com.macedonga.visor.kit.protocol.LayoutColor
import com.macedonga.visor.kit.protocol.LayoutIconName
import com.macedonga.visor.kit.protocol.LayoutStyle
import com.macedonga.visor.kit.protocol.change
import com.macedonga.visor.kit.protocol.field
import com.macedonga.visor.kit.protocol.header
import com.macedonga.visor.kit.protocol.progress
import com.macedonga.visor.kit.protocol.stat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The recipes of docs/briefing-cards.md, as the sample's buttons run them.

/** A card like Meta's Weather, with three pages behind it: now, the next hours, the week. */
internal fun weatherCard(): BriefingCard = BriefingCard("weather", "Lake weather", LayoutIconName.CLOUD) {
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
                for ((hour, sky, temperature) in HOURS) {
                    column {
                        gap = 8
                        text(hour) { style = LayoutStyle.META; align = LayoutAlign.CENTER }
                        icon(sky) { size = 48; alignSelf = LayoutAlign.CENTER }
                        text("$temperature°") { align = LayoutAlign.CENTER }
                    }
                }
            }
            chart(HOURS.map { it.third.toDouble() }) { height = 90 }
        }
        page {
            gap = 14
            header("This week")
            for ((day, low, high) in WEEK) {
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

/** A watchlist: one line a stock on the card, a page a stock behind it. */
internal fun stocksCard(): BriefingCard = BriefingCard("stocks", "My stocks", LayoutIconName.CHART_LINE) {
    header(LayoutIconName.CHART_LINE, "My stocks")
    for (stock in STOCKS) {
        row {
            align = LayoutAlign.SPACE_BETWEEN
            crossAlign = LayoutAlign.CENTER
            text(stock.name) { maxLines = 1; shrink = 1.0 }
            change("%+.1f%%".format(stock.percent), rising = stock.percent >= 0)
        }
    }
    pages {
        for (stock in STOCKS) {
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

/**
 * A kitchen timer that counts down on the Briefing, with buttons in its detail. Runs until the time is up or Stop is
 * clicked, then takes the card off.
 */
internal suspend fun kitchenTimer(visor: Visor, total: Duration): Unit = coroutineScope {
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

private val HOURS = listOf(
    Triple("Now", LayoutIconName.WEATHER_PARTLY_CLOUDY, 18),
    Triple("15", LayoutIconName.WEATHER_SUNNY, 19),
    Triple("16", LayoutIconName.WEATHER_SUNNY, 18),
    Triple("17", LayoutIconName.WEATHER_CLOUDY, 16),
    Triple("18", LayoutIconName.WEATHER_NIGHT_CLEAR, 14),
)

private val WEEK = listOf(Triple("Mon", 9, 21), Triple("Tue", 11, 23), Triple("Wed", 8, 17), Triple("Thu", 6, 14))

private class Stock(val name: String, val price: String, val percent: Double, val high: String, val week: List<Double>)

private val STOCKS = listOf(
    Stock("NAS100/USD", "27,012.40", 1.8, "31,400.80", listOf(26.1, 26.4, 26.2, 26.7, 27.0)),
    Stock("Meta", "610.15", -0.4, "779.82", listOf(615.0, 612.4, 618.9, 612.6, 610.2)),
)
