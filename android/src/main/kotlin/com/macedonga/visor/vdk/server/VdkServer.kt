package com.macedonga.visor.vdk.server

import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import com.macedonga.visor.vdk.ipc.IVdkClient
import com.macedonga.visor.vdk.ipc.IVdkService
import com.macedonga.visor.vdk.ipc.IVdkSession
import com.macedonga.visor.vdk.protocol.Failure
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.VdkJson

/** The VDK over binder, for apps: what Visor's VDK service returns from onBind. */
@VdkInternal
public class VdkServer(private val dispatcher: Dispatcher) : IVdkService.Stub() {
    override fun open(client: IVdkClient): IVdkSession? {
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

    private inner class BinderSession(private val caller: Caller, private val client: IVdkClient) :
        IVdkSession.Stub(), IBinder.DeathRecipient, Peer {
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

        override fun binderDied() = conversation.end()

        override fun result(id: Long, result: String) = sent { client.result(id, result) }

        override fun failure(id: Long, failure: Failure) =
            sent { client.failure(id, VdkJson.encodeToString(Failure.serializer(), failure)) }

        override fun event(subscription: Long, event: String) = sent { client.event(subscription, event) }

        override fun ended(subscription: Long, failure: Failure?) =
            sent { client.ended(subscription, failure?.let { VdkJson.encodeToString(Failure.serializer(), it) }) }

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
