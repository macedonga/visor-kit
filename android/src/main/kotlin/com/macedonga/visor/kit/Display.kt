package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.BriefingCard
import com.macedonga.visor.kit.protocol.CardRemoval
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutBox
import com.macedonga.visor.kit.protocol.LayoutClick
import com.macedonga.visor.kit.protocol.LayoutIconName
import com.macedonga.visor.kit.protocol.LayoutState
import com.macedonga.visor.kit.protocol.LayoutVideoEvent
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.PrompterRequest
import com.macedonga.visor.kit.protocol.PrompterStyle
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.coroutines.flow.Flow

/** What the Meta Ray-Ban Display shows. Needs [Scope.DISPLAY]; on glasses without a display, every call fails. */
public class Display internal constructor(visor: Visor) {
    public val prompter: Prompter = Prompter(visor)
    public val layout: DisplayLayout = DisplayLayout(visor)
    public val cards: DisplayCards = DisplayCards(visor)
}

/**
 * The app's cards on the Display home's Briefing, beside Meta's weather and sports. A card stays, whether or not the
 * app runs, until the app removes it; the Display shows a change within about a minute. Cards show, and take no
 * clicks.
 *
 * ```
 * visor.display.cards.set("coffee", "Coffee") { text("Coffee") { style = LayoutStyle.HEADING }; text("Ready in 2 minutes") }
 * ```
 */
public class DisplayCards internal constructor(private val visor: Visor) {
    public suspend fun set(card: BriefingCard): Unit = visor.call(Methods.DISPLAY_CARDS_SET, card)

    public suspend fun set(id: String, name: String, block: LayoutBox.() -> Unit): Unit = set(BriefingCard(id, name, block = block))

    public suspend fun set(id: String, name: String, icon: LayoutIconName, block: LayoutBox.() -> Unit): Unit =
        set(BriefingCard(id, name, icon, block))

    public suspend fun remove(id: String): Unit = visor.call(Methods.DISPLAY_CARDS_REMOVE, CardRemoval(id))
}

/**
 * The app's own screen on the Display: a layout of text, images, icons, buttons and a video, which a new [show] replaces in place.
 *
 * ```
 * visor.display.layout.show(Layout { text("Garage") { style = LayoutStyle.HEADING }; button("open", "Open") })
 * visor.display.layout.clicks.collect { click -> if (click.id == "open") openGarage() }
 * ```
 */
public class DisplayLayout internal constructor(private val visor: Visor) {
    public suspend fun show(layout: Layout): Unit = visor.call(Methods.DISPLAY_LAYOUT_SHOW, layout)

    /** Empties the screen. The wearer leaves it as any other app. */
    public suspend fun close(): Unit = visor.call(Methods.DISPLAY_LAYOUT_CLOSE, Unit)

    /** Each click on a button or a box with an id, while collected. */
    public val clicks: Flow<LayoutClick>
        get() = visor.subscribe(Topics.DISPLAY_LAYOUT_CLICKS, Unit)

    /** Whether the layout shows, now and on each change: it goes when the wearer goes back, as on DAT. */
    public val state: Flow<LayoutState>
        get() = visor.subscribe(Topics.DISPLAY_LAYOUT_STATE, Unit)

    /** The layout's video: starting, playing, paused, ended or failed. */
    public val video: Flow<LayoutVideoEvent>
        get() = visor.subscribe(Topics.DISPLAY_VIDEO, Unit)
}

/** The Display's teleprompter: one script at a time, which a new [show] replaces. */
public class Prompter internal constructor(private val visor: Visor) {
    public suspend fun show(text: String): Unit = visor.call(Methods.DISPLAY_PROMPTER_SHOW, PrompterRequest(text))

    public suspend fun show(text: String, style: PrompterStyle): Unit =
        visor.call(Methods.DISPLAY_PROMPTER_SHOW, PrompterRequest(text, style))

    public suspend fun close(): Unit = visor.call(Methods.DISPLAY_PROMPTER_CLOSE, Unit)
}
