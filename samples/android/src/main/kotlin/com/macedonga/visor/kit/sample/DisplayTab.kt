package com.macedonga.visor.kit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macedonga.visor.kit.protocol.Layout
import com.macedonga.visor.kit.protocol.LayoutIconName
import com.macedonga.visor.kit.protocol.Link
import com.macedonga.visor.kit.protocol.MotionRequest
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

private val CARDS = listOf("weather", "stocks", "pasta")

/** What only glasses with a display and a Neural Band do. */
@Composable
fun DisplayTab(lab: Lab) {
    PrompterPanel(lab)
    LayoutPanel(lab)
    CardsPanel(lab)
    NoticePanel(lab)
    BandPanel(lab)
}

@Composable
private fun PrompterPanel(lab: Lab) {
    var script by remember { mutableStateOf("Hello from VDK Lab. This text scrolls on the Display as you read it.") }
    Panel("Teleprompter") {
        OutlinedTextField(script, { script = it }, Modifier.fillMaxWidth(), label = { Text("Script") }, minLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ lab.run("display.prompter.show") { it.display.prompter.show(script) } }) { Text("Show") }
            OutlinedButton({ lab.run("display.prompter.close") { it.display.prompter.close() } }) { Text("Close") }
        }
    }
}

@Composable
private fun LayoutPanel(lab: Lab) {
    var count by remember { mutableIntStateOf(0) }
    var showing by remember { mutableStateOf(false) }
    val clicks = remember { mutableStateListOf<String>() }
    fun layout() = Layout {
        gap = 12
        padding = 24
        row {
            gap = 10
            icon(LayoutIconName.CHART_LINE)
            text("VDK Lab")
        }
        text("Pressed $count times")
        buttons {
            button("more", "Count")
            button("reset", "Reset")
        }
    }
    LaunchedEffect(lab.grants) {
        with(lab) {
            launch { visor.display.layout.state.logged("display.layout.state").collect { showing = it.showing } }
            visor.display.layout.clicks.logged("display.layout.clicks").collect { click ->
                count = if (click.id == "reset") 0 else count + 1
                clicks.add(0, click.id)
                lab.run("display.layout.show") { it.display.layout.show(layout()) }
            }
        }
    }
    Panel("A page of the app's", trailing = { Tag(if (showing) "Showing" else "Hidden", showing) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ lab.run("display.layout.show") { it.display.layout.show(layout()) } }) { Text("Show") }
            OutlinedButton({ lab.run("display.layout.close") { it.display.layout.close() } }) { Text("Close") }
        }
        clicks.take(6).forEach { EventLine(it, null, Ink.dim) }
    }
}

@Composable
private fun CardsPanel(lab: Lab) {
    val clicks = remember { mutableStateListOf<String>() }
    LaunchedEffect(lab.grants) {
        with(lab) { visor.display.cards.clicks.logged("display.cards.clicks").collect { clicks.add(0, "${it.card}: ${it.id}") } }
    }
    // The recipes of the Briefing cards page of the docs, in Cards.kt.
    Panel("Briefing cards") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton({ lab.run("weather card") { it.display.cards.set(weatherCard()) } }) { Text("Weather") }
            FilledTonalButton({ lab.run("stocks card") { it.display.cards.set(stocksCard()) } }) { Text("Stocks") }
            FilledTonalButton({ lab.run("timer") { kitchenTimer(it, 4.minutes) } }) { Text("Timer, 4 min") }
            OutlinedButton({ lab.run("remove cards") { CARDS.forEach { id -> it.display.cards.remove(id) } } }) { Text("Remove") }
        }
        clicks.take(6).forEach { EventLine(it, null, Ink.dim) }
    }
}

@Composable
private fun NoticePanel(lab: Lab) {
    var text by remember { mutableStateOf("The oven is preheated.") }
    Panel("Notification") {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Text") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ lab.run("notify.post") { it.notify.post("lab", "VDK Lab", text) } }) { Text("Post") }
            OutlinedButton({ lab.run("notify.remove") { it.notify.remove("lab") } }) { Text("Remove") }
        }
    }
}

@Composable
private fun BandPanel(lab: Lab) {
    val band by remember(lab.grants) { with(lab) { visor.band.status.logged("band.status") } }.collectAsState(null)
    var gestures by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf(false) }
    val seen = remember { mutableStateListOf<String>() }
    val trace = remember { MotionTrace() }
    Panel("Neural Band") {
        val b = band
        if (b == null || !b.paired) {
            Absent("No band")
            return@Panel
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(b.battery?.let { "$it%" } ?: "-", if (b.charging == true) "Charging" else "Battery", Modifier.weight(1f))
            Stat(if (b.leftHanded == true) "Left" else "Right", "Wrist", Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(b.link.id.replaceFirstChar { it.uppercase() }, b.link == Link.CONNECTED, Ink.good)
            Tag("Shared with the phone", b.shared)
            Tag("Borrowed", b.borrowed, Ink.warn)
        }
        Button({ lab.run("band.buzz") { it.band.buzz() } }) { Text("Buzz") }
        StreamSwitch("Follow band.gestures", gestures) { gestures = it }
        if (gestures) {
            LaunchedEffect(lab.grants) {
                with(lab) {
                    visor.band.gestures.logged("band.gestures").collect { g ->
                        seen.add(0, listOfNotNull(g.finger.id, g.action.id, g.derived?.id).joinToString(" "))
                        if (seen.size > 8) seen.removeRange(8, seen.size)
                    }
                }
            }
        }
        seen.forEach { EventLine(it.replace('_', ' '), null, Ink.dim) }
        StreamSwitch("Follow band.motion", motion) { motion = it }
        if (motion) {
            LaunchedEffect(lab.grants) {
                trace.clear()
                with(lab) { visor.band.motion(MotionRequest.DEFAULT_RATE_HZ).logged("band.motion").collect(trace::add) }
            }
            Trace("Accelerometer", "m/s²", trace, trace.accel, floor = 12f)
            Trace("Gyroscope", "rad/s", trace, trace.gyro, floor = 2f)
        }
    }
}
