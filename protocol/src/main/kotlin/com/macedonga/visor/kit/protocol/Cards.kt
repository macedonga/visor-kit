package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** A card on the Display home's Briefing, from [Methods.DISPLAY_CARDS_SET]. Build one with [BriefingCard]. */
@Poko
@Serializable
public class BriefingCard @KitInternal constructor(
    /** The app's own id for the card: a card set with the same id replaces it. */
    public val id: String,
    /** The card's name in the Display's list of Briefing cards, at most [MAX_NAME] characters. */
    public val name: String,
    /** The card's icon in that list. */
    public val icon: LayoutIconName = LayoutIconName.BELL,
    /** What the card shows. On the Briefing a video, buttons and clicks do nothing. */
    public val layout: Layout,
    /** What a click on the card opens, full screen, as Meta's cards open their own; null for a card without one. */
    public val detail: Layout? = null,
) {
    public companion object {
        public const val MAX_NAME: Int = 40

        /**
         * The most a card and its detail take as JSON, images inside them included: 256 KiB. An image given as an
         * `https://` URL costs only its URL: the glasses fetch it through the phone.
         */
        public const val MAX_BYTES: Int = 262_144
    }
}

/** Why Visor refuses this card, or null when it takes it. Visor, FakeVisor and the Android client check with it. */
@KitInternal
public fun BriefingCard.problem(): String? {
    if (id.isBlank()) return "A card needs an id."
    if (name.isBlank() || name.length > BriefingCard.MAX_NAME) return "A card needs a name of 1 to ${BriefingCard.MAX_NAME} characters."
    val size = KitJson.encodeToString(BriefingCard.serializer(), this).encodeToByteArray().size
    if (size > BriefingCard.MAX_BYTES) {
        return "The card and its detail take ${size / KIB} KiB, over ${BriefingCard.MAX_BYTES / KIB} KiB: scale its images down, " +
            "or give them as https:// URLs, which the glasses fetch."
    }
    return null
}

private const val KIB = 1024

/** The params of [Methods.DISPLAY_CARDS_REMOVE]. */
@Poko
@Serializable
public class CardRemoval @KitInternal constructor(
    /** A [BriefingCard.id] of the app's. */
    public val id: String,
)

/**
 * A Briefing card being built: its column, as any [LayoutBox], and what a click on it opens, set by [detail] or
 * [pages].
 */
public class LayoutCard internal constructor() : LayoutBox(LayoutType.COLUMN) {
    private var detail: Layout? = null

    /** What a click on the card opens, full screen, as Meta's cards open theirs. */
    public fun detail(block: LayoutBox.() -> Unit) {
        detail = Layout(block)
    }

    /** A detail of pages the wearer swipes through, as Meta's Weather and Stocks cards open. */
    public fun pages(block: LayoutPager.() -> Unit) {
        detail = Layout { pager(block) }
    }

    internal fun card(id: String, name: String, icon: LayoutIconName) = BriefingCard(id, name, icon, Layout(build()), detail)
}

/**
 * A [BriefingCard] whose column [block] fills, for example:
 *
 * ```
 * BriefingCard("pasta", "Kitchen timer", LayoutIconName.CLOCK) {
 *     header(LayoutIconName.CLOCK, "Kitchen timer")
 *     stat("4:00", "Pasta")
 *     progress(0.6)
 *     detail {
 *         field("Started", "19:42")
 *         field("Ends", "19:52")
 *     }
 * }
 * ```
 */
public fun BriefingCard(id: String, name: String, icon: LayoutIconName, block: LayoutCard.() -> Unit): BriefingCard =
    LayoutCard().apply(block).card(id, name, icon)

/** A [BriefingCard] with the bell icon. */
public fun BriefingCard(id: String, name: String, block: LayoutCard.() -> Unit): BriefingCard =
    BriefingCard(id, name, LayoutIconName.BELL, block)
