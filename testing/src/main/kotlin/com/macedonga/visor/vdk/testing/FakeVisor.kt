package com.macedonga.visor.vdk.testing

import com.macedonga.visor.vdk.VdkException
import com.macedonga.visor.vdk.Visor
import com.macedonga.visor.vdk.protocol.BandStatus
import com.macedonga.visor.vdk.protocol.Capture
import com.macedonga.visor.vdk.protocol.CaptureProgress
import com.macedonga.visor.vdk.protocol.CaptureType
import com.macedonga.visor.vdk.protocol.ErrorCode
import com.macedonga.visor.vdk.protocol.Finger
import com.macedonga.visor.vdk.protocol.Gesture
import com.macedonga.visor.vdk.protocol.GestureAction
import com.macedonga.visor.vdk.protocol.GlassesStatus
import com.macedonga.visor.vdk.protocol.Hello
import com.macedonga.visor.vdk.protocol.Link
import com.macedonga.visor.vdk.protocol.Methods
import com.macedonga.visor.vdk.protocol.PrompterStyle
import com.macedonga.visor.vdk.protocol.Scope
import com.macedonga.visor.vdk.protocol.Topics
import com.macedonga.visor.vdk.server.Caller
import com.macedonga.visor.vdk.server.Dispatcher
import com.macedonga.visor.vdk.server.Gate
import com.macedonga.visor.vdk.server.Routes
import java.util.Collections
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Visor inside the app's own process, for tests and for building without Visor or glasses. It answers every method of
 * the VDK from the state set here, and keeps what the app asked for.
 *
 * ```
 * val fake = FakeVisor()
 * val visor = fake.connect()
 * fake.band.gesture(Finger.INDEX, GestureAction.TAP)
 * ```
 */
public class FakeVisor {
    /** The scopes the fake person granted: all of them at first. Taking one back ends the streams that need it. */
    public var granted: Set<Scope> by Delegates.observable(setOf(Scope.STATE, Scope.CAMERA, Scope.BAND, Scope.DISPLAY)) { _, _, _ ->
        dispatcher.recheck()
    }

    public val glasses: FakeGlasses = FakeGlasses()
    public val band: FakeBand = FakeBand()
    public val display: FakeDisplay = FakeDisplay()

    private val _calls = Collections.synchronizedList(mutableListOf<String>())

    /** The name of each method the app called, in order, [Methods.HELLO] included. */
    public val calls: List<String> get() = synchronized(_calls) { _calls.toList() }

    private val gate = object : Gate {
        override fun admit(uid: Int): Caller = Caller(uid, APP)

        override fun granted(caller: Caller): Set<Scope> = granted

        override fun grants(): Map<String, Set<Scope>> = mapOf(APP to granted)

        override fun setGranted(packageName: String, scopes: Set<Scope>): Boolean {
            if (packageName != APP) return false
            granted = scopes
            return true
        }

        override fun greeted(caller: Caller, hello: Hello) {
            _calls += Methods.HELLO.name
        }
    }

    private val dispatcher = Dispatcher(VERSION, gate, routes(), log = {})

    /** A [Visor] connected to this fake, in this process, with no binder: it works in a plain JVM unit test. */
    public suspend fun connect(): Visor = Visor.connect(dispatcher, Caller(UID, APP))

    /** Ends every connection to this fake. */
    public fun close(): Unit = dispatcher.stop()

    private fun routes() = Routes().apply {
        answer(Methods.GLASSES_STATUS) { _, _ ->
            called(Methods.GLASSES_STATUS.name)
            glasses.status.value
        }
        stream(Topics.GLASSES_STATUS) { _, _ -> glasses.status }

        answer(Methods.CAMERA_PHOTO) { _, request ->
            called(Methods.CAMERA_PHOTO.name)
            glasses.ready()
            if (glasses.recording) throw VdkException(ErrorCode.BUSY, "The fake glasses record a video.")
            val capture = glasses.add(CaptureType.PHOTO)
            if (request.import) glasses.import(capture.id) else capture
        }
        answer(Methods.CAMERA_VIDEO_START) { _, _ ->
            called(Methods.CAMERA_VIDEO_START.name)
            glasses.ready()
            glasses.recording = true
        }
        answer(Methods.CAMERA_VIDEO_STOP) { _, _ ->
            called(Methods.CAMERA_VIDEO_STOP.name)
            if (glasses.recording) {
                glasses.recording = false
                glasses.add(CaptureType.VIDEO)
            }
        }
        answer(Methods.CAPTURES_LIST) { _, _ ->
            called(Methods.CAPTURES_LIST.name)
            glasses.waiting()
        }
        answer(Methods.CAPTURES_IMPORT) { _, request ->
            called(Methods.CAPTURES_IMPORT.name)
            glasses.ready()
            glasses.import(request.id)
        }
        stream(Topics.CAPTURES_AVAILABLE) { _, _ -> glasses.available }
        stream(Topics.CAPTURES_PROGRESS) { _, _ -> glasses.progress }
        stream(Topics.CAPTURES_IMPORTED) { _, _ -> glasses.imported }
        stream(Topics.CAPTURES_FAILED) { _, _ -> MutableSharedFlow() }

        answer(Methods.BAND_STATUS) { _, _ ->
            called(Methods.BAND_STATUS.name)
            band.status.value
        }
        answer(Methods.BAND_BUZZ) { _, request ->
            called(Methods.BAND_BUZZ.name)
            band.paired()
            band.buzzed(request.durationMs.milliseconds)
        }
        stream(Topics.BAND_STATUS) { _, _ -> band.status }
        stream(Topics.BAND_GESTURES) { _, _ -> band.borrow() }

        answer(Methods.DISPLAY_PROMPTER_SHOW) { _, request ->
            called(Methods.DISPLAY_PROMPTER_SHOW.name)
            glasses.withDisplay()
            display.shown(request.text, request.style)
        }
        answer(Methods.DISPLAY_PROMPTER_CLOSE) { _, _ ->
            called(Methods.DISPLAY_PROMPTER_CLOSE.name)
            glasses.withDisplay()
            display.shown(null, null)
        }
    }

    private fun called(name: String) {
        _calls += name
    }

    private companion object {
        const val VERSION = "fake"

        // The fake knows one app: the one in its own process.
        const val APP = "app"
        const val UID = 10_000
    }
}

/** The fake glasses. Each change of a property reaches the app's [com.macedonga.visor.vdk.Glasses.status]. */
public class FakeGlasses internal constructor() {
    public var paired: Boolean by published(true)
    public var connected: Boolean by published(true)
    public var display: Boolean by published(true)
    public var model: String? by published("Fake glasses")
    public var battery: Int? by published(80)
    public var charging: Boolean? by published(false)
    public var worn: Boolean? by published(true)
    public var folded: Boolean? by published(false)

    /** True while a video records, from the app's start to its stop. */
    public var recording: Boolean by published(false)
        internal set

    internal val status: MutableStateFlow<GlassesStatus> = MutableStateFlow(build())
    private val captures = LinkedHashMap<String, Capture>()
    private var next = 0
    internal val available = MutableSharedFlow<Capture>(extraBufferCapacity = 64)
    internal val progress = MutableSharedFlow<CaptureProgress>(extraBufferCapacity = 64)
    internal val imported = MutableSharedFlow<Capture>(extraBufferCapacity = 64)

    /** Puts a capture of [type] on the fake glasses, as if their button took it, and returns it. */
    public fun add(type: CaptureType): Capture = synchronized(captures) {
        val capture = Capture(id = "${type.id}-${++next}", type = type, time = System.currentTimeMillis(), size = SIZE)
        captures[capture.id] = capture
        capture
    }.also { available.tryEmit(it) }

    internal fun waiting(): List<Capture> = synchronized(captures) { captures.values.filter { it.uri == null } }

    internal fun import(id: String): Capture {
        val done = synchronized(captures) {
            val capture = captures[id]
                ?: throw VdkException(ErrorCode.FAILED, "The fake glasses have no capture $id.", reason = "not_found")
            capture.uri?.let { return capture }
            Capture(capture.id, capture.type, capture.time, capture.size, "content://$AUTHORITY/captures/$id").also { captures[id] = it }
        }
        progress.tryEmit(CaptureProgress(id, SIZE, SIZE))
        imported.tryEmit(done)
        return done
    }

    internal fun ready() {
        if (!paired || !connected) throw VdkException(ErrorCode.NOT_CONNECTED, "The fake glasses are not connected.")
    }

    internal fun withDisplay() {
        ready()
        if (!display) throw VdkException(ErrorCode.UNSUPPORTED, "The fake glasses have no display.")
    }

    private fun build() = if (!paired) {
        GlassesStatus(paired = false, link = Link.DISCONNECTED)
    } else {
        GlassesStatus(
            paired = true,
            link = if (connected) Link.CONNECTED else Link.DISCONNECTED,
            model = model,
            serial = "FAKE0001",
            firmware = "fake",
            display = display,
            battery = battery,
            charging = charging,
            worn = worn,
            folded = folded,
            recording = recording,
        )
    }

    private fun <T> published(initial: T) = Delegates.observable(initial) { _, _, _ -> status.value = build() }

    private companion object {
        const val AUTHORITY = "com.macedonga.visor.vdk.testing"
        const val SIZE = 1024L
    }
}

/** The fake Neural Band. */
public class FakeBand internal constructor() {
    public var paired: Boolean by published(true)
    public var battery: Int? by published(70)
    public var charging: Boolean? by published(false)
    public var leftHanded: Boolean? by published(false)

    private var borrowers = 0
    private val gestures = MutableSharedFlow<Gesture>(extraBufferCapacity = 64)
    internal val status: MutableStateFlow<BandStatus> = MutableStateFlow(build())

    private val _buzzes = MutableStateFlow<List<Duration>>(emptyList())

    /** How long each buzz the app asked for lasted, oldest first. */
    public val buzzes: StateFlow<List<Duration>> = _buzzes.asStateFlow()

    /** True while the app collects gestures. */
    public val borrowed: Boolean get() = synchronized(this) { borrowers > 0 }

    /** Sends a gesture to every app that collects gestures now. */
    public fun gesture(finger: Finger, action: GestureAction) {
        gesture(finger, action, null)
    }

    public fun gesture(finger: Finger, action: GestureAction, derived: GestureAction?) {
        gestures.tryEmit(Gesture(finger, action, derived, System.currentTimeMillis()))
    }

    internal fun paired() {
        if (!paired) throw VdkException(ErrorCode.UNSUPPORTED, "No fake band is paired.")
    }

    internal fun buzzed(duration: Duration) {
        _buzzes.value += duration
    }

    internal fun borrow(): Flow<Gesture> {
        paired()
        return gestures.asSharedFlow()
            .onStart { lend(+1) }
            .onCompletion { lend(-1) }
    }

    private fun lend(change: Int) {
        synchronized(this) { borrowers += change }
        status.value = build()
    }

    private fun build() = if (!paired) {
        BandStatus(paired = false, link = Link.DISCONNECTED)
    } else {
        BandStatus(
            paired = true,
            link = if (borrowed) Link.CONNECTED else Link.DISCONNECTED,
            battery = battery,
            charging = charging,
            leftHanded = leftHanded,
            borrowed = borrowed,
        )
    }

    private fun <T> published(initial: T) = Delegates.observable(initial) { _, _, _ -> status.value = build() }
}

/** The fake Display. */
public class FakeDisplay internal constructor() {
    private val _prompter = MutableStateFlow<String?>(null)

    /** The script on the teleprompter, null while it is closed. */
    public val prompter: StateFlow<String?> = _prompter.asStateFlow()

    /** How the script shows, null while the teleprompter is closed. */
    public var prompterStyle: PrompterStyle? = null
        private set

    internal fun shown(text: String?, style: PrompterStyle?) {
        prompterStyle = style
        _prompter.value = text
    }
}
