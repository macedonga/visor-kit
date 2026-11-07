package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.BandStatus
import com.macedonga.visor.kit.protocol.Capture
import com.macedonga.visor.kit.protocol.CaptureProgress
import com.macedonga.visor.kit.protocol.CaptureType
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Feeds
import com.macedonga.visor.kit.protocol.Finger
import com.macedonga.visor.kit.protocol.Gesture
import com.macedonga.visor.kit.protocol.GestureAction
import com.macedonga.visor.kit.protocol.GlassesInput
import com.macedonga.visor.kit.protocol.GlassesPair
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutClick
import com.macedonga.visor.kit.protocol.LayoutNode
import com.macedonga.visor.kit.protocol.InputAction
import com.macedonga.visor.kit.protocol.InputSource
import com.macedonga.visor.kit.protocol.Link
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.PrompterStyle
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.ThermalLevel
import com.macedonga.visor.kit.protocol.Topics
import com.macedonga.visor.kit.server.Caller
import com.macedonga.visor.kit.server.Dispatcher
import com.macedonga.visor.kit.server.Gate
import com.macedonga.visor.kit.server.Routes
import java.util.Collections
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    public var granted: Set<Scope> by Delegates.observable(setOf(Scope.STATE, Scope.PAIRS, Scope.INPUTS, Scope.MICROPHONE, Scope.CAMERA, Scope.BAND, Scope.DISPLAY, Scope.VOICE, Scope.ASSISTANT, Scope.NOTIFY, Scope.SETTINGS)) { _, _, _ ->
        dispatcher.recheck()
    }

    public val glasses: FakeGlasses = FakeGlasses()

    /** The pairs set up in the fake Visor besides the active one, which is [glasses]. */
    public val pairs: FakePairs = FakePairs(glasses)
    public val live: FakeLive = FakeLive(glasses)
    public val band: FakeBand = FakeBand()
    public val display: FakeDisplay = FakeDisplay()
    public val voice: FakeVoice = FakeVoice()
    public val assistant: FakeAssistant = FakeAssistant()
    public val notify: FakeNotify = FakeNotify()
    public val settings: FakeSettings = FakeSettings()

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

    /** The fake's server engine, for a transport to carry: `connect()` on Android, `visor fake` on a computer. */
    @KitInternal
    public val dispatcher: Dispatcher = Dispatcher(VERSION, gate, routes(), log = {})

    /** The one app the fake admits, granted [granted]. */
    @KitInternal
    public val caller: Caller get() = Caller(UID, APP)

    /** Ends every connection to this fake. */
    public fun close(): Unit = dispatcher.stop()

    private fun routes() = Routes().apply {
        answer(Methods.GLASSES_STATUS) { _, _ ->
            called(Methods.GLASSES_STATUS.name)
            glasses.status.value
        }
        stream(Topics.GLASSES_STATUS) { _, _ -> glasses.status }
        answer(Methods.GLASSES_PAIRS) { _, _ ->
            called(Methods.GLASSES_PAIRS.name)
            pairs.now()
        }
        stream(Topics.GLASSES_PAIRS) { _, _ -> pairs.list }
        stream(Topics.GLASSES_INPUTS) { _, _ -> glasses.inputs }
        answer(Methods.GLASSES_SELECT) { _, selection ->
            called(Methods.GLASSES_SELECT.name)
            pairs.select(selection.serial)
        }

        answer(Methods.CAMERA_PHOTO) { _, request ->
            called(Methods.CAMERA_PHOTO.name)
            glasses.ready()
            if (glasses.recording) throw KitException(ErrorCode.BUSY, "The fake glasses record a video.")
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
        feed(Feeds.CAMERA_LIVE) { _, config -> live.watch(config) }
        answer(Methods.CAMERA_LIVE_PHOTO) { _, _ ->
            called(Methods.CAMERA_LIVE_PHOTO.name)
            live.photo()
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
        answer(Methods.DISPLAY_LAYOUT_SHOW) { _, layout ->
            called(Methods.DISPLAY_LAYOUT_SHOW.name)
            glasses.withDisplay()
            display.showLayout(layout)
        }
        answer(Methods.DISPLAY_LAYOUT_CLOSE) { _, _ ->
            called(Methods.DISPLAY_LAYOUT_CLOSE.name)
            glasses.withDisplay()
            display.showLayout(null)
        }
        stream(Topics.DISPLAY_LAYOUT_CLICKS) { _, _ -> display.clicks }

        stream(Topics.VOICE_REQUESTS) { _, offer -> voice.offer(offer.phrases) }
        stream(Topics.VOICE_TRANSCRIPTS) { _, request -> voice.transcripts(request) }
        answer(Methods.VOICE_REPLY) { _, reply ->
            called(Methods.VOICE_REPLY.name)
            voice.replied(reply)
        }
        stream(Topics.ASSISTANT_TOOLS) { _, offer -> assistant.offer(offer.tools) }
        answer(Methods.ASSISTANT_TOOLS_ANSWER) { _, answer ->
            called(Methods.ASSISTANT_TOOLS_ANSWER.name)
            assistant.answered(answer)
        }
        answer(Methods.NOTIFY_POST) { _, notice ->
            called(Methods.NOTIFY_POST.name)
            glasses.withDisplay()
            notify.posted(notice)
        }
        answer(Methods.NOTIFY_REMOVE) { _, removal ->
            called(Methods.NOTIFY_REMOVE.name)
            glasses.withDisplay()
            notify.removed(removal.id)
        }
        answer(Methods.SETTINGS_LIST) { _, query ->
            called(Methods.SETTINGS_LIST.name)
            settings.list(query)
        }
        answer(Methods.SETTINGS_GET) { _, request ->
            called(Methods.SETTINGS_GET.name)
            settings.get(request.key)
        }
        answer(Methods.SETTINGS_SET) { _, write ->
            called(Methods.SETTINGS_SET.name)
            settings.set(write.key, write.value)
        }
        stream(Topics.SETTINGS_CHANGES) { _, query -> settings.changes(query) }
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

/** The fake glasses. Each change of a property reaches the app's [com.macedonga.visor.kit.Glasses.status]. */
public class FakeGlasses internal constructor() {
    public var paired: Boolean by published(true)
    public var connected: Boolean by published(true)
    public var serial: String by published("FAKE0001")
    public var name: String? by published("My glasses")
    public var display: Boolean by published(true)
    public var model: String? by published("Fake glasses")
    public var battery: Int? by published(80)
    public var charging: Boolean? by published(false)
    public var worn: Boolean? by published(true)
    public var folded: Boolean? by published(false)
    public var thermal: ThermalLevel? by published(ThermalLevel.NORMAL)

    /** True while a video records, from the app's start to its stop. */
    public var recording: Boolean by published(false)
        internal set

    internal val status: MutableStateFlow<GlassesStatus> = MutableStateFlow(build())
    private val captures = LinkedHashMap<String, Capture>()
    private var next = 0
    internal val available = MutableSharedFlow<Capture>(extraBufferCapacity = 64)
    internal val inputs = MutableSharedFlow<GlassesInput>(extraBufferCapacity = 64)
    internal val progress = MutableSharedFlow<CaptureProgress>(extraBufferCapacity = 64)
    internal val imported = MutableSharedFlow<Capture>(extraBufferCapacity = 64)

    /** Sends a touch or a press to every app that collects inputs now, for example `input(TOUCHPAD, TAP)`. */
    public fun input(source: InputSource, action: InputAction) {
        inputs.tryEmit(GlassesInput(source, action, System.currentTimeMillis()))
    }

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
                ?: throw KitException(ErrorCode.FAILED, "The fake glasses have no capture $id.", reason = "not_found")
            capture.uri?.let { return capture }
            Capture(capture.id, capture.type, capture.time, capture.size, "content://$AUTHORITY/captures/$id").also { captures[id] = it }
        }
        progress.tryEmit(CaptureProgress(id, SIZE, SIZE))
        imported.tryEmit(done)
        return done
    }

    internal fun ready() {
        if (!paired || !connected) throw KitException(ErrorCode.NOT_CONNECTED, "The fake glasses are not connected.")
    }

    internal fun withDisplay() {
        ready()
        if (!display) throw KitException(ErrorCode.UNSUPPORTED, "The fake glasses have no display.")
    }

    private fun build() = if (!paired) {
        GlassesStatus(paired = false, link = Link.DISCONNECTED)
    } else {
        GlassesStatus(
            paired = true,
            link = if (connected) Link.CONNECTED else Link.DISCONNECTED,
            model = model,
            name = name,
            serial = serial,
            firmware = "fake",
            display = display,
            battery = battery,
            charging = charging,
            worn = worn,
            folded = folded,
            recording = recording,
            thermal = thermal,
        )
    }

    private fun <T> published(initial: T) = Delegates.observable(initial) { _, _, _ -> status.value = build() }

    private companion object {
        const val AUTHORITY = "com.macedonga.visor.kit.testing"
        const val SIZE = 1024L
    }
}

/** The fake Neural Band. */
public class FakeBand internal constructor() {
    public var paired: Boolean by published(true)
    public var battery: Int? by published(70)
    public var charging: Boolean? by published(false)
    public var leftHanded: Boolean? by published(false)

    /** False for a band only the fake Display holds: its status shows, but gestures and buzzes fail. */
    public var held: Boolean by published(true)

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
        if (!paired) throw KitException(ErrorCode.UNSUPPORTED, "No fake band is paired.")
        if (!held) throw KitException(ErrorCode.UNSUPPORTED, "Only the fake Display holds the band.", reason = "not_set_up")
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
            held = held,
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

    private val _layout = MutableStateFlow<Layout?>(null)

    /** The layout the app shows, null while none does. */
    public val layout: StateFlow<Layout?> = _layout.asStateFlow()

    internal val clicks = MutableSharedFlow<LayoutClick>(extraBufferCapacity = 64)

    /**
     * Clicks the element with [id] in the layout shown, as the wearer would. Fails when no element has that id, so a
     * test finds a misspelt id.
     */
    public fun click(id: String) {
        val shown = checkNotNull(_layout.value) { "The fake Display shows no layout." }
        check(ids(shown.root).contains(id)) { "The layout shown has no element with id $id." }
        clicks.tryEmit(LayoutClick(id, System.currentTimeMillis()))
    }

    internal fun showLayout(layout: Layout?) {
        _layout.value = layout
    }

    private fun ids(node: LayoutNode): List<String> = listOfNotNull(node.id) + node.children.flatMap(::ids)
}

/** The pairs set up in the fake Visor besides the active one. [select] swaps a pair with the active [FakeGlasses]. */
public class FakePairs internal constructor(private val glasses: FakeGlasses) {
    private val others = MutableStateFlow<List<GlassesPair>>(emptyList())

    /** Sets up another pair, not active, with no display. */
    public fun add(serial: String, name: String?) {
        add(serial, name, display = false)
    }

    public fun add(serial: String, name: String?, display: Boolean) {
        others.value = others.value.filter { it.serial != serial } +
            GlassesPair(serial, name, active = false, link = Link.DISCONNECTED, display = display)
    }

    internal val list: Flow<List<GlassesPair>> = combine(glasses.status, others) { _, rest -> listOf(active()) + rest }

    internal fun now(): List<GlassesPair> = listOf(active()) + others.value

    internal fun select(serial: String): GlassesPair {
        if (serial == glasses.serial) return active()
        val target = others.value.firstOrNull { it.serial == serial }
            ?: throw KitException(ErrorCode.INVALID_PARAMS, "The fake Visor has no pair $serial.", reason = "not_found")
        val before = active()
        others.value = others.value - target + GlassesPair(before.serial, before.name, active = false, link = Link.DISCONNECTED, display = before.display)
        glasses.serial = target.serial
        glasses.name = target.name
        glasses.display = target.display == true
        return active()
    }

    private fun active() = GlassesPair(
        glasses.serial, glasses.name, active = true,
        link = if (glasses.connected) Link.CONNECTED else Link.DISCONNECTED,
        display = glasses.display, battery = glasses.battery,
    )
}
