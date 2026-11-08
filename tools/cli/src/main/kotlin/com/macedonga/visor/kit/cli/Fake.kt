package com.macedonga.visor.kit.cli

import com.macedonga.visor.kit.protocol.CaptureType
import com.macedonga.visor.kit.protocol.Finger
import com.macedonga.visor.kit.protocol.GestureAction
import com.macedonga.visor.kit.protocol.InputAction
import com.macedonga.visor.kit.protocol.InputSource
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutNode
import com.macedonga.visor.kit.protocol.LayoutState
import com.macedonga.visor.kit.protocol.LayoutVideoState
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.ThermalLevel
import com.macedonga.visor.kit.server.Caller
import com.macedonga.visor.kit.server.JsonRpcSession
import com.macedonga.visor.kit.server.McpAdapter
import com.macedonga.visor.kit.testing.FakeVisor
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val FAKE_USAGE = """
Usage: visor fake [--port PORT] [--lan]

FakeVisor on this computer, for building without a phone or glasses. Apps and scripts reach it as they reach
Visor's network door: JSON-RPC over WebSocket on ws://127.0.0.1:PORT/rpc and MCP on http://127.0.0.1:PORT/mcp
(7333 by default; no token). The visor command reaches it with --fake, for example: visor --fake watch
band.gestures. The Display simulator is at http://127.0.0.1:PORT/display. Every scope is granted; grant and
revoke below change that.

Type here to play the glasses:
  battery N                 The glasses' battery, 0 to 100.
  charging|worn|folded|connected|display on|off
  thermal LEVEL             normal, light, moderate, severe, critical.
  input SOURCE ACTION       A touch, a press or the Display's navigation: touchpad tap, band nav_left, ...
  input SOURCE ACTION X Y DX DY   A drag step: band drag_move 120 40 6 0.
  gesture FINGER ACTION     A band gesture: index tap, middle double_tap, ...
  motion AX AY AZ           An accelerometer reading in m/s², for glasses.motion: motion 0 9.8 0.
  photo|video               A new capture on the glasses.
  ask QUESTION              The wearer asks; the app whose phrase the question holds answers.
  say TEXT                  The glasses hear TEXT, for voice.transcripts.
  tool NAME [JSON]          The assistant calls an app's tool with JSON arguments.
  turn QUESTION / ANSWER    The wearer asks the assistant QUESTION and it answers ANSWER.
  click ID                  A tap on an element of the layout on the Display.
  back                      The wearer goes back: the layout on the Display goes.
  video STATE [ERROR]       The layout's video: playing, paused, ended, or failed with an error.
  grant|revoke SCOPE...     Changes the scopes the fake person granted.
  quit
""".trim()

private val SIMULATOR_FILES = mapOf(
    "renderer.js" to ContentType.Text.JavaScript,
    "display.css" to ContentType.Text.CSS,
    "simulator.html" to ContentType.Text.Html,
)

/** The port the visor command reaches the fake on, next to the network door's. */
internal fun fakeShellPort(port: Int): Int = port + 1

/** `visor fake`: FakeVisor behind the doors Visor opens on a phone, with a console that plays the glasses. */
internal class FakeServer(private val port: Int, private val lan: Boolean, version: String) {
    private val fake = FakeVisor()
    private val mcp = McpAdapter(fake.dispatcher, "Visor fake", version)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun run() {
        val host = if (lan) "0.0.0.0" else "127.0.0.1"
        // Ktor reports a taken port from a worker thread after start returns, so the port is tried first.
        try {
            ServerSocket(port, 1, InetAddress.getByName(host)).close()
        } catch (e: IOException) {
            throw CliError("Port $port is taken, by another visor fake or another program. Pick one with --port.")
        }
        embeddedServer(CIO, port = port, host = host) {
            install(WebSockets)
            routing {
                webSocket("/rpc") {
                    // Messages wait in order here, so a slow reader never makes the fake think the client went away.
                    val out = Channel<String>(Channel.UNLIMITED)
                    val writer = launch { for (message in out) outgoing.send(Frame.Text(message)) }
                    val session = JsonRpcSession(fake.dispatcher, fake.caller, send = { out.trySend(it).isSuccess }, close = { out.close() })
                    try {
                        for (frame in incoming) if (frame is Frame.Text) session.receive(frame.readText())
                    } finally {
                        session.end()
                        out.close()
                        writer.join()
                    }
                }
                post("/mcp") {
                    val answer = mcp.answer(fake.caller, call.receiveText())
                    if (answer == null) call.respond(HttpStatusCode.Accepted) else call.respondText(answer, ContentType.Application.Json)
                }
                get("/display") { call.respondText(simulatorFile("simulator.html"), ContentType.Text.Html) }
                get("/display/{file}") {
                    val name = call.parameters["file"]
                    val type = SIMULATOR_FILES[name] ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respondText(simulatorFile(name!!), type)
                }
                webSocket("/display/ws") { simulator(this) }
            }
        }.start(wait = false)
        val shell = try {
            ServerSocket(fakeShellPort(port), BACKLOG, InetAddress.getLoopbackAddress())
        } catch (e: IOException) {
            throw CliError("Port ${fakeShellPort(port)} is taken: ${e.message}")
        }
        Thread({ accept(shell) }, "visor-fake-shell").apply { isDaemon = true }.start()
        report()
        System.err.println("FakeVisor on ws://$host:$port/rpc and http://$host:$port/mcp; visor --fake on port ${fakeShellPort(port)}. Type help.")
        System.err.println("The Display simulator: http://127.0.0.1:$port/display")
        while (true) {
            val line = readlnOrNull()?.trim() ?: return
            if (line.isEmpty()) continue
            if (line == "quit" || line == "exit") return
            try {
                play(line.split(Regex("\\s+"), limit = 3), line)
            } catch (e: CliError) {
                System.err.println("visor: ${e.message}")
            } catch (e: IllegalArgumentException) {
                System.err.println("visor: ${e.message}")
            } catch (e: IllegalStateException) {
                System.err.println("visor: ${e.message}")
            }
        }
    }

    // The visor command's connections: JSON-RPC lines, as on Visor's adb socket, with the shell's own methods.
    private fun accept(server: ServerSocket) {
        while (true) {
            val socket = runCatching { server.accept() }.getOrNull() ?: return
            Thread({ serve(socket) }, "visor-fake-line").apply { isDaemon = true }.start()
        }
    }

    private fun serve(socket: Socket) {
        val out = socket.getOutputStream().bufferedWriter()
        val send = { line: String ->
            synchronized(out) {
                runCatching {
                    out.write(line)
                    out.write("\n")
                    out.flush()
                }.isSuccess
            }
        }
        val session = JsonRpcSession(fake.dispatcher, Caller(SHELL_UID, SHELL, shell = true), send, close = { socket.close() })
        try {
            socket.getInputStream().bufferedReader().forEachLine(session::receive)
        } catch (_: IOException) {
            // The command went away.
        } finally {
            session.end()
        }
    }

    private fun play(words: List<String>, line: String) {
        val glasses = fake.glasses
        val rest = line.substringAfter(' ', "").trim()
        when (words[0]) {
            "help" -> println(FAKE_USAGE)
            "battery" -> glasses.battery = rest.toIntOrNull()?.takeIf { it in 0..100 } ?: throw CliError("battery needs 0 to 100.")
            "charging" -> glasses.charging = onOff(words)
            "worn" -> glasses.worn = onOff(words)
            "folded" -> glasses.folded = onOff(words)
            "connected" -> glasses.connected = onOff(words)
            "display" -> glasses.display = onOff(words)
            "thermal" -> glasses.thermal = ThermalLevel(rest.ifEmpty { throw CliError("thermal needs a level.") })
            "input" -> {
                val all = line.split(Regex("\\s+")).drop(1)
                val at = all.drop(2).mapNotNull(String::toDoubleOrNull)
                when {
                    all.size == 2 -> glasses.input(InputSource(all[0]), InputAction(all[1]))
                    all.size == 6 && at.size == 4 -> glasses.drag(InputSource(all[0]), InputAction(all[1]), at[0], at[1], at[2], at[3])
                    else -> throw CliError("Type input SOURCE ACTION, or input SOURCE ACTION X Y DX DY for a drag.")
                }
            }
            "gesture" -> pair(words, "gesture FINGER ACTION").let { (a, b) -> fake.band.gesture(Finger(a), GestureAction(b)) }
            "motion" -> {
                val axes = words.drop(1).joinToString(" ").split(Regex("\\s+")).mapNotNull(String::toFloatOrNull)
                if (axes.size != 3) throw CliError("Type motion AX AY AZ, in m/s².")
                if (glasses.motionRate.value == null) say("Motion: no app collects motion; nobody got it")
                glasses.motion(axes[0], axes[1], axes[2])
            }
            "photo" -> say("Glasses: ${glasses.add(CaptureType.PHOTO).id} taken")
            "video" -> say("Glasses: ${glasses.add(CaptureType.VIDEO).id} filmed")
            "ask" -> {
                val question = rest.ifEmpty { throw CliError("ask needs a question.") }
                scope.launch { answer(question) }
            }
            "say" -> {
                if (fake.voice.listening.value == 0) say("Voice: no app collects transcripts; nobody heard it")
                fake.voice.say(rest)
            }
            "tool" -> {
                val name = words.getOrNull(1) ?: throw CliError("tool needs a name.")
                val arguments = words.getOrNull(2)?.let { parseObject(it) } ?: JsonObject(emptyMap())
                scope.launch {
                    val answer = fake.assistant.call(name, arguments)
                    say(if (answer == null) "Assistant: no app answered $name" else "Assistant: $name answered \"${answer.text}\"" + if (answer.error) " (an error)" else "")
                }
            }
            "turn" -> {
                val (question, answer) = rest.split(" / ", limit = 2).takeIf { it.size == 2 } ?: throw CliError("Type turn QUESTION / ANSWER.")
                fake.assistant.turn(question.trim(), answer.trim())
            }
            "click" -> fake.display.click(words.getOrNull(1) ?: throw CliError("click needs an element id."))
            "back" -> fake.display.back()
            "video" -> fake.display.video(
                LayoutVideoState(words.getOrNull(1) ?: throw CliError("video needs a state: playing, paused, ended or failed.")),
                words.getOrNull(2),
            )
            "grant" -> fake.granted = fake.granted + scopes(words)
            "revoke" -> fake.granted = fake.granted - scopes(words).toSet()
            else -> throw CliError("No command ${words[0]}. Type help.")
        }
    }

    private suspend fun answer(question: String) {
        val reply = fake.voice.ask(question)
        say(if (reply == null) "Voice: no app answered" else "Voice: the glasses say \"${reply.text}\"" + if (reply.listen) ", then listen" else "")
    }

    // What apps do to the fake glasses, as it happens.
    private fun report() {
        scope.launch {
            fake.display.prompter.drop(1).collect { say(if (it == null) "Display: the prompter closed" else "Display: the prompter shows \"${it.take(SHOWN)}\"") }
        }
        scope.launch {
            fake.display.layout.drop(1).collect { if (it != null) say("Display: a layout of ${count(it.root)} elements") }
        }
        scope.launch {
            fake.display.state.drop(1).collect { if (!it.showing) say("Display: the layout went (${it.reason})") }
        }
        scope.launch {
            var before = fake.notify.shown.value
            fake.notify.shown.drop(1).collect { now ->
                (now.keys - before.keys).forEach { say("Display: notification ${it}: ${now.getValue(it).title}: ${now.getValue(it).text}") }
                (before.keys - now.keys).forEach { say("Display: notification $it removed") }
                before = now
            }
        }
        scope.launch {
            fake.band.buzzes.drop(1).collect { all -> all.lastOrNull()?.let { say("Band: a buzz of ${it.inWholeMilliseconds} ms") } }
        }
        scope.launch { fake.live.watching.drop(1).collect { say(if (it) "Camera: live" else "Camera: live stopped") } }
        scope.launch { fake.voice.listening.drop(1).collect { say("Voice: $it collecting transcripts") } }
    }

    /** The Display simulator's page: the fake Display's layout as it changes, and the wearer's clicks, back and video back. */
    private suspend fun simulator(socket: DefaultWebSocketServerSession) {
        val updates = socket.launch {
            combine(fake.display.layout, fake.display.state) { layout, state ->
                buildJsonObject {
                    put("layout", layout?.let { KitJson.encodeToJsonElement(Layout.serializer(), it) } ?: JsonNull)
                    put("state", KitJson.encodeToJsonElement(LayoutState.serializer(), state))
                }.toString()
            }.collect { socket.outgoing.send(Frame.Text(it)) }
        }
        try {
            for (frame in socket.incoming) {
                if (frame !is Frame.Text) continue
                val message = runCatching { KitJson.parseToJsonElement(frame.readText()).jsonObject }.getOrNull() ?: continue
                runCatching {
                    message["click"]?.jsonPrimitive?.contentOrNull?.let(fake.display::click)
                    message["input"]?.jsonPrimitive?.contentOrNull?.let { fake.glasses.input(InputSource.TOUCHPAD, InputAction(it)) }
                    if (message["back"] != null) fake.display.back()
                    message["video"]?.jsonObject?.let { video ->
                        val state = video["state"]?.jsonPrimitive?.contentOrNull ?: return@let
                        fake.display.video(LayoutVideoState(state), video["error"]?.jsonPrimitive?.contentOrNull)
                    }
                }.onFailure { say("Simulator: ${it.message}") }
            }
        } finally {
            updates.cancel()
        }
    }

    private fun simulatorFile(name: String): String =
        checkNotNull(FakeVisor::class.java.getResourceAsStream("display/$name")) { "The simulator's $name is missing from the build." }
            .use { it.readBytes().decodeToString() }

    private fun count(node: LayoutNode): Int = 1 + node.children.orEmpty().sumOf(::count)

    private fun say(text: String) = println("${LocalTime.now().format(clock)}  $text")

    private fun onOff(words: List<String>): Boolean = when (words.getOrNull(1)) {
        "on" -> true
        "off" -> false
        else -> throw CliError("${words[0]} needs on or off.")
    }

    private fun pair(words: List<String>, shape: String): Pair<String, String> {
        val parts = words.drop(1).joinToString(" ").split(Regex("\\s+"))
        if (parts.size != 2) throw CliError("Type $shape.")
        return parts[0] to parts[1]
    }

    private fun scopes(words: List<String>): List<Scope> =
        words.drop(1).joinToString(" ").split(Regex("\\s+")).filter { it.isNotEmpty() }.map(::Scope).ifEmpty { throw CliError("Name a scope.") }

    private fun parseObject(text: String): JsonObject = try {
        KitJson.parseToJsonElement(text).jsonObject
    } catch (e: IllegalArgumentException) {
        throw CliError("Not a JSON object: $text")
    }

    private companion object {
        const val SHELL = "shell"
        const val SHELL_UID = 2000
        const val BACKLOG = 8
        const val SHOWN = 60
    }
}
