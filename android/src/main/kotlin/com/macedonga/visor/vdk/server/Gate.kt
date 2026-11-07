package com.macedonga.visor.vdk.server

import com.macedonga.visor.vdk.protocol.Hello
import com.macedonga.visor.vdk.protocol.VdkInternal
import com.macedonga.visor.vdk.protocol.Scope

/** An app with an open session, or the adb shell when [shell] is true. */
@VdkInternal
public class Caller(public val uid: Int, public val packageName: String, public val shell: Boolean = false) {
    override fun toString(): String = packageName
}

/** What a [Dispatcher] asks of the app that hosts it. Called on any thread. */
@VdkInternal
public interface Gate {
    /** The app behind [uid], or null to turn it away. */
    public fun admit(uid: Int): Caller?

    /** The scopes [caller] holds now. Never asked for the shell, which holds them all. */
    public fun granted(caller: Caller): Set<Scope>

    /** Every app that holds a scope, by package. */
    public fun grants(): Map<String, Set<Scope>>

    /** Sets the scopes of [packageName]. False when no such app is installed. */
    public fun setGranted(packageName: String, scopes: Set<Scope>): Boolean

    /** [caller] said [hello]. */
    public fun greeted(caller: Caller, hello: Hello)
}
