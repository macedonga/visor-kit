package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.Visor

/** A [Visor] connected to this fake, in this process, with no binder: it works in a plain JVM unit test. */
public suspend fun FakeVisor.connect(): Visor = Visor.connect(dispatcher, caller)
