package com.termux.zerocore.editor.lsp

import com.termux.shared.termux.TermuxConstants
import org.json.JSONObject
import java.io.File

data class EditorLspLaunchSpec(
    val executable: String,
    val arguments: List<String>
)

object EditorLspCommandResolver {
    private val npmGlobalBinDirs = listOf(
        File(TermuxConstants.TERMUX_HOME_DIR, ".npm-global/bin"),
        File(TermuxConstants.TERMUX_HOME_DIR, ".local/bin")
    )

    private val npmCommandToPackage = mapOf(
        "vscode-json-language-server" to "vscode-langservers-extracted",
        "pyright-langserver" to "pyright"
    )

    fun resolveLaunchSpec(commandLine: String): EditorLspLaunchSpec? {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(Regex("\\s+"))
        val commandName = parts.firstOrNull() ?: return null
        val arguments = parts.drop(1)

        resolveNodeLaunchSpec(commandName, arguments)?.let { return it }

        resolveExecutablePath(commandName)?.let { executable ->
            return EditorLspLaunchSpec(executable, arguments)
        }
        return null
    }

    fun isCommandAvailable(commandLine: String): Boolean {
        return resolveLaunchSpec(commandLine) != null
    }

    fun resolveExecutablePath(commandName: String): String? {
        val directCandidates = buildList {
            add(File(TermuxConstants.TERMUX_BIN_PREFIX_DIR, commandName))
            add(File(localBinDir(), commandName))
            npmGlobalBinDirs.forEach { dir ->
                add(File(dir, commandName))
            }
        }
        directCandidates.forEach { candidate ->
            resolveExistingCommandFile(candidate)?.let { return it }
        }
        return null
    }

    fun resolveNodeCliPath(commandName: String): String? {
        val packageName = npmPackageName(commandName)
        moduleRoots().forEach { modulesRoot ->
            val moduleRoot = File(modulesRoot, packageName)
            if (!moduleRoot.isDirectory) return@forEach
            readBinEntry(moduleRoot, commandName)?.let { relativePath ->
                resolveExistingCommandFile(File(moduleRoot, relativePath))?.let { return it }
            }
            cliRelativePaths(commandName).forEach { relativePath ->
                resolveExistingCommandFile(File(moduleRoot, relativePath))?.let { return it }
            }
        }
        return null
    }

    fun environmentForLanguage(languageId: String): Map<String, String> {
        if (languageId != EditorLspManager.LANGUAGE_SHELL) {
            return emptyMap()
        }
        return mapOf(
            "SHELLCHECK_PATH" to "/dev/null",
            "SHFMT_PATH" to "/dev/null",
            "BACKGROUND_ANALYSIS_MAX_FILES" to "0",
            "ENABLE_SOURCE_ERROR_DIAGNOSTICS" to "false"
        )
    }

    fun initializationOptionsForLanguage(languageId: String): JSONObject? {
        if (languageId != EditorLspManager.LANGUAGE_SHELL) {
            return null
        }
        return JSONObject()
            .put("shellcheckPath", "")
            .put("globPattern", "")
            .put("backgroundAnalysisMaxFiles", 0)
    }

    fun buildPath(existingPath: String?): String {
        val parts = arrayListOf(
            localBinDir().absolutePath,
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH
        )
        npmGlobalBinDirs.forEach { parts.add(it.absolutePath) }
        parts.add("/system/bin")
        parts.add("/system/xbin")
        existingPath?.split(':')?.filterTo(parts) { it.isNotBlank() }
        return parts.distinct().joinToString(":")
    }

    fun moduleRoots(): List<File> {
        return listOf(
            File(EditorLspInstaller.baseDir(), "node_modules"),
            File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/node_modules"),
            File(TermuxConstants.TERMUX_HOME_DIR, ".npm/lib/node_modules")
        )
    }

    private fun localBinDir(): File {
        return File(EditorLspInstaller.baseDir(), "node_modules/.bin")
    }

    private fun npmPackageName(commandName: String): String {
        return npmCommandToPackage[commandName] ?: commandName
    }

    private fun resolveNodeLaunchSpec(commandName: String, arguments: List<String>): EditorLspLaunchSpec? {
        val nodePath = resolveExecutablePath("node") ?: return null
        val cliPath = resolveNodeCliPath(commandName) ?: return null
        return EditorLspLaunchSpec(nodePath, listOf(cliPath) + arguments)
    }

    private fun readBinEntry(moduleRoot: File, commandName: String): String? {
        val packageJson = File(moduleRoot, "package.json")
        if (!packageJson.isFile) return null
        return try {
            val json = JSONObject(packageJson.readText())
            when (val bin = json.opt("bin")) {
                is String -> bin
                is JSONObject -> {
                    bin.optString(commandName).takeIf { it.isNotBlank() } ?: run {
                        val keys = bin.keys()
                        while (keys.hasNext()) {
                            val value = bin.optString(keys.next())
                            if (value.isNotBlank()) return value
                        }
                        null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cliRelativePaths(commandName: String): List<String> {
        return when (commandName) {
            "vscode-json-language-server" -> listOf("bin/vscode-json-language-server")
            "pyright-langserver" -> listOf(
                "langserver/index.js",
                "langserver/pyright-langserver.js",
                "dist/pyright-langserver.js"
            )
            "typescript-language-server" -> listOf(
                "lib/cli.mjs",
                "lib/cli.js",
                "bin/typescript-language-server"
            )
            "yaml-language-server" -> listOf(
                "out/server/src/server.js",
                "bin/yaml-language-server"
            )
            else -> listOf(
                "out/cli.js",
                "cli.js",
                "bin/main.js",
                "bin/$commandName.js",
                "bin/$commandName"
            )
        }
    }

    private fun resolveExistingCommandFile(file: File): String? {
        if (!file.exists()) return null
        val target = runCatching { file.canonicalFile }.getOrDefault(file)
        if (!target.isFile || !target.canRead()) return null
        if (target.length() <= 0L) return null
        return target.absolutePath
    }
}
