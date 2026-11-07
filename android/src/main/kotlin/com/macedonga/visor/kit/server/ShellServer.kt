package com.macedonga.visor.kit.server

import android.net.LocalServerSocket
import android.net.LocalSocket
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.Shell
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
                    dispatcher.log("VDK: turned away uid $uid on the shell socket")
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
        val session = JsonRpcSession(
            dispatcher, Caller(uid, SHELL, shell = true),
            send = { lines.trySend(it).isSuccess },
            close = {
                lines.close()
                socket.close()
            },
        )
        try {
            socket.inputStream.bufferedReader().useLines { input -> input.forEach(session::receive) }
        } catch (e: IOException) {
            // The computer went away, or stop() closed the socket.
        } finally {
            session.end()
            lines.close()
            writer.join()
            sockets -= socket
        }
    }

    private companion object {
        // android.os.Process.SHELL_UID and ROOT_UID, which the SDK hides.
        const val SHELL_UID = 2000
        const val ROOT_UID = 0
        const val SHELL = "shell"
    }
}
