package com.macedonga.visor.kit

import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.macedonga.visor.kit.ipc.IKitClient
import com.macedonga.visor.kit.ipc.IKitService
import com.macedonga.visor.kit.ipc.IKitSession
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.server.Caller
import com.macedonga.visor.kit.server.Conversation
import com.macedonga.visor.kit.server.Dispatcher
import com.macedonga.visor.kit.server.FeedSink
import com.macedonga.visor.kit.server.Peer
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Visor's side as a [KitClient] sees it, whatever carries the messages: binder to Visor, or a call in this process. */
internal fun interface Endpoint {
    /** A session whose answers go to [answers]; null when Visor turns the app away. Throws [Gone]. */
    fun open(answers: Answers): Line?
}

/** One open session. Each send throws [Gone] once Visor is gone. */
internal interface Line {
    fun call(id: Long, method: String, params: String)

    fun cancel(id: Long)

    fun subscribe(id: Long, topic: String, params: String)

    /** Opens a feed, whose records come to [Answers.record] and whose end comes to [Answers.ended]. */
    fun openFeed(id: Long, feed: String, params: String)

    /** Stops a stream or a feed. */
    fun unsubscribe(id: Long)

    fun close()
}

/** What Visor sends back, in the order it sent it. Failures are [Failure]s as JSON. */
internal interface Answers {
    fun result(id: Long, result: String)

    fun failure(id: Long, failure: String)

    fun event(subscription: Long, event: String)

    /** One record of a feed. May block, to slow the feed down to the app's pace. */
    fun record(subscription: Long, header: String, payload: ByteArray)

    fun ended(subscription: Long, failure: String?)
}

/** Visor went away mid-send. */
internal class Gone(cause: Throwable) : Exception(cause)

/** The VDK over binder, to Visor's service. */
internal class BinderEndpoint(private val service: IKitService) : Endpoint {
    override fun open(answers: Answers): Line? {
        val client = object : IKitClient.Stub() {
            override fun result(id: Long, result: String) = answers.result(id, result)

            override fun failure(id: Long, failure: String) = answers.failure(id, failure)

            override fun event(subscription: Long, event: String) = answers.event(subscription, event)

            override fun ended(subscription: Long, failure: String?) = answers.ended(subscription, failure)
        }
        val session = binder { service.open(client) } ?: return null
        return BinderLine(session, answers)
    }

    private class BinderLine(private val session: IKitSession, private val answers: Answers) : Line {
        // The app's end of each feed's socket, to cut a reader loose when the app stops the feed.
        private val feeds = ConcurrentHashMap<Long, ParcelFileDescriptor>()

        override fun call(id: Long, method: String, params: String) = binder { session.call(id, method, params) }

        override fun cancel(id: Long) = binder { session.cancel(id) }

        override fun subscribe(id: Long, topic: String, params: String) = binder { session.subscribe(id, topic, params) }

        override fun openFeed(id: Long, feed: String, params: String) {
            val (mine, visors) = ParcelFileDescriptor.createSocketPair()
            feeds[id] = mine
            try {
                binder { session.openFeed(id, feed, params, visors) }
            } catch (e: Gone) {
                feeds.remove(id)
                mine.close()
                throw e
            } finally {
                // Visor holds its own copy now.
                visors.close()
            }
            thread(name = "kit-feed-$id", isDaemon = true) { read(id, mine) }
        }

        override fun unsubscribe(id: Long) {
            try {
                binder { session.unsubscribe(id) }
            } finally {
                feeds.remove(id)?.let(::shutDown)
            }
        }

        override fun close() {
            try {
                binder { session.close() }
            } finally {
                feeds.values.forEach(::shutDown)
                feeds.clear()
            }
        }

        private fun read(id: Long, socket: ParcelFileDescriptor) {
            val input = DataInputStream(BufferedInputStream(ParcelFileDescriptor.AutoCloseInputStream(socket), BUFFER))
            try {
                input.use {
                    while (true) {
                        val headerSize = input.readInt()
                        if (headerSize == KitProtocol.FEED_END) {
                            val failure = input.readSized()
                            return answers.ended(id, failure.takeIf { it.isNotEmpty() }?.decodeToString())
                        }
                        val header = input.readSized(headerSize).decodeToString()
                        answers.record(id, header, input.readSized())
                    }
                }
            } catch (e: IOException) {
                // Visor died mid-feed, or the app stopped it: then nobody listens for this.
                answers.ended(id, KitJson.encodeToString(Failure.serializer(), Failure(ErrorCode.VISOR_GONE, "The feed from Visor broke off.")))
            } finally {
                feeds.remove(id, socket)
            }
        }

        // Wakes a reader blocked on the socket, which closing alone does not.
        private fun shutDown(socket: ParcelFileDescriptor) {
            try {
                Os.shutdown(socket.fileDescriptor, OsConstants.SHUT_RDWR)
            } catch (e: ErrnoException) {
                // Closed already.
            }
        }
    }

    private companion object {
        const val BUFFER = 64 * 1024

        // Far above any record Visor sends: a length beyond it means the bytes are not a feed.
        const val MAX_RECORD = 64 shl 20

        inline fun <T> binder(block: () -> T): T = try {
            block()
        } catch (e: RemoteException) {
            throw Gone(e)
        }

        fun DataInputStream.readSized(size: Int = readInt()): ByteArray {
            if (size !in 0..MAX_RECORD) throw IOException("a record of $size bytes")
            return ByteArray(size).also(::readFully)
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
        return LocalLine(dispatcher.open(caller, peer), answers)
    }

    private inner class LocalLine(private val conversation: Conversation, private val answers: Answers) : Line {
        override fun call(id: Long, method: String, params: String) = conversation.call(id, method, params)

        override fun cancel(id: Long) = conversation.cancel(id)

        override fun subscribe(id: Long, topic: String, params: String) = conversation.subscribe(id, topic, params)

        override fun openFeed(id: Long, feed: String, params: String) {
            val sink = object : FeedSink {
                override fun write(header: String, payload: ByteArray) = delivered { answers.record(id, header, payload) }

                override fun end(failure: Failure?) = answers.ended(id, failure?.let(::json))

                override fun close() = Unit
            }
            conversation.openFeed(id, feed, params, sink)
        }

        override fun unsubscribe(id: Long) = conversation.unsubscribe(id)

        override fun close() = conversation.end()
    }

    private fun json(failure: Failure) = KitJson.encodeToString(Failure.serializer(), failure)

    // In one process, a sent answer always arrives.
    private inline fun delivered(send: () -> Unit): Boolean {
        send()
        return true
    }
}
