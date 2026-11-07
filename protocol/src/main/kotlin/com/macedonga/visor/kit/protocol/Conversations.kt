package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** Where a question to the assistant came from. New sources come with new releases. */
@Serializable
@JvmInline
public value class TurnSource(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** "Hey Meta", a follow-up, or the glasses' button. */
        public val GLASSES: TurnSource = TurnSource("glasses")

        /** Typed or spoken in Visor's Meta AI page. */
        public val PHONE: TurnSource = TurnSource("phone")

        /** A Live AI session, which sees what the camera sees. */
        public val LIVE_AI: TurnSource = TurnSource("live_ai")
    }
}

/** One thing the assistant did with the person's tools while it answered. */
@Poko
@Serializable
public class ToolUse @KitInternal constructor(
    /** The tool's owner: an MCP server's name, or an app's package. */
    public val server: String,
    /** What it did, in the words the tool reported. */
    public val what: String,
    public val ok: Boolean,
)

/** One question to the assistant and its answer. */
@Poko
@Serializable
public class AssistantTurn @KitInternal constructor(
    /** Stable for the life of the turn: a turn asked again keeps its id and comes again with the new answer. */
    public val id: String,
    /** When the question was asked, in milliseconds since the epoch. */
    public val time: Long,
    public val question: String,
    /** Empty while the assistant has not answered, or when it failed. */
    public val answer: String = "",
    /** Why the turn failed, for a person reading a log; empty when it did not. */
    public val failure: String = "",
    public val source: TurnSource = TurnSource.GLASSES,
    /** Turns asked close together share one, as do the turns of one Live AI session. */
    public val conversation: String = "",
    /** True when the glasses sent a photo with the question. */
    public val photo: Boolean = false,
    public val tools: List<ToolUse> = emptyList(),
)

/** The params of [Methods.ASSISTANT_HISTORY]. */
@Poko
@Serializable
public class HistoryQuery @KitInternal constructor(
    /** Only turns asked after this time, in milliseconds since the epoch; all when null. */
    public val since: Long? = null,
    /** At most this many, the newest; 1 to 500. */
    public val limit: Int = DEFAULT_LIMIT,
) {
    public companion object {
        public const val DEFAULT_LIMIT: Int = 50
        public const val MAX_LIMIT: Int = 500
    }
}
