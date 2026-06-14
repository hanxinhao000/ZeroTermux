package com.termux.zerocore.workstation

import com.google.gson.Gson
import com.termux.shared.termux.TermuxConstants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ZtWorkstationFileHelper {

    private val gson = Gson()
    private val rootDir = File(TermuxConstants.TERMUX_FILES_DIR_PATH)
    private val homeDir = File(rootDir, "home")

    private val externalStorageRoots: List<String> by lazy {
        buildList {
            add("/storage/emulated/0")
            add("/storage/self/primary")
            add("/sdcard")
            listOf("/storage/emulated/0", "/storage/self/primary", "/sdcard").forEach { path ->
                try {
                    add(File(path).canonicalPath)
                } catch (_: Exception) {
                }
            }
        }.map { normalizePathParts(it) }.distinct()
    }

    data class FileEntry(
        val name: String,
        val path: String,
        val directory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    fun resolveSafePath(relativeOrAbsolute: String?): File? {
        if (relativeOrAbsolute.isNullOrBlank()) {
            return if (homeDir.isDirectory) homeDir else rootDir
        }
        val target = if (relativeOrAbsolute.startsWith("/")) {
            File(relativeOrAbsolute)
        } else {
            File(homeDir, relativeOrAbsolute)
        }
        val normalized = normalizePathParts(target.absolutePath)
        if (!isAllowedNormalizedPath(normalized)) return null
        return try {
            val resolved = if (target.exists()) target.canonicalFile else target
            val resolvedNorm = normalizePathParts(resolved.absolutePath)
            if (!isAllowedNormalizedPath(resolvedNorm)) null else resolved
        } catch (_: Exception) {
            target
        }
    }

    fun listDirectory(path: String?): String {
        val dir = resolveSafePath(path) ?: return errorJson("invalid path")
        if (!dir.exists() || !dir.isDirectory) return errorJson("not a directory")
        val parent = resolveParentPath(dir)
        val items = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { file ->
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    directory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified()
                )
            } ?: emptyList()
        return gson.toJson(
            mapOf(
                "ok" to true,
                "path" to dir.absolutePath,
                "parent" to parent,
                "root" to homeDir.absolutePath,
                "items" to items
            )
        )
    }

    private fun resolveParentPath(dir: File): String? {
        dir.parentFile?.absolutePath?.let { parentPath ->
            resolveSafePath(parentPath)?.absolutePath?.let { return it }
        }
        findHomeAliasFor(dir)?.parentFile?.absolutePath?.let { parentPath ->
            resolveSafePath(parentPath)?.absolutePath?.let { return it }
        }
        return null
    }

    private fun findHomeAliasFor(dir: File): File? {
        val canonicalDir = try {
            dir.canonicalFile
        } catch (_: Exception) {
            return null
        }
        homeDir.listFiles()?.forEach { entry ->
            try {
                if (entry.canonicalFile == canonicalDir) return entry
            } catch (_: Exception) {
            }
        }
        val storageDir = File(homeDir, "storage")
        if (!storageDir.isDirectory) return null
        storageDir.listFiles()?.forEach { entry ->
            try {
                if (entry.canonicalFile == canonicalDir) return entry
            } catch (_: Exception) {
            }
            if (entry.isDirectory) {
                entry.listFiles()?.forEach { sub ->
                    try {
                        if (sub.canonicalFile == canonicalDir) return sub
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return null
    }

    private fun normalizePathParts(path: String): String {
        val parts = path.split('/').filter { it.isNotEmpty() }
        val stack = ArrayDeque<String>()
        for (part in parts) {
            when (part) {
                ".", "" -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return if (stack.isEmpty()) "/" else "/" + stack.joinToString("/")
    }

    private fun isUnderRoot(normalizedAbsolute: String, root: String): Boolean {
        val normalizedRoot = normalizePathParts(root.trimEnd('/'))
        return normalizedAbsolute == normalizedRoot ||
            normalizedAbsolute.startsWith("$normalizedRoot/")
    }

    private fun isAllowedNormalizedPath(normalizedAbsolute: String): Boolean {
        if (isUnderRoot(normalizedAbsolute, rootDir.absolutePath)) return true
        return externalStorageRoots.any { root -> isUnderRoot(normalizedAbsolute, root) }
    }

    fun rename(path: String, newName: String): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (file == rootDir || file == homeDir) return errorJson("cannot rename root")
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")) {
            return errorJson("invalid name")
        }
        val dest = File(file.parentFile, trimmed)
        resolveSafePath(dest.absolutePath) ?: return errorJson("invalid destination")
        if (dest.exists()) return errorJson("target exists")
        return if (file.renameTo(dest)) {
            gson.toJson(mapOf("ok" to true, "path" to dest.absolutePath))
        } else {
            errorJson("rename failed")
        }
    }

    fun createFile(path: String): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (file.exists()) return errorJson("already exists")
        if (file.isDirectory) return errorJson("target is directory")
        file.parentFile?.mkdirs()
        return if (file.createNewFile()) {
            gson.toJson(mapOf("ok" to true, "path" to file.absolutePath))
        } else {
            errorJson("create failed")
        }
    }

    fun readText(path: String, maxBytes: Int = 512 * 1024): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (!file.isFile) return errorJson("not a file")
        if (file.length() > maxBytes) return errorJson("file too large")
        return try {
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "path" to file.absolutePath,
                    "name" to file.name,
                    "size" to file.length(),
                    "content" to file.readText()
                )
            )
        } catch (e: Exception) {
            errorJson(e.message ?: "read failed")
        }
    }

    fun writeText(path: String, content: String): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (file.isDirectory) return errorJson("target is directory")
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            gson.toJson(mapOf("ok" to true, "path" to file.absolutePath, "size" to file.length()))
        } catch (e: Exception) {
            errorJson(e.message ?: "write failed")
        }
    }

    fun stat(path: String): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (!file.exists()) return errorJson("not found")
        return gson.toJson(
            mapOf(
                "ok" to true,
                "name" to file.name,
                "path" to file.absolutePath,
                "directory" to file.isDirectory,
                "size" to if (file.isFile) file.length() else 0L,
                "lastModified" to file.lastModified()
            )
        )
    }

    fun openRawStream(path: String): Triple<File, FileInputStream, String>? {
        val file = resolveSafePath(path) ?: return null
        if (!file.isFile) return null
        return Triple(file, FileInputStream(file), guessMimeType(file.name))
    }

    fun mkdir(path: String): String {
        val dir = resolveSafePath(path) ?: return errorJson("invalid path")
        return if (dir.exists() || dir.mkdirs()) {
            gson.toJson(mapOf("ok" to true, "path" to dir.absolutePath))
        } else {
            errorJson("mkdir failed")
        }
    }

    fun delete(path: String): String {
        val file = resolveSafePath(path) ?: return errorJson("invalid path")
        if (file == rootDir || file == homeDir) return errorJson("cannot delete root")
        val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
        return gson.toJson(mapOf("ok" to ok))
    }

    fun copy(from: String, to: String): String {
        val src = resolveSafePath(from) ?: return errorJson("invalid source")
        val dst = resolveSafePath(to) ?: return errorJson("invalid destination")
        return try {
            if (src.isDirectory) {
                src.copyRecursively(dst, overwrite = true)
            } else {
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
            }
            gson.toJson(mapOf("ok" to true))
        } catch (e: Exception) {
            errorJson(e.message ?: "copy failed")
        }
    }

    fun move(from: String, to: String): String {
        val src = resolveSafePath(from) ?: return errorJson("invalid source")
        val dst = resolveSafePath(to) ?: return errorJson("invalid destination")
        dst.parentFile?.mkdirs()
        val renamed = src.renameTo(dst)
        if (renamed) return gson.toJson(mapOf("ok" to true))
        return try {
            if (src.isDirectory) src.copyRecursively(dst, overwrite = true) else src.copyTo(dst, overwrite = true)
            if (src.isDirectory) src.deleteRecursively() else src.delete()
            gson.toJson(mapOf("ok" to true))
        } catch (e: Exception) {
            errorJson(e.message ?: "move failed")
        }
    }

    fun saveUploadedFile(targetPath: String, input: java.io.InputStream): String {
        val file = resolveSafePath(targetPath) ?: return errorJson("invalid path")
        if (file.isDirectory) return errorJson("target is directory")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out -> input.copyTo(out) }
        return gson.toJson(mapOf("ok" to true, "path" to file.absolutePath))
    }

    fun openDownloadStream(path: String): Pair<File, FileInputStream>? {
        val file = resolveSafePath(path) ?: return null
        if (!file.isFile) return null
        return file to FileInputStream(file)
    }

    private fun errorJson(message: String): String {
        return gson.toJson(mapOf("ok" to false, "error" to message))
    }

    private fun guessMimeType(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            "txt", "log", "md", "sh", "py", "kt", "java", "xml", "yml", "yaml", "conf", "cfg", "properties" ->
                "text/plain; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js" -> "application/javascript; charset=utf-8"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
