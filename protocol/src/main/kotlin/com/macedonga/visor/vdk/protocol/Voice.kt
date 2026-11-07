package com.macedonga.visor.vdk.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** The params of [Topics.VOICE_REQUESTS]. */
@Poko
@Serializable
public class VoiceOffer @VdkInternal constructor(
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
public class VoiceRequest @VdkInternal constructor(
    /** What [Methods.VOICE_REPLY] answers. */
    public val id: String,
    /** The question, as the glasses heard it. */
    public val text: String,
    /** The phrase of the app's that matched. */
    public val phrase: String,
    /** What the glasses understood the question as, such as `IN:QUESTION`, when they said. */
    public val intent: String? = null,
)

/** The params of [Methods.VOICE_REPLY]. */
@Poko
@Serializable
public class VoiceReply @VdkInternal constructor(
    /** A [VoiceRequest.id]. */
    public val request: String,
    /** What the glasses say to the wearer. */
    public val text: String,
    /** True to keep the microphone open for a follow-up, as after a question. */
    public val listen: Boolean = false,
)
