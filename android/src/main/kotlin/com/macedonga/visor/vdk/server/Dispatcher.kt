package com.macedonga.visor.vdk.server

import com.macedonga.visor.vdk.VdkException
import com.macedonga.visor.vdk.protocol.AppInfo
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.Failure
import com.macedonga.visor.vdk.protocol.Hello
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.VdkJson
import com.macedonga.visor.vdk.protocol.VdkProtocol
import com.macedonga.visor.vdk.protocol.Methods
import com.macedonga.visor.vdk.protocol.Scope
import com.macedonga.visor.vdk.protocol.Shell
import com.macedonga.visor.vdk.protocol.TraceKind
import com.macedonga.visor.vdk.protocol.TraceRecord
import com.macedonga.visor.vdk.protocol.Welcome
import com.macedonga.visor.vdk.toFailure
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Visor's side of the VDK, whatever the transport: the scope checks, [Methods.HELLO], the shell's own methods, the
 * injected and simulated events, and the trace. [VdkServer] carries it over binder to apps, [ShellServer] over a
 * socket to adb.
 */
@VdkInternal
public class Dispatcher(
    private val visorVersion: String,
    internal val gate: Gate,
    private val routes: Routes,
    internal val log: (String) -> Unit,
) {
    internal val scope = CoroutineScope(
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

    /** Ends the streams callers may no longer have, after a scope was taken back. */
    public fun recheck() {
        conversations.forEach { it.recheck() }
    }

    /** Ends every conversation. */
    public fun stop() {
        conversations.forEach { it.end() }
        scope.cancel()
    }

    internal fun open(caller: Caller, peer: Peer): Conversation = Conversation(this, caller, peer).also { conversations += it }

    internal fun closed(conversation: Conversation) {
        conversations -= conversation
        if (conversation.caller === simulator) simulate(null, emptySet())
    }

    internal suspend fun call(caller: Caller, method: String, params: String?): String {
        if (method == Methods.HELLO.name) return hello(caller, params)
        val answer = (if (caller.shell) shellRoutes.methods[method] else null)
            ?: routes.methods[method]
            ?: throw VdkException(ErrorCode.UNKNOWN_METHOD, "Visor does not answer $method.")
        allow(caller, answer.method.scope)
        return answer.run(caller, params)
    }

    internal fun events(caller: Caller, topic: String, params: String?): Flow<String> {
        shellRoutes.topics[topic]?.takeIf { caller.shell }?.let { return it.events(caller, params) }
        val stream = routes.topics[topic] ?: throw VdkException(ErrorCode.UNKNOWN_METHOD, "Visor does not stream $topic.")
        allow(caller, stream.topic.scope)
        val fake = injected(topic)
        return if (topic in simulated.value) fake else merge(stream.events(caller, params), fake)
    }

    /** The scope [topic] needs, or null. */
    internal fun scopeOf(topic: String): Scope? = routes.topics[topic]?.topic?.scope

    /** Why [caller] may no longer stream what needs [needed], or null when it still may. */
    internal fun revoked(caller: Caller, needed: Scope?): VdkException? =
        if (needed != null && !caller.shell && needed !in gate.granted(caller)) {
            VdkException(ErrorCode.SCOPE_DENIED, "The $needed scope was taken back from ${caller.packageName}.", scope = needed)
        } else {
            null
        }

    internal fun trace(caller: Caller, kind: TraceKind, name: String, id: Long, body: String?, startedNanos: Long? = null) {
        if (name == Shell.TRACE.name || trace.subscriptionCount.value == 0) return
        val parsed = body?.let { runCatching { VdkJson.parseToJsonElement(it) }.getOrElse { JsonPrimitive(body) } }
        record(caller, kind, name, id, parsed, startedNanos)
    }

    internal fun trace(caller: Caller, kind: TraceKind, name: String, id: Long, failure: Failure, startedNanos: Long) {
        if (trace.subscriptionCount.value == 0) return
        record(caller, kind, name, id, VdkJson.encodeToJsonElement(Failure.serializer(), failure), startedNanos)
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
        val welcome = Welcome(VdkProtocol.VERSION, visorVersion, granted(caller).toList(), methods, topics.toList())
        return VdkJson.encodeToString(Welcome.serializer(), welcome)
    }

    private fun granted(caller: Caller): Set<Scope> =
        if (caller.shell) routes.methods.values.mapNotNullTo(LinkedHashSet()) { it.method.scope } else gate.granted(caller)

    private fun allow(caller: Caller, needed: Scope?) {
        if (needed != null && !caller.shell && needed !in gate.granted(caller)) {
            throw VdkException(ErrorCode.SCOPE_DENIED, "${caller.packageName} does not hold the $needed scope.", scope = needed)
        }
    }

    private fun injected(topic: String) = injected.getOrPut(topic) { MutableSharedFlow(extraBufferCapacity = 64) }

    private fun apps(): List<AppInfo> {
        val grants = gate.grants()
        val connected = conversations.mapNotNullTo(HashSet()) { it.caller.takeUnless { c -> c.shell }?.packageName }
        return (grants.keys + connected + hellos.keys).sorted().map { app ->
            val hello = hellos[app]
            AppInfo(app, grants[app].orEmpty().toList(), app in connected, hello?.vdk, hello?.protocol)
        }
    }

    private fun shellRoutes() = Routes().apply {
        answer(Shell.APPS) { _, _ -> apps() }
        answer(Shell.GRANT) { _, request -> setGranted(request.packageName) { it + request.scopes } }
        answer(Shell.REVOKE) { _, request -> setGranted(request.packageName) { it - request.scopes.toSet() } }
        answer(Shell.SIMULATE) { caller, request ->
            val unknown = request.topics.filterNot(routes.topics::containsKey)
            if (unknown.isNotEmpty()) throw VdkException(ErrorCode.INVALID_PARAMS, "Visor does not stream ${unknown.joinToString()}.")
            simulate(caller, request.topics.toSet())
            request.topics
        }
        answer(Shell.INJECT) { _, request ->
            val stream = routes.topics[request.topic]
                ?: throw VdkException(ErrorCode.INVALID_PARAMS, "Visor does not stream ${request.topic}.")
            injected(request.topic).emit(stream.check(request.event.toString()))
        }
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
        if (!gate.setGranted(app, now)) throw VdkException(ErrorCode.INVALID_PARAMS, "No app $app is installed.")
        recheck()
        return apps().first { it.packageName == app }
    }
}

/** Where a [Conversation] sends its answers. Each send returns false once the other side is gone. */
internal interface Peer {
    fun result(id: Long, result: String): Boolean

    fun failure(id: Long, failure: Failure): Boolean

    fun event(subscription: Long, event: String): Boolean

    fun ended(subscription: Long, failure: Failure?): Boolean

    /** The conversation is over: let go of the transport. */
    fun closed()
}

/** One caller's calls and streams, whatever the transport. */
internal class Conversation(private val dispatcher: Dispatcher, val caller: Caller, private val peer: Peer) {
    private val calls = ConcurrentHashMap<Long, Job>()
    private val streams = ConcurrentHashMap<Long, Running>()

    private class Running(val topic: String, val params: String?, val job: Job)

    fun call(id: Long, method: String, params: String?) {
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

    fun cancel(id: Long) {
        calls.remove(id)?.cancel()
    }

    fun subscribe(id: Long, topic: String, params: String?) {
        dispatcher.trace(caller, TraceKind.SUBSCRIBE, topic, id, params)
        start(id, topic, params)
    }

    fun unsubscribe(id: Long) {
        val running = streams.remove(id) ?: return
        running.job.cancel()
        dispatcher.trace(caller, TraceKind.UNSUBSCRIBE, running.topic, id, null)
    }

    /** Starts the streams of [topic] over, the app none the wiser: after the shell changed what is simulated. */
    fun restart(topic: String) {
        for ((id, running) in streams) {
            if (running.topic != topic || !streams.remove(id, running)) continue
            running.job.cancel()
            start(id, running.topic, running.params)
        }
    }

    fun recheck() {
        for ((id, running) in streams) {
            val ending = dispatcher.revoked(caller, dispatcher.scopeOf(running.topic)) ?: continue
            if (streams.remove(id, running)) {
                running.job.cancel()
                finish(id, running.topic, ending.toFailure())
            }
        }
    }

    fun end() {
        dispatcher.closed(this)
        calls.values.forEach { it.cancel() }
        streams.values.forEach { it.job.cancel() }
        calls.clear()
        streams.clear()
        peer.closed()
    }

    private fun start(id: Long, topic: String, params: String?) {
        val job = dispatcher.scope.launch(start = CoroutineStart.LAZY) {
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
            val running = streams[id]
            if (running?.job === coroutineContext.job && streams.remove(id, running)) finish(id, topic, ending)
        }
        streams[id] = Running(topic, params, job)
        job.start()
    }

    private fun finish(id: Long, topic: String, failure: Failure?) {
        dispatcher.trace(caller, TraceKind.ENDED, topic, id, failure?.let { VdkJson.encodeToString(Failure.serializer(), it) })
        deliver { peer.ended(id, failure) }
    }

    private fun failureOf(e: Exception, name: String): Failure = when (e) {
        is VdkException -> e.toFailure()
        else -> {
            dispatcher.log("VDK: ${caller.packageName} $name failed: $e")
            VdkException(ErrorCode.FAILED, "$name failed in Visor: $e").toFailure()
        }
    }

    private inline fun deliver(send: () -> Boolean) {
        if (!send()) end()
    }
}
