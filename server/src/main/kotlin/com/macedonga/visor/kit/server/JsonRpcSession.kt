package com.macedonga.visor.kit.server

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.CancelRequest
import com.macedonga.visor.kit.protocol.EndedNotice
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.JsonRpc
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.RpcError
import com.macedonga.visor.kit.protocol.RpcRequest
import com.macedonga.visor.kit.protocol.RpcResponse
import com.macedonga.visor.kit.protocol.SubscribeRequest
import com.macedonga.visor.kit.protocol.Subscribed
import com.macedonga.visor.kit.protocol.UnsubscribeRequest
import com.macedonga.visor.kit.toFailure
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive

/**
 * One JSON-RPC connection to the kit, whatever carries its messages: lines on the adb socket, frames on a WebSocket.
 * [send] puts one message on the way out and returns false once the other side is gone; [close] lets go of the
 * transport when the session ends.
 */
@KitInternal
public class JsonRpcSession(dispatcher: Dispatcher, caller: Caller, private val send: (String) -> Boolean, close: () -> Unit) {
    private val conversation = dispatcher.open(caller, MessagePeer(send, close))

    /** Handles one message from the other side. */
    public fun receive(message: String) {
        if (message.isBlank()) return
        val request = try {
            KitJson.decodeFromString(RpcRequest.serializer(), message)
        } catch (e: SerializationException) {
            val text = JsonPrimitive("Not a JSON-RPC request: ${e.message}")
            send("""{"jsonrpc":"2.0","id":null,"error":{"code":${JsonRpc.PARSE_ERROR},"message":$text}}""")
            return
        }
        // The kit defines no notifications to Visor.
        val id = request.id ?: return
        val params = request.params?.toString()
        try {
            when (request.method) {
                JsonRpc.SUBSCRIBE -> {
                    val subscribe = decodeParams(SubscribeRequest.serializer(), params, JsonRpc.SUBSCRIBE)
                    // The answer goes out first, so the other side knows the id before the first event.
                    send(resultOf(id, KitJson.encodeToString(Subscribed.serializer(), Subscribed(id))))
                    conversation.subscribe(id, subscribe.topic, subscribe.params?.toString())
                }
                JsonRpc.UNSUBSCRIBE -> {
                    conversation.unsubscribe(decodeParams(UnsubscribeRequest.serializer(), params, JsonRpc.UNSUBSCRIBE).subscription)
                    send(resultOf(id, "{}"))
                }
                JsonRpc.CANCEL -> {
                    conversation.cancel(decodeParams(CancelRequest.serializer(), params, JsonRpc.CANCEL).id)
                    send(resultOf(id, "{}"))
                }
                else -> conversation.call(id, request.method, params)
            }
        } catch (e: KitException) {
            send(errorOf(id, e.toFailure()))
        }
    }

    /** Ends the session: its calls and streams stop. */
    public fun end() {
        conversation.end()
    }

    private class MessagePeer(private val send: (String) -> Boolean, private val close: () -> Unit) : Peer {
        override fun result(id: Long, result: String) = send(resultOf(id, result))

        override fun failure(id: Long, failure: Failure) = send(errorOf(id, failure))

        override fun event(subscription: Long, event: String) =
            send("""{"jsonrpc":"2.0","method":"${JsonRpc.EVENT}","params":{"subscription":$subscription,"event":$event}}""")

        override fun ended(subscription: Long, failure: Failure?): Boolean {
            val notice = KitJson.encodeToString(EndedNotice.serializer(), EndedNotice(subscription, failure))
            return send("""{"jsonrpc":"2.0","method":"${JsonRpc.ENDED}","params":$notice}""")
        }

        override fun closed() = close()
    }

    internal companion object {
        // The kit's JSON is a complete value already, so the envelope is written around it rather than parsed again.
        fun resultOf(id: Long, json: String) = """{"jsonrpc":"2.0","id":$id,"result":$json}"""

        fun errorOf(id: Long?, failure: Failure): String {
            val code = when (failure.code) {
                ErrorCode.UNKNOWN_METHOD -> JsonRpc.METHOD_NOT_FOUND
                ErrorCode.INVALID_PARAMS -> JsonRpc.INVALID_PARAMS
                else -> JsonRpc.FAILED
            }
            return KitJson.encodeToString(RpcResponse.serializer(), RpcResponse(id = id, error = RpcError(code, failure.message, failure)))
        }
    }
}
