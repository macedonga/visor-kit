package com.macedonga.visor.vdk.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** How the Display's teleprompter shows a script. Build one with [PrompterStyle]. */
@Poko
@Serializable
public class PrompterStyle @VdkInternal constructor(
    /** The text size in sp on the Display. */
    public val sizeSp: Double = DEFAULT_SIZE_SP,
    /** The scroll speed, in the glasses' own unit. */
    public val speed: Double = DEFAULT_SPEED,
    /** True to show the script a page at a time instead of scrolling it. */
    public val paged: Boolean = false,
    /** True to scroll as the wearer reads aloud, with the glasses' own speech recognition. */
    public val followVoice: Boolean = false,
) {
    /** Sets a [PrompterStyle]'s fields. What it leaves alone keeps the glasses' default. */
    public class Builder {
        public var sizeSp: Double = DEFAULT_SIZE_SP
        public var speed: Double = DEFAULT_SPEED
        public var paged: Boolean = false
        public var followVoice: Boolean = false

        public fun build(): PrompterStyle = PrompterStyle(sizeSp, speed, paged, followVoice)
    }

    public companion object {
        /** The size Visor's own teleprompter starts at. */
        public const val DEFAULT_SIZE_SP: Double = 35.0

        /** The glasses' default speed. */
        public const val DEFAULT_SPEED: Double = 32.0
    }
}

/** A [PrompterStyle], for example `PrompterStyle { paged = true }`. */
public fun PrompterStyle(block: PrompterStyle.Builder.() -> Unit): PrompterStyle =
    PrompterStyle.Builder().apply(block).build()

/** The params of [Methods.DISPLAY_PROMPTER_SHOW]. */
@Poko
@Serializable
public class PrompterRequest @VdkInternal constructor(
    public val text: String,
    public val style: PrompterStyle = PrompterStyle(),
)
