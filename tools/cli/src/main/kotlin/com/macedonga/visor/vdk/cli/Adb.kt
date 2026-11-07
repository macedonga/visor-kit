package com.macedonga.visor.vdk.cli

import com.macedonga.visor.vdk.protocol.Shell
import java.io.File
import java.io.IOException

/** adb, found where Android Studio puts it, or on the PATH. [serial] picks a device, as `adb -s` does. */
internal class Adb(private val serial: String?) {
    private val adb = locate()

    /** Forwards a free port on this computer to Visor's shell socket on the phone, and returns the port. */
    fun forward(): Int {
        val out = run("forward", "tcp:0", "localabstract:${Shell.SOCKET}").trim()
        return out.toIntOrNull() ?: throw CliError("adb forward answered: $out")
    }

    fun removeForward(port: Int) {
        run("forward", "--remove", "tcp:$port", check = false)
    }

    private fun run(vararg args: String, check: Boolean = true): String {
        val command = listOf(adb) + serial?.let { listOf("-s", it) }.orEmpty() + args
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (e: IOException) {
            throw CliError("Cannot run adb. Install Android's platform tools, or set ANDROID_HOME to the SDK.")
        }
        val out = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (check && code != 0) throw CliError("adb: ${out.trim().ifEmpty { "exit code $code" }}")
        return out
    }

    private companion object {
        fun locate(): String {
            val exe = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
            for (variable in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
                val file = File(System.getenv(variable) ?: continue, "platform-tools/$exe")
                if (file.isFile) return file.path
            }
            return exe
        }
    }
}

/** A failure to report and exit with. */
internal class CliError(message: String, val exitCode: Int = 1) : Exception(message)
