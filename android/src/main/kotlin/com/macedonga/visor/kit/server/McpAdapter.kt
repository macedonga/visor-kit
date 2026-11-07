package com.macedonga.visor.kit.server

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.JsonRpc
import com.macedonga.visor.kit.protocol.JsonSchemas
import com.macedonga.visor.kit.protocol.KitInternal
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.Method
import com.macedonga.visor.kit.protocol.TraceKind
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The kit as an MCP server, for AI agents: each method the caller may call is a tool, named with `_` for `.`
 * (`camera_photo`), whose input schema is the method's params and whose result is the method's answer as JSON text.
 * Streams stay on JSON-RPC. The host carries the messages, one request and one answer at a time.
 */
@KitInternal
public class McpAdapter(private val dispatcher: Dispatcher, private val name: String, private val version: String) {
    /** The answer to one MCP message from [caller], or null when it was a notification. */
    public suspend fun answer(caller: Caller, message: String): String? {
        val request = try {
            KitJson.parseToJsonElement(message).jsonObject
        } catch (e: SerializationException) {
            return error(JsonNull, JsonRpc.PARSE_ERROR, "Not JSON: ${e.message}")
        } catch (e: IllegalArgumentException) {
            return error(JsonNull, JsonRpc.PARSE_ERROR, "Not a JSON-RPC object.")
        }
        val id = request["id"] ?: return null
        val params = request["params"] as? JsonObject ?: JsonObject(emptyMap())
        return when (val method = request["method"]?.jsonPrimitive?.contentOrNull) {
            "initialize" -> result(id, initialized(params))
            "ping" -> result(id, JsonObject(emptyMap()))
            "tools/list" -> result(id, buildJsonObject { put("tools", buildJsonArray { tools(caller).forEach { add(it) } }) })
            "tools/call" -> result(id, call(caller, params))
            else -> error(id, JsonRpc.METHOD_NOT_FOUND, "Visor answers no MCP method $method.")
        }
    }

    private fun initialized(params: JsonObject) = buildJsonObject {
        // The client's version when it asks for one: the kit's tools are the same under every version so far.
        put("protocolVersion", params["protocolVersion"]?.jsonPrimitive?.contentOrNull ?: PROTOCOL)
        putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
        putJsonObject("serverInfo") {
            put("name", name)
            put("version", version)
        }
        put("instructions", INSTRUCTIONS)
    }

    private fun tools(caller: Caller): List<JsonObject> {
        val schemas = JsonSchemas(inline = true)
        return dispatcher.methodsFor(caller).map { method ->
            buildJsonObject {
                put("name", toolName(method))
                put("description", method.summary)
                put("inputSchema", schemas.of(method.params.descriptor))
            }
        }
    }

    private suspend fun call(caller: Caller, params: JsonObject): JsonObject {
        val tool = params["name"]?.jsonPrimitive?.contentOrNull
        val method = dispatcher.methodsFor(caller).firstOrNull { toolName(it) == tool }
            ?: return toolResult("No tool $tool: it does not exist, or this client does not hold its scope.", error = true)
        val arguments = (params["arguments"] as? JsonObject)?.toString()
        val started = System.nanoTime()
        dispatcher.trace(caller, TraceKind.CALL, method.name, 0, arguments)
        return try {
            val answer = dispatcher.call(caller, method.name, arguments)
            dispatcher.trace(caller, TraceKind.RESULT, method.name, 0, answer, started)
            toolResult(answer, error = false)
        } catch (e: KitException) {
            dispatcher.trace(caller, TraceKind.FAILURE, method.name, 0, e.message, started)
            toolResult("${e.code}: ${e.message}" + (e.reason?.let { " ($it)" } ?: ""), error = true)
        }
    }

    private fun toolResult(text: String, error: Boolean) = buildJsonObject {
        putJsonArray("content") {
            add(buildJsonObject {
                put("type", "text")
                put("text", text)
            })
        }
        put("isError", error)
    }

    private fun result(id: JsonElement, result: JsonElement) = """{"jsonrpc":"2.0","id":$id,"result":$result}"""

    private fun error(id: JsonElement, code: Int, message: String) =
        """{"jsonrpc":"2.0","id":$id,"error":{"code":$code,"message":${JsonPrimitive(message)}}}"""

    private companion object {
        const val PROTOCOL = "2025-06-18"
        const val INSTRUCTIONS = "Tools for the person's Meta glasses, through Visor on their phone: the glasses' status, " +
            "the camera and its captures, the Neural Band, the Display, and notifications. Each tool answers with JSON."

        fun toolName(method: Method<*, *>) = method.name.replace('.', '_')
    }
}
