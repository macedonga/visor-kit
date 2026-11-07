package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/**
 * What only the adb shell may do, for a developer at a computer: see the apps, grant scopes without the consent
 * screen, send made-up events to the apps, and watch everything that passes. Visor answers these only on the shell's
 * socket, [SOCKET], and only while Developer mode is on. The shell holds every scope.
 */
public object Shell {
    /** The abstract Unix socket Visor listens on. `adb forward tcp:0 localabstract:visor-kit` reaches it. */
    public const val SOCKET: String = "visor-kit"

    public val APPS: Method<Unit, List<AppInfo>> = Method(
        "shell.apps", null, 1,
        "Lists the apps that hold scopes or have a session open now.",
        Unit.serializer(), ListSerializer(AppInfo.serializer()),
    )

    /** Fails with [ErrorCode.INVALID_PARAMS] for a package that is not installed. */
    public val GRANT: Method<GrantRequest, AppInfo> = Method(
        "shell.grant", null, 1,
        "Grants an app scopes, as the consent screen would.",
        GrantRequest.serializer(), AppInfo.serializer(),
    )

    public val REVOKE: Method<GrantRequest, AppInfo> = Method(
        "shell.revoke", null, 1,
        "Takes scopes back from an app. Its streams that need them end.",
        GrantRequest.serializer(), AppInfo.serializer(),
    )

    /**
     * Topics whose subscriptions get only injected events, so an app can be tried without the glasses or the band:
     * a simulated `band.gestures` borrows no band. The list replaces the one before; streams already open switch over
     * without the app noticing. The result is the list now in force.
     */
    public val SIMULATE: Method<SimulateRequest, List<String>> = Method(
        "shell.simulate", null, 1,
        "Sets the topics that carry only injected events, with no glasses or band behind them.",
        SimulateRequest.serializer(), ListSerializer(String.serializer()),
    )

    /** The event goes to the apps only: the glasses, the band and Visor's own screens never see it. */
    public val INJECT: Method<InjectRequest, Unit> = Method(
        "shell.inject", null, 1,
        "Sends a made-up event to every app subscribed to a topic, for example a band gesture.",
        InjectRequest.serializer(), Unit.serializer(),
    )

    public val TRACE: Topic<Unit, TraceRecord> = Topic(
        "shell.trace", null, 1,
        "Every call, answer and event between the apps and Visor, as it happens.",
        Unit.serializer(), TraceRecord.serializer(),
    )

    public val methods: List<Method<*, *>> = listOf(APPS, GRANT, REVOKE, SIMULATE, INJECT)
    public val topics: List<Topic<*, *>> = listOf(TRACE)
}

/** An app as Visor's kit knows it. */
@Poko
@Serializable
public class AppInfo @KitInternal constructor(
    public val packageName: String,
    public val granted: List<Scope>,
    /** True while the app has a session open. */
    public val connected: Boolean,
    /** The kit version the app runs, from its last [Hello]. */
    public val kit: String? = null,
    /** The protocol version the app speaks, from its last [Hello]. */
    public val protocol: Int? = null,
)

/** The params of [Shell.GRANT] and [Shell.REVOKE]. */
@Poko
@Serializable
public class GrantRequest @KitInternal constructor(
    public val packageName: String,
    public val scopes: List<Scope>,
)

/** The params of [Shell.SIMULATE]. */
@Poko
@Serializable
public class SimulateRequest @KitInternal constructor(
    /** Topic names. An empty list ends every simulation. */
    public val topics: List<String>,
)

/** The params of [Shell.INJECT]. */
@Poko
@Serializable
public class InjectRequest @KitInternal constructor(
    public val topic: String,
    /** The event, in the topic's own JSON. Visor checks it fits the topic before it sends it. */
    public val event: JsonElement,
)

/** What one trace line is about. */
@Serializable
@JvmInline
public value class TraceKind(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val CALL: TraceKind = TraceKind("call")
        public val RESULT: TraceKind = TraceKind("result")
        public val FAILURE: TraceKind = TraceKind("failure")
        public val SUBSCRIBE: TraceKind = TraceKind("subscribe")
        public val EVENT: TraceKind = TraceKind("event")
        public val ENDED: TraceKind = TraceKind("ended")
        public val UNSUBSCRIBE: TraceKind = TraceKind("unsubscribe")
    }
}

/** One thing that passed between an app and Visor. */
@Poko
@Serializable
public class TraceRecord @KitInternal constructor(
    /** Milliseconds since 1970. */
    public val time: Long,
    /** The app's package, or `shell`. */
    public val app: String,
    public val kind: TraceKind,
    /** The method or topic. */
    public val name: String,
    /** The call's or the subscription's id. */
    public val id: Long,
    /** The params, the result, the event or the failure. */
    public val body: JsonElement? = null,
    /** For a result or a failure, how long the call took. */
    public val durationMs: Long? = null,
)
