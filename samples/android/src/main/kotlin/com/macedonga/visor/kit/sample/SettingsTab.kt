package com.macedonga.visor.kit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.protocol.GlassesSetting
import com.macedonga.visor.kit.protocol.SettingKind

@Composable
fun SettingsTab(lab: Lab) {
    // By key, in the order settings.list gave them; each change on settings.changes replaces its entry.
    val settings = remember { mutableStateMapOf<String, GlassesSetting>() }
    val order = remember { mutableStateMapOf<String, Int>() }

    fun load() = lab.run("settings.list") { v ->
        val list = v.settings.list()
        list.forEachIndexed { i, s ->
            settings[s.key] = s
            order[s.key] = i
        }
        "${list.size} settings, ${list.count { it.writable }} writable"
    }

    LaunchedEffect(lab.grants) {
        load()
        with(lab) { visor.settings.changes.logged("settings.changes").collect { settings[it.key] = it } }
    }

    if (settings.isEmpty()) {
        Panel("Settings", trailing = { OutlinedButton(::load) { Text("Load") } }) { Absent("Not read yet") }
        return
    }
    settings.values.sortedBy { order[it.key] ?: Int.MAX_VALUE }.groupBy { it.group ?: "Other" }.forEach { (group, list) ->
        Panel(group) {
            list.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = Ink.line)
                SettingRow(s) { value -> lab.run("settings.set ${s.key}") { it.settings.set(s.key, value).value } }
            }
        }
    }
}

@Composable
private fun SettingRow(s: GlassesSetting, onSet: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.label ?: s.key, color = if (s.writable) Ink.text else Ink.dim, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(s.key, color = Ink.faint, fontSize = 11.sp, fontFamily = Mono)
            }
            when {
                s.pending -> Tag("Saving", true, Ink.warn)
                s.kind == SettingKind.BOOL && s.writable -> Switch(s.value == "true", { onSet(it.toString()) })
                s.options.isEmpty() -> Text(shown(s), color = Ink.dim, fontSize = 14.sp, fontFamily = Mono)
            }
        }
        if (s.options.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.options.forEach { o ->
                    FilterChip(
                        selected = o.value == s.value,
                        onClick = { if (s.writable && o.value != s.value) onSet(o.value) },
                        enabled = s.writable || o.value == s.value,
                        label = { Text(o.label) },
                    )
                }
            }
        }
    }
}

private fun shown(s: GlassesSetting): String {
    val value = s.value ?: return "-"
    val unit = s.range?.unit.orEmpty()
    return if (unit.isEmpty()) value else "$value $unit"
}
