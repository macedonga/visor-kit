package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.GlassesPair
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.PairSelection
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.coroutines.flow.Flow

/** The glasses: the active pair's status with [Scope.STATE], and every pair set up in Visor with [Scope.PAIRS]. */
public class Glasses internal constructor(private val visor: Visor) {
    /** The active pair's status now, then each change. `status.first()` reads it once. */
    public val status: Flow<GlassesStatus>
        get() = visor.subscribe(Topics.GLASSES_STATUS, Unit)

    /** Every pair set up in Visor, the active one marked, now and on each change. Needs [Scope.PAIRS]. */
    public val pairs: Flow<List<GlassesPair>>
        get() = visor.subscribe(Topics.GLASSES_PAIRS, Unit)

    /**
     * Makes the pair [serial] the active one, which every other call then acts on. Returns once Visor has switched,
     * before the pair is connected: watch [status] for that. Needs [Scope.PAIRS].
     */
    public suspend fun select(serial: String): GlassesPair = visor.call(Methods.GLASSES_SELECT, PairSelection(serial))
}
