package com.macedonga.visor.kit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.macedonga.visor.kit.ipc.IKitService
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.KitProtocol

/** Where a [KitClient] gets Visor's service from. */
internal interface Binding {
    /**
     * Starts delivering the service to [onService]: the service each time it comes up, null each time it goes away
     * and may come back. [onDead] once when it cannot come back.
     */
    fun start(onService: (IKitService?) -> Unit, onDead: (KitException) -> Unit)

    fun stop()
}

/** Visor's service through Android's bindService. Android brings the binding back by itself after Visor restarts. */
internal class AndroidBinding(private val context: Context) : Binding {
    private var connection: ServiceConnection? = null

    override fun start(onService: (IKitService?) -> Unit, onDead: (KitException) -> Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) =
                onService(IKitService.Stub.asInterface(service))

            override fun onServiceDisconnected(name: ComponentName) = onService(null)

            // Visor was updated or force-stopped: this binding is spent, so bind again.
            override fun onBindingDied(name: ComponentName) {
                stop()
                onService(null)
                start(onService, onDead)
            }

            override fun onNullBinding(name: ComponentName) =
                onDead(KitException(ErrorCode.VISOR_TOO_OLD, "Visor's kit service gave no binder. Update Visor."))
        }
        val intent = Intent(KitProtocol.ACTION_BIND).setPackage(KitProtocol.VISOR_PACKAGE)
        this.connection = connection
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            stop()
            val error = if (Visor.isInstalled(context)) {
                KitException(ErrorCode.VISOR_TOO_OLD, "This Visor has no kit service. Update Visor.")
            } else {
                KitException(ErrorCode.VISOR_MISSING, "Visor is not installed.")
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

/** A service in this process, for FakeVisor. */
internal class LocalBinding(private val service: IKitService) : Binding {
    override fun start(onService: (IKitService?) -> Unit, onDead: (KitException) -> Unit) = onService(service)

    override fun stop() = Unit
}
