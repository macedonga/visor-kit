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
) {
    public class Builder {
        public var language: String? = null

        public fun build(): TranscriptRequest = TranscriptRequest(language)
    }
}

/** A [TranscriptRequest], for example `TranscriptRequest { language = "it-IT" }`. */
public fun TranscriptRequest(block: TranscriptRequest.Builder.() -> Unit): TranscriptRequest =
    TranscriptRequest.Builder().apply(block).build()

/** One finished utterance, from [Topics.VOICE_TRANSCRIPTS]. */
@Poko
@Serializable
public class Transcript @KitInternal constructor(
    public val text: String,
    /** The language it was heard in, as a BCP 47 tag. */
    public val language: String,
    /** When the utterance ended, in milliseconds since 1970. */
    public val time: Long,
)

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
