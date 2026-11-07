package com.macedonga.visor.kit.protocol

import kotlinx.serialization.json.Json

/** What both sides agree on outside the messages themselves. None of these values ever changes. */
public object KitProtocol {
    /**
     * The protocol version this build speaks. Each release that adds a method, a topic or a field raises it by one.
     * It never goes down, and a released version keeps its meaning.
     */
    public const val VERSION: Int = 1

    /** Visor's package name. */
    public const val VISOR_PACKAGE: String = "com.macedonga.visor"

    /** The action of Visor's VDK service, for `bindService`. */
    public const val ACTION_BIND: String = "com.macedonga.visor.kit.action.BIND"

    /** The action of Visor's consent screen, for an activity result. */
    public const val ACTION_REQUEST_SCOPES: String = "com.macedonga.visor.kit.action.REQUEST_SCOPES"

    /** The scope ids, a `String[]`: the ones asked for in the request, the ones the app holds in the result. */
    public const val EXTRA_SCOPES: String = "com.macedonga.visor.kit.extra.SCOPES"

    /**
     * A feed's records over binder: a 4-byte big-endian header length, the header as UTF-8 JSON, a 4-byte big-endian
     * payload length, the payload. A header length of FEED_END closes the feed: a 4-byte length and a [Failure] as
     * JSON follow, a length of 0 when the feed simply ended.
     */
    public const val FEED_END: Int = 0
}

/**
 * The JSON of every message. A peer ignores the keys it does not know, so either side can be the newer one, and
 * leaves out nulls, so an optional field costs nothing on the wire.
 */
@KitInternal
public val KitJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
