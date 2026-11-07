package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.Scope

/** Every failure of a call or a stream: [code] says why, the message is for logs. */
public class KitException @KitInternal constructor(
    public val code: ErrorCode,
    message: String,
    /** With [ErrorCode.SCOPE_DENIED], the scope to ask the person for with [RequestScopes]. */
    public val scope: Scope? = null,
    /** A finer reason, from the set the method documents. */
    public val reason: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

@KitInternal
public fun Failure.toException(): KitException = KitException(code, message, scope, reason)

@KitInternal
public fun KitException.toFailure(): Failure = Failure(code, message.orEmpty(), scope, reason)
