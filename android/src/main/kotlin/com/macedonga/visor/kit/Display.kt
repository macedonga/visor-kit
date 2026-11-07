package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.PrompterRequest
import com.macedonga.visor.kit.protocol.PrompterStyle
import com.macedonga.visor.kit.protocol.Scope

/** What the Meta Ray-Ban Display shows. Needs [Scope.DISPLAY]; on glasses without a display, every call fails. */
public class Display internal constructor(visor: Visor) {
    public val prompter: Prompter = Prompter(visor)
}

/** The Display's teleprompter: one script at a time, which a new [show] replaces. */
public class Prompter internal constructor(private val visor: Visor) {
    public suspend fun show(text: String): Unit = visor.call(Methods.DISPLAY_PROMPTER_SHOW, PrompterRequest(text))

    public suspend fun show(text: String, style: PrompterStyle): Unit =
        visor.call(Methods.DISPLAY_PROMPTER_SHOW, PrompterRequest(text, style))

    public suspend fun close(): Unit = visor.call(Methods.DISPLAY_PROMPTER_CLOSE, Unit)
}
