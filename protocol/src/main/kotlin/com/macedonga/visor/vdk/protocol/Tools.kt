package com.macedonga.visor.vdk.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A tool an app offers the assistant's model. Build one with [ToolSpec]. */
@Poko
@Serializable
public class ToolSpec @VdkInternal constructor(
    /** What the model calls it: a letter, then letters, digits, `_` or `-`, 40 at most. */
    public val name: String,
    /** What it does and when to use it, for the model. */
    public val description: String,
    /** The JSON Schema of its arguments, an object schema. */
    public val schema: JsonElement = NO_ARGUMENTS,
) {
    public class Builder {
        public var name: String = ""
        public var description: String = ""

        /** An object schema; without one, the tool takes no arguments. */
        public var schema: JsonObject = NO_ARGUMENTS

        public fun build(): ToolSpec = ToolSpec(name, description, schema)
    }
}

/** A [ToolSpec], for example `ToolSpec { name = "set_lights"; description = "Turns the lights on or off." }`. */
public fun ToolSpec(block: ToolSpec.Builder.() -> Unit): ToolSpec = ToolSpec.Builder().apply(block).build()

/** The params of [Topics.ASSISTANT_TOOLS]. */
@Poko
@Serializable
public class ToolsOffer @VdkInternal constructor(public val tools: List<ToolSpec>)

/** The model calling one of the app's tools, from [Topics.ASSISTANT_TOOLS]. */
@Poko
@Serializable
public class ToolCall @VdkInternal constructor(
    /** What [Methods.ASSISTANT_TOOLS_ANSWER] answers. */
    public val id: String,
    /** The [ToolSpec.name]. */
    public val tool: String,
    /** The arguments, as the tool's schema describes them. */
    public val arguments: JsonElement,
)

/** The params of [Methods.ASSISTANT_TOOLS_ANSWER]. */
@Poko
@Serializable
public class ToolAnswer @VdkInternal constructor(
    /** A [ToolCall.id]. */
    public val call: String,
    /** What the tool did or found, in words the model reads. */
    public val text: String,
    /** True when the tool failed; [text] then says why. */
    public val error: Boolean = false,
)

/** An object schema with no properties: a tool that takes no arguments. */
private val NO_ARGUMENTS: JsonObject = JsonObject(mapOf("type" to JsonPrimitive("object")))
