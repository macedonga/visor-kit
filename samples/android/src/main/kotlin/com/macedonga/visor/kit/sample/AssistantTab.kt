package com.macedonga.visor.kit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.protocol.AssistantTurn
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.ToolCall
import com.macedonga.visor.kit.protocol.ToolSpec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** The tools the app offers the assistant's model while the switch is on. */
private val TOOLS = listOf(
    ToolSpec {
        name = "glasses_battery"
        description = "Tells the glasses' battery, the case's battery and whether they charge."
    },
    ToolSpec {
        name = "roll_dice"
        description = "Rolls dice and tells the result. Use it when the wearer asks to roll dice."
        schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("count") {
                    put("type", "integer")
                    put("description", "How many six-sided dice, 1 to 6.")
                }
            }
        }
    },
    ToolSpec {
        name = "lab_note"
        description = "Writes a note into VDK Lab on the phone. Use it when the wearer asks to note something down."
        schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "The note.")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("text"))))
        }
    },
)

@Composable
fun AssistantTab(lab: Lab, status: GlassesStatus?) {
    ToolsPanel(lab, status)
    TurnsPanel(lab)
}

@Composable
private fun ToolsPanel(lab: Lab, status: GlassesStatus?) {
    var on by remember { mutableStateOf(false) }
    val calls = remember { mutableStateListOf<Pair<ToolCall, String>>() }
    val now by rememberUpdatedState(status)
    Panel("The app's tools") {
        StreamSwitch("Offer them on assistant.tools", on) { on = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TOOLS.forEach { Tag(it.name, on) }
        }
        if (on) {
            LaunchedEffect(lab.grants) {
                with(lab) {
                    visor.assistant.tools(TOOLS).logged("assistant.tools").collect { call ->
                        val text = use(call, now, lab)
                        lab.run("assistant.tools.answer") { it.assistant.answer(call, text) }
                        calls.add(0, call to text)
                    }
                }
            }
        }
        if (calls.isEmpty()) Absent(if (on) "\"Hey Meta, roll two dice\"" else "Off")
        calls.forEach { (call, text) -> EventLine(text, call.tool) }
    }
}

private fun use(call: ToolCall, status: GlassesStatus?, lab: Lab): String {
    val args = call.arguments as? JsonObject
    return when (call.tool) {
        "glasses_battery" -> listOfNotNull(
            status?.battery?.let { "The glasses are at $it%" },
            status?.caseBattery?.let { "the case at $it%" },
            if (status?.charging == true) "charging" else null,
        ).joinToString(", ").ifEmpty { "The battery is not known yet." }
        "roll_dice" -> {
            val n = (args?.get("count")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1).coerceIn(1, 6)
            val rolls = List(n) { (1..6).random() }
            if (n == 1) "Rolled ${rolls[0]}." else "Rolled ${rolls.joinToString(", ")}: ${rolls.sum()} in all."
        }
        "lab_note" -> {
            val text = args?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
            lab.note("Note: $text")
            "Noted in VDK Lab."
        }
        else -> "The lab has no tool called ${call.tool}."
    }
}

@Composable
private fun TurnsPanel(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    val turns = remember { mutableStateListOf<AssistantTurn>() }
    Panel("Questions and answers", trailing = {
        OutlinedButton({
            lab.run("assistant.history") { v ->
                val history = v.assistant.history()
                turns.clear()
                turns.addAll(history)
                "${history.size} turns"
            }
        }) { Text("History") }
    }) {
        StreamSwitch("Follow assistant.turns", on) { on = it }
        if (on) {
            LaunchedEffect(lab.grants) {
                with(lab) {
                    visor.assistant.turns.logged("assistant.turns").collect { turn ->
                        turns.removeAll { it.id == turn.id }
                        turns.add(0, turn)
                    }
                }
            }
        }
        if (turns.isEmpty()) Absent("None yet")
        turns.forEachIndexed { i, turn ->
            if (i > 0) HorizontalDivider(color = Ink.line)
            Turn(turn)
        }
    }
}

@Composable
private fun Turn(turn: AssistantTurn) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(turn.question, color = Ink.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (turn.answer.isNotEmpty()) Text(turn.answer, color = Ink.dim, fontSize = 14.sp)
        if (turn.failure.isNotEmpty()) Text(turn.failure, color = Ink.bad, fontSize = 14.sp)
        if (turn.tools.isNotEmpty()) Text(turn.tools.joinToString { it.what }, color = Ink.faint, fontSize = 12.sp, fontFamily = Mono)
    }
}
