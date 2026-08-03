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
        if (u.startsWith("jar:", ignoreCase = true) && u.contains(".class", ignoreCase = true)) return true
        if (u.contains(".class?", ignoreCase = true)) return true
        if (u.endsWith(".class", ignoreCase = true)) return true
        val path = EditorLspUris.pathOf(u)
        return path.endsWith(".class", ignoreCase = true)
    }

    fun needsClassFileContents(location: EditorLspLocation): Boolean {
        if (location.uri.isNotBlank() && isClassContentUri(location.uri)) return true
        if (location.file.name.endsWith(".class", ignoreCase = true)) return true
        // 本地文件不存在时：JDK/依赖里的类型常只有 URI，需走 classFileContents 拉源码
        if (!location.file.isFile && location.uri.isNotBlank()) {
            val u = location.uri
            if (u.startsWith("jdt:", ignoreCase = true)) return true
            if (u.contains(".class", ignoreCase = true)) return true
            if (u.startsWith("jar:", ignoreCase = true)) return true
            // file:///…/src.zip!/java.base/java/lang/System.java
            if (u.contains("!/") || u.contains(".zip!", ignoreCase = true) || u.contains(".jar!", ignoreCase = true)) {
                return true
            }
        }
        return false
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
        if (uri.isBlank()) {
            return location.takeIf { it.file.isFile }
        }
        if (location.file.isFile && !isClassContentUri(uri) && !uri.contains("!/")) {
            return location
        }
        // zip/jar 内 .java：优先解压到缓存
        extractZipEntryToCache(uri)?.let { cached ->
            return location.copy(file = cached, uri = uri)
        }
        // jdt:// 或 *.class：java/classFileContents（反编译或附着源码）
        if (isClassContentUri(uri) || uri.startsWith("jdt:", ignoreCase = true)) {
            val cache = cacheFileFor(uri)
            if (!cache.isFile || cache.length() == 0L) {
                val contents = client.classFileContents(uri)?.takeIf { it.isNotBlank() }
                if (contents.isNullOrBlank()) {
                    EditorLspDebugStore.recordEvent(
                        "error",
                        "classFileContents failed",
                        mapOf("uri" to uri.take(240))
                    )
                    return null
                }
                runCatching {
                    cache.parentFile?.mkdirs()
                    cache.writeText(contents)
                }.getOrElse { return null }
            }
            return location.copy(file = cache, uri = uri)
        }
        return location.takeIf { it.file.isFile }
    }

    /** file:///path/src.zip!/entry 或 jar:file:///path.jar!/entry → 缓存 .java */
    private fun extractZipEntryToCache(uri: String): File? {
        val decoded = runCatching {
            URLDecoder.decode(uri, StandardCharsets.UTF_8.name())
        }.getOrDefault(uri)
        val marker = "!/"
        val idx = decoded.indexOf(marker)
        if (idx < 0) return null
        var archivePart = decoded.substring(0, idx)
        val entryName = decoded.substring(idx + marker.length).trimStart('/')
        if (entryName.isBlank()) return null
        if (archivePart.startsWith("jar:", ignoreCase = true)) {
            archivePart = archivePart.removePrefix("jar:").removePrefix("JAR:")
        }
        val archivePath = when {
            archivePart.startsWith("file:", ignoreCase = true) -> EditorLspUris.pathOf(archivePart)
            archivePart.startsWith("/") -> archivePart
            else -> EditorLspUris.pathOf(archivePart)
        }
        if (archivePath.isBlank()) return null
        val archive = File(archivePath)
        if (!archive.isFile) return null
        val cache = cacheFileFor(uri)
        if (cache.isFile && cache.length() > 0L) return cache
        return runCatching {
            java.util.zip.ZipFile(archive).use { zip ->
                val entry = zip.getEntry(entryName)
                    ?: zip.getEntry(entryName.removePrefix("/"))
                    ?: return@use null
                zip.getInputStream(entry).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val text = reader.readText()
                    if (text.isBlank()) return@use null
                    cache.parentFile?.mkdirs()
                    cache.writeText(text)
                    cache
                }
            }
        }.getOrNull()
    }

    private fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
