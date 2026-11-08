package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.protocol.KitInternal
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * The bytes behind the `content://` URIs FakeVisor hands out: imported captures and live photos. kit-testing's
 * provider serves them to the app, and tells FakeVisor its authority, as FakeVisor itself runs in a plain JVM.
 */
@KitInternal
public object FakeFiles {
    /** The provider's authority, which kit-testing sets to the app's own when the app starts. */
    @Volatile
    public var authority: String = "com.macedonga.visor.kit.testing"

    private val files = ConcurrentHashMap<String, Pair<String, ByteArray>>()

    /** A 1x1 PNG, for a capture or photo whose bytes the test did not give. */
    internal val PLACEHOLDER: ByteArray =
        Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")
    internal const val PNG = "image/png"

    /** Keeps [bytes] at [path] and returns its URI. */
    internal fun put(path: String, mime: String, bytes: ByteArray): String {
        files[path] = mime to bytes
        return "content://$authority/$path"
    }

    /** The type and bytes at [path], the URI's path without its leading slash, or null. */
    public fun get(path: String): Pair<String, ByteArray>? = files[path]
}
