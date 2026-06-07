package com.termux.zerocore.editor

import android.content.Context
import com.termux.shared.termux.TermuxConstants
import java.io.File
import java.io.FileOutputStream

object AndroidProjectManager {

    private const val ASSET_TEMPLATE_ROOT = "project/Android_project"
    private const val MARKER_RELATIVE_PATH = ".zerotermux/android_project"

    fun createFromAssets(context: Context, targetDir: File): Boolean {
        return try {
            if (targetDir.exists()) {
                if (!targetDir.isDirectory) return false
            } else if (!targetDir.mkdirs()) {
                return false
            }
            if (!copyAssetDir(context, ASSET_TEMPLATE_ROOT, targetDir)) {
                return false
            }
            makeGradlewExecutable(targetDir)
            writeLocalProperties(targetDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun repairProjectGradleFiles(context: Context, projectRoot: File) {
        if (!isAndroidProjectRoot(projectRoot)) return
        copyAssetFile(context, "$ASSET_TEMPLATE_ROOT/settings.gradle", File(projectRoot, "settings.gradle"))
        copyAssetFile(context, "$ASSET_TEMPLATE_ROOT/build.gradle", File(projectRoot, "build.gradle"))
        copyAssetFile(context, "$ASSET_TEMPLATE_ROOT/gradle.properties", File(projectRoot, "gradle.properties"))
        copyAssetFile(context, "$ASSET_TEMPLATE_ROOT/app/build.gradle", File(projectRoot, "app/build.gradle"))
        copyAssetDir(context, "$ASSET_TEMPLATE_ROOT/gradle", File(projectRoot, "gradle"))
        copyAssetFile(context, "$ASSET_TEMPLATE_ROOT/gradlew", File(projectRoot, "gradlew"))
        makeGradlewExecutable(projectRoot)
        writeLocalProperties(projectRoot)
    }

    fun writeLocalProperties(projectRoot: File): Boolean {
        val sdkDir = resolveAndroidSdkDir() ?: return false
        val escaped = sdkDir.absolutePath.replace("\\", "\\\\")
        File(projectRoot, "local.properties").writeText("sdk.dir=$escaped\n")
        return true
    }

    fun resolveAndroidSdkDir(): File? {
        val candidates = mutableListOf<File>()
        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { key ->
            System.getenv(key)?.let { candidates.add(File(it)) }
        }
        candidates.add(File(TermuxConstants.TERMUX_HOME_DIR_PATH, "android-sdk"))
        candidates.add(File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "share/android-sdk"))
        return candidates.firstOrNull { dir ->
            dir.isDirectory && File(dir, "platforms").isDirectory
        }
    }

    fun isAndroidProjectRoot(directory: File?): Boolean {
        if (directory == null || !directory.isDirectory) return false
        val marker = File(directory, MARKER_RELATIVE_PATH)
        val settings = File(directory, "settings.gradle")
        val settingsKts = File(directory, "settings.gradle.kts")
        val gradlew = File(directory, "gradlew")
        return marker.isFile && (settings.isFile || settingsKts.isFile) && gradlew.isFile
    }

    fun findProjectRoot(start: File?): File? {
        var current: File? = when {
            start == null -> null
            start.isFile -> start.parentFile
            start.isDirectory -> start
            else -> null
        }
        while (current != null) {
            if (isAndroidProjectRoot(current)) return current
            current = current.parentFile
        }
        return null
    }

    fun isPathInsideProject(projectRoot: File, path: File?): Boolean {
        if (path == null) return false
        return try {
            val rootPath = projectRoot.canonicalPath
            val targetPath = path.canonicalPath
            targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    fun defaultEntryFile(projectRoot: File): File? {
        val candidates = listOf(
            File(projectRoot, "app/src/main/java/com/zerotermux/template/MainActivity.java"),
            File(projectRoot, "app/build.gradle"),
            File(projectRoot, "settings.gradle")
        )
        return candidates.firstOrNull { it.isFile }
    }

    private fun copyAssetDir(context: Context, assetPath: String, targetDir: File): Boolean {
        return try {
            val children = context.assets.list(assetPath)
            if (children == null || children.isEmpty()) {
                return copyAssetFile(context, assetPath, targetDir)
            }
            if (!targetDir.exists() && !targetDir.mkdirs()) return false
            for (child in children) {
                if (!copyAssetDir(context, "$assetPath/$child", File(targetDir, child))) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, targetFile: File): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun makeGradlewExecutable(projectRoot: File) {
        val gradlew = File(projectRoot, "gradlew")
        if (gradlew.isFile) {
            gradlew.setExecutable(true, false)
        }
    }
}
