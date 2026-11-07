package com.macedonga.visor.kit.contract

import com.macedonga.visor.kit.protocol.JsonSchemas
import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.Topic
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The wire contract as an OpenRPC document: each method with its params by name and its result, the topics under
 * `x-topics`, and every type under `components.schemas`. `x-scope` and `x-since` carry the VDK's own rules.
 */
internal fun openRpc(): JsonObject {
    val schemas = JsonSchemas(inline = false)
    val methods = (Methods.all + Shell.methods).map { method(it, schemas) }
    val topics = (Topics.all + Shell.topics).map { topic(it, schemas) }
    return buildJsonObject {
        put("openrpc", "1.3.2")
        put("info", buildJsonObject {
            put("title", "Visor Development Kit")
            put("version", KitProtocol.VERSION.toString())
            put("description", "Methods marked x-shell are answered only on the adb shell's socket.")
        })
        put("methods", JsonArray(methods))
        put("x-topics", JsonArray(topics))
        put("components", buildJsonObject { put("schemas", JsonObject(schemas.definitions)) })
    }
}

private fun method(method: Method<*, *>, schemas: JsonSchemas) = buildJsonObject {
    put("name", method.name)
    put("summary", method.summary)
    put("paramStructure", "by-name")
    put("params", params(method.params.descriptor, schemas))
    put("result", buildJsonObject {
        put("name", "result")
        put("schema", schemas.of(method.result.descriptor))
    })
    common(method.scope?.id, method.since, method in Shell.methods)
}

private fun topic(topic: Topic<*, *>, schemas: JsonSchemas) = buildJsonObject {
    put("name", topic.name)
    put("summary", topic.summary)
    put("params", params(topic.params.descriptor, schemas))
    put("event", buildJsonObject {
        put("name", "event")
        put("schema", schemas.of(topic.event.descriptor))
    })
    common(topic.scope?.id, topic.since, topic in Shell.topics)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.common(scope: String?, since: Int, shell: Boolean) {
    scope?.let { put("x-scope", it) }
    put("x-since", since)
    if (shell) put("x-shell", true)
}

/** A params object as OpenRPC's by-name params: one content descriptor per field. Unit has none. */
private fun params(descriptor: SerialDescriptor, schemas: JsonSchemas): JsonArray = buildJsonArray {
    if (descriptor.kind != StructureKind.CLASS) return@buildJsonArray
    for (i in 0 until descriptor.elementsCount) {
        add(buildJsonObject {
            put("name", descriptor.getElementName(i))
            put("required", !descriptor.isElementOptional(i))
            put("schema", schemas.of(descriptor.getElementDescriptor(i)))
        })
    }
}
