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
    /** An image's height in pixels; null to fit its width. */
    public val height: Int? = null,
    /** A box's space between its children, and around them, in pixels. */
    public val gap: Int? = null,
    public val padding: Int? = null,
    /** Where a box puts its children along its direction, and across it; a button group, where its buttons go. */
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
    /** A text's colour. */
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

        /** A video from [LayoutNode.url]: MP4 over `https://`, at most 400 pixels a side. One per layout. */
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

/** A text's colour on the Display. */
@Serializable
@JvmInline
public value class LayoutColor(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** White. */
        public val PRIMARY: LayoutColor = LayoutColor("primary")

        /** Light grey. */
        public val SECONDARY: LayoutColor = LayoutColor("secondary")
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
}

/** A column or a row being built, and its children. */
@LayoutDsl
public class LayoutBox internal constructor(private val type: LayoutType) : LayoutItem() {
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
            LayoutType.TEXT, text = text, style = spec.style, color = spec.color,
            grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf,
        )
    }

    public fun image(url: String) {
        image(url) {}
    }

    public fun image(url: String, block: LayoutImage.() -> Unit) {
        val spec = LayoutImage().apply(block)
        children += LayoutNode(
            LayoutType.IMAGE, url = url, height = spec.height, card = spec.rounded.takeIf { it }, size = spec.size,
            corners = spec.corners, grow = spec.grow, shrink = spec.shrink, alignSelf = spec.alignSelf,
        )
    }

    /** One of the Display's own icons. */
    public fun icon(name: LayoutIconName) {
        icon(name) {}
    }

    public fun icon(name: LayoutIconName, block: LayoutIcon.() -> Unit) {
        val spec = LayoutIcon().apply(block)
        children += LayoutNode(
            LayoutType.ICON, icon = name, style = spec.style, grow = spec.grow, shrink = spec.shrink,
            alignSelf = spec.alignSelf,
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

    /** A video: MP4 over `https://`, at most 400 pixels a side. One per layout. */
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
    )
}

@LayoutDsl
public class LayoutText internal constructor() : LayoutItem() {
    public var style: LayoutStyle? = null
    public var color: LayoutColor? = null
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
}

@LayoutDsl
public class LayoutButton internal constructor() : LayoutItem() {
    public var style: LayoutStyle? = null

    /** An icon beside the label. */
    public var icon: LayoutIconName? = null

    /** Makes this the button the Display focuses first. */
    public var primary: Boolean = false

    internal fun node(id: String, label: String) = LayoutNode(
        LayoutType.BUTTON, id = id, text = label, style = style, icon = icon, primary = primary.takeIf { it },
        grow = grow, shrink = shrink, alignSelf = alignSelf,
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
        alignSelf = alignSelf,
    )
}

@LayoutDsl
public class LayoutVideo internal constructor() : LayoutItem() {
    /** Shows play and pause controls. */
    public var controls: Boolean = false
}
