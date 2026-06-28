package com.termux.zerocore.aidebug

import com.google.gson.Gson
import com.termux.zerocore.workstation.ZtWorkstationFileHelper
import android.util.Base64

object ZtAiDebugFileWriteHelper {

    private val gson = Gson()

    fun listJson(path: String, useRoot: Boolean): String {
        if (useRoot) {
            val abs = path.trim()
            val result = ZtAiDebugRootHelper.execRoot("ls -la ${shellQuote(abs)} 2>&1", 8000)
            return gson.toJson(
                mapOf(
                    "ok" to result.ok,
                    "path" to abs,
                    "root" to true,
                    "listing" to result.stdout
                )
            )
        }
        val dir = ZtWorkstationFileHelper.resolveSafePath(path) ?: return errorJson("invalid path")
        if (!dir.exists()) return errorJson("not found")
        val entries = if (dir.isDirectory) {
            dir.listFiles()?.map { f ->
                mapOf(
                    "name" to f.name,
                    "path" to f.absolutePath,
                    "isDirectory" to f.isDirectory,
                    "size" to f.length()
                )
            } ?: emptyList()
        } else {
            listOf(mapOf("name" to dir.name, "path" to dir.absolutePath, "isDirectory" to false, "size" to dir.length()))
        }
        return gson.toJson(mapOf("ok" to true, "path" to path, "root" to false, "entries" to entries))
    }

    fun writeJson(path: String, content: String, encoding: String, useRoot: Boolean): String {
        if (useRoot) {
            val abs = path.trim()
            val tmp = "/data/local/tmp/zt_ai_write_${System.currentTimeMillis()}.tmp"
            val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            ZtAiDebugRootHelper.execRoot("echo '$b64' | base64 -d > ${shellQuote(tmp)}", 8000)
            val result = ZtAiDebugRootHelper.execRoot(
                "cat ${shellQuote(tmp)} > ${shellQuote(abs)} && rm -f ${shellQuote(tmp)}",
                8000
            )
            return gson.toJson(mapOf("ok" to result.ok, "path" to abs, "root" to true, "bytes" to content.length))
        }
        val file = ZtWorkstationFileHelper.resolveSafePath(path) ?: return errorJson("invalid path")
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        return gson.toJson(mapOf("ok" to true, "path" to path, "root" to false, "bytes" to content.length))
    }

    private fun errorJson(msg: String): String {
        return gson.toJson(mapOf("ok" to false, "error" to msg))
    }

    private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
