package com.macedonga.visor.kit.cli

import com.macedonga.visor.kit.protocol.AppInfo
import com.macedonga.visor.kit.protocol.BandStatus
import com.macedonga.visor.kit.protocol.Failure
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.GrantRequest
import com.macedonga.visor.kit.protocol.Hello
import com.macedonga.visor.kit.protocol.InjectRequest
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.SimulateRequest
import com.macedonga.visor.kit.protocol.Topics
import com.macedonga.visor.kit.protocol.TraceRecord
import com.macedonga.visor.kit.protocol.Welcome
import java.net.InetAddress
import java.net.Socket
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val USAGE = """
Usage: visor [-s SERIAL] [--json] COMMAND

The Visor Development Kit (VDK) from a computer, through adb. The phone needs Visor with Developer mode on.

Commands:
  status                    Visor, the glasses, the band, and the apps that use the VDK.
  methods                   The methods and topics this Visor answers.
  call METHOD [JSON]        Runs a method and prints its result.
                              visor call camera.photo '{"import":false}'
  watch TOPIC [JSON]...     Prints each event as it comes, until Ctrl+C. JSON after a topic is its params.
                              visor watch band.gestures glasses.status
                              visor watch voice.requests '{"phrases":["the garage"]}'
  trace                     Prints every call, answer and event between the apps and Visor.
  simulate TOPIC...         While it runs, apps get only the events you type, one per line, and no
                            glasses or band are used. For band.gestures type FINGER ACTION, for
                            example "index double_tap"; for other topics, the event's JSON.
  inject TOPIC EVENT        Sends one made-up event to the apps subscribed to TOPIC.
                              visor inject band.gestures middle hold
  apps                      The apps that use the VDK, with their scopes.
  grant PACKAGE SCOPE...    Grants scopes without the consent screen.
  revoke PACKAGE SCOPE...   Takes scopes back.

Scopes: state, camera, band, display.

Options:
  -s SERIAL                 The device, as for adb -s.
  --json                    JSON only, one value per line.
""".trim()

private val COMMANDS = setOf("status", "methods", "call", "watch", "trace", "simulate", "inject", "apps", "grant", "revoke")

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
    val words = mutableListOf<String>()
    val it = args.iterator()
    while (it.hasNext()) {
        when (val word = it.next()) {
            "-s" -> serial = if (it.hasNext()) it.next() else usage("-s needs a device serial.")
            "--json" -> json = true
            "-h", "--help", "help" -> return println(USAGE)
            "--version" -> return println("visor $VERSION, protocol ${KitProtocol.VERSION}")
            else -> words += word
        }
    }
    val command = words.firstOrNull() ?: usage("Name a command.")
    if (command !in COMMANDS) usage("No command $command.")
    val session = Session.open(serial)
    // Ctrl+C ends watch, trace and simulate: the forward goes with the session, and with it any simulation.
    Runtime.getRuntime().addShutdownHook(Thread(session::close))
    Commands(session, json).run(command, words.drop(1))
}

private fun usage(problem: String): Nothing = throw CliError("$problem\n\n$USAGE", exitCode = 2)

/** A connection to Visor through an adb forward, which it removes when closed. */
internal class Session private constructor(private val adb: Adb, private val port: Int, val rpc: Rpc, val welcome: Welcome) : AutoCloseable {
    @Volatile
    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        rpc.close()
        adb.removeForward(port)
    }

    companion object {
        fun open(serial: String?): Session {
            val adb = Adb(serial)
            val port = adb.forward()
            try {
                val rpc = Rpc(Socket(InetAddress.getLoopbackAddress(), port))
                val hello = KitJson.encodeToJsonElement(Hello.serializer(), Hello(KitProtocol.VERSION, "cli $VERSION"))
                val welcome = KitJson.decodeFromJsonElement(Welcome.serializer(), rpc.call(Methods.HELLO.name, hello))
                return Session(adb, port, rpc, welcome)
            } catch (e: Exception) {
                adb.removeForward(port)
                throw e
            }
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
        "watch" -> watch(args.ifEmpty { usage("watch needs a topic.") })
        "trace" -> trace()
        "simulate" -> simulate(args.ifEmpty { usage("simulate needs a topic.") })
        "inject" -> inject(args.getOrNull(0) ?: usage("inject needs a topic."), args.drop(1).ifEmpty { usage("inject needs an event.") })
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
        val welcome = session.welcome
        if (json) {
            println(buildJsonObject {
                put("methods", KitJson.encodeToJsonElement(ListSerializer(String.serializer()), welcome.methods))
                put("topics", KitJson.encodeToJsonElement(ListSerializer(String.serializer()), welcome.topics))
            })
            return
        }
        println("Methods")
        welcome.methods.forEach { println("  ${it.padEnd(NAME_WIDTH)}${methods[it].orEmpty()}") }
        println("Topics")
        welcome.topics.forEach { println("  ${it.padEnd(NAME_WIDTH)}${topics[it].orEmpty()}") }
    }

    private fun call(method: String, params: String?) {
        val result = rpc.call(method, params?.let(::parse))
        println(if (json) result.toString() else pretty.encodeToString(JsonElement.serializer(), result))
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
        val event = if (topic == Topics.BAND_GESTURES.name && !words.first().startsWith("{")) gesture(words) else parse(words.joinToString(" "))
        rpc.call(Shell.INJECT.name, KitJson.encodeToJsonElement(InjectRequest.serializer(), InjectRequest(topic, event)))
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
            if (b.borrowed) "borrowed by the phone" else "with the Display",
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
