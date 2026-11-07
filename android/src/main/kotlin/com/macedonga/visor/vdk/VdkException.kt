package com.macedonga.visor.vdk

import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.Failure
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.Scope

/** Every failure of a call or a stream: [code] says why, the message is for logs. */
public class VdkException @VdkInternal constructor(
    public val code: ErrorCode,
    message: String,
    /** With [ErrorCode.SCOPE_DENIED], the scope to ask the person for with [RequestScopes]. */
    public val scope: Scope? = null,
    /** A finer reason, from the set the method documents. */
    public val reason: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

@VdkInternal
public fun Failure.toException(): VdkException = VdkException(code, message, scope, reason)

@VdkInternal
public fun VdkException.toFailure(): Failure = Failure(code, message.orEmpty(), scope, reason)
