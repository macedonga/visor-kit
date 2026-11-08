package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** The params of [Topics.VOICE_REQUESTS]. */
@Poko
@Serializable
public class VoiceOffer @KitInternal constructor(
    /**
     * The phrases that make a question the app's, such as "the garage" or "my lights". A question is the app's when
     * it holds all the words of one of them, in order and next to each other; case and punctuation do not count.
     * When phrases of several apps match, the longest phrase wins.
     */
    public val phrases: List<String>,
)

/** A question the wearer asked that names the app, from [Topics.VOICE_REQUESTS]. */
@Poko
@Serializable
public class VoiceRequest @KitInternal constructor(
    /** What [Methods.VOICE_REPLY] answers. */
    public val id: String,
    /** The question, as the glasses heard it. */
    public val text: String,
    /** The phrase of the app's that matched. */
    public val phrase: String,
    /** What the glasses understood the question as, such as `IN:QUESTION`, when they said. */
    public val intent: String? = null,
)

/** The params of [Topics.VOICE_TRANSCRIPTS]. Build one with [TranscriptRequest]. */
@Poko
@Serializable
public class TranscriptRequest @KitInternal constructor(
    /** The language to hear, as a BCP 47 tag such as `en-US`; null for the phone's language. */
    public val language: String? = null,
    /** True to get the text while the wearer speaks, as [Transcript.partial] transcripts, as DAT gives it. */
    public val partials: Boolean = false,
) {
    public class Builder {
        public var language: String? = null
        public var partials: Boolean = false

        public fun build(): TranscriptRequest = TranscriptRequest(language, partials)
    }
}

/** A [TranscriptRequest], for example `TranscriptRequest { language = "it-IT" }`. */
public fun TranscriptRequest(block: TranscriptRequest.Builder.() -> Unit): TranscriptRequest =
    TranscriptRequest.Builder().apply(block).build()

/**
 * One utterance, from [Topics.VOICE_TRANSCRIPTS]: finished, or while it is spoken when the app asked for
 * [TranscriptRequest.partials]. Each partial replaces the one before; keep only the finished ones.
 */
@Poko
@Serializable
public class Transcript @KitInternal constructor(
    public val text: String,
    /** The language it was heard in, as a BCP 47 tag. */
    public val language: String,
    /** When the utterance ended, or when this part was heard, in milliseconds since 1970. */
    public val time: Long,
    /** True while the wearer still speaks: the text so far, which the next transcript replaces. */
    public val partial: Boolean = false,
    /** How sure the recogniser is, from 0 to 1; null when it does not say. */
    public val confidence: Double? = null,
)

/** The params of [Methods.VOICE_SPEAK]. */
@Poko
@Serializable
public class SpeakRequest @KitInternal constructor(
    /** What the glasses say, at most [MAX_LENGTH] characters. */
    public val text: String,
) {
    public companion object {
        public const val MAX_LENGTH: Int = 500
    }
}

/** The params of [Methods.VOICE_REPLY]. */
@Poko
@Serializable
public class VoiceReply @KitInternal constructor(
    /** A [VoiceRequest.id]. */
    public val request: String,
    /** What the glasses say to the wearer. */
    public val text: String,
    /** True to keep the microphone open for a follow-up, as after a question. */
    public val listen: Boolean = false,
)
