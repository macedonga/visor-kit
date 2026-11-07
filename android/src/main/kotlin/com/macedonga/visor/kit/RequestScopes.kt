package com.macedonga.visor.kit

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Scope

/**
 * Asks the person, on Visor's consent screen, to let this app use some [Scope]s:
 *
 * ```
 * val ask = registerForActivityResult(RequestScopes()) { held -> ... }
 * ask.launch(setOf(Scope.CAMERA, Scope.BAND))
 * ```
 *
 * The result is every scope the app holds once the person is done, which can be fewer than it asked for. Launching
 * throws `ActivityNotFoundException` when Visor is not installed: check [Visor.isInstalled] first.
 */
public class RequestScopes : ActivityResultContract<Set<Scope>, Set<Scope>>() {
    override fun createIntent(context: Context, input: Set<Scope>): Intent =
        Intent(KitProtocol.ACTION_REQUEST_SCOPES)
            .setPackage(KitProtocol.VISOR_PACKAGE)
            .putExtra(KitProtocol.EXTRA_SCOPES, input.map { it.id }.toTypedArray())

    override fun parseResult(resultCode: Int, intent: Intent?): Set<Scope> =
        intent?.getStringArrayExtra(KitProtocol.EXTRA_SCOPES).orEmpty().mapTo(LinkedHashSet(), ::Scope)
}
