package com.macedonga.visor.vdk.contract

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What [now] breaks for peers built against [released], under the rules of CLAUDE.md. Either side of a connection
 * can be the older one, so the rules run both ways: nothing released goes or changes, and everything added is
 * optional and comes with a higher protocol version.
 */
internal fun breaks(released: JsonObject, now: JsonObject): List<String> {
    val problems = mutableListOf<String>()
    val releasedVersion = released.version()
    val version = now.version()
    if (version < releasedVersion) problems += "the protocol version went down from $releasedVersion to $version"

    var added = false
    for (section in listOf("methods", "x-topics")) {
        val old = released.named(section)
        val new = now.named(section)
        for ((name, before) in old) {
            val after = new[name] ?: run {
                problems += "$name is gone"
                null
            } ?: continue
            for (key in listOf("x-scope", "x-since", "x-shell", "result", "event")) {
                if (before[key] != after[key]) problems += "$name changed its $key"
            }
            added = compareFields("$name params", before.params(), after.params(), problems) || added
        }
        for ((name, entry) in new - old.keys) {
            added = true
            val since = entry["x-since"]?.jsonPrimitive?.int
            if (since != version) problems += "$name is new, so its x-since must be $version, not $since"
        }
    }

    val oldTypes = released.schemas()
    val newTypes = now.schemas()
    for ((name, before) in oldTypes) {
        val after = newTypes[name] ?: run {
            problems += "type $name is gone"
            null
        } ?: continue
        added = compareFields("type $name", before.properties(), after.properties(), problems) || added
    }
    if ((newTypes.keys - oldTypes.keys).isNotEmpty()) added = true

    if (added && version <= releasedVersion) problems += "fields, methods or types were added, so the protocol version must go above $releasedVersion"
    return problems
}

private class Field(val schema: JsonElement?, val required: Boolean)

/** Reports what [now] drops or changes of [released]; true when it adds fields. */
private fun compareFields(where: String, released: Map<String, Field>, now: Map<String, Field>, problems: MutableList<String>): Boolean {
    for ((name, before) in released) {
        val after = now[name]
        when {
            after == null -> problems += "$where: $name is gone"
            after.schema != before.schema -> problems += "$where: $name changed its type"
            after.required != before.required -> problems += "$where: $name changed whether it is required"
        }
    }
    val added = now - released.keys
    added.filterValues { it.required }.keys.forEach { name -> problems += "$where: $name is new and required; a new field must be optional" }
    return added.isNotEmpty()
}

private fun JsonObject.version(): Int = getValue("info").jsonObject.getValue("version").jsonPrimitive.content.toInt()

private fun JsonObject.named(section: String): Map<String, JsonObject> =
    (this[section] as? JsonArray).orEmpty().associate { it.jsonObject.getValue("name").jsonPrimitive.content to it.jsonObject }

private fun JsonObject.params(): Map<String, Field> = getValue("params").jsonArray.associate {
    val param = it.jsonObject
    param.getValue("name").jsonPrimitive.content to Field(param["schema"], param["required"]?.jsonPrimitive?.boolean == true)
}

private fun JsonObject.schemas(): Map<String, JsonObject> =
    getValue("components").jsonObject.getValue("schemas").jsonObject.mapValues { it.value.jsonObject }

private fun JsonObject.properties(): Map<String, Field> {
    val required = this["required"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty().toSet()
    return this["properties"]?.jsonObject.orEmpty().mapValues { (name, schema) -> Field(schema, name in required) }
}
