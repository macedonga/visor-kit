package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** A screen for the Display, from [Methods.DISPLAY_LAYOUT_SHOW]. Build one with [Layout]. */
@Poko
@Serializable
public class Layout @KitInternal constructor(
    /** A column, which fills the 600 by 600 screen. */
    public val root: LayoutNode,
)

/**
 * One element of a [Layout]. [type] says which, and the fields that do not apply to it stay null. A Visor that does
 * not know a type leaves the element out.
 */
@Poko
@Serializable
public class LayoutNode @KitInternal constructor(
    public val type: LayoutType,
    /** A click target's id: a button's, or a box's whose whole area can be clicked. */
    public val id: String? = null,
    /** A text's words, or a button's label. */
    public val text: String? = null,
    /** How a text, a button or an icon looks. */
    public val style: LayoutStyle? = null,
    /** An image or a video: an `https://` URL; an image may also be a `data:` URI. */
    public val url: String? = null,
    /**
     * An image's height in pixels, null to fit its width; an icon's size, 32 when null; a box's or a chart's height,
     * null for what its content needs.
     */
    public val height: Int? = null,
    /** A box's space between its children, and around them, in pixels. */
    public val gap: Int? = null,
    public val padding: Int? = null,
    /**
     * Where a box puts its children along its direction, and across it; a button group, where its buttons go; a text,
     * where its lines sit in its width.
     */
    public val align: LayoutAlign? = null,
    public val crossAlign: LayoutAlign? = null,
    /** True to draw a box on a card, or an image with rounded corners. */
    public val card: Boolean? = null,
    public val children: List<LayoutNode> = emptyList(),
    /** True for a box that lays its children out from the end: bottom to top, or right to left. */
    public val reverse: Boolean? = null,
    /** True for a box that starts a new line when its children do not fit. */
    public val wrap: Boolean? = null,
    /** A box's padding on one edge, in pixels, instead of [padding]. */
    public val paddingTop: Int? = null,
    public val paddingBottom: Int? = null,
    public val paddingStart: Int? = null,
    public val paddingEnd: Int? = null,
    /** How much of the free space along its box this element takes, and gives up when space is short. */
    public val grow: Double? = null,
    public val shrink: Double? = null,
    /** Where this element sits across its box, instead of the box's [crossAlign]. */
    public val alignSelf: LayoutAlign? = null,
    /** A text's or an icon's colour. */
    public val color: LayoutColor? = null,
    /** An image's size: a small square, or the width it is given. */
    public val size: LayoutImageSize? = null,
    /** An image's corners. */
    public val corners: LayoutCorners? = null,
    /** An icon's name, or the icon on a button. */
    public val icon: LayoutIconName? = null,
    /** True for the button the Display focuses first. */
    public val primary: Boolean? = null,
    /** True to show a video's play and pause controls. */
    public val controls: Boolean? = null,
    /** The most lines a text takes, cut with an ellipsis; null for as many as it needs. */
    public val maxLines: Int? = null,
    /** A box's fill behind its children. */
    public val background: LayoutColor? = null,
    /** A chart's values, left to right. */
    public val values: List<Double>? = null,
    /**
     * In a Briefing card's detail, what the Display shows for a moment once Visor has the click on this button or
     * box, such as "Snoozed". Layouts shown with [Methods.DISPLAY_LAYOUT_SHOW] ignore it.
     */
    public val toast: String? = null,
    /**
     * Pins the element to its box's edges, this many pixels in, over the box's other children instead of in line with
     * them. Null on all four for an element in line.
     */
    public val pinTop: Int? = null,
    public val pinBottom: Int? = null,
    public val pinStart: Int? = null,
    public val pinEnd: Int? = null,
)

/** What a [LayoutNode] is. */
@Serializable
@JvmInline
public value class LayoutType(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** A box that lays its children out top to bottom. */
        public val COLUMN: LayoutType = LayoutType("column")

        /** A box that lays its children out side by side. */
        public val ROW: LayoutType = LayoutType("row")
        public val TEXT: LayoutType = LayoutType("text")
        public val IMAGE: LayoutType = LayoutType("image")
        public val BUTTON: LayoutType = LayoutType("button")

        /** One of the Display's own icons, named by [LayoutNode.icon]. */
        public val ICON: LayoutType = LayoutType("icon")

        /** A row of buttons, the Display's own button bar. Its children are buttons. */
        public val BUTTON_GROUP: LayoutType = LayoutType("button_group")

        /** Pages side by side, which the wearer swipes through, with dots under them. Its children are columns, one a page. */
        public val PAGER: LayoutType = LayoutType("pager")

        /** [LayoutNode.values] drawn as a line or as bars, across the width it is given. Visor draws it on the phone. */
        public val CHART: LayoutType = LayoutType("chart")

        /**
         * A video from [LayoutNode.url]: MP4 over `https://`, at most 400 pixels a side. It fills the screen alone: a
         * layout with a video holds nothing else, as on DAT.
         */
        public val VIDEO: LayoutType = LayoutType("video")
    }
}

/** How a text, a button or an icon looks. */
@Serializable
@JvmInline
public value class LayoutStyle(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** Text: 40 px, bold. */
        public val HEADING: LayoutStyle = LayoutStyle("heading")

        /** Text: 24 px, bold: the first line of Meta's Briefing cards. */
        public val TITLE: LayoutStyle = LayoutStyle("title")

        /** Text: 28 px, the default. */
        public val BODY: LayoutStyle = LayoutStyle("body")

        /** Text: 22 px, in the secondary colour unless [LayoutNode.color] says otherwise. */
        public val META: LayoutStyle = LayoutStyle("meta")

        /** A button: filled, the default. */
        public val PRIMARY: LayoutStyle = LayoutStyle("primary")

        /** A button: the quieter fill. */
        public val SECONDARY: LayoutStyle = LayoutStyle("secondary")

        /** A button or an icon: outlined. */
        public val OUTLINE: LayoutStyle = LayoutStyle("outline")

        /** An icon: filled, the default. */
        public val FILLED: LayoutStyle = LayoutStyle("filled")

        /** A chart: a line through the values, the default. */
        public val LINE: LayoutStyle = LayoutStyle("line")

        /** A chart: a bar for each value. */
        public val BARS: LayoutStyle = LayoutStyle("bars")
    }
}

/** Where a box puts its children. */
@Serializable
@JvmInline
public value class LayoutAlign(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val START: LayoutAlign = LayoutAlign("start")
        public val CENTER: LayoutAlign = LayoutAlign("center")
        public val END: LayoutAlign = LayoutAlign("end")

        /** Along the direction only: the space shared out between the children. */
        public val SPACE_BETWEEN: LayoutAlign = LayoutAlign("space_between")

        /** Across the direction only: each child as wide (in a column) or as tall (in a row) as the box. */
        public val STRETCH: LayoutAlign = LayoutAlign("stretch")
    }
}

/** A colour on the Display: one of its own, or any as `#RRGGBB`. */
@Serializable
@JvmInline
public value class LayoutColor(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** White. */
        public val PRIMARY: LayoutColor = LayoutColor("primary")

        /** Light grey. */
        public val SECONDARY: LayoutColor = LayoutColor("secondary")

        /** The green of a rise in Meta's Stocks card. */
        public val GREEN: LayoutColor = LayoutColor("#009914")

        /** A red for a fall, beside [GREEN]. */
        public val RED: LayoutColor = LayoutColor("#E5484D")

        /** A blue for charts and bars. */
        public val BLUE: LayoutColor = LayoutColor("#4DA3FF")

        /** The dark grey under a [progress] bar. */
        public val TRACK: LayoutColor = LayoutColor("#2A2D33")

        /** The colour [rgb] says, as `0xRRGGBB`. */
        public fun rgb(rgb: Int): LayoutColor = LayoutColor("#" + (rgb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase())
    }
}

/** An image's size. */
@Serializable
@JvmInline
public value class LayoutImageSize(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** A 32 px square, filled edge to edge. */
        public val ICON: LayoutImageSize = LayoutImageSize("icon")

        /** The width it is given, the whole image shown. The default. */
        public val FILL: LayoutImageSize = LayoutImageSize("fill")
    }
}

/** An image's corners. */
@Serializable
@JvmInline
public value class LayoutCorners(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val NONE: LayoutCorners = LayoutCorners("none")
        public val SMALL: LayoutCorners = LayoutCorners("small")
        public val MEDIUM: LayoutCorners = LayoutCorners("medium")
    }
}

/** A click on a [LayoutNode] with an id, from [Topics.DISPLAY_LAYOUT_CLICKS]. */
@Poko
@Serializable
public class LayoutClick @KitInternal constructor(
    public val id: String,
    /** When Visor heard it, in milliseconds since 1970. */
    public val time: Long,
)

/** Whether the app's layout shows, from [Topics.DISPLAY_LAYOUT_STATE]. */
@Poko
@Serializable
public class LayoutState @KitInternal constructor(
    public val showing: Boolean,
    /** Why it went, while [showing] is false; null before the app showed anything. */
    public val reason: LayoutEnd? = null,
    /** When, in milliseconds since 1970. */
    public val time: Long,
)

/** Why a layout went away. */
@Serializable
@JvmInline
public value class LayoutEnd(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** The wearer went back. */
        public val BACK: LayoutEnd = LayoutEnd("back")

        /** The app closed it. */
        public val CLOSED: LayoutEnd = LayoutEnd("closed")

        /** Another app showed its own layout. */
        public val REPLACED: LayoutEnd = LayoutEnd("replaced")

        /** The glasses could not draw it, or lost the link. */
        public val FAILED: LayoutEnd = LayoutEnd("failed")
    }
}

/** The video in the app's layout, from [Topics.DISPLAY_VIDEO]. */
@Poko
@Serializable
public class LayoutVideoEvent @KitInternal constructor(
    public val state: LayoutVideoState,
    /** Why it failed: `invalid_url`, `invalid_dimensions`, `stream_rejected` or `playback_failed`. */
    public val error: String? = null,
    /** When, in milliseconds since 1970. */
    public val time: Long,
)

/** Where a layout's video is. */
@Serializable
@JvmInline
public value class LayoutVideoState(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val STARTING: LayoutVideoState = LayoutVideoState("starting")
        public val PLAYING: LayoutVideoState = LayoutVideoState("playing")
        public val PAUSED: LayoutVideoState = LayoutVideoState("paused")
        public val ENDED: LayoutVideoState = LayoutVideoState("ended")
        public val FAILED: LayoutVideoState = LayoutVideoState("failed")
    }
}

/**
 * A [Layout] whose root column [block] fills, for example:
 *
 * ```
 * Layout {
 *     text("Garage") { style = LayoutStyle.HEADING }
 *     row { button("open", "Open"); button("close", "Close") { style = LayoutStyle.SECONDARY } }
 * }
 * ```
 */
public fun Layout(block: LayoutBox.() -> Unit): Layout = Layout(LayoutBox(LayoutType.COLUMN).apply(block).build())

/** Keeps a nested builder from reaching the one around it. */
@DslMarker
public annotation class LayoutDsl

/** How an element sits in the box around it. */
@LayoutDsl
public abstract class LayoutItem internal constructor() {
    /** How much of the free space along the box it takes. */
    public var grow: Double? = null

    /** How much it gives up when space is short. */
    public var shrink: Double? = null

    /** Where it sits across the box, instead of the box's own [LayoutBox.crossAlign]. */
    public var alignSelf: LayoutAlign? = null

    /** Pins it this many pixels in from its box's edges, over the box's other children. */
    public var pinTop: Int? = null
    public var pinBottom: Int? = null
    public var pinStart: Int? = null
    public var pinEnd: Int? = null
}

/** A column or a row being built, and its children. */
@LayoutDsl
public open class LayoutBox internal constructor(private val type: LayoutType) : LayoutItem() {
    public var gap: Int? = null
    public var padding: Int? = null
    public var paddingTop: Int? = null
    public var paddingBottom: Int? = null
    public var paddingStart: Int? = null
    public var paddingEnd: Int? = null
    public var align: LayoutAlign? = null
    public var crossAlign: LayoutAlign? = null
    public var card: Boolean = false

    /** Lays the children out from the end: bottom to top, or right to left. */
    public var reverse: Boolean = false

    /** Starts a new line when the children do not fit. */
    public var wrap: Boolean = false

    /** Makes the whole box a click target with this id. */
    public var id: String? = null

    /** Fills the box behind its children. */
    public var background: LayoutColor? = null

    /** In a Briefing card's detail, what the Display shows for a moment after a click on the box. */
    public var toast: String? = null

    /** Its height in pixels, instead of what its children need. */
    public var height: Int? = null

    private val children = mutableListOf<LayoutNode>()

    public fun column(block: LayoutBox.() -> Unit) {
        children += LayoutBox(LayoutType.COLUMN).apply(block).build()
    }

    public fun row(block: LayoutBox.() -> Unit) {
        children += LayoutBox(LayoutType.ROW).apply(block).build()
    }

    public fun text(text: String) {
        text(text) {}
    }

    public fun text(text: String, block: LayoutText.() -> Unit) {
        val spec = LayoutText().apply(block)
        children += LayoutNode(
            LayoutType.TEXT, text = text, style = spec.style, color = spec.color, align = spec.align,
            maxLines = spec.maxLines, grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf, pinTop = spec.pinTop,
            pinBottom = spec.pinBottom, pinStart = spec.pinStart, pinEnd = spec.pinEnd,
        )
    }

    public fun image(url: String) {
        image(url) {}
    }

    public fun image(url: String, block: LayoutImage.() -> Unit) {
        val spec = LayoutImage().apply(block)
        children += LayoutNode(
            LayoutType.IMAGE, url = url, height = spec.height, card = spec.rounded.takeIf { it }, size = spec.size,
            corners = spec.corners, grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf, pinTop = spec.pinTop,
            pinBottom = spec.pinBottom, pinStart = spec.pinStart, pinEnd = spec.pinEnd,
        )
    }

    /** One of the Display's own icons. */
    public fun icon(name: LayoutIconName) {
        icon(name) {}
    }

    public fun icon(name: LayoutIconName, block: LayoutIcon.() -> Unit) {
        val spec = LayoutIcon().apply(block)
        children += LayoutNode(
            LayoutType.ICON, icon = name, style = spec.style, color = spec.color, height = spec.size,
            grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf, pinTop = spec.pinTop,
            pinBottom = spec.pinBottom, pinStart = spec.pinStart, pinEnd = spec.pinEnd,
        )
    }

    /** A button whose clicks come with [id]. */
    public fun button(id: String, label: String) {
        button(id, label) {}
    }

    public fun button(id: String, label: String, block: LayoutButton.() -> Unit) {
        children += LayoutButton().apply(block).node(id, label)
    }

    /** A bar of buttons, as the Display draws them at the foot of its own screens. */
    public fun buttons(block: LayoutButtons.() -> Unit) {
        children += LayoutButtons().apply(block).build()
    }

    /** Pages the wearer swipes through, one [LayoutPager.page] each. */
    public fun pager(block: LayoutPager.() -> Unit) {
        children += LayoutPager().apply(block).build()
    }

    /** [values] as a line, 120 pixels high, in [LayoutColor.BLUE]. */
    public fun chart(values: List<Double>) {
        chart(values) {}
    }

    public fun chart(values: List<Double>, block: LayoutChart.() -> Unit) {
        val spec = LayoutChart().apply(block)
        children += LayoutNode(
            LayoutType.CHART, values = values, style = spec.style, color = spec.color, height = spec.height,
            grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf, pinTop = spec.pinTop,
            pinBottom = spec.pinBottom, pinStart = spec.pinStart, pinEnd = spec.pinEnd,
        )
    }

    /** A video: MP4 over `https://`, at most 400 pixels a side. It fills the screen alone, as on DAT. */
    public fun video(url: String) {
        video(url) {}
    }

    public fun video(url: String, block: LayoutVideo.() -> Unit) {
        val spec = LayoutVideo().apply(block)
        children += LayoutNode(
            LayoutType.VIDEO, url = url, controls = spec.controls.takeIf { it }, grow = spec.grow,
            shrink = spec.shrink, alignSelf = spec.alignSelf,
        )
    }

    internal fun build(): LayoutNode = LayoutNode(
        type, id = id, gap = gap, padding = padding, align = align, crossAlign = crossAlign,
        card = card.takeIf { it }, children = children.toList(), reverse = reverse.takeIf { it },
        wrap = wrap.takeIf { it }, paddingTop = paddingTop, paddingBottom = paddingBottom,
        paddingStart = paddingStart, paddingEnd = paddingEnd, grow = grow, shrink = shrink, alignSelf = alignSelf,
        background = background, pinTop = pinTop, pinBottom = pinBottom, pinStart = pinStart, pinEnd = pinEnd,
        height = height, toast = toast,
    )
}

@LayoutDsl
public class LayoutText internal constructor() : LayoutItem() {
    public var style: LayoutStyle? = null
    public var color: LayoutColor? = null

    /** [LayoutAlign.START], the default, [LayoutAlign.CENTER] or [LayoutAlign.END]. */
    public var align: LayoutAlign? = null

    /** The most lines it takes, cut with an ellipsis. */
    public var maxLines: Int? = null
}

@LayoutDsl
public class LayoutImage internal constructor() : LayoutItem() {
    public var height: Int? = null
    public var rounded: Boolean = false
    public var size: LayoutImageSize? = null
    public var corners: LayoutCorners? = null
}

@LayoutDsl
public class LayoutIcon internal constructor() : LayoutItem() {
    /** [LayoutStyle.FILLED], the default, or [LayoutStyle.OUTLINE]. */
    public var style: LayoutStyle? = null

    /** White unless set. */
    public var color: LayoutColor? = null

    /** Its width and height in pixels, 32 unless set. */
    public var size: Int? = null
}

@LayoutDsl
public class LayoutButton internal constructor() : LayoutItem() {
    public var style: LayoutStyle? = null

    /** An icon beside the label. */
    public var icon: LayoutIconName? = null

    /** Makes this the button the Display focuses first. */
    public var primary: Boolean = false

    /** In a Briefing card's detail, what the Display shows for a moment after a click, such as "Snoozed". */
    public var toast: String? = null

    internal fun node(id: String, label: String) = LayoutNode(
        LayoutType.BUTTON, id = id, text = label, style = style, icon = icon, primary = primary.takeIf { it },
        grow = grow, shrink = shrink, alignSelf = alignSelf, pinTop = pinTop, pinBottom = pinBottom, pinStart = pinStart,
        pinEnd = pinEnd, toast = toast,
    )
}

/** A bar of buttons being built. */
@LayoutDsl
public class LayoutButtons internal constructor() : LayoutItem() {
    /** [LayoutAlign.START], [LayoutAlign.CENTER] or [LayoutAlign.END]. */
    public var align: LayoutAlign? = null

    private val buttons = mutableListOf<LayoutNode>()

    public fun button(id: String, label: String) {
        button(id, label) {}
    }

    public fun button(id: String, label: String, block: LayoutButton.() -> Unit) {
        buttons += LayoutButton().apply(block).node(id, label)
    }

    internal fun build() = LayoutNode(
        LayoutType.BUTTON_GROUP, align = align, children = buttons.toList(), grow = grow, shrink = shrink,
        alignSelf = alignSelf, pinTop = pinTop, pinBottom = pinBottom, pinStart = pinStart, pinEnd = pinEnd,
    )
}

/** A pager being built, and its pages. */
@LayoutDsl
public class LayoutPager internal constructor() : LayoutItem() {
    private val pages = mutableListOf<LayoutNode>()

    /** One page: a column. */
    public fun page(block: LayoutBox.() -> Unit) {
        pages += LayoutBox(LayoutType.COLUMN).apply(block).build()
    }

    internal fun build() = LayoutNode(
        LayoutType.PAGER, children = pages.toList(), grow = grow, shrink = shrink, alignSelf = alignSelf,
        pinTop = pinTop, pinBottom = pinBottom, pinStart = pinStart, pinEnd = pinEnd,
    )
}

@LayoutDsl
public class LayoutChart internal constructor() : LayoutItem() {
    /** [LayoutStyle.LINE], the default, or [LayoutStyle.BARS]. */
    public var style: LayoutStyle? = null

    /** [LayoutColor.BLUE] unless set. */
    public var color: LayoutColor? = null

    /** In pixels, 120 unless set. */
    public var height: Int? = null
}

@LayoutDsl
public class LayoutVideo internal constructor() : LayoutItem() {
    /** Shows play and pause controls. */
    public var controls: Boolean = false
}
