package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.Capture
import com.macedonga.visor.kit.protocol.CaptureFailure
import com.macedonga.visor.kit.protocol.CaptureProgress
import com.macedonga.visor.kit.protocol.ImportRequest
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.coroutines.flow.Flow

/**
 * The photos and videos on the glasses, and their way into the phone's gallery. Needs [Scope.CAMERA]. Imports run
 * over Wi-Fi Direct, so the phone's Wi-Fi must be on.
 */
public class Captures internal constructor(private val visor: Visor) {
    /** The captures on the glasses that the phone does not have yet. */
    public suspend fun list(): List<Capture> = visor.call(Methods.CAPTURES_LIST, Unit)

    /**
     * Copies the capture [id] into the gallery and returns it with its [Capture.uri]. Returns at once when the phone
     * has it already. The glasses also send what else waits in their queue; each of those comes on [imported].
     */
    public suspend fun import(id: String): Capture = visor.call(Methods.CAPTURES_IMPORT, ImportRequest(id))

    /** Each new capture on the glasses that the phone does not have yet. */
    public val available: Flow<Capture>
        get() = visor.subscribe(Topics.CAPTURES_AVAILABLE, Unit)

    /** Each import's progress, Visor's own imports included. */
    public val progress: Flow<CaptureProgress>
        get() = visor.subscribe(Topics.CAPTURES_PROGRESS, Unit)

    /** Each capture that reaches the gallery, Visor's own imports included. */
    public val imported: Flow<Capture>
        get() = visor.subscribe(Topics.CAPTURES_IMPORTED, Unit)

    /** Each import that did not finish. */
    public val failed: Flow<CaptureFailure>
        get() = visor.subscribe(Topics.CAPTURES_FAILED, Unit)
}
