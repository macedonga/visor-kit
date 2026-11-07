package com.macedonga.visor.kit.server

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Feed
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Topic
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException

/** The methods, topics and feeds a [Dispatcher] serves, each with its handler. Handlers fail with a [KitException]. */
@KitInternal
public class Routes {
    internal val methods = LinkedHashMap<String, Answer<*, *>>()
    internal val topics = LinkedHashMap<String, Stream<*, *>>()
    internal val feeds = LinkedHashMap<String, Supply<*, *>>()

    public fun <P, R> answer(method: Method<P, R>, handler: suspend (Caller, P) -> R) {
        require(method.name != Methods.HELLO.name) { "the dispatcher answers ${Methods.HELLO.name} itself" }
        check(methods.put(method.name, Answer(method, handler)) == null) { "${method.name} answered twice" }
    }

    /** [producer]'s flow runs while a caller subscribes; its end ends the stream. */
    public fun <P, E> stream(topic: Topic<P, E>, producer: (Caller, P) -> Flow<E>) {
        check(topic.name !in feeds && topics.put(topic.name, Stream(topic, producer)) == null) { "${topic.name} served twice" }
    }

    /** [producer]'s flow of headers and payloads runs while a caller reads the feed; its end ends the feed. */
    public fun <P, H> feed(feed: Feed<P, H>, producer: (Caller, P) -> Flow<Pair<H, ByteArray>>) {
        check(feed.name !in topics && feeds.put(feed.name, Supply(feed, producer)) == null) { "${feed.name} served twice" }
    }

    internal class Answer<P, R>(val method: Method<P, R>, private val handler: suspend (Caller, P) -> R) {
        suspend fun run(caller: Caller, params: String?): String =
            KitJson.encodeToString(method.result, handler(caller, decodeParams(method.params, params, method.name)))
    }

    internal class Stream<P, E>(val topic: Topic<P, E>, private val producer: (Caller, P) -> Flow<E>) {
        fun events(caller: Caller, params: String?): Flow<String> =
            producer(caller, decodeParams(topic.params, params, topic.name)).map { KitJson.encodeToString(topic.event, it) }

        /** [event] as this topic's JSON, or [ErrorCode.INVALID_PARAMS] when it does not fit. */
        fun check(event: String): String =
            KitJson.encodeToString(topic.event, decodeParams(topic.event, event, topic.name))
    }

    internal class Supply<P, H>(val feed: Feed<P, H>, private val producer: (Caller, P) -> Flow<Pair<H, ByteArray>>) {
        fun records(caller: Caller, params: String?): Flow<Record> =
            producer(caller, decodeParams(feed.params, params, feed.name)).map { (header, payload) ->
                Record(KitJson.encodeToString(feed.header, header), payload)
            }
    }
}

/** One record of a feed: its header as JSON, and its payload. */
internal class Record(val header: String, val payload: ByteArray) {
    /** The record as a JSON-RPC event: `{"header": ..., "payload": "<Base64>"}`. */
    fun json(): String = """{"header":$header,"payload":"${Base64.getEncoder().encodeToString(payload)}"}"""
}

internal fun <P> decodeParams(strategy: DeserializationStrategy<P>, json: String?, name: String): P = try {
    KitJson.decodeFromString(strategy, json ?: "{}")
} catch (e: SerializationException) {
    throw KitException(ErrorCode.INVALID_PARAMS, "This does not fit $name: ${e.message}")
}
