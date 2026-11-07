package com.macedonga.visor.kit.server

import android.net.LocalServerSocket
import android.net.LocalSocket
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
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.SubscribeRequest
import com.macedonga.visor.kit.protocol.Subscribed
import com.macedonga.visor.kit.protocol.UnsubscribeRequest
import com.macedonga.visor.kit.toFailure
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive

/**
 * The kit over JSON-RPC, one object per line, on the abstract socket [Shell.SOCKET], for the `visor` command on a
 * computer. Only the adb shell gets in: other apps can reach an abstract socket too, so each connection's uid is
 * checked, as Chrome does for its remote debugging socket.
 */
@KitInternal
public class ShellServer(private val dispatcher: Dispatcher) {
    private var server: LocalServerSocket? = null
    private var accepting: Job? = null
    private val sockets = ConcurrentHashMap.newKeySet<LocalSocket>()

    /** Listens until [stop]. Fails with an IOException when another process holds the socket name. */
    @Synchronized
    public fun start() {
        if (server != null) return
        val server = LocalServerSocket(Shell.SOCKET)
        this.server = server
        accepting = dispatcher.scope.launch(Dispatchers.IO) {
            while (isActive) {
                val socket = try {
                    server.accept()
                } catch (e: IOException) {
                    // stop() closed the server.
                    break
                }
                val uid = socket.peerCredentials.uid
                if (uid != SHELL_UID && uid != ROOT_UID) {
                    dispatcher.log("Kit: turned away uid $uid on the shell socket")
                    socket.close()
                    continue
                }
                sockets += socket
                launch { serve(socket, uid) }
            }
        }
    }

    @Synchronized
    public fun stop() {
        accepting?.cancel()
        accepting = null
        server?.close()
        server = null
        sockets.forEach(LocalSocket::close)
        sockets.clear()
    }

    private suspend fun serve(socket: LocalSocket, uid: Int) {
        val lines = Channel<String>(Channel.UNLIMITED)
        val writer = dispatcher.scope.launch(Dispatchers.IO) {
            val out = socket.outputStream.bufferedWriter()
            try {
                for (line in lines) {
                    out.write(line)
                    out.write("\n")
                    out.flush()
                }
            } catch (e: IOException) {
                socket.close()
            }
        }
        val conversation = dispatcher.open(Caller(uid, SHELL, shell = true), LinePeer(lines, socket))
        try {
            socket.inputStream.bufferedReader().useLines { input -> input.forEach { handle(it, conversation, lines) } }
        } catch (e: IOException) {
            // The computer went away, or stop() closed the socket.
        } finally {
            conversation.end()
            lines.close()
            writer.join()
            sockets -= socket
        }
    }

    private fun handle(line: String, conversation: Conversation, lines: Channel<String>) {
        if (line.isBlank()) return
        val request = try {
            KitJson.decodeFromString(RpcRequest.serializer(), line)
        } catch (e: SerializationException) {
            val message = JsonPrimitive("Not a JSON-RPC request: ${e.message}")
            lines.trySend("""{"jsonrpc":"2.0","id":null,"error":{"code":${JsonRpc.PARSE_ERROR},"message":$message}}""")
            return
        }
        // The kit defines no notifications from the computer.
        val id = request.id ?: return
        val params = request.params?.toString()
        try {
            when (request.method) {
                JsonRpc.SUBSCRIBE -> {
                    val subscribe = decodeParams(SubscribeRequest.serializer(), params, JsonRpc.SUBSCRIBE)
                    // The answer goes out first, so the computer knows the id before the first event.
                    lines.trySend(resultLine(id, KitJson.encodeToString(Subscribed.serializer(), Subscribed(id))))
                    conversation.subscribe(id, subscribe.topic, subscribe.params?.toString())
                }
                JsonRpc.UNSUBSCRIBE -> {
                    conversation.unsubscribe(decodeParams(UnsubscribeRequest.serializer(), params, JsonRpc.UNSUBSCRIBE).subscription)
                    lines.trySend(resultLine(id, "{}"))
                }
                JsonRpc.CANCEL -> {
                    conversation.cancel(decodeParams(CancelRequest.serializer(), params, JsonRpc.CANCEL).id)
                    lines.trySend(resultLine(id, "{}"))
                }
                else -> conversation.call(id, request.method, params)
            }
        } catch (e: KitException) {
            lines.trySend(errorLine(id, e.toFailure()))
        }
    }

    private class LinePeer(private val lines: Channel<String>, private val socket: LocalSocket) : Peer {
        override fun result(id: Long, result: String) = lines.trySend(resultLine(id, result)).isSuccess

        override fun failure(id: Long, failure: Failure) = lines.trySend(errorLine(id, failure)).isSuccess

        override fun event(subscription: Long, event: String) =
            lines.trySend("""{"jsonrpc":"2.0","method":"${JsonRpc.EVENT}","params":{"subscription":$subscription,"event":$event}}""").isSuccess

        override fun ended(subscription: Long, failure: Failure?): Boolean {
            val notice = KitJson.encodeToString(EndedNotice.serializer(), EndedNotice(subscription, failure))
            return lines.trySend("""{"jsonrpc":"2.0","method":"${JsonRpc.ENDED}","params":$notice}""").isSuccess
        }

        override fun closed() {
            lines.close()
            socket.close()
        }
    }

    private companion object {
        // android.os.Process.SHELL_UID and ROOT_UID, which the SDK hides.
        const val SHELL_UID = 2000
        const val ROOT_UID = 0
        const val SHELL = "shell"

        // The kit's JSON is a complete value already, so the envelope is written around it rather than parsed again.
        fun resultLine(id: Long, json: String) = """{"jsonrpc":"2.0","id":$id,"result":$json}"""

        fun errorLine(id: Long, failure: Failure): String {
            val code = when (failure.code) {
                ErrorCode.UNKNOWN_METHOD -> JsonRpc.METHOD_NOT_FOUND
                ErrorCode.INVALID_PARAMS -> JsonRpc.INVALID_PARAMS
                else -> JsonRpc.FAILED
            }
            return KitJson.encodeToString(RpcResponse.serializer(), RpcResponse(id = id, error = RpcError(code, failure.message, failure)))
        }
    }
}
