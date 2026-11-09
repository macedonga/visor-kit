package com.macedonga.visor.kit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.RequestScopes
import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.VisorState
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.Link

/** Every part of the VDK, one tab each, on whatever glasses Visor runs. */
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LabColors) {
                Surface(Modifier.fillMaxSize(), color = Ink.page) { App() }
            }
        }
    }
}

private enum class Page(val title: String) { GLASSES("Glasses"), MOTION("Motion"), CAMERA("Camera"), VOICE("Voice"), ASSISTANT("Assistant"), SETTINGS("Settings"), DISPLAY("Display and band"), LOG("Log") }

@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lab by remember { mutableStateOf<Lab?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!Visor.isInstalled(context)) {
            problem = "Visor is not installed on this phone."
            return@LaunchedEffect
        }
        try {
            lab = Lab(Visor.connect(context), scope)
        } catch (e: KitException) {
            problem = "${e.code}: ${e.message}"
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        val current = lab
        if (current == null) {
            Header(null, null, onAsk = {})
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(problem ?: "Connecting to Visor", color = if (problem != null) Ink.bad else Ink.dim, fontSize = 15.sp)
            }
            return@Column
        }
        Connected(current)
    }
}

@Composable
private fun Connected(lab: Lab) {
    val ask = rememberLauncherForActivityResult(RequestScopes()) { held ->
        lab.held = held
        lab.grants++
        lab.note("Holds ${held.size} of ${SCOPES.size} scopes")
    }
    LaunchedEffect(lab.grants) {
        lab.held = runCatching { lab.visor.scopes() }.getOrDefault(emptySet())
    }
    val status by remember(lab.grants) { with(lab) { visor.glasses.status.logged("glasses.status") } }.collectAsState(null)
    val state by lab.visor.state.collectAsState()

    Header(status, state, onAsk = { ask.launch(SCOPES) }, missing = SCOPES - lab.held)

    // The Display and band tab only for glasses with a display: other pairs have neither.
    val pages = Page.entries.filter { it != Page.DISPLAY || status?.display == true }
    var selected by rememberSaveable { mutableStateOf(Page.GLASSES.name) }
    val page = pages.firstOrNull { it.name == selected } ?: Page.GLASSES
    PrimaryScrollableTabRow(selectedTabIndex = pages.indexOf(page), edgePadding = 16.dp, containerColor = Ink.page) {
        pages.forEach { p ->
            Tab(selected = p == page, onClick = { selected = p.name }, text = { Text(p.title) })
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        when (page) {
            Page.GLASSES -> GlassesTab(lab, status)
            Page.MOTION -> MotionTab(lab)
            Page.CAMERA -> CameraTab(lab, status)
            Page.VOICE -> VoiceTab(lab)
            Page.ASSISTANT -> AssistantTab(lab, status)
            Page.SETTINGS -> SettingsTab(lab)
            Page.DISPLAY -> DisplayTab(lab)
            Page.LOG -> LogTab(lab)
        }
        Spacer(Modifier.size(24.dp))
    }
}

/** The pair and the connection in one line, and the way to the consent screen while scopes are missing. */
@Composable
private fun Header(status: GlassesStatus?, state: VisorState?, onAsk: () -> Unit, missing: Set<com.macedonga.visor.kit.protocol.Scope> = emptySet()) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val link = status?.link
            Box(Modifier.size(10.dp).clip(CircleShape).background(when (link) { Link.CONNECTED -> Ink.good; Link.CONNECTING -> Ink.warn; else -> Ink.faint }))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(status?.name ?: status?.model ?: "VDK Lab", color = Ink.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        state is VisorState.Open -> listOfNotNull(status?.model, "Visor ${state.welcome.visor}").joinToString(", ")
                        state is VisorState.Closed -> "Visor closed the connection"
                        else -> "Connecting to Visor"
                    },
                    color = Ink.dim, fontSize = 13.sp,
                )
            }
            if (status?.battery != null) Text("${status.battery}%", color = if (status.charging == true) Ink.good else Ink.text, fontSize = 18.sp, fontFamily = Mono)
        }
        if (state is VisorState.Open && missing.isNotEmpty()) {
            Button(onAsk, Modifier.fillMaxWidth()) { Text("Allow ${missing.size} more on Visor") }
        }
    }
}

@Composable
private fun LogTab(lab: Lab) {
    Panel("Every call and stream, newest first") {
        if (lab.log.isEmpty()) Absent("Nothing yet.")
        lab.log.forEach { line -> EventLine(line.text, line.time, if (line.failed) Ink.bad else Ink.text) }
    }
}
