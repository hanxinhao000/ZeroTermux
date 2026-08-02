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
        return when (languageId) {
            EditorLspManager.LANGUAGE_SHELL -> mapOf(
                "SHELLCHECK_PATH" to "/dev/null",
                "SHFMT_PATH" to "/dev/null",
                "BACKGROUND_ANALYSIS_MAX_FILES" to "0",
                "ENABLE_SOURCE_ERROR_DIAGNOSTICS" to "false"
            )
            EditorLspManager.LANGUAGE_JAVA -> EditorJdtLsSupport.environmentExtras()
            EditorLspManager.LANGUAGE_C, EditorLspManager.LANGUAGE_CPP -> emptyMap()
            EditorLspManager.LANGUAGE_PYTHON -> emptyMap()
            else -> emptyMap()
        }
    }

    fun initializationOptionsForLanguage(languageId: String): JSONObject? {
        return when (languageId) {
            EditorLspManager.LANGUAGE_SHELL -> JSONObject()
                .put("shellcheckPath", "")
                .put("globPattern", "")
                .put("backgroundAnalysisMaxFiles", 0)
            EditorLspManager.LANGUAGE_JAVA -> JSONObject()
                .put(
                    "extendedClientCapabilities",
                    JSONObject()
                        .put("resolveAdditionalTextEditsSupport", true)
                        .put("classFileContentsSupport", true)
                        .put("overrideMethodsPromptSupport", true)
                )
                .put("settings", javaSettings())
            EditorLspManager.LANGUAGE_PYTHON -> EditorPyrightSupport.initializationOptions()
            EditorLspManager.LANGUAGE_C, EditorLspManager.LANGUAGE_CPP -> null
            else -> null
        }
    }

    /** jdt-ls workspace/configuration 与 initializationOptions.settings 共用。 */
    fun javaSettings(): JSONObject {
        val configuration = JSONObject()
            .put("updateBuildConfiguration", "automatic")
        val javaHome = EditorJdtLsSupport.resolveJavaHome()
        if (!javaHome.isNullOrBlank()) {
            val runtimeName = when {
                javaHome.contains("21") -> "JavaSE-21"
                javaHome.contains("17") -> "JavaSE-17"
                javaHome.contains("11") -> "JavaSE-11"
                else -> "JavaSE-21"
            }
            configuration.put(
                "runtimes",
                org.json.JSONArray().put(
                    JSONObject()
                        .put("name", runtimeName)
                        .put("path", javaHome)
                        .put("default", true)
                )
            )
        }
        val java = JSONObject()
            .put("configuration", configuration)
            .put("autobuild", JSONObject().put("enabled", true))
            .put(
                "completion",
                JSONObject()
                    .put("enabled", true)
                    .put("postfix", JSONObject().put("enabled", true))
                    .put(
                        "importOrder",
                        org.json.JSONArray().put("java").put("javax").put("org").put("com")
                    )
            )
            // 缺 classpath 时也要提示，避免「能补全却不报错」
            .put(
                "errors",
                JSONObject().put(
                    "incompleteClasspath",
                    JSONObject().put("severity", "warning")
                )
            )
            .put(
                "sources",
                JSONObject().put(
                    "organizeImports",
                    JSONObject().put("starThreshold", 99).put("staticStarThreshold", 99)
                )
            )
            // 引用/定义可落到反编译源码（无附着 sources.jar 时）
            .put(
                "references",
                JSONObject().put("includeDecompiledSources", true)
            )
        if (!javaHome.isNullOrBlank()) {
            // 供 jdt-ls 自身/项目使用的 JDK
            java.put("home", javaHome)
            java.put(
                "jdt",
                JSONObject().put("ls", JSONObject().put("java", JSONObject().put("home", javaHome)))
            )
        }
        return JSONObject().put("java", java)
    }

    /** 按 section 返回配置片段，供 workspace/configuration 使用。 */
    fun javaConfigurationForSection(section: String): Any {
        val root = javaSettings()
        val java = root.optJSONObject("java") ?: return root
        // section "java" → 返回 java 节点本身；"java.completion" → 子节点
        if (section.isEmpty()) return root
        if (section == "java") return java
        if (!section.startsWith("java.")) return JSONObject.NULL
        val path = section.removePrefix("java.").split('.')
        var current: Any? = java
        for (part in path) {
            current = when (current) {
                is JSONObject -> if (current.has(part)) current.get(part) else return JSONObject.NULL
                else -> return JSONObject.NULL
            }
        }
        return current ?: JSONObject.NULL
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
                "langserver.index.js",
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
