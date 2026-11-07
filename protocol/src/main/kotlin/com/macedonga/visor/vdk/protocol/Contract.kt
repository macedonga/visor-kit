package com.macedonga.visor.vdk.protocol

import kotlinx.serialization.KSerializer

/**
 * A request an app sends and Visor answers once, with a result of type [R] or a [Failure].
 *
 * A released method keeps its [name], [scope] and [since] for good. Its params and its result only gain fields, and
 * every field added after [since] is optional.
 */
public class Method<P, R> internal constructor(
    /** The name on the wire, for example `camera.photo`. */
    public val name: String,
    /** The scope the app needs, or null when any app Visor lets in may call it. */
    public val scope: Scope?,
    /** The [VdkProtocol.VERSION] that added it. */
    public val since: Int,
    /** One sentence for the reference, the OpenRPC document and the MCP tool list. */
    public val summary: String,
    public val params: KSerializer<P>,
    public val result: KSerializer<R>,
) {
    override fun toString(): String = name
}

/**
 * A stream of events of type [E]. Visor sends events until the app unsubscribes, or ends the stream, with a
 * [Failure] when something went wrong. The same rules as for a [Method] apply.
 */
public class Topic<P, E> internal constructor(
    /** The name on the wire, for example `band.gestures`. */
    public val name: String,
    /** The scope the app needs, or null when any app Visor lets in may subscribe. */
    public val scope: Scope?,
    /** The [VdkProtocol.VERSION] that added it. */
    public val since: Int,
    /** One sentence for the reference and the OpenRPC document. */
    public val summary: String,
    public val params: KSerializer<P>,
    public val event: KSerializer<E>,
) {
    override fun toString(): String = name
}
