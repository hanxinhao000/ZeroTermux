package com.termux.zerocore.editor

import java.io.File

object EditorFileTreeClipboard {

    enum class Mode {
        COPY,
        CUT
    }

    private var source: File? = null
    private var mode: Mode? = null

    fun hasContent(): Boolean = source?.exists() == true && mode != null

    fun source(): File? = source?.takeIf { it.exists() }

    fun mode(): Mode? = mode

    fun setCopy(file: File) {
        source = file
        mode = Mode.COPY
    }

    fun setCut(file: File) {
        source = file
        mode = Mode.CUT
    }

    fun clear() {
        source = null
        mode = null
    }

    fun clearIfMatches(file: File) {
        val current = source ?: return
        if (EditorFileTreeOperations.isSameOrDescendant(file, current) ||
            EditorFileTreeOperations.isSameOrDescendant(current, file)
        ) {
            clear()
        }
    }
}

object EditorFileTreeOperations {

    fun allocateUniqueName(parent: File, name: String): File {
        val first = File(parent, name)
        if (!first.exists()) return first
        val dotIndex = name.lastIndexOf('.')
        val hasExtension = dotIndex > 0
        val base = if (hasExtension) name.substring(0, dotIndex) else name
        val extension = if (hasExtension) name.substring(dotIndex) else ""
        var index = 1
        while (true) {
            val candidate = File(parent, "$base ($index)$extension")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun canPasteInto(source: File, targetDir: File): Boolean {
        if (!source.exists() || !targetDir.isDirectory) return false
        return try {
            val sourcePath = source.canonicalPath
            val targetPath = targetDir.canonicalPath
            if (sourcePath == targetPath) return false
            if (isSameOrDescendant(targetDir, source)) return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun paste(source: File, targetDir: File, cut: Boolean): File? {
        if (!canPasteInto(source, targetDir)) return null
        val destination = allocateUniqueName(targetDir, source.name)
        val success = if (cut) {
            moveEntry(source, destination)
        } else {
            copyEntry(source, destination)
        }
        if (!success) return null
        if (cut) {
            EditorFileTreeClipboard.clear()
        }
        return destination
    }

    fun copyEntry(source: File, destination: File): Boolean {
        return if (source.isDirectory) {
            copyDirectory(source, destination)
        } else {
            runCatching {
                destination.parentFile?.let { parent ->
                    if (!parent.exists()) parent.mkdirs()
                }
                source.copyTo(destination, overwrite = false)
            }.isSuccess
        }
    }

    fun moveEntry(source: File, destination: File): Boolean {
        return try {
            if (source.canonicalPath == destination.canonicalPath) return true
            destination.parentFile?.takeIf { !it.exists() }?.mkdirs()
            if (source.renameTo(destination)) return true
            if (!copyEntry(source, destination)) return false
            deleteEntry(source)
        } catch (_: Exception) {
            false
        }
    }

    fun deleteEntry(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun isSameOrDescendant(path: File, ancestor: File): Boolean {
        return try {
            val childPath = path.canonicalPath
            val ancestorPath = ancestor.canonicalPath
            childPath == ancestorPath || childPath.startsWith(ancestorPath + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    private fun copyDirectory(source: File, destination: File): Boolean {
        if (!destination.exists() && !destination.mkdirs()) return false
        val children = source.listFiles() ?: return true
        for (child in children) {
            val childDest = File(destination, child.name)
            if (child.isDirectory) {
                if (!copyDirectory(child, childDest)) return false
            } else if (runCatching { child.copyTo(childDest, overwrite = false) }.isFailure) {
                return false
            }
        }
        return true
    }
}
