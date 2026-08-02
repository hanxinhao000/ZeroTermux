package com.termux.zerocore.editor.lsp

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * jdt-ls 对 JDK / jar 内符号常返回 jdt://contents/... 下的 .class URI。
 * 客户端声明 classFileContentsSupport 后，通过 java/classFileContents 取附着源码或反编译结果。
 */
object EditorJdtClassFileSupport {
    private const val CACHE_DIR_NAME = "jdt-sources"

    fun cacheDir(): File = File(EditorLspInstaller.baseDir(), CACHE_DIR_NAME).also { it.mkdirs() }

    fun isClassContentUri(uri: String): Boolean {
        val u = uri.trim()
        if (u.isEmpty()) return false
        if (u.startsWith("jdt:", ignoreCase = true)) return true
        if (u.contains(".class?", ignoreCase = true)) return true
        if (u.endsWith(".class", ignoreCase = true)) return true
        val path = EditorLspUris.pathOf(u)
        return path.endsWith(".class", ignoreCase = true)
    }

    fun needsClassFileContents(location: EditorLspLocation): Boolean {
        if (location.uri.isNotBlank() && isClassContentUri(location.uri)) return true
        return location.file.name.endsWith(".class", ignoreCase = true)
    }

    fun cacheFileFor(uri: String): File {
        val simple = simpleClassName(uri)
        val hash = sha1Hex(uri).take(16)
        return File(cacheDir(), "${hash}_$simple.java")
    }

    /** 引用列表等 UI 展示用短名，如 ArrayList.java */
    fun displayJavaName(uri: String): String = "${simpleClassName(uri)}.java"

    fun simpleClassName(uri: String): String {
        val decoded = runCatching {
            URLDecoder.decode(uri, StandardCharsets.UTF_8.name())
        }.getOrDefault(uri)
        val withoutQuery = decoded.substringBefore('?').substringBefore('#')
        val segment = withoutQuery.substringAfterLast('/').substringAfterLast('\\')
        val name = segment.removeSuffix(".class").removeSuffix(".CLASS")
            .ifBlank { "Unknown" }
        return name.replace(Regex("[^A-Za-z0-9_$]"), "_")
    }

    /**
     * 拉取并缓存源码；成功则返回指向本地 .java 的 location。
     */
    fun resolve(client: EditorLspClient, location: EditorLspLocation): EditorLspLocation? {
        val uri = location.uri.ifBlank {
            if (location.file.name.endsWith(".class", ignoreCase = true)) {
                EditorLspUris.forFile(location.file)
            } else {
                ""
            }
        }
        if (uri.isBlank() || !isClassContentUri(uri)) {
            return location.takeIf { it.file.isFile }
        }
        val cache = cacheFileFor(uri)
        if (!cache.isFile || cache.length() == 0L) {
            val contents = client.classFileContents(uri)?.takeIf { it.isNotBlank() } ?: return null
            runCatching {
                cache.parentFile?.mkdirs()
                cache.writeText(contents)
            }.getOrElse { return null }
        }
        return location.copy(file = cache, uri = uri)
    }

    private fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
