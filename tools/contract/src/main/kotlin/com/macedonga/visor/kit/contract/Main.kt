package com.macedonga.visor.kit.contract

import java.io.File
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private val json = Json { prettyPrint = true }

/**
 * `update DOCUMENT` writes the contract. `check DOCUMENT REPOSITORY` fails when the document is stale, or when it
 * breaks the one at the last release tag.
 */
fun main(args: Array<String>) {
    val document = File(args[1])
    val text = json.encodeToString(JsonObject.serializer(), openRpc()) + "\n"
    when (args[0]) {
        "update" -> document.writeText(text)
        "check" -> {
            if (!document.exists() || document.readText().replace("\r\n", "\n") != text) {
                fail("${document.path} is stale. Run ./gradlew :contract:updateContract, review the diff and commit it.")
            }
            val root = File(args[2])
            val tag = git(root, "describe", "--tags", "--abbrev=0", "--match", "v*") ?: return println("No release tag yet: nothing to stay compatible with.")
            val path = document.relativeTo(root).invariantSeparatorsPath
            val released = git(root, "show", "$tag:$path") ?: return println("$path is new since $tag.")
            val problems = breaks(Json.parseToJsonElement(released).jsonObject, Json.parseToJsonElement(text).jsonObject)
            if (problems.isNotEmpty()) fail("The wire contract breaks peers built against $tag:\n" + problems.joinToString("\n") { "  $it" })
        }
        else -> fail("Unknown command ${args[0]}")
    }
}

private fun git(root: File, vararg args: String): String? {
    val process = ProcessBuilder(listOf("git") + args).directory(root).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    val out = process.inputStream.bufferedReader().readText()
    return out.trim().takeIf { process.waitFor() == 0 }
}

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}
