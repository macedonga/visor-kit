package com.macedonga.visor.kit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.LiveVideoDecoder
import com.macedonga.visor.kit.RequestScopes
import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.VisorState
import com.macedonga.visor.kit.protocol.LiveConfig
import com.macedonga.visor.kit.protocol.LiveQuality
import com.macedonga.visor.kit.protocol.Scope
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/** Every capability of the VDK on one screen: the app's side of the VDK, tried on the phone. */
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Sample() }
            }
        }
    }
}

private val CARDS = listOf("weather", "stocks", "pasta")

@Composable
private fun Sample() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    var visor by remember { mutableStateOf<Visor?>(null) }
    // Raised after the consent screen, so the streams the person just allowed start again.
    var grants by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(RequestScopes()) { held ->
        log.add(0, "Holds: ${held.joinToString().ifEmpty { "nothing" }}")
        grants++
    }

    fun run(what: String, block: suspend (Visor) -> Any?) {
        val v = visor ?: return
        scope.launch {
            val answer = try {
                block(v)?.takeUnless { it == Unit }?.toString() ?: "done"
            } catch (e: KitException) {
                "${e.code}: ${e.message}"
            }
            log.add(0, "$what: $answer")
        }
    }

    LaunchedEffect(Unit) {
        visor = try {
            Visor.connect(context)
        } catch (e: KitException) {
            log.add(0, "connect: ${e.code}: ${e.message}")
            null
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Visor Development Kit", style = MaterialTheme.typography.headlineSmall)
        val v = visor
        if (v != null) {
            val state by v.state.collectAsState()
            Text(
                when (val s = state) {
                    VisorState.Connecting -> "Connecting to Visor"
                    is VisorState.Open -> "Visor ${s.welcome.visor}, protocol ${s.welcome.protocol}"
                    is VisorState.Closed -> "Closed: ${s.error?.message}"
                },
            )
            Button({ ask.launch(setOf(Scope.STATE, Scope.INPUTS, Scope.MICROPHONE, Scope.CAMERA, Scope.BAND, Scope.DISPLAY, Scope.SETTINGS, Scope.CONVERSATIONS)) }) { Text("Grant access") }

            val glasses by remember(v, grants) { v.glasses.status.logged(log, "glasses") }.collectAsState(null)
            Text("Glasses: ${glasses ?: "-"}", style = MaterialTheme.typography.bodySmall)
            val band by remember(v, grants) { v.band.status.logged(log, "band") }.collectAsState(null)
            Text("Band: ${band ?: "-"}", style = MaterialTheme.typography.bodySmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ run("photo") { it.camera.takePhoto() } }) { Text("Take photo") }
                OutlinedButton({ run("video") { it.camera.startVideo() } }) { Text("Start video") }
                OutlinedButton({ run("stop") { it.camera.stopVideo() } }) { Text("Stop") }
                OutlinedButton({ run("captures") { it.captures.list() } }) { Text("Captures") }
                OutlinedButton({ run("buzz") { it.band.buzz() } }) { Text("Buzz") }
                OutlinedButton({ run("sound") { it.settings.list("Sound").map { s -> "${s.label ?: s.key} = ${s.value}" } } }) { Text("Sound settings") }
                OutlinedButton({ run("history") { it.assistant.history().take(3).map { t -> t.question } } }) { Text("Last questions") }
            }

            var listening by remember { mutableStateOf(false) }
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                Switch(listening, { listening = it })
                Text("  Band gestures (borrows the band)")
            }
            if (listening) {
                LaunchedEffect(v, grants) {
                    v.band.gestures.logged(log, "gestures").collect { log.add(0, "gesture: ${it.finger} ${it.action} ${it.derived ?: ""}") }
                }
            }

            var touches by remember { mutableStateOf(false) }
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                Switch(touches, { touches = it })
                Text("  Touchpad and buttons")
            }
            if (touches) {
                LaunchedEffect(v, grants) {
                    v.glasses.inputs.logged(log, "inputs").collect { log.add(0, "input: ${it.source} ${it.action}") }
                }
            }

            var hearing by remember { mutableStateOf(false) }
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                Switch(hearing, { hearing = it })
                Text("  What the glasses hear")
            }
            if (hearing) {
                LaunchedEffect(v, grants) {
                    v.voice.transcripts().logged(log, "transcripts").collect { log.add(0, "heard: ${it.text}") }
                }
            }

            var asking by remember { mutableStateOf(false) }
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                Switch(asking, { asking = it })
                Text("  What the wearer asks the assistant")
            }
            if (asking) {
                LaunchedEffect(v, grants) {
                    v.assistant.turns.logged(log, "turns").collect { log.add(0, "asked: ${it.question} -> ${it.answer.ifEmpty { it.failure }}") }
                }
            }

            var watching by remember { mutableStateOf(false) }
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                Switch(watching, { watching = it })
                Text("  Camera live")
            }
            if (watching) {
                LivePreview(v, grants, log)
                OutlinedButton({ run("live photo") { it.camera.takeLivePhoto().uri } }) { Text("Take live photo") }
            }

            var script by remember { mutableStateOf("Hello from the Visor Development Kit") }
            OutlinedTextField(script, { script = it }, Modifier.fillMaxWidth(), label = { Text("Teleprompter") })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ run("prompter") { it.display.prompter.show(script) } }) { Text("Show") }
                OutlinedButton({ run("prompter") { it.display.prompter.close() } }) { Text("Close") }
            }

            // The recipes of docs/briefing-cards.md: cards on the Display home's Briefing.
            Text("Briefing", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ run("weather card") { it.display.cards.set(weatherCard()) } }) { Text("Weather card") }
                OutlinedButton({ run("stocks card") { it.display.cards.set(stocksCard()) } }) { Text("Stocks card") }
                OutlinedButton({ run("timer") { kitchenTimer(it, 4.minutes) } }) { Text("Timer, 4 min") }
                OutlinedButton({ run("remove") { CARDS.forEach { id -> it.display.cards.remove(id) } } }) { Text("Remove cards") }
            }
        }
        log.take(LOG_LINES).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

/** The camera live on a SurfaceView, decoded by the phone, for as long as it is shown. */
@Composable
private fun LivePreview(visor: Visor, grants: Int, log: MutableList<String>) {
    var surface by remember { mutableStateOf<Surface?>(null) }
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        surface = holder.surface
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surface = null
                    }
                })
            }
        },
        // The glasses film upright.
        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
    )
    val target = surface ?: return
    LaunchedEffect(visor, grants, target) {
        LiveVideoDecoder(target).use { video ->
            visor.camera.live(LiveConfig { quality = LiveQuality.HIGH })
                .logged(log, "live")
                .flowOn(Dispatchers.IO)
                .collect { video.decode(it) }
        }
    }
}

/** The flow, with its failure written to [log] instead of thrown: a stream ends on a missing scope, for one. */
private fun <T> Flow<T>.logged(log: MutableList<String>, what: String): Flow<T> =
    catch { e -> log.add(0, "$what: ${(e as? KitException)?.code ?: "error"}: ${e.message}") }

private const val LOG_LINES = 30
