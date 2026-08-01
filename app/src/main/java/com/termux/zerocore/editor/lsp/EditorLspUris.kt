package com.termux.zerocore.editor.lsp

import java.io.File
import java.net.URI

/**
 * 统一 file URI：Java [File.toURI] 常为 `file:/path`，jdt-ls 多为 `file:///path`。
 */
object EditorLspUris {
    fun forFile(file: File): String {
        val absolute = try {
            file.absoluteFile.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }.replace('\\', '/')
        val path = if (absolute.startsWith("/")) absolute else "/$absolute"
        return "file://$path"
    }

    fun pathOf(uri: String): String {
        val raw = uri.trim()
        if (raw.isEmpty()) return ""
        return try {
            val parsed = URI(raw)
            val path = parsed.path
            if (!path.isNullOrEmpty()) {
                path.replace("%20", " ")
            } else {
                fallbackPath(raw)
            }
        } catch (_: Exception) {
            fallbackPath(raw)
        }
    }

    fun same(a: String, b: String): Boolean {
        if (a == b) return true
        val pa = pathOf(a)
        val pb = pathOf(b)
        return pa.isNotEmpty() && pa == pb
    }

    /** 统一成 `file:///abs/path`，便于与 jdt-ls 对齐。 */
    fun normalize(uri: String): String {
        val path = pathOf(uri)
        return if (path.isEmpty()) uri else "file://$path"
    }

    private fun fallbackPath(uri: String): String {
        var s = uri
        if (s.startsWith("file:")) {
            s = s.removePrefix("file:")
        }
        s = s.replace("%20", " ")
        while (s.startsWith("//")) {
            s = s.removePrefix("/")
        }
        if (!s.startsWith("/")) {
            s = "/$s"
        }
        return s.trimEnd('/')
    }
}
