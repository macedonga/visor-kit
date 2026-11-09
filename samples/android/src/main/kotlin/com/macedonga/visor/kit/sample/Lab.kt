package com.macedonga.visor.kit.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.Visor
import com.macedonga.visor.kit.protocol.Scope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Every scope the app uses. Raw messages stay with the `visor` command and network clients. */
val SCOPES: Set<Scope> = setOf(
    Scope.STATE, Scope.PAIRS, Scope.INPUTS, Scope.MOTION, Scope.MICROPHONE, Scope.CAMERA, Scope.BAND, Scope.DISPLAY,
    Scope.VOICE, Scope.ASSISTANT, Scope.NOTIFY, Scope.SETTINGS, Scope.CONVERSATIONS,
)

/** One connection to Visor and what the app has said about it: the log every tab writes to. */
@Stable
class Lab(val visor: Visor, private val scope: CoroutineScope) {
    val log = mutableStateListOf<Line>()

    /** Raised after the consent screen, so streams the person just allowed start again. */
    var grants by mutableIntStateOf(0)

    var held by mutableStateOf<Set<Scope>>(emptySet())

    fun note(text: String, failed: Boolean = false) {
        log.add(0, Line(LocalTime.now().format(CLOCK), text, failed))
        if (log.size > LOG_LINES) log.removeRange(LOG_LINES, log.size)
    }

    /** Runs a call and writes what Visor answered, or why it failed. */
    fun run(what: String, block: suspend (Visor) -> Any?) {
        scope.launch {
            try {
                val answer = block(visor)?.takeUnless { it == Unit }?.toString()
                note(if (answer == null) what else "$what: $answer")
            } catch (e: KitException) {
                note("$what: ${e.code} ${e.message}", failed = true)
            }
        }
    }

    /** The stream, with the failure that ends it written to the log instead of thrown. */
    fun <T> Flow<T>.logged(what: String): Flow<T> =
        catch { e -> note("$what ended: ${(e as? KitException)?.code ?: "error"} ${e.message}", failed = true) }

    class Line(val time: String, val text: String, val failed: Boolean)

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        const val LOG_LINES = 200
    }
}

/** The palette: ink on graphite, one cool accent for what is live. */
object Ink {
    val page = Color(0xFF0E0F11)
    val panel = Color(0xFF17191C)
    val raised = Color(0xFF22252A)
    val line = Color(0xFF2C3036)
    val text = Color(0xFFECEEF1)
    val dim = Color(0xFF9BA1AA)
    val faint = Color(0xFF636A74)
    val live = Color(0xFF5CC8FF)
    val good = Color(0xFF6BD49B)
    val warn = Color(0xFFFFB45C)
    val bad = Color(0xFFFF6B6B)
    val x = Color(0xFFFF7A7A)
    val y = Color(0xFF7BE0A0)
    val z = Color(0xFF7AB8FF)
}

val LabColors = darkColorScheme(
    primary = Ink.live,
    onPrimary = Color(0xFF00121C),
    secondary = Ink.dim,
    background = Ink.page,
    surface = Ink.page,
    surfaceVariant = Ink.raised,
    surfaceContainer = Ink.panel,
    surfaceContainerHigh = Ink.raised,
    surfaceContainerHighest = Ink.raised,
    onSurface = Ink.text,
    onSurfaceVariant = Ink.dim,
    outline = Ink.line,
    outlineVariant = Ink.line,
    error = Ink.bad,
)

val Mono = FontFamily.Monospace

/** A group of related controls on a raised panel, with its name above. */
@Composable
fun Panel(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Ink.dim, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            trailing()
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ink.panel).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** A labelled switch for a stream that runs while it is on. */
@Composable
fun StreamSwitch(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(on, onChange)
    }
}

/** A small rounded label: a state such as Worn or Recording, lit when it holds. */
@Composable
fun Tag(text: String, lit: Boolean, color: Color = Ink.live) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (lit) color.copy(alpha = 0.16f) else Ink.raised)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = if (lit) color else Ink.faint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** A value with its name under it. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = Ink.text) {
    Column(modifier) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = Mono)
        Text(label, color = Ink.faint, fontSize = 12.sp)
    }
}

/** A line of the stream a tab follows, newest on top. */
@Composable
fun EventLine(text: String, detail: String? = null, color: Color = Ink.text) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(text, color = color, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, color = Ink.faint, fontSize = 12.sp, fontFamily = Mono, modifier = Modifier.padding(start = 8.dp))
    }
}

/** What a tab shows while the glasses cannot do it. */
@Composable
fun Absent(text: String) {
    Text(text, color = Ink.faint, fontSize = 14.sp)
}
