package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/**
 * A permission the person grants an app on Visor's consent screen. New scopes come with new releases, so an app
 * meets ids it does not know when Visor is newer.
 */
@Serializable
@JvmInline
public value class Scope(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** The glasses' link, battery, model, and whether they are worn or folded. */
        public val STATE: Scope = Scope("state")

        /** Photos, videos, and the captures on the glasses. */
        public val CAMERA: Scope = Scope("camera")

        /** The Neural Band: its gestures, its battery and its vibration. */
        public val BAND: Scope = Scope("band")

        /** What the Display shows. */
        public val DISPLAY: Scope = Scope("display")
    }
}

/** Why a call failed. Visor adds codes over time: treat a code you do not know as [FAILED]. */
@Serializable
@JvmInline
public value class ErrorCode(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** The person has not granted the scope this needs. [Failure.scope] names it. */
        public val SCOPE_DENIED: ErrorCode = ErrorCode("scope_denied")

        /** Visor does not know the method or topic. */
        public val UNKNOWN_METHOD: ErrorCode = ErrorCode("unknown_method")

        /** The params do not fit the method. */
        public val INVALID_PARAMS: ErrorCode = ErrorCode("invalid_params")

        /** These glasses cannot do it, for example the prompter on glasses without a display. */
        public val UNSUPPORTED: ErrorCode = ErrorCode("unsupported")

        /** The glasses are not connected and did not connect in time. */
        public val NOT_CONNECTED: ErrorCode = ErrorCode("not_connected")

        /** Something else holds what this needs. */
        public val BUSY: ErrorCode = ErrorCode("busy")

        /** The glasses turned the request down. */
        public val REFUSED: ErrorCode = ErrorCode("refused")

        /** The glasses did not answer in time. */
        public val TIMEOUT: ErrorCode = ErrorCode("timeout")

        /** Anything else. [Failure.message] says what. */
        public val FAILED: ErrorCode = ErrorCode("failed")

        /** Visor is not installed. The client library raises it, never Visor. */
        public val VISOR_MISSING: ErrorCode = ErrorCode("visor_missing")

        /** Visor is older than this needs: update Visor. The client library raises it, never Visor. */
        public val VISOR_TOO_OLD: ErrorCode = ErrorCode("visor_too_old")

        /** The connection to Visor broke while this ran, for example because Visor restarted. Client side only. */
        public val VISOR_GONE: ErrorCode = ErrorCode("visor_gone")

        /** The app closed the connection while this ran. Client side only. */
        public val CLOSED: ErrorCode = ErrorCode("closed")
    }
}

/** Why a call or a stream failed. */
@Poko
@Serializable
public class Failure @KitInternal constructor(
    public val code: ErrorCode,
    /** For a person reading a log. The wording can change in any release, so code never parses it. */
    public val message: String,
    /** With [ErrorCode.SCOPE_DENIED], the scope that is missing. */
    public val scope: Scope? = null,
    /** A finer reason, from a set each method documents, for example `wifi_off` for an import. */
    public val reason: String? = null,
)

/** The first call of every session, [Methods.HELLO]. */
@Poko
@Serializable
public class Hello @KitInternal constructor(
    /** The [KitProtocol.VERSION] the app was built with. */
    public val protocol: Int,
    /** The client library's version, for Visor's list of apps. */
    public val kit: String,
)

/** Visor's answer to [Hello]. */
@Poko
@Serializable
public class Welcome @KitInternal constructor(
    /** The [KitProtocol.VERSION] Visor speaks. Lower than the app's when Visor is the older side. */
    public val protocol: Int,
    /** Visor's version name, for example 2.3.0. */
    public val visor: String,
    /** The scopes the person granted this app. */
    public val granted: List<Scope>,
    /** The names of the methods Visor answers. */
    public val methods: List<String>,
    /** The names of the topics Visor streams. */
    public val topics: List<String>,
)
