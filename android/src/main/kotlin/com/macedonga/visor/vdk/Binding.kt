package com.macedonga.visor.vdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.macedonga.visor.vdk.ipc.IVdkService
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.VdkProtocol

/** Where a [VdkClient] gets Visor from. */
internal interface Binding {
    /**
     * Starts delivering Visor to [onEndpoint]: an endpoint each time Visor comes up, null each time it goes away and
     * may come back. [onDead] once when it cannot come back.
     */
    fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (VdkException) -> Unit)

    fun stop()
}

/** Visor's service through Android's bindService. Android brings the binding back by itself after Visor restarts. */
internal class AndroidBinding(private val context: Context) : Binding {
    private var connection: ServiceConnection? = null

    override fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (VdkException) -> Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) =
                onEndpoint(BinderEndpoint(IVdkService.Stub.asInterface(service)))

            override fun onServiceDisconnected(name: ComponentName) = onEndpoint(null)

            // Visor was updated or force-stopped: this binding is spent, so bind again.
            override fun onBindingDied(name: ComponentName) {
                stop()
                onEndpoint(null)
                start(onEndpoint, onDead)
            }

            override fun onNullBinding(name: ComponentName) =
                onDead(VdkException(ErrorCode.VISOR_TOO_OLD, "Visor's VDK service gave no binder. Update Visor."))
        }
        val intent = Intent(VdkProtocol.ACTION_BIND).setPackage(VdkProtocol.VISOR_PACKAGE)
        this.connection = connection
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            stop()
            val error = if (Visor.isInstalled(context)) {
                VdkException(ErrorCode.VISOR_TOO_OLD, "This Visor has no VDK service. Update Visor.")
            } else {
                VdkException(ErrorCode.VISOR_MISSING, "Visor is not installed.")
            }
            onDead(error)
        }
    }

    // Also after bindService returned false: Android keeps the connection registered until it is unbound.
    override fun stop() {
        connection?.let(context::unbindService)
        connection = null
    }
}

/** An endpoint in this process, which is always there. */
internal class LocalBinding(private val endpoint: Endpoint) : Binding {
    override fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (VdkException) -> Unit) = onEndpoint(endpoint)

    override fun stop() = Unit
}
