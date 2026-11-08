package com.macedonga.visor.kit.server

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.AppInfo
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.TraceKind
import com.macedonga.visor.kit.protocol.TraceRecord
import com.macedonga.visor.kit.protocol.Welcome
import com.macedonga.visor.kit.toFailure
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Visor's side of the VDK, whatever the transport: the scope checks, [Methods.HELLO], the shell's own methods, the
 * injected and simulated events, and the trace. On Android, KitServer carries it over binder to apps and ShellServer
 * over a socket to adb; [JsonRpcSession] carries it over any line or frame transport.
 */
@KitInternal
public class Dispatcher(
    private val visorVersion: String,
    public val gate: Gate,
    private val routes: Routes,
    public val log: (String) -> Unit,
) {
    public val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> log("VDK: a handler crashed: $e") },
    )
    private val conversations = ConcurrentHashMap.newKeySet<Conversation>()
    private val hellos = ConcurrentHashMap<String, Hello>()
    private val injected = ConcurrentHashMap<String, MutableSharedFlow<String>>()
    private val simulated = MutableStateFlow<Set<String>>(emptySet())

    // The shell connection that set the simulation: when it goes, the simulation goes, so none outlives the computer.
    @Volatile
    private var simulator: Caller? = null
    private val trace = MutableSharedFlow<TraceRecord>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val shellRoutes = shellRoutes()

    /** Sends [event], checked against [topic]'s type, to the callers subscribed to it now, as the shell's inject does. */
    @KitInternal
    public suspend fun inject(topic: String, event: String) {
        val stream = routes.topics[topic] ?: throw KitException(ErrorCode.INVALID_PARAMS, "Visor does not stream $topic.")
        injected(topic).emit(stream.check(event))
    }

    /** Ends the streams callers may no longer have, after a scope was taken back. */
    public fun recheck() {
        conversations.forEach { it.recheck() }
    }

    /** Ends every conversation. */
    public fun stop() {
        conversations.forEach { it.end() }
        scope.cancel()
    }

    @KitInternal
    public fun open(caller: Caller, peer: Peer): Conversation = Conversation(this, caller, peer).also { conversations += it }

    internal fun closed(conversation: Conversation) {
        conversations -= conversation
        if (conversation.caller === simulator) simulate(null, emptySet())
    }

    internal suspend fun call(caller: Caller, method: String, params: String?): String {
        if (method == Methods.HELLO.name) return hello(caller, params)
        val answer = (if (caller.shell) shellRoutes.methods[method] else null)
            ?: routes.methods[method]
            ?: throw KitException(ErrorCode.UNKNOWN_METHOD, "Visor does not answer $method.")
        allow(caller, answer.method.scope)
        return answer.run(caller, params)
    }

    internal fun events(caller: Caller, topic: String, params: String?): Flow<String> {
        shellRoutes.topics[topic]?.takeIf { caller.shell }?.let { return it.events(caller, params) }
        // JSON-RPC has no feeds of its own: there a feed is a topic of Base64 records.
        if (topic in routes.feeds) return records(caller, topic, params).map(Record::json)
        val stream = routes.topics[topic] ?: throw KitException(ErrorCode.UNKNOWN_METHOD, "Visor does not stream $topic.")
        allow(caller, stream.topic.scope)
        val fake = injected(topic)
        return if (topic in simulated.value) fake else merge(stream.events(caller, params), fake)
    }

    internal fun records(caller: Caller, feed: String, params: String?): Flow<Record> {
        val supply = routes.feeds[feed] ?: throw KitException(ErrorCode.UNKNOWN_METHOD, "Visor does not send $feed.")
        allow(caller, supply.feed.scope)
        return supply.records(caller, params)
    }

    /** The methods [caller] may call now, in the order of the catalogue, without the hello and the shell's own. */
    internal fun methodsFor(caller: Caller): List<Method<*, *>> {
        val granted = if (caller.shell) null else gate.granted(caller)
        return routes.methods.values.map { it.method }.filter { m -> m.scope == null || granted == null || m.scope in granted }
    }

    /** The scope a topic or a feed needs, or null. */
    internal fun scopeOf(name: String): Scope? = routes.topics[name]?.topic?.scope ?: routes.feeds[name]?.feed?.scope

    /** Why [caller] may no longer stream what needs [needed], or null when it still may. */
    internal fun revoked(caller: Caller, needed: Scope?): KitException? =
        if (needed != null && !caller.shell && needed !in gate.granted(caller)) {
            KitException(ErrorCode.SCOPE_DENIED, "The $needed scope was taken back from ${caller.packageName}.", scope = needed)
        } else {
            null
        }

    internal fun trace(caller: Caller, kind: TraceKind, name: String, id: Long, body: String?, startedNanos: Long? = null) {
        if (name == Shell.TRACE.name || trace.subscriptionCount.value == 0) return
        val parsed = body?.let { runCatching { KitJson.parseToJsonElement(it) }.getOrElse { JsonPrimitive(body) } }
        record(caller, kind, name, id, parsed, startedNanos)
    }

    internal fun trace(caller: Caller, kind: TraceKind, name: String, id: Long, failure: Failure, startedNanos: Long) {
        if (trace.subscriptionCount.value == 0) return
        record(caller, kind, name, id, KitJson.encodeToJsonElement(Failure.serializer(), failure), startedNanos)
    }

    private fun record(caller: Caller, kind: TraceKind, name: String, id: Long, body: JsonElement?, startedNanos: Long?) {
        val took = startedNanos?.let { (System.nanoTime() - it) / 1_000_000 }
        trace.tryEmit(TraceRecord(System.currentTimeMillis(), caller.packageName, kind, name, id, body, took))
    }

    private fun hello(caller: Caller, params: String?): String {
        val hello = decodeParams(Hello.serializer(), params, Methods.HELLO.name)
        if (!caller.shell) {
            hellos[caller.packageName] = hello
            gate.greeted(caller, hello)
        }
        val methods = listOf(Methods.HELLO.name) + routes.methods.keys + if (caller.shell) shellRoutes.methods.keys else emptySet()
        val topics = routes.topics.keys + if (caller.shell) shellRoutes.topics.keys else emptySet()
        val welcome = Welcome(KitProtocol.VERSION, visorVersion, granted(caller).toList(), methods, topics.toList(), routes.feeds.keys.toList())
        return KitJson.encodeToString(Welcome.serializer(), welcome)
    }

    private fun granted(caller: Caller): Set<Scope> =
        if (caller.shell) routes.methods.values.mapNotNullTo(LinkedHashSet()) { it.method.scope } else gate.granted(caller)

    private fun allow(caller: Caller, needed: Scope?) {
        if (needed != null && !caller.shell && needed !in gate.granted(caller)) {
            throw KitException(ErrorCode.SCOPE_DENIED, "${caller.packageName} does not hold the $needed scope.", scope = needed)
        }
    }

    private fun injected(topic: String) = injected.getOrPut(topic) { MutableSharedFlow(extraBufferCapacity = 64) }

    private fun apps(): List<AppInfo> {
        val grants = gate.grants()
        val connected = conversations.mapNotNullTo(HashSet()) { it.caller.takeUnless { c -> c.shell }?.packageName }
        return (grants.keys + connected + hellos.keys).sorted().map { app ->
            val hello = hellos[app]
            AppInfo(app, grants[app].orEmpty().toList(), app in connected, hello?.kit, hello?.protocol)
        }
    }

    private fun shellRoutes() = Routes().apply {
        answer(Shell.APPS) { _, _ -> apps() }
        answer(Shell.GRANT) { _, request -> setGranted(request.packageName) { it + request.scopes } }
        answer(Shell.REVOKE) { _, request -> setGranted(request.packageName) { it - request.scopes.toSet() } }
        answer(Shell.SIMULATE) { caller, request ->
            val unknown = request.topics.filterNot(routes.topics::containsKey)
            if (unknown.isNotEmpty()) throw KitException(ErrorCode.INVALID_PARAMS, "Visor does not stream ${unknown.joinToString()}.")
            simulate(caller, request.topics.toSet())
            request.topics
        }
        answer(Shell.INJECT) { _, request -> inject(request.topic, request.event.toString()) }
        stream(Shell.TRACE) { _, _ -> trace }
    }

    @Synchronized
    private fun simulate(by: Caller?, topics: Set<String>) {
        val before = simulated.value
        simulated.value = topics
        simulator = by.takeIf { topics.isNotEmpty() }
        val changed = (before - topics) + (topics - before)
        conversations.forEach { c -> changed.forEach(c::restart) }
    }

    private fun setGranted(app: String, change: (Set<Scope>) -> Set<Scope>): AppInfo {
        val now = change(gate.grants()[app].orEmpty())
        if (!gate.setGranted(app, now)) throw KitException(ErrorCode.INVALID_PARAMS, "No app $app is installed.")
        recheck()
        return apps().first { it.packageName == app }
    }
}

/** Where a [Conversation] sends its answers. Each send returns false once the other side is gone. */
@KitInternal
public interface Peer {
    public fun result(id: Long, result: String): Boolean

    public fun failure(id: Long, failure: Failure): Boolean

    public fun event(subscription: Long, event: String): Boolean

    public fun ended(subscription: Long, failure: Failure?): Boolean

    /** The conversation is over: let go of the transport. */
    public fun closed()
}

/** Where one feed's records go. Only the feed's own coroutine calls it, and a write may block. */
@KitInternal
public interface FeedSink {
    /** False once the reader is gone, or stopped reading for too long. */
    public fun write(header: String, payload: ByteArray): Boolean

    /** Tells the reader the feed is over, [failure] null when it simply ended, and lets go of the transport. */
    public fun end(failure: Failure?)

    /** Lets go of the transport without a word. */
    public fun close()
}

/** One caller's calls, streams and feeds, whatever the transport. */
@KitInternal
public class Conversation internal constructor(private val dispatcher: Dispatcher, public val caller: Caller, private val peer: Peer) {
    private val calls = ConcurrentHashMap<Long, Job>()
    private val streams = ConcurrentHashMap<Long, Running>()

    private class Running(val name: String, val params: String?, val feed: Boolean) {
        lateinit var job: Job

        // Set before a feed is cancelled for a scope taken back, so the feed tells its reader why.
        @Volatile
        var revoked: Failure? = null
    }

    public fun call(id: Long, method: String, params: String?) {
        val started = System.nanoTime()
        dispatcher.trace(caller, TraceKind.CALL, method, id, params)
        val job = dispatcher.scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = dispatcher.call(caller, method, params)
                dispatcher.trace(caller, TraceKind.RESULT, method, id, result, started)
                deliver { peer.result(id, result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failure = failureOf(e, method)
                dispatcher.trace(caller, TraceKind.FAILURE, method, id, failure, started)
                deliver { peer.failure(id, failure) }
            } finally {
                calls.remove(id)
            }
        }
        calls[id] = job
        job.start()
    }

    public fun cancel(id: Long) {
        calls.remove(id)?.cancel()
    }

    public fun subscribe(id: Long, topic: String, params: String?) {
        dispatcher.trace(caller, TraceKind.SUBSCRIBE, topic, id, params)
        start(id, topic, params)
    }

    /** Runs a feed into [sink] until it ends, or [unsubscribe] stops it. */
    public fun openFeed(id: Long, feed: String, params: String?, sink: FeedSink) {
        dispatcher.trace(caller, TraceKind.SUBSCRIBE, feed, id, params)
        val running = Running(feed, params, feed = true)
        // The feed's only writer, so its last word comes after its last record. Writes block: hence IO.
        running.job = dispatcher.scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            var readerGone = false
            val ending = try {
                dispatcher.records(caller, feed, params)
                    .takeWhile { record ->
                        dispatcher.trace(caller, TraceKind.EVENT, feed, id, record.header)
                        sink.write(record.header, record.payload).also { readerGone = !it }
                    }
                    .collect()
                null
            } catch (e: CancellationException) {
                running.revoked ?: run {
                    sink.close()
                    throw e
                }
            } catch (e: Exception) {
                failureOf(e, feed)
            }
            withContext(NonCancellable) {
                streams.remove(id, running)
                if (readerGone) {
                    sink.close()
                } else {
                    dispatcher.trace(caller, TraceKind.ENDED, feed, id, ending?.let { KitJson.encodeToString(Failure.serializer(), it) })
                    sink.end(ending)
                }
            }
        }
        streams[id] = running
        running.job.start()
    }

    /** Stops a stream or a feed. */
    public fun unsubscribe(id: Long) {
        val running = streams.remove(id) ?: return
        running.job.cancel()
        dispatcher.trace(caller, TraceKind.UNSUBSCRIBE, running.name, id, null)
    }

    /** Starts the streams of [topic] over, the app none the wiser: after the shell changed what is simulated. */
    internal fun restart(topic: String) {
        for ((id, running) in streams) {
            if (running.feed || running.name != topic || !streams.remove(id, running)) continue
            running.job.cancel()
            start(id, running.name, running.params)
        }
    }

    internal fun recheck() {
        for ((id, running) in streams) {
            val ending = dispatcher.revoked(caller, dispatcher.scopeOf(running.name))?.toFailure() ?: continue
            if (running.feed) {
                running.revoked = ending
                running.job.cancel()
            } else if (streams.remove(id, running)) {
                running.job.cancel()
                finish(id, running.name, ending)
            }
        }
    }

    public fun end() {
        dispatcher.closed(this)
        calls.values.forEach { it.cancel() }
        streams.values.forEach { it.job.cancel() }
        calls.clear()
        streams.clear()
        peer.closed()
    }

    private fun start(id: Long, topic: String, params: String?) {
        val running = Running(topic, params, feed = false)
        running.job = dispatcher.scope.launch(start = CoroutineStart.LAZY) {
            val ending = try {
                dispatcher.events(caller, topic, params).collect { event ->
                    dispatcher.trace(caller, TraceKind.EVENT, topic, id, event)
                    deliver { peer.event(id, event) }
                }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failureOf(e, topic)
            }
            // Whoever takes the stream out of the map tells the app it ended: this job, or recheck.
            if (streams.remove(id, running)) finish(id, topic, ending)
        }
        streams[id] = running
        running.job.start()
    }

    private fun finish(id: Long, topic: String, failure: Failure?) {
        dispatcher.trace(caller, TraceKind.ENDED, topic, id, failure?.let { KitJson.encodeToString(Failure.serializer(), it) })
        deliver { peer.ended(id, failure) }
    }

    private fun failureOf(e: Exception, name: String): Failure = when (e) {
        is KitException -> e.toFailure()
        else -> {
            dispatcher.log("VDK: ${caller.packageName} $name failed: $e")
            KitException(ErrorCode.FAILED, "$name failed in Visor: $e").toFailure()
        }
    }

    private inline fun deliver(send: () -> Boolean) {
        if (!send()) end()
    }
}
