package com.macedonga.visor.kit.protocol

// Pieces of Meta's own Briefing cards, sized and coloured as the Display draws them, so a layout looks at home there
// without tuning. Each is a plain row or text: a Visor that knows the elements draws them.

/** A card's first line, as Meta's: [icon] beside [title], bold. */
public fun LayoutBox.header(icon: LayoutIconName, title: String) {
    row {
        gap = 8
        crossAlign = LayoutAlign.CENTER
        icon(icon) { size = HEADER_ICON }
        text(title) {
            style = LayoutStyle.TITLE
            maxLines = 1
            shrink = 1.0
        }
    }
}

/** A card's first line without an icon. */
public fun LayoutBox.header(title: String) {
    text(title) {
        style = LayoutStyle.TITLE
        maxLines = 1
    }
}

/** A figure in large type, as the temperature on Meta's Weather card. */
public fun LayoutBox.stat(value: String) {
    text(value) {
        style = LayoutStyle.HEADING
        maxLines = 1
    }
}

/** A figure in large type with [caption] under it, as "8°" and "Low 3°". */
public fun LayoutBox.stat(value: String, caption: String) {
    column {
        stat(value)
        text(caption) {
            style = LayoutStyle.META
            maxLines = 1
        }
    }
}

/** A rise or a fall: a small triangle and [text], green going up and red going down, as Meta's Stocks card. */
public fun LayoutBox.change(text: String, rising: Boolean) {
    val tone = if (rising) LayoutColor.GREEN else LayoutColor.RED
    row {
        gap = 4
        crossAlign = LayoutAlign.CENTER
        icon(if (rising) LayoutIconName.SMALL_TRIANGLE_UP else LayoutIconName.SMALL_TRIANGLE_DOWN) {
            size = CHANGE_ICON
            color = tone
        }
        text(text) {
            color = tone
            maxLines = 1
            shrink = 1.0
        }
    }
}

/** A label and its value on one line, the value at the end, as the rows of Meta's Stocks detail. */
public fun LayoutBox.field(label: String, value: String) {
    row {
        align = LayoutAlign.SPACE_BETWEEN
        crossAlign = LayoutAlign.CENTER
        gap = 16
        text(label) {
            color = LayoutColor.SECONDARY
            maxLines = 1
            shrink = 1.0
        }
        text(value) {
            align = LayoutAlign.END
            maxLines = 1
        }
    }
}

/** A bar filled [fraction] of the way, from 0 to 1, in [LayoutColor.BLUE]. */
public fun LayoutBox.progress(fraction: Double) {
    progress(fraction, LayoutColor.BLUE)
}

/** A bar filled [fraction] of the way, from 0 to 1, in [color]. */
public fun LayoutBox.progress(fraction: Double, color: LayoutColor) {
    val filled = if (fraction.isNaN()) 0.0 else fraction.coerceIn(0.0, 1.0)
    row {
        height = PROGRESS_HEIGHT
        background = LayoutColor.TRACK
        // A row places its children at their own height; the fill takes the track's.
        crossAlign = LayoutAlign.STRETCH
        row {
            grow = filled
            background = color
        }
        row { grow = 1 - filled }
    }
}

private const val HEADER_ICON = 24
private const val CHANGE_ICON = 20
private const val PROGRESS_HEIGHT = 8
