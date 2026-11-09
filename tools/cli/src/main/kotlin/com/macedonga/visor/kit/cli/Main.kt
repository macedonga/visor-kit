package com.macedonga.visor.kit.cli

import com.macedonga.visor.kit.protocol.AppInfo
import com.macedonga.visor.kit.protocol.BandStatus
import com.macedonga.visor.kit.protocol.BriefingCard
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.Feeds
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.GrantRequest
import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.InjectRequest
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.LiveHeader
import com.macedonga.visor.kit.protocol.LiveKind
import com.macedonga.visor.kit.protocol.LivePhoto
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.SimulateRequest
import com.macedonga.visor.kit.protocol.Topics
import com.macedonga.visor.kit.protocol.TraceRecord
import com.macedonga.visor.kit.protocol.Welcome
import com.macedonga.visor.kit.protocol.problem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

private val USAGE = """
Usage: visor [-s SERIAL | --fake[=PORT]] [--json] COMMAND

The Visor Development Kit (VDK) from a computer, through adb. The phone needs Visor with Developer mode on.

Commands:
  status                    Visor, the glasses, the band, and the apps that use the VDK.
  docs [QUERY]              Searches the VDK's docs, bundled here; with no query, lists the pages.
  docs-mcp                  The docs as an MCP server over standard input and output, for a coding
                            assistant: claude mcp add vdk-docs -- visor docs-mcp
  agents                    Prints guidance for coding assistants, to save as AGENTS.md in your app.
  methods                   The methods and topics this Visor answers.
  call METHOD [JSON]        Runs a method and prints its result. JSON is the params, @FILE reads them from a file
                            and - from standard input, for params past the command line's length.
                              visor call camera.photo '{"import":false}'
                              visor call display.cards.set @card.json
  card FILE                 Puts the card in FILE (a BriefingCard's JSON; - for standard input) on the Display's
                            Briefing, checked first as Visor checks it.
  watch TOPIC [JSON]...     Prints each event as it comes, until Ctrl+C. JSON after a topic is its params.
                              visor watch band.gestures glasses.status
                              visor watch voice.requests '{"phrases":["the garage"]}'
  live FILE [JSON]          Saves the camera live to FILE as raw HEVC or H.264, until Ctrl+C. With FILE -,
                            writes to standard output for a player. JSON is the feed's params; with
                            "audio": true the sound goes to FILE.aac. The glasses film while it runs.
                              visor live - | ffplay -fflags nobuffer -
                              visor live out.hevc '{"quality":"high","audio":true}'
  live-photo FILE           Takes a photo while the camera is live and saves the HEIC to FILE.
  trace                     Prints every call, answer and event between the apps and Visor.
  simulate TOPIC...         While it runs, apps get only the events you type, one per line, and no
                            glasses or band are used. For band.gestures type FINGER ACTION, for
                            example "index double_tap"; for glasses.inputs SOURCE ACTION, for
                            example "touchpad swipe_forward"; for other topics, the event's JSON.
  inject TOPIC EVENT        Sends one made-up event to the apps subscribed to TOPIC.
                              visor inject band.gestures middle hold
  record FILE [TOPIC...]    Saves each event of the topics to FILE as JSON lines, until Ctrl+C. By default
                            glasses.status, glasses.inputs, band.status and band.gestures.
  replay FILE [--loop]      Plays a recording into the apps at its own pace. Its topics are simulated while
                            it plays, as with simulate. With --loop it starts over at the end.
  apps                      The apps that use the VDK, with their scopes.
  grant PACKAGE SCOPE...    Grants scopes without the consent screen.
  revoke PACKAGE SCOPE...   Takes scopes back.
  fake [--port N] [--lan]   Runs FakeVisor on this computer, for building without a phone or glasses.
                            visor fake --help says more.

Scopes: state, pairs, inputs, motion, microphone, camera, band, display, voice, assistant, notify, settings,
conversations, raw.

Options:
  -s SERIAL                 The device, as for adb -s.
  --fake[=PORT]             The FakeVisor that visor fake runs on this computer, instead of a phone. PORT is
                            the one visor fake was given, 7333 unless set.
  --json                    JSON only, one value per line.
""".trim()

private val COMMANDS = setOf(
    "status", "methods", "call", "card", "watch", "live", "live-photo", "trace", "simulate", "inject", "record", "replay", "apps", "grant", "revoke",
    "fake", "docs", "docs-mcp", "agents",
)

// What record saves when no topic is named: what the wearer does, and the state it happens in.
private val RECORDED = listOf("glasses.status", "glasses.inputs", "band.status", "band.gestures")

private val VERSION = Session::class.java.`package`?.implementationVersion ?: "dev"

fun main(args: Array<String>) {
    val code = try {
        run(args.toList())
        0
    } catch (e: CliError) {
        System.err.println("visor: ${e.message}")
        e.exitCode
    }
    exitProcess(code)
}

private fun run(args: List<String>) {
    var serial: String? = null
    var json = false
    var fake: Int? = null
    val words = mutableListOf<String>()
    val it = args.iterator()
    while (it.hasNext()) {
        when (val word = it.next()) {
            "-s" -> serial = if (it.hasNext()) it.next() else usage("-s needs a device serial.")
            "--json" -> json = true
            "--fake" -> fake = FAKE_PORT
            "-h", "--help", "help" -> return println(if (words.firstOrNull() == "fake") FAKE_USAGE else USAGE)
            "--version" -> return println("visor $VERSION, protocol ${KitProtocol.VERSION}")
            else -> if (word.startsWith("--fake=")) {
                fake = word.removePrefix("--fake=").toIntOrNull() ?: usage("--fake=PORT takes a port number.")
            } else {
                words += word
            }
        }
    }
    val command = words.firstOrNull() ?: usage("Name a command.")
    if (command !in COMMANDS) usage("No command $command.")
    if (command == "fake") return runFake(words.drop(1))
    // The docs ride in the command: no phone, adb or fake needed.
    if (command == "docs") return printDocs(words.drop(1))
    if (command == "docs-mcp") return serveDocsMcp(VERSION)
    if (command == "agents") return println(Docs.page("for-agents.md"))
    val session = fake?.let(Session::fake) ?: Session.open(serial)
    // Ctrl+C ends watch, trace and simulate: the forward goes with the session, and with it any simulation.
    Runtime.getRuntime().addShutdownHook(Thread(session::close))
    Commands(session, json).run(command, words.drop(1))
}

private fun usage(problem: String): Nothing = throw CliError("$problem\n\n$USAGE", exitCode = 2)

private const val FAKE_PORT = 7333
private const val NANOS_PER_MS = 1_000_000L

private fun runFake(args: List<String>) {
    var port = FAKE_PORT
    var lan = false
    val it = args.iterator()
    while (it.hasNext()) {
        when (val word = it.next()) {
            "--port" -> port = it.takeIf { it.hasNext() }?.next()?.toIntOrNull()?.takeIf { it in 1..65534 }
                ?: throw CliError("--port needs a number.\n\n$FAKE_USAGE", exitCode = 2)
            "--lan" -> lan = true
            else -> throw CliError("No option $word.\n\n$FAKE_USAGE", exitCode = 2)
        }
    }
    FakeServer(port, lan, VERSION).run()
}

/** A connection to Visor through an adb forward, which it removes when closed, or to `visor fake` on this computer. */
internal class Session private constructor(private val adb: Adb?, private val port: Int, val rpc: Rpc, val welcome: Welcome) : AutoCloseable {
    @Volatile
    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        rpc.close()
        adb?.removeForward(port)
    }

    companion object {
        fun open(serial: String?): Session {
            val adb = Adb(serial)
            val port = adb.forward()
            try {
                return greet(adb, port)
            } catch (e: Exception) {
                adb.removeForward(port)
                throw e
            }
        }

        fun fake(port: Int): Session = try {
            greet(null, fakeShellPort(port))
        } catch (e: ConnectException) {
            throw CliError("No visor fake answers on port ${fakeShellPort(port)} (${e.message}). Start one with: visor fake")
        }

        private fun greet(adb: Adb?, port: Int): Session {
            val rpc = Rpc(Socket(InetAddress.getLoopbackAddress(), port))
            val hello = KitJson.encodeToJsonElement(Hello.serializer(), Hello(KitProtocol.VERSION, "cli $VERSION"))
            val welcome = KitJson.decodeFromJsonElement(Welcome.serializer(), rpc.call(Methods.HELLO.name, hello))
            return Session(adb, port, rpc, welcome)
        }
    }
}

private class Commands(private val session: Session, private val json: Boolean) {
    private val rpc = session.rpc
    private val pretty = Json(KitJson) { prettyPrint = true }
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    fun run(command: String, args: List<String>) = when (command) {
        "status" -> status()
        "methods" -> methods()
        "call" -> call(args.getOrNull(0) ?: usage("call needs a method."), args.getOrNull(1))
        "card" -> card(args.getOrNull(0) ?: usage("card needs a file, or - for standard input."))
        "watch" -> watch(args.ifEmpty { usage("watch needs a topic.") })
        "live" -> live(args.getOrNull(0) ?: usage("live needs a file, or - for standard output."), args.getOrNull(1))
        "live-photo" -> livePhoto(args.getOrNull(0) ?: usage("live-photo needs a file."))
        "trace" -> trace()
        "simulate" -> simulate(args.ifEmpty { usage("simulate needs a topic.") })
        "inject" -> inject(args.getOrNull(0) ?: usage("inject needs a topic."), args.drop(1).ifEmpty { usage("inject needs an event.") })
        "record" -> record(args.getOrNull(0) ?: usage("record needs a file."), args.drop(1))
        "replay" -> replay(args.firstOrNull { !it.startsWith("--") } ?: usage("replay needs a file."), "--loop" in args)
        "apps" -> apps()
        "grant" -> grant(Shell.GRANT.name, args)
        "revoke" -> grant(Shell.REVOKE.name, args)
        else -> usage("No command $command.")
    }

    private fun status() {
        val glasses = decode(GlassesStatus.serializer(), rpc.call(Methods.GLASSES_STATUS.name))
        val band = try {
            decode(BandStatus.serializer(), rpc.call(Methods.BAND_STATUS.name))
        } catch (e: CliError) {
            null
        }
        val apps = decode(ListSerializer(AppInfo.serializer()), rpc.call(Shell.APPS.name))
        if (json) {
            println(buildJsonObject {
                put("visor", session.welcome.visor)
                put("protocol", session.welcome.protocol)
                put("glasses", KitJson.encodeToJsonElement(GlassesStatus.serializer(), glasses))
                band?.let { put("band", KitJson.encodeToJsonElement(BandStatus.serializer(), it)) }
                put("apps", KitJson.encodeToJsonElement(ListSerializer(AppInfo.serializer()), apps))
            })
            return
        }
        line("Visor", "${session.welcome.visor}, protocol ${session.welcome.protocol}")
        line("Glasses", describe(glasses))
        line("Band", band?.let(::describe) ?: "none")
        if (apps.isEmpty()) line("Apps", "none")
        apps.forEach { line("App", describe(it)) }
    }

    private fun methods() {
        // Separate: a method and a topic can share a name, as glasses.status does.
        val methods = (Methods.all + Shell.methods).associate { it.name to it.summary }
        val topics = (Topics.all + Shell.topics).associate { it.name to it.summary }
        val feeds = Feeds.all.associate { it.name to it.summary }
        val welcome = session.welcome
        if (json) {
            println(buildJsonObject {
                put("methods", KitJson.encodeToJsonElement(ListSerializer(String.serializer()), welcome.methods))
                put("topics", KitJson.encodeToJsonElement(ListSerializer(String.serializer()), welcome.topics))
                put("feeds", KitJson.encodeToJsonElement(ListSerializer(String.serializer()), welcome.feeds))
            })
            return
        }
        println("Methods")
        welcome.methods.forEach { println("  ${it.padEnd(NAME_WIDTH)}${methods[it].orEmpty()}") }
        println("Topics")
        welcome.topics.forEach { println("  ${it.padEnd(NAME_WIDTH)}${topics[it].orEmpty()}") }
        if (welcome.feeds.isEmpty()) return
        println("Feeds")
        welcome.feeds.forEach { println("  ${it.padEnd(NAME_WIDTH)}${feeds[it].orEmpty()}") }
    }

    private fun call(method: String, params: String?) {
        val result = rpc.call(method, params?.let { parse(read(it)) })
        println(if (json) result.toString() else pretty.encodeToString(JsonElement.serializer(), result))
    }

    /** JSON as given, from a file for @FILE, or from standard input for -. */
    private fun read(text: String): String = when {
        text == "-" -> System.`in`.readBytes().decodeToString()
        text.startsWith("@") -> File(text.drop(1)).takeIf { it.isFile }?.readText() ?: usage("No file ${text.drop(1)}.")
        else -> text
    }

    private fun card(source: String) {
        val element = parse(read(if (source == "-" || source.startsWith("@")) source else "@$source"))
        val card = decode(BriefingCard.serializer(), element)
        card.problem()?.let { throw CliError(it) }
        rpc.call(Methods.DISPLAY_CARDS_SET.name, element)
        println(if (json) "{}" else "Card ${card.id} is on the Briefing; the Display shows it within seconds while the Briefing is open.")
    }

    /** Each word is a topic, and a JSON object after one is that topic's params. */
    private fun watch(words: List<String>) {
        val topics = LinkedHashMap<String, JsonElement?>()
        for (word in words) {
            if (word.startsWith("{")) {
                val last = topics.keys.lastOrNull() ?: usage("Name a topic before its params.")
                topics[last] = parse(word)
            } else {
                topics[word] = null
            }
        }
        val open = CountDownLatch(topics.size)
        for ((topic, params) in topics) {
            rpc.subscribe(topic, params, object : Rpc.Listener {
                override fun event(event: JsonElement) = println(
                    if (json) buildJsonObject { put("topic", topic); put("event", event) }.toString()
                    else "${clock.format(Instant.now())}  $topic  $event",
                )

                override fun ended(failure: Failure?) {
                    failure?.let { System.err.println("visor: $topic ended: ${Rpc.describe(it)}") }
                    open.countDown()
                }
            })
        }
        open.await()
    }

    /** Over JSON-RPC a feed is a topic: each event is a record, `{"header", "payload"}` with the payload in Base64. */
    private fun live(file: String, params: String?) {
        val video: OutputStream = if (file == "-") System.out else FileOutputStream(file)
        var audio: OutputStream? = null
        var adts: Adts? = null
        System.err.println("Filming with the glasses into ${if (file == "-") "standard output" else file}. Ctrl+C stops.")
        val over = CountDownLatch(1)
        rpc.subscribe(Feeds.CAMERA_LIVE.name, params?.let(::parse), object : Rpc.Listener {
            override fun event(event: JsonElement) {
                val record = event.jsonObject
                val header = decode(LiveHeader.serializer(), record.getValue("header"))
                val payload = Base64.getDecoder().decode(record.getValue("payload").jsonPrimitive.content)
                when {
                    header.kind == LiveKind.VIDEO -> video.write(payload)
                    file == "-" -> Unit
                    header.config -> adts = Adts.of(payload)
                    else -> adts?.let { format ->
                        val out = audio ?: FileOutputStream("$file.aac").also { audio = it }
                        out.write(format.header(payload.size))
                        out.write(payload)
                    }
                }
            }

            override fun ended(failure: Failure?) {
                failure?.let { System.err.println("visor: the live ended: ${Rpc.describe(it)}") }
                over.countDown()
            }
        })
        over.await()
        video.flush()
        audio?.close()
    }

    private fun livePhoto(file: String) {
        val photo = decode(LivePhoto.serializer(), rpc.call(Methods.CAMERA_LIVE_PHOTO.name))
        val data = photo.data ?: throw CliError("Visor sent the photo as ${photo.uri}, which a computer cannot read.")
        File(file).writeBytes(Base64.getDecoder().decode(data))
        if (!json) println("Saved $file")
    }

    private fun trace() {
        System.err.println("Tracing the kit. Ctrl+C stops.")
        val over = CountDownLatch(1)
        rpc.subscribe(Shell.TRACE.name, null, object : Rpc.Listener {
            override fun event(event: JsonElement) {
                if (json) return println(event)
                val record = decode(TraceRecord.serializer(), event)
                val took = record.durationMs?.let { " $it ms" }.orEmpty()
                println("${clock.format(Instant.ofEpochMilli(record.time))}  ${record.app}  ${record.kind.id.padEnd(KIND_WIDTH)}${record.name} #${record.id}$took  ${record.body ?: ""}")
            }

            override fun ended(failure: Failure?) {
                failure?.let { System.err.println("visor: the trace ended: ${Rpc.describe(it)}") }
                over.countDown()
            }
        })
        over.await()
    }

    private fun simulate(topics: List<String>) {
        val now = rpc.call(Shell.SIMULATE.name, KitJson.encodeToJsonElement(SimulateRequest.serializer(), SimulateRequest(topics)))
        System.err.println("Simulating $now: apps get only what you type here. Ctrl+C or end of input stops.")
        if (topics.size > 1) System.err.println("Start each line with its topic.")
        while (true) {
            val line = readlnOrNull()?.trim() ?: return
            if (line.isEmpty()) continue
            val words = line.split(Regex("\\s+"))
            val (topic, event) = if (topics.size > 1) words.first() to words.drop(1) else topics.single() to words
            try {
                inject(topic, event)
            } catch (e: CliError) {
                System.err.println("visor: ${e.message}")
            }
        }
    }

    private fun inject(topic: String, words: List<String>) {
        val event = when {
            words.first().startsWith("{") -> parse(words.joinToString(" "))
            topic == Topics.BAND_GESTURES.name -> gesture(words)
            topic == Topics.GLASSES_INPUTS.name -> input(words)
            else -> parse(words.joinToString(" "))
        }
        rpc.call(Shell.INJECT.name, KitJson.encodeToJsonElement(InjectRequest.serializer(), InjectRequest(topic, event)))
    }

    /** Each event as one line, `{"t", "topic", "event"}`, with t in milliseconds from the start. */
    private fun record(file: String, topics: List<String>) {
        val names = topics.ifEmpty { RECORDED }
        val out = try {
            File(file).bufferedWriter()
        } catch (e: IOException) {
            throw CliError("Cannot write $file: ${e.message}")
        }
        val started = System.nanoTime()
        System.err.println("Recording ${names.joinToString()} into $file. Ctrl+C stops.")
        val open = CountDownLatch(names.size)
        for (topic in names) {
            rpc.subscribe(topic, null, object : Rpc.Listener {
                override fun event(event: JsonElement) {
                    val line = buildJsonObject {
                        put("t", (System.nanoTime() - started) / NANOS_PER_MS)
                        put("topic", topic)
                        put("event", event)
                    }
                    // Line by line, so Ctrl+C loses nothing.
                    synchronized(out) {
                        out.write(line.toString())
                        out.newLine()
                        out.flush()
                    }
                }

                override fun ended(failure: Failure?) {
                    failure?.let { System.err.println("visor: $topic ended: ${Rpc.describe(it)}") }
                    open.countDown()
                }
            })
        }
        open.await()
        synchronized(out) { out.close() }
    }

    private fun replay(file: String, loop: Boolean) {
        val events = try {
            File(file).readLines()
        } catch (e: IOException) {
            throw CliError("Cannot read $file: ${e.message}")
        }.filter { it.isNotBlank() }.mapIndexed { i, line ->
            val o = runCatching { parse(line).jsonObject }.getOrNull()
            val t = o?.get("t")?.jsonPrimitive?.longOrNull
            val topic = o?.get("topic")?.jsonPrimitive?.contentOrNull
            val event = o?.get("event")
            if (t == null || topic == null || event == null) throw CliError("Line ${i + 1} of $file is not a recorded event.")
            Triple(t, topic, event)
        }
        if (events.isEmpty()) throw CliError("$file holds no events.")
        val topics = events.map { it.second }.distinct()
        rpc.call(Shell.SIMULATE.name, KitJson.encodeToJsonElement(SimulateRequest.serializer(), SimulateRequest(topics)))
        System.err.println("Replaying ${events.size} events of ${topics.joinToString()}: apps get only these. Ctrl+C stops.")
        do {
            val started = System.nanoTime()
            for ((t, topic, event) in events) {
                val wait = t - (System.nanoTime() - started) / NANOS_PER_MS
                if (wait > 0) Thread.sleep(wait)
                rpc.call(Shell.INJECT.name, KitJson.encodeToJsonElement(InjectRequest.serializer(), InjectRequest(topic, event)))
            }
        } while (loop)
    }

    private fun apps() {
        val apps = decode(ListSerializer(AppInfo.serializer()), rpc.call(Shell.APPS.name))
        if (json) return apps.forEach { println(KitJson.encodeToString(AppInfo.serializer(), it)) }
        if (apps.isEmpty()) println("No app uses the VDK yet.")
        apps.forEach { println(describe(it)) }
    }

    private fun grant(method: String, args: List<String>) {
        val app = args.getOrNull(0) ?: usage("Name the app's package.")
        val scopes = args.drop(1).ifEmpty { usage("Name at least one scope.") }.map(::Scope)
        val request = KitJson.encodeToJsonElement(GrantRequest.serializer(), GrantRequest(app, scopes))
        val info = decode(AppInfo.serializer(), rpc.call(method, request))
        println(if (json) KitJson.encodeToString(AppInfo.serializer(), info) else describe(info))
    }

    /** A touch or a press from words: SOURCE ACTION. */
    private fun input(words: List<String>): JsonElement {
        val at = words.drop(2).map { it.toDoubleOrNull() ?: usage("A drag's X Y DX DY are numbers.") }
        if (words.size != 2 && words.size != 6) usage("An input is SOURCE ACTION [X Y DX DY], for example: touchpad double_tap")
        return buildJsonObject {
            put("source", words[0])
            put("action", words[1])
            put("time", System.currentTimeMillis())
            listOf("x", "y", "dx", "dy").zip(at).forEach { (name, value) -> put(name, value) }
        }
    }

    /** A gesture from words: FINGER ACTION, and the band's DERIVED action when it differs. */
    private fun gesture(words: List<String>): JsonElement {
        if (words.size !in 2..3) usage("A gesture is FINGER ACTION [DERIVED], for example: index double_tap")
        return buildJsonObject {
            put("finger", words[0])
            put("action", words[1])
            put("derived", words.getOrElse(2) { words[1] })
            put("time", System.currentTimeMillis())
        }
    }

    private fun describe(g: GlassesStatus): String {
        if (!g.paired) return "none set up"
        return listOfNotNull(
            g.link.id,
            g.model,
            g.battery?.let { "battery $it%" + if (g.charging == true) ", charging" else "" },
            g.worn?.let { if (it) "worn" else "not worn" },
            "folded".takeIf { g.folded == true },
            "recording".takeIf { g.recording == true },
            g.firmware?.let { "firmware $it" },
        ).joinToString("  ")
    }

    private fun describe(b: BandStatus): String {
        if (!b.paired) return "none paired"
        return listOfNotNull(
            when {
                b.shared -> "shared: on the phone, the Display through it"
                b.borrowed -> "borrowed by the phone"
                b.held == false -> "with the Display, not set up in Visor"
                else -> "with the Display"
            },
            b.battery?.let { "battery $it%" + if (b.charging == true) ", charging" else "" },
            b.leftHanded?.let { if (it) "left wrist" else "right wrist" },
        ).joinToString("  ")
    }

    private fun describe(a: AppInfo): String = listOfNotNull(
        a.packageName,
        if (a.connected) "connected" else "not connected",
        a.granted.joinToString(", ").ifEmpty { "no scopes" },
        a.kit?.let { "kit $it" },
    ).joinToString("  ")

    private fun line(label: String, text: String) = println("${label.padEnd(LABEL_WIDTH)}$text")

    private fun parse(text: String): JsonElement = try {
        KitJson.parseToJsonElement(text)
    } catch (e: SerializationException) {
        usage("Not JSON: $text")
    }

    private fun <T> decode(strategy: DeserializationStrategy<T>, element: JsonElement): T = try {
        KitJson.decodeFromJsonElement(strategy, element)
    } catch (e: SerializationException) {
        throw CliError("Visor answered something this command cannot read. Update the visor command. ${e.message}")
    }

    private companion object {
        const val LABEL_WIDTH = 9
        const val NAME_WIDTH = 24
        const val KIND_WIDTH = 12
    }
}
