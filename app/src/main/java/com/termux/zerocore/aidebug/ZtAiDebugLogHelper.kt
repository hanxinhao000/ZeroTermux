package com.termux.zerocore.aidebug

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

object ZtAiDebugLogHelper {

    private val gson = Gson()

    fun readLogcat(lines: Int, filter: String?): String {
        val lineCount = lines.coerceIn(50, 2000)
        val tags = filter?.split(',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        return try {
            val output = if (tags.isEmpty()) {
                runLogcat(lineCount, null)
            } else {
                tags.joinToString("\n") { tag ->
                    "=== $tag ===\n${runLogcat(lineCount, tag)}"
                }
            }
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "lines" to lineCount,
                    "filter" to (filter ?: ""),
                    "tags" to tags,
                    "content" to output
                )
            )
        } catch (e: Exception) {
            gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to (e.message ?: "logcat failed"),
                    "hint" to "Try adb logcat on PC, or enable log output in ZeroTermux settings"
                )
            )
        }
    }

    private fun runLogcat(lines: Int, tag: String?): String {
        val args = mutableListOf("logcat", "-d", "-t", lines.toString())
        if (!tag.isNullOrBlank()) {
            args.add("-s")
            args.add(tag.trim())
        }
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        process.waitFor()
        return output
    }
}
