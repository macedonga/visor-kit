package com.macedonga.visor.kit.server

import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.Scope

/** An app with an open session, or the adb shell when [shell] is true. */
@KitInternal
public class Caller(public val uid: Int, public val packageName: String, public val shell: Boolean = false) {
    override fun toString(): String = packageName
}

/** What a [Dispatcher] asks of the app that hosts it. Called on any thread. */
@KitInternal
public interface Gate {
    /** The app behind [uid], or null to turn it away. */
    public fun admit(uid: Int): Caller?

    /** False while the host answers no one at all. */
    public fun enabled(): Boolean

    /** The scopes [caller] holds now. Never asked for the shell, which holds them all. */
    public fun granted(caller: Caller): Set<Scope>

    /** Every app that holds a scope, by package. */
    public fun grants(): Map<String, Set<Scope>>

    /** Sets the scopes of [packageName]. False when no such app is installed. */
    public fun setGranted(packageName: String, scopes: Set<Scope>): Boolean

    /** [caller] said [hello]. */
    public fun greeted(caller: Caller, hello: Hello)
}
