package com.macedonga.visor.vdk.cli

import com.macedonga.visor.vdk.protocol.EndedNotice
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.EventNotice
import com.macedonga.visor.vdk.protocol.Failure
import com.macedonga.visor.vdk.protocol.JsonRpc
import com.macedonga.visor.vdk.protocol.VdkJson
import com.macedonga.visor.vdk.protocol.RpcRequest
import com.macedonga.visor.vdk.protocol.RpcResponse
import com.macedonga.visor.vdk.protocol.SubscribeRequest
import java.io.IOException
import java.net.Socket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

/** A JSON-RPC connection to Visor's shell socket, one object per line. Answers come on a reader thread. */
internal class Rpc(private val socket: Socket) : AutoCloseable {
    private val out = socket.getOutputStream().bufferedWriter()
    private val ids = AtomicLong()
    private val pending = ConcurrentHashMap<Long, CompletableFuture<RpcResponse>>()
    private val streams = ConcurrentHashMap<Long, Listener>()

    @Volatile
    private var broken: CliError? = null

    interface Listener {
        fun event(event: JsonElement)

        /** [failure] is null when Visor simply ended the stream. */
        fun ended(failure: Failure?)
    }

    init {
        Thread(::read, "visor-rpc").apply { isDaemon = true }.start()
    }

    fun call(method: String, params: JsonElement? = null): JsonElement {
        val response = request(ids.incrementAndGet(), method, params)
        response.error?.let { throw CliError(describe(it.data ?: Failure(ErrorCode.FAILED, it.message))) }
        return response.result ?: JsonNull
    }

    /** Subscribes [listener] to [topic]. The listener is in place before the request goes, so no event is missed. */
    fun subscribe(topic: String, params: JsonElement?, listener: Listener) {
        val id = ids.incrementAndGet()
        streams[id] = listener
        val response = request(id, JsonRpc.SUBSCRIBE, VdkJson.encodeToJsonElement(SubscribeRequest.serializer(), SubscribeRequest(topic, params)))
        response.error?.let {
            streams.remove(id)
            throw CliError(describe(it.data ?: Failure(ErrorCode.FAILED, it.message)))
        }
    }

    override fun close() = socket.close()

    private fun request(id: Long, method: String, params: JsonElement?): RpcResponse {
        broken?.let { throw it }
        val answer = CompletableFuture<RpcResponse>()
        pending[id] = answer
        try {
            synchronized(out) {
                out.write(VdkJson.encodeToString(RpcRequest.serializer(), RpcRequest(id = id, method = method, params = params)))
                out.write("\n")
                out.flush()
            }
            return answer.get()
        } catch (e: IOException) {
            throw CliError("The connection to Visor broke: ${e.message}")
        } catch (e: ExecutionException) {
            throw e.cause as? CliError ?: CliError("The connection to Visor broke: ${e.cause}")
        } finally {
            pending.remove(id)
        }
    }

    private fun read() {
        val ending = try {
            socket.getInputStream().bufferedReader().forEachLine(::dispatch)
            CliError(NOT_LISTENING)
        } catch (e: IOException) {
            CliError("The connection to Visor broke: ${e.message}")
        }
        broken = ending
        pending.values.forEach { it.completeExceptionally(ending) }
        streams.values.forEach { it.ended(Failure(ErrorCode.VISOR_GONE, ending.message.orEmpty())) }
        streams.clear()
    }

    private fun dispatch(line: String) {
        val message = VdkJson.parseToJsonElement(line).jsonObject
        if ("method" !in message) {
            val response = VdkJson.decodeFromJsonElement(RpcResponse.serializer(), message)
            response.id?.let { pending[it]?.complete(response) }
            return
        }
        val notice = VdkJson.decodeFromJsonElement(RpcRequest.serializer(), message)
        val params = notice.params ?: return
        when (notice.method) {
            JsonRpc.EVENT -> VdkJson.decodeFromJsonElement(EventNotice.serializer(), params).let { streams[it.subscription]?.event(it.event) }
            JsonRpc.ENDED -> VdkJson.decodeFromJsonElement(EndedNotice.serializer(), params).let { streams.remove(it.subscription)?.ended(it.failure) }
        }
    }

    companion object {
        // adb accepts the forwarded connection even when nothing listens on the phone, then closes it.
        const val NOT_LISTENING = "Visor is not listening on the phone. Open Visor and turn on Developer mode in Settings, Advanced."

        fun describe(failure: Failure): String = buildString {
            append(failure.code).append(": ").append(failure.message)
            failure.reason?.let { append(" (").append(it).append(")") }
        }
    }
}
