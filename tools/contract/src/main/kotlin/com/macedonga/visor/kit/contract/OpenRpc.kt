package com.macedonga.visor.kit.contract

import com.macedonga.visor.kit.protocol.KitProtocol
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Shell
import com.macedonga.visor.kit.protocol.Topic
import com.macedonga.visor.kit.protocol.Topics
import java.util.TreeMap
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The wire contract as an OpenRPC document: each method with its params by name and its result, the topics under
 * `x-topics`, and every type under `components.schemas`. `x-scope` and `x-since` carry the kit's own rules.
 */
internal fun openRpc(): JsonObject {
    val schemas = Schemas()
    val methods = (Methods.all + Shell.methods).map { method(it, schemas) }
    val topics = (Topics.all + Shell.topics).map { topic(it, schemas) }
    return buildJsonObject {
        put("openrpc", "1.3.2")
        put("info", buildJsonObject {
            put("title", "Visor kit")
            put("version", KitProtocol.VERSION.toString())
            put("description", "Methods marked x-shell are answered only on the adb shell's socket.")
        })
        put("methods", JsonArray(methods))
        put("x-topics", JsonArray(topics))
        put("components", buildJsonObject { put("schemas", JsonObject(schemas.definitions)) })
    }
}

private fun method(method: Method<*, *>, schemas: Schemas) = buildJsonObject {
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

private fun topic(topic: Topic<*, *>, schemas: Schemas) = buildJsonObject {
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
private fun params(descriptor: SerialDescriptor, schemas: Schemas): JsonArray = buildJsonArray {
    if (descriptor.kind != StructureKind.CLASS) return@buildJsonArray
    for (i in 0 until descriptor.elementsCount) {
        add(buildJsonObject {
            put("name", descriptor.getElementName(i))
            put("required", !descriptor.isElementOptional(i))
            put("schema", schemas.of(descriptor.getElementDescriptor(i)))
        })
    }
}

/** JSON Schemas of serializable types; each class becomes a named definition the others refer to. */
private class Schemas {
    val definitions = TreeMap<String, JsonElement>()

    fun of(d: SerialDescriptor): JsonObject {
        val schema = when {
            d.isInline -> named(of(d.getElementDescriptor(0)), d)
            d.kind == PrimitiveKind.STRING || d.kind == PrimitiveKind.CHAR -> type("string")
            d.kind == PrimitiveKind.BOOLEAN -> type("boolean")
            d.kind == PrimitiveKind.INT || d.kind == PrimitiveKind.LONG || d.kind == PrimitiveKind.SHORT || d.kind == PrimitiveKind.BYTE -> type("integer")
            d.kind == PrimitiveKind.FLOAT || d.kind == PrimitiveKind.DOUBLE -> type("number")
            d.kind == StructureKind.LIST -> buildJsonObject {
                put("type", "array")
                put("items", of(d.getElementDescriptor(0)))
            }
            d.kind == StructureKind.MAP -> buildJsonObject {
                put("type", "object")
                put("additionalProperties", of(d.getElementDescriptor(1)))
            }
            // Unit: a method without params or without a result.
            d.kind == StructureKind.OBJECT -> type("object")
            d.kind == StructureKind.CLASS -> reference(d)
            // JsonElement: any JSON, the params and events the shell passes through.
            d.kind is PolymorphicKind -> JsonObject(emptyMap())
            else -> error("No schema for ${d.serialName} of kind ${d.kind}")
        }
        return if (d.isNullable) buildJsonObject { put("anyOf", JsonArray(listOf(schema, type("null")))) } else schema
    }

    private fun reference(d: SerialDescriptor): JsonObject {
        val name = nameOf(d)
        if (name !in definitions) {
            definitions[name] = JsonObject(emptyMap())
            val required = (0 until d.elementsCount).filterNot(d::isElementOptional).map { JsonPrimitive(d.getElementName(it)) }
            definitions[name] = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    for (i in 0 until d.elementsCount) put(d.getElementName(i), of(d.getElementDescriptor(i)))
                })
                put("required", JsonArray(required))
            }
        }
        return buildJsonObject { put("\$ref", "#/components/schemas/$name") }
    }

    private fun named(schema: JsonObject, d: SerialDescriptor) = JsonObject(schema + ("title" to JsonPrimitive(nameOf(d))))

    private fun type(name: String) = buildJsonObject { put("type", name) }

    private fun nameOf(d: SerialDescriptor) = d.serialName.removeSuffix("?").substringAfterLast('.')
}
