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
    /** How a text or a button looks. */
    public val style: LayoutStyle? = null,
    /** An image: an `https://` URL, or a `data:` URI. */
    public val url: String? = null,
    /** An image's height in pixels; null to fit its width. */
    public val height: Int? = null,
    /** A box's space between its children, and around them, in pixels. */
    public val gap: Int? = null,
    public val padding: Int? = null,
    /** Where a box puts its children along its direction, and across it. */
    public val align: LayoutAlign? = null,
    public val crossAlign: LayoutAlign? = null,
    /** True to draw a box on a card, or an image with rounded corners. */
    public val card: Boolean? = null,
    public val children: List<LayoutNode> = emptyList(),
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
    }
}

/** How a text or a button looks. */
@Serializable
@JvmInline
public value class LayoutStyle(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** Text: 40 px, bold. */
        public val HEADING: LayoutStyle = LayoutStyle("heading")

        /** Text: 28 px, the default. */
        public val BODY: LayoutStyle = LayoutStyle("body")

        /** Text: 24 px, in the secondary colour. */
        public val META: LayoutStyle = LayoutStyle("meta")

        /** A button: filled, the default. */
        public val PRIMARY: LayoutStyle = LayoutStyle("primary")

        /** A button: outlined. */
        public val SECONDARY: LayoutStyle = LayoutStyle("secondary")
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

/** A column or a row being built, and its children. */
@LayoutDsl
public class LayoutBox internal constructor(private val type: LayoutType) {
    public var gap: Int? = null
    public var padding: Int? = null
    public var align: LayoutAlign? = null
    public var crossAlign: LayoutAlign? = null
    public var card: Boolean = false

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
        children += LayoutNode(LayoutType.TEXT, text = text, style = spec.style)
    }

    public fun image(url: String) {
        image(url) {}
    }

    public fun image(url: String, block: LayoutImage.() -> Unit) {
        val spec = LayoutImage().apply(block)
        children += LayoutNode(LayoutType.IMAGE, url = url, height = spec.height, card = spec.rounded.takeIf { it })
    }

    /** A button whose clicks come with [id]. */
    public fun button(id: String, label: String) {
        button(id, label) {}
    }

    public fun button(id: String, label: String, block: LayoutButton.() -> Unit) {
        val spec = LayoutButton().apply(block)
        children += LayoutNode(LayoutType.BUTTON, id = id, text = label, style = spec.style)
    }

    internal fun build(): LayoutNode = LayoutNode(
        type, id = id, gap = gap, padding = padding, align = align, crossAlign = crossAlign,
        card = card.takeIf { it }, children = children.toList(),
    )
}

@LayoutDsl
public class LayoutText internal constructor() {
    public var style: LayoutStyle? = null
}

@LayoutDsl
public class LayoutImage internal constructor() {
    public var height: Int? = null
    public var rounded: Boolean = false
}

@LayoutDsl
public class LayoutButton internal constructor() {
    public var style: LayoutStyle? = null
}
