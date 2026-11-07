package com.macedonga.visor.kit.server

import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructTimeval
import com.macedonga.visor.kit.ipc.IKitClient
import com.macedonga.visor.kit.ipc.IKitService
import com.macedonga.visor.kit.ipc.IKitSession
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.IOException

/** The VDK over binder, for apps: what Visor's VDK service returns from onBind. */
@KitInternal
public class KitServer(private val dispatcher: Dispatcher) : IKitService.Stub() {
    override fun open(client: IKitClient): IKitSession? {
        val caller = dispatcher.gate.admit(Binder.getCallingUid()) ?: return null
        val session = BinderSession(caller, client)
        try {
            client.asBinder().linkToDeath(session, 0)
        } catch (e: RemoteException) {
            session.conversation.end()
            return null
        }
        return session
    }

    private inner class BinderSession(private val caller: Caller, private val client: IKitClient) :
        IKitSession.Stub(), IBinder.DeathRecipient, Peer {
        val conversation = dispatcher.open(caller, this)

        override fun call(id: Long, method: String, params: String?) {
            if (fromCaller()) conversation.call(id, method, params)
        }

        override fun cancel(id: Long) {
            if (fromCaller()) conversation.cancel(id)
        }

        override fun subscribe(id: Long, topic: String, params: String?) {
            if (fromCaller()) conversation.subscribe(id, topic, params)
        }

        override fun unsubscribe(id: Long) {
            if (fromCaller()) conversation.unsubscribe(id)
        }

        override fun close() {
            if (fromCaller()) conversation.end()
        }

        override fun openFeed(id: Long, feed: String, params: String?, sink: ParcelFileDescriptor) {
            if (fromCaller()) conversation.openFeed(id, feed, params, SocketSink(sink)) else sink.close()
        }

        override fun binderDied() = conversation.end()

        override fun result(id: Long, result: String) = sent { client.result(id, result) }

        override fun failure(id: Long, failure: Failure) =
            sent { client.failure(id, KitJson.encodeToString(Failure.serializer(), failure)) }

        override fun event(subscription: Long, event: String) = sent { client.event(subscription, event) }

        override fun ended(subscription: Long, failure: Failure?) =
            sent { client.ended(subscription, failure?.let { KitJson.encodeToString(Failure.serializer(), it) }) }

        override fun closed() {
            client.asBinder().unlinkToDeath(this, 0)
        }

        // Only the app that opened the session may use it, even if its binder reached another app.
        private fun fromCaller(): Boolean = Binder.getCallingUid() == caller.uid

        private inline fun sent(send: () -> Unit): Boolean = try {
            send()
            true
        } catch (e: RemoteException) {
            false
        }
    }
}

/** A feed's records into the app's end of a socket pair, framed as [KitProtocol.FEED_END] says. */
private class SocketSink(private val socket: ParcelFileDescriptor) : FeedSink {
    private val out = DataOutputStream(BufferedOutputStream(FileOutputStream(socket.fileDescriptor), BUFFER))

    init {
        // A write the app does not take for this long ends the feed: an app that stopped reading, or that Android
        // froze, must not keep the glasses filming. Visor needs Android 12, so the check only quiets lint.
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                Os.setsockoptTimeval(socket.fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(STALL_MS))
            } catch (e: ErrnoException) {
                // Not a socket: the app gets no time limit.
            }
        }
    }

    override fun write(header: String, payload: ByteArray): Boolean = try {
        val bytes = header.encodeToByteArray()
        out.writeInt(bytes.size)
        out.write(bytes)
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
        true
    } catch (e: IOException) {
        false
    }

    override fun end(failure: Failure?) {
        try {
            val bytes = failure?.let { KitJson.encodeToString(Failure.serializer(), it).encodeToByteArray() } ?: ByteArray(0)
            out.writeInt(KitProtocol.FEED_END)
            out.writeInt(bytes.size)
            out.write(bytes)
            out.flush()
        } catch (e: IOException) {
            // The app is gone; there is nobody to tell.
        }
        close()
    }

    override fun close() {
        try {
            socket.close()
        } catch (e: IOException) {
            // Closed already.
        }
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val STALL_MS = 5_000L
    }
}
