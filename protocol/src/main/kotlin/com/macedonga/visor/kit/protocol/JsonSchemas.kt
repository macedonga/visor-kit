package com.macedonga.visor.kit.protocol

import java.util.TreeMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * JSON Schemas of the kit's types, from their serializers. With [inline] every class is written out where it is used,
 * as an MCP tool's input schema needs, and a class that contains itself goes once into the schema's own `$defs`;
 * without, each class becomes a definition in [definitions] that the others refer to, as in the OpenRPC document.
 */
@KitInternal
@OptIn(ExperimentalSerializationApi::class)
public class JsonSchemas(private val inline: Boolean) {
    /** The named definitions, without [inline]: what `#/components/schemas/` refers to. */
    public val definitions: MutableMap<String, JsonElement> = TreeMap()

    // With inline: the classes being written out now, and those met inside themselves, for $defs.
    private val expanding = HashSet<String>()
    private val recursive = TreeMap<String, SerialDescriptor>()

    public fun of(d: SerialDescriptor): JsonObject {
        val schema = schemaOf(d)
        if (recursive.isEmpty()) return schema
        val defs = TreeMap<String, JsonElement>()
        while (true) {
            val (name, descriptor) = recursive.entries.firstOrNull { it.key !in defs } ?: break
            defs[name] = expanded(name) { objectOf(descriptor) }
        }
        recursive.clear()
        return JsonObject(schema + ("\$defs" to JsonObject(defs)))
    }

    private fun schemaOf(d: SerialDescriptor): JsonObject {
        val schema = when {
            d.isInline -> named(schemaOf(d.getElementDescriptor(0)), d)
            d.kind == PrimitiveKind.STRING || d.kind == PrimitiveKind.CHAR -> type("string")
            d.kind == PrimitiveKind.BOOLEAN -> type("boolean")
            d.kind == PrimitiveKind.INT || d.kind == PrimitiveKind.LONG || d.kind == PrimitiveKind.SHORT || d.kind == PrimitiveKind.BYTE -> type("integer")
            d.kind == PrimitiveKind.FLOAT || d.kind == PrimitiveKind.DOUBLE -> type("number")
            d.kind == StructureKind.LIST -> buildJsonObject {
                put("type", "array")
                put("items", schemaOf(d.getElementDescriptor(0)))
            }
            d.kind == StructureKind.MAP -> buildJsonObject {
                put("type", "object")
                put("additionalProperties", schemaOf(d.getElementDescriptor(1)))
            }
            // Unit: a method without params or without a result.
            d.kind == StructureKind.OBJECT -> type("object")
            d.kind == StructureKind.CLASS -> if (inline) inlined(d) else reference(d)
            // JsonElement: any JSON, such as a tool's arguments or the events the shell injects.
            d.kind is PolymorphicKind -> JsonObject(emptyMap())
            else -> error("No schema for ${d.serialName} of kind ${d.kind}")
        }
        return if (d.isNullable) buildJsonObject { put("anyOf", JsonArray(listOf(schema, type("null")))) } else schema
    }

    private fun reference(d: SerialDescriptor): JsonObject {
        val name = nameOf(d)
        if (name !in definitions) {
            // Taken before it is written, so a type that contains itself refers to itself instead of recursing.
            definitions[name] = JsonObject(emptyMap())
            definitions[name] = objectOf(d)
        }
        return buildJsonObject { put("\$ref", "#/components/schemas/$name") }
    }

    private fun inlined(d: SerialDescriptor): JsonObject {
        val name = nameOf(d)
        if (name !in expanding) return expanded(name) { objectOf(d) }
        recursive.putIfAbsent(name, d)
        return buildJsonObject { put("\$ref", "#/\$defs/$name") }
    }

    private inline fun expanded(name: String, write: () -> JsonObject): JsonObject {
        expanding += name
        try {
            return write()
        } finally {
            expanding -= name
        }
    }

    private fun objectOf(d: SerialDescriptor): JsonObject {
        val required = (0 until d.elementsCount).filterNot(d::isElementOptional).map { JsonPrimitive(d.getElementName(it)) }
        return buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                for (i in 0 until d.elementsCount) put(d.getElementName(i), schemaOf(d.getElementDescriptor(i)))
            })
            put("required", JsonArray(required))
        }
    }

    private fun named(schema: JsonObject, d: SerialDescriptor) = JsonObject(schema + ("title" to JsonPrimitive(nameOf(d))))

    private fun type(name: String) = buildJsonObject { put("type", name) }

    private fun nameOf(d: SerialDescriptor) = d.serialName.removeSuffix("?").substringAfterLast('.')
}
