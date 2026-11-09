package com.macedonga.visor.kit.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.protocol.GlassesInput
import com.macedonga.visor.kit.protocol.GlassesPair
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.Link
import com.macedonga.visor.kit.protocol.ThermalLevel
import kotlinx.coroutines.launch

@Composable
fun GlassesTab(lab: Lab, status: GlassesStatus?) {
    StatusPanel(status)
    PairsPanel(lab)
    InputsPanel(lab)
}

@Composable
private fun StatusPanel(s: GlassesStatus?) {
    Panel("Status") {
        if (s == null) {
            Absent("Waiting for glasses.status")
            return@Panel
        }
        if (!s.paired) {
            Absent("Visor has no glasses set up.")
            return@Panel
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(s.battery?.let { "$it%" } ?: "-", if (s.charging == true) "Charging" else "Battery", Modifier.weight(1f), if (s.charging == true) Ink.good else Ink.text)
            Stat(s.caseBattery?.let { "$it%" } ?: "-", "Case", Modifier.weight(1f))
            Stat(s.thermal?.id ?: "-", if (s.throttling == true) "Throttling" else "Thermal", Modifier.weight(1f), thermalColor(s.thermal))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(s.link.id.replaceFirstChar { it.uppercase() }, s.link == Link.CONNECTED, Ink.good)
            Tag("Worn", s.worn == true)
            Tag("Folded", s.folded == true, Ink.warn)
            Tag("Recording", s.recording == true, Ink.bad)
            Tag(if (s.display == true) "Display" else "No display", s.display == true)
        }
        HorizontalDivider(color = Ink.line)
        Fact("Model", s.model)
        Fact("Serial", s.serial)
        Fact("Firmware", s.firmware)
    }
}

@Composable
private fun Fact(name: String, value: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(name, color = Ink.dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value ?: "-", color = Ink.text, fontSize = 14.sp, fontFamily = Mono)
    }
}

private fun thermalColor(level: ThermalLevel?) = when (level) {
    null, ThermalLevel.NORMAL -> Ink.text
    ThermalLevel.LIGHT, ThermalLevel.MODERATE -> Ink.warn
    else -> Ink.bad
}

@Composable
private fun PairsPanel(lab: Lab) {
    val pairs by remember(lab.grants) { with(lab) { visor.glasses.pairs.logged("glasses.pairs") } }.collectAsState(null)
    Panel("Pairs in Visor") {
        val list = pairs
        when {
            list == null -> Absent("Waiting for glasses.pairs")
            list.isEmpty() -> Absent("No pairs set up.")
            else -> list.forEachIndexed { i, pair ->
                if (i > 0) HorizontalDivider(color = Ink.line)
                PairRow(pair) { lab.run("glasses.select") { it.glasses.select(pair.serial).name ?: pair.serial } }
            }
        }
    }
}

@Composable
private fun PairRow(pair: GlassesPair, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(pair.name ?: pair.serial, color = Ink.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            val look = listOfNotNull(pair.look?.styleName, pair.look?.colorName).joinToString(", ")
            val line = listOfNotNull(look.ifEmpty { null }, pair.battery?.let { "$it%" }, if (pair.display == true) "Display" else null).joinToString(", ")
            Text(line.ifEmpty { pair.serial }, color = Ink.dim, fontSize = 13.sp)
        }
        if (pair.active) Tag("Active", true, Ink.good) else FilledTonalButton(onSelect) { Text("Use") }
    }
}

@Composable
private fun InputsPanel(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    val seen = remember { mutableStateListOf<GlassesInput>() }
    val flash = remember { Animatable(0f) }
    Panel("Touchpad and buttons") {
        StreamSwitch("Follow glasses.inputs", on) { on = it }
        if (on) {
            LaunchedEffect(lab.grants) {
                with(lab) {
                    visor.glasses.inputs.logged("glasses.inputs").collect { input ->
                        seen.add(0, input)
                        if (seen.size > 12) seen.removeRange(12, seen.size)
                        launch {
                            flash.snapTo(1f)
                            flash.animateTo(0f)
                        }
                    }
                }
            }
        }
        val last = seen.firstOrNull()
        Box(
            Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(Ink.raised),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.matchParentSize().alpha(flash.value * 0.35f).background(Ink.live))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(last?.action?.id?.replace('_', ' ') ?: if (on) "Tap or swipe the glasses" else "Off", color = if (last != null) Ink.text else Ink.faint, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                if (last != null) Text(last.source.id.replace('_', ' '), color = Ink.dim, fontSize = 13.sp)
            }
        }
        seen.drop(1).forEach { input ->
            EventLine(input.action.id.replace('_', ' '), input.source.id.replace('_', ' '), Ink.dim)
        }
    }
}
