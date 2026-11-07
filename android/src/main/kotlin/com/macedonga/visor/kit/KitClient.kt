package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Topic
import com.macedonga.visor.kit.protocol.Welcome
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException

/**
 * The session with Visor behind a [Visor]: it opens one each time the service comes up, says hello, runs calls, and
 * subscribes every open stream again after Visor restarts.
 *
 * [lock] guards [endpoint], [session] and the moves of [state], so a stream registered while a session opens is
 * subscribed exactly once, and a session opened on an endpoint that has since gone away is never used. Visor's
 * answers are handled on the thread they arrive on, a binder thread for the real Visor, in the order Visor sent
 * them.
 */
internal class KitClient(private val binding: Binding) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ids = AtomicLong()
    private val lock = Any()
    private var endpoint: Endpoint? = null
    private var session: Line? = null
    private val calls = ConcurrentHashMap<Long, CompletableDeferred<String>>()
    private val streams = ConcurrentHashMap<Long, Stream>()

    private val _state = MutableStateFlow<VisorState>(VisorState.Connecting)
    val state: StateFlow<VisorState> = _state.asStateFlow()

    private class Stream(val topic: String, val params: String, val events: ProducerScope<String>) {
        fun end(error: KitException?) {
            events.close(error)
        }
    }

    private val answers = object : Answers {
        override fun result(id: Long, result: String) {
            calls[id]?.complete(result)
        }

        override fun failure(id: Long, failure: String) {
            calls[id]?.completeExceptionally(failureOf(failure))
        }

        override fun event(subscription: Long, event: String) {
            streams[subscription]?.events?.trySend(event)
        }

        override fun ended(subscription: Long, failure: String?) {
            streams.remove(subscription)?.end(failure?.let(::failureOf))
        }
    }

    fun start() = binding.start(onEndpoint = ::onEndpoint, onDead = ::finish)

    fun close() = finish(null)

    suspend fun <P, R> call(method: Method<P, R>, params: P): R {
        val (session, welcome) = awaitOpen()
        if (method.name !in welcome.methods) {
            throw KitException(ErrorCode.VISOR_TOO_OLD, "Visor ${welcome.visor} does not answer ${method.name}. Update Visor.")
        }
        return callOn(session, method, params)
    }

    fun <P, E> subscribe(topic: Topic<P, E>, params: P): Flow<E> = callbackFlow {
        val id = ids.incrementAndGet()
        register(id, Stream(topic.name, KitJson.encodeToString(topic.params, params), this))
        awaitClose { unregister(id) }
    }.buffer(Channel.UNLIMITED).map { decode(topic.event, it, topic.name) }

    /** Says hello again, for the scopes the app holds now. */
    suspend fun hello(): Welcome {
        val (session, _) = awaitOpen()
        val welcome = callOn(session, Methods.HELLO, Hello(KitProtocol.VERSION, KIT_VERSION))
        synchronized(lock) { if (this.session === session) _state.value = VisorState.Open(welcome) }
        return welcome
    }

    private fun onEndpoint(endpoint: Endpoint?) {
        if (endpoint == null) return lost()
        synchronized(lock) { this.endpoint = endpoint }
        scope.launch { open(endpoint) }
    }

    private suspend fun open(endpoint: Endpoint) {
        val session = try {
            withContext(Dispatchers.IO) { endpoint.open(answers) }
        } catch (e: Gone) {
            // Visor died while opening; the binding brings the next endpoint.
            return
        } ?: return finish(KitException(ErrorCode.FAILED, "Visor refused this app a session."))
        synchronized(lock) {
            if (_state.value is VisorState.Closed || this.endpoint !== endpoint) return
            this.session = session
        }
        val welcome = try {
            callOn(session, Methods.HELLO, Hello(KitProtocol.VERSION, KIT_VERSION))
        } catch (e: KitException) {
            if (e.code == ErrorCode.VISOR_GONE) return
            return finish(e)
        }
        val tooOld = mutableListOf<Stream>()
        synchronized(lock) {
            if (this.session !== session) return
            _state.value = VisorState.Open(welcome)
            for ((id, stream) in streams) {
                if (stream.topic in welcome.topics) {
                    send(session, id, stream)
                } else if (streams.remove(id) != null) {
                    tooOld += stream
                }
            }
        }
        tooOld.forEach { it.end(tooOld(welcome, it.topic)) }
    }

    private fun lost() {
        synchronized(lock) {
            if (_state.value is VisorState.Closed) return
            endpoint = null
            session = null
            _state.value = VisorState.Connecting
        }
        failCalls(KitException(ErrorCode.VISOR_GONE, "The connection to Visor broke."))
    }

    private fun finish(error: KitException?) {
        val old = synchronized(lock) {
            if (_state.value is VisorState.Closed) return
            _state.value = VisorState.Closed(error)
            endpoint = null
            session.also { session = null }
        }
        binding.stop()
        try {
            old?.close()
        } catch (e: Gone) {
            // Visor is gone already, and the session with it.
        }
        val reason = error ?: KitException(ErrorCode.CLOSED, "The app closed the connection to Visor.")
        failCalls(reason)
        streams.values.forEach { it.end(reason) }
        streams.clear()
        scope.cancel()
    }

    private fun failCalls(error: KitException) {
        calls.values.forEach { it.completeExceptionally(error) }
    }

    private suspend fun awaitOpen(): Pair<Line, Welcome> {
        while (true) {
            when (val now = state.first { it !is VisorState.Connecting }) {
                is VisorState.Closed -> throw now.error ?: KitException(ErrorCode.CLOSED, "The app closed the connection to Visor.")
                is VisorState.Open -> synchronized(lock) { session }?.let { return it to now.welcome }
                VisorState.Connecting -> Unit
            }
        }
    }

    private suspend fun <P, R> callOn(session: Line, method: Method<P, R>, params: P): R {
        val id = ids.incrementAndGet()
        val answer = CompletableDeferred<String>()
        calls[id] = answer
        try {
            session.call(id, method.name, KitJson.encodeToString(method.params, params))
            return decode(method.result, answer.await(), method.name)
        } catch (e: Gone) {
            throw KitException(ErrorCode.VISOR_GONE, "The connection to Visor broke during ${method.name}.", cause = e)
        } catch (e: CancellationException) {
            if (calls.containsKey(id)) {
                try {
                    session.cancel(id)
                } catch (gone: Gone) {
                    // Visor is gone, and the call with it.
                }
            }
            throw e
        } finally {
            calls.remove(id)
        }
    }

    private fun register(id: Long, stream: Stream) {
        val ended: KitException? = synchronized(lock) {
            when (val now = _state.value) {
                is VisorState.Closed -> now.error ?: KitException(ErrorCode.CLOSED, "The app closed the connection to Visor.")
                is VisorState.Open -> if (stream.topic !in now.welcome.topics) {
                    tooOld(now.welcome, stream.topic)
                } else {
                    streams[id] = stream
                    session?.let { send(it, id, stream) }
                    null
                }
                VisorState.Connecting -> {
                    streams[id] = stream
                    null
                }
            }
        }
        ended?.let(stream::end)
    }

    private fun unregister(id: Long) {
        val session = synchronized(lock) {
            streams.remove(id) ?: return
            session
        }
        try {
            session?.unsubscribe(id)
        } catch (e: Gone) {
            // Visor is gone, and the stream with it.
        }
    }

    private fun send(session: Line, id: Long, stream: Stream) {
        try {
            session.subscribe(id, stream.topic, stream.params)
        } catch (e: Gone) {
            // The binding reports the loss, and the stream is subscribed again on the next session.
        }
    }

    private fun tooOld(welcome: Welcome, name: String) =
        KitException(ErrorCode.VISOR_TOO_OLD, "Visor ${welcome.visor} does not stream $name. Update Visor.")

    private fun failureOf(json: String): KitException = try {
        KitJson.decodeFromString(Failure.serializer(), json).toException()
    } catch (e: SerializationException) {
        KitException(ErrorCode.FAILED, "Visor sent a failure this VDK cannot read: $json", cause = e)
    }

    private fun <T> decode(strategy: DeserializationStrategy<T>, json: String, name: String): T = try {
        KitJson.decodeFromString(strategy, json)
    } catch (e: SerializationException) {
        throw KitException(ErrorCode.FAILED, "Visor's answer to $name does not fit this VDK: $json", cause = e)
    }
}
