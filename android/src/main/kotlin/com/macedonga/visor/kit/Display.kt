package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutClick
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
