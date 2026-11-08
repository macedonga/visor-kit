package com.macedonga.visor.kit.cli

import com.macedonga.visor.kit.protocol.KitJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** The VDK's docs, bundled with the command, cut into sections under their headings and searched by their words. */
internal object Docs {
    class Section(val page: String, val heading: String, val text: String) {
        val words: List<String> = words("$heading $text")
        val headingWords: Set<String> = words(heading).toSet()
    }

    val pages: List<String> by lazy { resource("index.txt").lines().filter { it.isNotBlank() } }

    private val sections: List<Section> by lazy { pages.flatMap { sections(it, page(it)) } }

    fun page(name: String): String = resource(name)

    /** The sections that hold the most of [query]'s words, a word in a heading counting three times. */
    fun search(query: String, limit: Int): List<Section> {
        val terms = words(query).filter { it.length > 1 }.toSet()
        if (terms.isEmpty()) return emptyList()
        return sections.map { section ->
            section to terms.sumOf { term -> section.words.count { it == term } + if (term in section.headingWords) 3 else 0 }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    private fun sections(page: String, markdown: String): List<Section> {
        val out = mutableListOf<Section>()
        var heading = page.removeSuffix(".md")
        val text = StringBuilder()
        var fenced = false
        fun flush() {
            if (text.isNotBlank()) out += Section(page, heading, text.toString().trim())
            text.clear()
        }
        for (line in markdown.lines()) {
            if (line.startsWith("```")) fenced = !fenced
            if (!fenced && line.startsWith("#")) {
                flush()
                heading = line.trimStart('#').trim()
            } else {
                text.appendLine(line)
            }
        }
        flush()
        return out
    }

    private fun words(text: String) = text.lowercase().split(Regex("[^\\p{L}\\p{N}_.]+")).map { it.trim('.') }.filter { it.isNotEmpty() }

    private fun resource(name: String): String =
        Docs::class.java.getResourceAsStream("/docs/$name")?.use { it.readBytes().decodeToString() }
            ?: throw CliError("No page $name in the VDK's docs. These are: ${pages.joinToString()}")

    fun excerpt(section: Section, max: Int): String =
        "## ${section.page} > ${section.heading}\n" + section.text.let { if (it.length > max) it.take(max) + "\n[...]" else it }
}

/** `visor docs [QUERY]`: the pages, or the sections that best answer QUERY. */
internal fun printDocs(query: List<String>) {
    if (query.isEmpty()) return println(Docs.pages.joinToString("\n"))
    val found = Docs.search(query.joinToString(" "), limit = 5)
    if (found.isEmpty()) return println("Nothing in the docs matches. Pages: ${Docs.pages.joinToString()}")
    println(found.joinToString("\n\n") { Docs.excerpt(it, max = 1200) })
}

/**
 * `visor docs-mcp`: an MCP server over standard input and output with two tools, search_vdk_docs and read_vdk_doc,
 * so a coding assistant reads the VDK's docs as DAT's docs MCP serves Meta's. One JSON-RPC message per line.
 */
internal fun serveDocsMcp(version: String) {
    while (true) {
        val line = readlnOrNull() ?: return
        if (line.isBlank()) continue
        val message = runCatching { KitJson.parseToJsonElement(line).jsonObject }.getOrNull() ?: continue
        val id = message["id"] ?: continue
        val method = message["method"]?.jsonPrimitive?.contentOrNull
        val params = message["params"] as? JsonObject
        val answer = runCatching { docsMcp(method, params, version) }
        val reply = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            answer.fold(
                { put("result", it) },
                { error -> putJsonObject("error") { put("code", if (error is NoSuchMethodException) -32601 else -32602); put("message", error.message ?: "failed") } },
            )
        }
        println(reply.toString())
        System.out.flush()
    }
}

private fun docsMcp(method: String?, params: JsonObject?, version: String): JsonElement = when (method) {
    "initialize" -> buildJsonObject {
        put("protocolVersion", params?.get("protocolVersion")?.jsonPrimitive?.contentOrNull ?: "2025-06-18")
        putJsonObject("capabilities") { putJsonObject("tools") {} }
        putJsonObject("serverInfo") { put("name", "vdk-docs"); put("version", version) }
    }
    "ping" -> JsonObject(emptyMap())
    "tools/list" -> buildJsonObject { put("tools", DOCS_TOOLS) }
    "tools/call" -> {
        val name = params?.get("name")?.jsonPrimitive?.contentOrNull
        val arguments = params?.get("arguments") as? JsonObject ?: JsonObject(emptyMap())
        val text = when (name) {
            "search_vdk_docs" -> {
                val query = arguments["query"]?.jsonPrimitive?.contentOrNull ?: throw IllegalArgumentException("search_vdk_docs needs a query.")
                val limit = arguments["max_results"]?.jsonPrimitive?.intOrNull?.coerceIn(1, 10) ?: 5
                Docs.search(query, limit).joinToString("\n\n") { Docs.excerpt(it, max = 2000) }
                    .ifEmpty { "Nothing matches. Pages: ${Docs.pages.joinToString()}" }
            }
            "read_vdk_doc" -> {
                val page = arguments["page"]?.jsonPrimitive?.contentOrNull ?: throw IllegalArgumentException("read_vdk_doc needs a page.")
                runCatching { Docs.page(if (page.endsWith(".md") || page.endsWith(".txt")) page else "$page.md") }
                    .getOrElse { "No page $page. Pages: ${Docs.pages.joinToString()}" }
            }
            else -> throw IllegalArgumentException("No tool $name.")
        }
        buildJsonObject { put("content", buildJsonArray { add(buildJsonObject { put("type", "text"); put("text", text) }) }) }
    }
    else -> throw NoSuchMethodException("No method $method.")
}

private val DOCS_TOOLS = JsonArray(
    listOf(
        buildJsonObject {
            put("name", "search_vdk_docs")
            put("description", "Searches the Visor Development Kit's docs (the VDK, Visor's alternative to Meta's DAT) and returns the best sections.")
            putJsonObject("inputSchema") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") { put("type", "string"); put("description", "Words to look for, such as \"layout button click\".") }
                    putJsonObject("max_results") { put("type", "integer"); put("description", "1 to 10, 5 by default.") }
                }
                put("required", JsonArray(listOf(JsonPrimitive("query"))))
            }
        },
        buildJsonObject {
            put("name", "read_vdk_doc")
            put("description", "Returns one whole page of the VDK's docs, such as display or porting-from-dat.")
            putJsonObject("inputSchema") {
                put("type", "object")
                putJsonObject("properties") { putJsonObject("page") { put("type", "string"); put("description", "A page name, with or without .md.") } }
                put("required", JsonArray(listOf(JsonPrimitive("page"))))
            }
        },
    ),
)
