package com.macedonga.visor.kit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.protocol.Transcript
import com.macedonga.visor.kit.protocol.TranscriptRequest
import com.macedonga.visor.kit.protocol.VoiceRequest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun VoiceTab(lab: Lab) {
    TranscriptsPanel(lab)
    SpeakPanel(lab)
    RequestsPanel(lab)
    LaunchPanel()
}

@Composable
private fun TranscriptsPanel(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    var partials by remember { mutableStateOf(true) }
    var partial by remember { mutableStateOf<Transcript?>(null) }
    val heard = remember { mutableStateListOf<Transcript>() }
    Panel("What the glasses hear") {
        StreamSwitch("Follow voice.transcripts", on) { on = it }
        FilterChip(selected = partials, onClick = { partials = !partials }, label = { Text("As it is spoken") })
        if (on) {
            LaunchedEffect(partials, lab.grants) {
                with(lab) {
                    visor.voice.transcripts(TranscriptRequest { this.partials = partials }).logged("voice.transcripts").collect { t ->
                        if (t.partial) {
                            partial = t
                        } else {
                            partial = null
                            heard.add(0, t)
                            if (heard.size > 20) heard.removeRange(20, heard.size)
                        }
                    }
                }
            }
        }
        partial?.let { Text(it.text, color = Ink.live, fontSize = 18.sp) }
        if (heard.isEmpty() && partial == null) Absent(if (on) "Listening. Music pauses meanwhile." else "Off")
        heard.forEach { t -> EventLine(t.text, t.confidence?.let { "%.0f%%".format(it * 100) }) }
    }
}

@Composable
private fun SpeakPanel(lab: Lab) {
    var text by remember { mutableStateOf("Hello from VDK Lab.") }
    Panel("Say something") {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Text") })
        Button({ lab.run("voice.speak") { it.voice.speak(text) } }, enabled = text.isNotBlank()) { Text("Speak on the glasses") }
    }
}

@Composable
private fun RequestsPanel(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    var phrases by remember { mutableStateOf("lab, the lab") }
    val asked = remember { mutableStateListOf<Pair<VoiceRequest, String>>() }
    Panel("Questions for the app") {
        OutlinedTextField(phrases, { phrases = it }, Modifier.fillMaxWidth(), label = { Text("Phrases, comma separated") }, enabled = !on)
        StreamSwitch("Follow voice.requests", on) { on = it }
        if (on) {
            LaunchedEffect(phrases, lab.grants) {
                val list = phrases.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                with(lab) {
                    visor.voice.requests(list).logged("voice.requests").collect { request ->
                        val answer = answer(visor, request.text)
                        lab.run("voice.reply") { it.voice.reply(request, answer) }
                        asked.add(0, request to answer)
                    }
                }
            }
        }
        if (asked.isEmpty()) Absent(if (on) "\"Hey Meta, ask the lab for the battery\"" else "Off")
        asked.forEach { (request, reply) -> EventLine(request.text, reply) }
    }
}

/** What the app says back: the battery, the time, or what it heard. */
internal suspend fun answer(visor: Visor, question: String): String {
    val q = question.lowercase()
    return when {
        "battery" in q -> withTimeoutOrNull(2_000) { visor.glasses.status.first() }?.battery
            ?.let { "The glasses are at $it percent." } ?: "The lab does not know the battery yet."
        "time" in q -> "It is ${LocalTime.now().format(DateTimeFormatter.ofPattern("H:mm"))}."
        else -> "The lab heard: $question"
    }
}

@Composable
private fun LaunchPanel() {
    val phrases = LocalContext.current.resources.getStringArray(R.array.voice_phrases)
    Panel("Started by voice") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            phrases.forEach { Tag(it, true) }
        }
    }
}
