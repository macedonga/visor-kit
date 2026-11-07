package com.macedonga.visor.vdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.VdkProtocol
import com.macedonga.visor.vdk.protocol.Method
import com.macedonga.visor.vdk.protocol.Scope
import com.macedonga.visor.vdk.protocol.Topic
import com.macedonga.visor.vdk.protocol.Welcome
import com.macedonga.visor.vdk.server.Caller
import com.macedonga.visor.vdk.server.Dispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * A connection to Visor, the app that runs the glasses. Get one with [connect], reach the glasses through [glasses],
 * [camera], [captures], [band] and [display], and [close] it when done.
 *
 * Every call is main-safe and fails with a [VdkException]. While Visor restarts, calls wait for it and streams pick up
 * again on their own; events sent in between are lost.
 */
public class Visor internal constructor(private val client: VdkClient) : AutoCloseable {
    /** Where the connection stands. */
    public val state: StateFlow<VisorState> get() = client.state

    public val glasses: Glasses = Glasses(this)
    public val camera: Camera = Camera(this)
    public val captures: Captures = Captures(this)
    public val band: Band = Band(this)
    public val display: Display = Display(this)

    /** True when this Visor answers [method]. False for a method newer than the Visor installed. */
    public fun supports(method: Method<*, *>): Boolean = welcome()?.methods?.contains(method.name) == true

    /** True when this Visor streams [topic]. False for a topic newer than the Visor installed. */
    public fun supports(topic: Topic<*, *>): Boolean = welcome()?.topics?.contains(topic.name) == true

    /** The scopes the person has granted this app now. Ask for more with [RequestScopes]. */
    public suspend fun scopes(): Set<Scope> = client.hello().granted.toSet()

    /** Ends the connection. Running calls and streams fail with [ErrorCode.CLOSED]. */
    override fun close(): Unit = client.close()

    internal suspend fun <P, R> call(method: Method<P, R>, params: P): R = client.call(method, params)

    internal fun <P, E> subscribe(topic: Topic<P, E>, params: P): Flow<E> = client.subscribe(topic, params)

    private fun welcome(): Welcome? = (state.value as? VisorState.Open)?.welcome

    public companion object {
        /** True when Visor is installed on this phone. */
        public fun isInstalled(context: Context): Boolean = try {
            val packages = context.packageManager
            if (Build.VERSION.SDK_INT >= 33) {
                packages.getPackageInfo(VdkProtocol.VISOR_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packages.getPackageInfo(VdkProtocol.VISOR_PACKAGE, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        /**
         * Connects to Visor and returns once it answered. Fails with [ErrorCode.VISOR_MISSING] or
         * [ErrorCode.VISOR_TOO_OLD]. Suspends while Visor starts: wrap it in `withTimeout` for a limit.
         */
        public suspend fun connect(context: Context): Visor {
            val app = context.applicationContext
            if (!isInstalled(app)) throw VdkException(ErrorCode.VISOR_MISSING, "Visor is not installed.")
            return open(VdkClient(AndroidBinding(app)))
        }

        /** Connects straight to [dispatcher] in this process, as [caller], with no binder in between: for FakeVisor. */
        @VdkInternal
        public suspend fun connect(dispatcher: Dispatcher, caller: Caller): Visor =
            open(VdkClient(LocalBinding(LocalEndpoint(dispatcher, caller))))

        private suspend fun open(client: VdkClient): Visor {
            try {
                client.start()
                val now = client.state.first { it !is VisorState.Connecting }
                if (now is VisorState.Closed) throw now.error ?: VdkException(ErrorCode.CLOSED, "The connection was closed.")
            } catch (e: CancellationException) {
                client.close()
                throw e
            }
            return Visor(client)
        }
    }
}

/** Where a [Visor] connection stands. */
public sealed interface VisorState {
    /** Binding to Visor, or binding again after Visor restarted. Calls wait meanwhile. */
    public data object Connecting : VisorState

    /** Visor answers. [welcome] says what it answers and what this app may do. */
    public class Open internal constructor(public val welcome: Welcome) : VisorState

    /** Over for good. [error] says why, null when the app closed it. Connect again for a new one. */
    public class Closed internal constructor(public val error: VdkException?) : VisorState
}
