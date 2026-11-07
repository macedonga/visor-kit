package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.coroutines.flow.Flow

/** The glasses themselves. Needs [Scope.STATE]. */
public class Glasses internal constructor(private val visor: Visor) {
    /** The status now, then each change. `status.first()` reads it once. */
    public val status: Flow<GlassesStatus>
        get() = visor.subscribe(Topics.GLASSES_STATUS, Unit)
}
