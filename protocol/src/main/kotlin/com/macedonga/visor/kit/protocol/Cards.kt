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
) {
    public companion object {
        public const val MAX_NAME: Int = 40
    }
}

/** The params of [Methods.DISPLAY_CARDS_REMOVE]. */
@Poko
@Serializable
public class CardRemoval @KitInternal constructor(
    /** A [BriefingCard.id] of the app's. */
    public val id: String,
)

/**
 * A [BriefingCard] whose column [block] fills, for example:
 *
 * ```
 * BriefingCard("coffee", "Coffee") {
 *     text("Coffee") { style = LayoutStyle.HEADING }
 *     text("Ready in 2 minutes")
 * }
 * ```
 */
public fun BriefingCard(id: String, name: String, icon: LayoutIconName = LayoutIconName.BELL, block: LayoutBox.() -> Unit): BriefingCard =
    BriefingCard(id, name, icon, Layout(block))
