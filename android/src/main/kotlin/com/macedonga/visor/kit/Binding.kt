package com.macedonga.visor.kit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.macedonga.visor.kit.ipc.IKitService
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.KitProtocol

/** Where a [KitClient] gets Visor from. */
internal interface Binding {
    /**
     * Starts delivering Visor to [onEndpoint]: an endpoint each time Visor comes up, null each time it goes away and
     * may come back. [onDead] once when it cannot come back.
     */
    fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (KitException) -> Unit)

    fun stop()
}

/** Visor's service through Android's bindService. Android brings the binding back by itself after Visor restarts. */
internal class AndroidBinding(private val context: Context) : Binding {
    private var connection: ServiceConnection? = null

    override fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (KitException) -> Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) =
                onEndpoint(BinderEndpoint(IKitService.Stub.asInterface(service)))

            override fun onServiceDisconnected(name: ComponentName) = onEndpoint(null)

            // Visor was updated or force-stopped: this binding is spent, so bind again.
            override fun onBindingDied(name: ComponentName) {
                stop()
                onEndpoint(null)
                start(onEndpoint, onDead)
            }

            override fun onNullBinding(name: ComponentName) =
                onDead(KitException(ErrorCode.VISOR_TOO_OLD, "Visor's VDK service gave no binder. Update Visor."))
        }
        val intent = Intent(KitProtocol.ACTION_BIND).setPackage(KitProtocol.VISOR_PACKAGE)
        this.connection = connection
        if (!context.bindService(intent, connection, FLAGS)) {
            stop()
            val error = if (Visor.isInstalled(context)) {
                KitException(ErrorCode.VISOR_TOO_OLD, "This Visor has no VDK service. Update Visor.")
            } else {
                KitException(ErrorCode.VISOR_MISSING, "Visor is not installed.")
            }
            onDead(error)
        }
    }

    private companion object {
        // While the app is on screen, Visor may use the microphone for it, as for voice.transcripts.
        val FLAGS = Context.BIND_AUTO_CREATE or (if (Build.VERSION.SDK_INT >= 29) Context.BIND_INCLUDE_CAPABILITIES else 0)
    }

    // Also after bindService returned false: Android keeps the connection registered until it is unbound.
    override fun stop() {
        connection?.let(context::unbindService)
        connection = null
    }
}

/** An endpoint in this process, which is always there. */
internal class LocalBinding(private val endpoint: Endpoint) : Binding {
    override fun start(onEndpoint: (Endpoint?) -> Unit, onDead: (KitException) -> Unit) = onEndpoint(endpoint)

    override fun stop() = Unit
}
