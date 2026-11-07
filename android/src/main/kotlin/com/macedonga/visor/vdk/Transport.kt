package com.macedonga.visor.vdk

import android.os.RemoteException
import com.macedonga.visor.vdk.ipc.IVdkClient
import com.macedonga.visor.vdk.ipc.IVdkService
import com.macedonga.visor.vdk.ipc.IVdkSession
import com.macedonga.visor.vdk.protocol.Failure
import com.macedonga.visor.vdk.protocol.VdkJson
import com.macedonga.visor.vdk.server.Caller
import com.macedonga.visor.vdk.server.Conversation
import com.macedonga.visor.vdk.server.Dispatcher
import com.macedonga.visor.vdk.server.Peer

/** Visor's side as a [VdkClient] sees it, whatever carries the messages: binder to Visor, or a call in this process. */
internal fun interface Endpoint {
    /** A session whose answers go to [answers]; null when Visor turns the app away. Throws [Gone]. */
    fun open(answers: Answers): Line?
}

/** One open session. Each send throws [Gone] once Visor is gone. */
internal interface Line {
    fun call(id: Long, method: String, params: String)

    fun cancel(id: Long)

    fun subscribe(id: Long, topic: String, params: String)

    fun unsubscribe(id: Long)

    fun close()
}

/** What Visor sends back, in the order it sent it. Failures are [Failure]s as JSON. */
internal interface Answers {
    fun result(id: Long, result: String)

    fun failure(id: Long, failure: String)

    fun event(subscription: Long, event: String)

    fun ended(subscription: Long, failure: String?)
}

/** Visor went away mid-send. */
internal class Gone(cause: Throwable) : Exception(cause)

/** The VDK over binder, to Visor's service. */
internal class BinderEndpoint(private val service: IVdkService) : Endpoint {
    override fun open(answers: Answers): Line? {
        val client = object : IVdkClient.Stub() {
            override fun result(id: Long, result: String) = answers.result(id, result)

            override fun failure(id: Long, failure: String) = answers.failure(id, failure)

            override fun event(subscription: Long, event: String) = answers.event(subscription, event)

            override fun ended(subscription: Long, failure: String?) = answers.ended(subscription, failure)
        }
        val session = binder { service.open(client) } ?: return null
        return BinderLine(session)
    }

    private class BinderLine(private val session: IVdkSession) : Line {
        override fun call(id: Long, method: String, params: String) = binder { session.call(id, method, params) }

        override fun cancel(id: Long) = binder { session.cancel(id) }

        override fun subscribe(id: Long, topic: String, params: String) = binder { session.subscribe(id, topic, params) }

        override fun unsubscribe(id: Long) = binder { session.unsubscribe(id) }

        override fun close() = binder { session.close() }
    }

    private companion object {
        inline fun <T> binder(block: () -> T): T = try {
            block()
        } catch (e: RemoteException) {
            throw Gone(e)
        }
    }
}

/**
 * The VDK in this process, straight to a [Dispatcher], with no binder: FakeVisor uses it, so tests run on a plain
 * JVM.
 */
internal class LocalEndpoint(private val dispatcher: Dispatcher, private val caller: Caller) : Endpoint {
    override fun open(answers: Answers): Line {
        val peer = object : Peer {
            override fun result(id: Long, result: String) = delivered { answers.result(id, result) }

            override fun failure(id: Long, failure: Failure) = delivered { answers.failure(id, json(failure)) }

            override fun event(subscription: Long, event: String) = delivered { answers.event(subscription, event) }

            override fun ended(subscription: Long, failure: Failure?) = delivered { answers.ended(subscription, failure?.let(::json)) }

            override fun closed() = Unit
        }
        return LocalLine(dispatcher.open(caller, peer))
    }

    private class LocalLine(private val conversation: Conversation) : Line {
        override fun call(id: Long, method: String, params: String) = conversation.call(id, method, params)

        override fun cancel(id: Long) = conversation.cancel(id)

        override fun subscribe(id: Long, topic: String, params: String) = conversation.subscribe(id, topic, params)

        override fun unsubscribe(id: Long) = conversation.unsubscribe(id)

        override fun close() = conversation.end()
    }

    private fun json(failure: Failure) = VdkJson.encodeToString(Failure.serializer(), failure)

    // In one process, a sent answer always arrives.
    private inline fun delivered(send: () -> Unit): Boolean {
        send()
        return true
    }
}
