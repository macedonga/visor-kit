package com.macedonga.visor.vdk.server

import com.macedonga.visor.vdk.VdkException
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.VdkJson
import com.macedonga.visor.vdk.protocol.Method
import com.macedonga.visor.vdk.protocol.Methods
import com.macedonga.visor.vdk.protocol.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException

/** The methods and topics a [Dispatcher] answers, each with its handler. Handlers fail with a [VdkException]. */
@VdkInternal
public class Routes {
    internal val methods = LinkedHashMap<String, Answer<*, *>>()
    internal val topics = LinkedHashMap<String, Stream<*, *>>()

    public fun <P, R> answer(method: Method<P, R>, handler: suspend (Caller, P) -> R) {
        require(method.name != Methods.HELLO.name) { "the dispatcher answers ${Methods.HELLO.name} itself" }
        check(methods.put(method.name, Answer(method, handler)) == null) { "${method.name} answered twice" }
    }

    /** [producer]'s flow runs while a caller subscribes; its end ends the stream. */
    public fun <P, E> stream(topic: Topic<P, E>, producer: (Caller, P) -> Flow<E>) {
        check(topics.put(topic.name, Stream(topic, producer)) == null) { "${topic.name} streamed twice" }
    }

    internal class Answer<P, R>(val method: Method<P, R>, private val handler: suspend (Caller, P) -> R) {
        suspend fun run(caller: Caller, params: String?): String =
            VdkJson.encodeToString(method.result, handler(caller, decodeParams(method.params, params, method.name)))
    }

    internal class Stream<P, E>(val topic: Topic<P, E>, private val producer: (Caller, P) -> Flow<E>) {
        fun events(caller: Caller, params: String?): Flow<String> =
            producer(caller, decodeParams(topic.params, params, topic.name)).map { VdkJson.encodeToString(topic.event, it) }

        /** [event] as this topic's JSON, or [ErrorCode.INVALID_PARAMS] when it does not fit. */
        fun check(event: String): String =
            VdkJson.encodeToString(topic.event, decodeParams(topic.event, event, topic.name))
    }
}

internal fun <P> decodeParams(strategy: DeserializationStrategy<P>, json: String?, name: String): P = try {
    VdkJson.decodeFromString(strategy, json ?: "{}")
} catch (e: SerializationException) {
    throw VdkException(ErrorCode.INVALID_PARAMS, "This does not fit $name: ${e.message}")
}
