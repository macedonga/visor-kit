package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** The params of [Methods.RAW_SEND]. */
@Poko
@Serializable
public class RawSend @KitInternal constructor(
    /** The glasses' service id, such as 28 for their status. */
    public val service: Int,
    /** The message type within the service. */
    public val type: Int,
    /** The message, Base64. */
    public val payload: String,
)

/** The params of [Topics.RAW_MESSAGES]. */
@Poko
@Serializable
public class RawFilter @KitInternal constructor(public val service: Int)

/** One message from the glasses, as it came, from [Topics.RAW_MESSAGES]. */
@Poko
@Serializable
public class RawMessage @KitInternal constructor(
    public val service: Int,
    public val type: Int,
    /** The message, Base64. */
    public val payload: String,
    /** When the phone got it, in milliseconds since 1970. */
    public val time: Long,
)
