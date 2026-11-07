package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The VDK over JSON-RPC 2.0, one JSON object per line: the door for the `visor` command on a computer, through adb.
 * Every [Method] is a JSON-RPC method of the same name. Streams, which JSON-RPC lacks, are the methods and
 * notifications below.
 */
public object JsonRpc {
    /** Subscribes to a topic. Its result is a [Subscribed]; events then come as [EVENT] notifications. */
    public const val SUBSCRIBE: String = "kit.subscribe"

    /** Ends a subscription. Visor sends no [ENDED] for it. */
    public const val UNSUBSCRIBE: String = "kit.unsubscribe"

    /** Stops a running call; Visor answers it no more. */
    public const val CANCEL: String = "kit.cancel"

    /** A notification from Visor: one event of a subscription, an [EventNotice]. */
    public const val EVENT: String = "kit.event"

    /** A notification from Visor: a subscription is over, an [EndedNotice]. */
    public const val ENDED: String = "kit.ended"

    /** The JSON-RPC error code of a [Failure] with [ErrorCode.UNKNOWN_METHOD]. */
    public const val METHOD_NOT_FOUND: Int = -32601

    /** The JSON-RPC error code of a [Failure] with [ErrorCode.INVALID_PARAMS]. */
    public const val INVALID_PARAMS: Int = -32602

    /** The JSON-RPC error code of every other [Failure]; its `data` is the [Failure]. */
    public const val FAILED: Int = -32000

    /** A line that is not JSON-RPC. */
    public const val PARSE_ERROR: Int = -32700
}

/** A request, or a notification when [id] is null. */
@Poko
@Serializable
public class RpcRequest @KitInternal constructor(
    public val jsonrpc: String = "2.0",
    public val id: Long? = null,
    public val method: String,
    public val params: JsonElement? = null,
)

/** The answer to a request: [result] or [error]. */
@Poko
@Serializable
public class RpcResponse @KitInternal constructor(
    public val jsonrpc: String = "2.0",
    public val id: Long?,
    public val result: JsonElement? = null,
    public val error: RpcError? = null,
)

@Poko
@Serializable
public class RpcError @KitInternal constructor(
    public val code: Int,
    public val message: String,
    public val data: Failure? = null,
)

/** The params of [JsonRpc.SUBSCRIBE]. */
@Poko
@Serializable
public class SubscribeRequest @KitInternal constructor(
    public val topic: String,
    public val params: JsonElement? = null,
)

/** The result of [JsonRpc.SUBSCRIBE]: the id of the subscription, which is the request's own id. */
@Poko
@Serializable
public class Subscribed @KitInternal constructor(public val subscription: Long)

/** The params of [JsonRpc.UNSUBSCRIBE]. */
@Poko
@Serializable
public class UnsubscribeRequest @KitInternal constructor(public val subscription: Long)

/** The params of [JsonRpc.CANCEL]. */
@Poko
@Serializable
public class CancelRequest @KitInternal constructor(public val id: Long)

/** The params of an [JsonRpc.EVENT] notification. */
@Poko
@Serializable
public class EventNotice @KitInternal constructor(
    public val subscription: Long,
    public val event: JsonElement,
)

/** The params of an [JsonRpc.ENDED] notification: [failure] is null when the stream simply ended. */
@Poko
@Serializable
public class EndedNotice @KitInternal constructor(
    public val subscription: Long,
    public val failure: Failure? = null,
)
