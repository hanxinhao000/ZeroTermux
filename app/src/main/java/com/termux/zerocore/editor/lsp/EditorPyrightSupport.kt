package com.termux.zerocore.editor.lsp

import com.termux.shared.termux.TermuxConstants
import org.json.JSONObject
import java.io.File

/**
 * Pyright（Python Language Server，stdio）。
 * 由 LSP 列表 npm 安装 `pyright`，命令 `pyright-langserver --stdio`。
 */
object EditorPyrightSupport {
    const val PACKAGE_ID = EditorLspInstaller.PYTHON_PACKAGE_ID
    /** Node + Pyright 冷启动在 ARM/Termux 上常超过默认 3s。 */
    const val INIT_TIMEOUT_MILLIS = 45_000L

    fun resolvePythonExecutable(): String? {
        listOf("python3", "python").forEach { name ->
            EditorLspCommandResolver.resolveExecutablePath(name)?.let { return it }
        }
        return null
    }

    fun findProjectRoot(file: File): File {
        val home = TermuxConstants.TERMUX_HOME_DIR
        var current: File? = if (file.isDirectory) file else file.parentFile
        val start = current ?: home
        var fallback = start
        while (current != null) {
            val path = current.absolutePath
            if (path == "/" || path == "/data" || path == "/data/data") {
                break
            }
            if (File(current, "pyrightconfig.json").isFile ||
                File(current, "pyproject.toml").isFile ||
                File(current, "setup.py").isFile ||
                File(current, "setup.cfg").isFile ||
                File(current, "requirements.txt").isFile ||
                File(current, ".git").exists()
            ) {
                return current
            }
            fallback = current
            if (current == home || path == home.absolutePath) {
                break
            }
            current = current.parentFile
        }
        return when {
            fallback.absolutePath == "/" -> home
            fallback.isDirectory -> fallback
            else -> home
        }
    }

    fun initializationOptions(): JSONObject {
        val pythonPath = resolvePythonExecutable().orEmpty()
        val analysis = JSONObject()
            .put("typeCheckingMode", "basic")
            .put("diagnosticMode", "openFilesOnly")
            .put("useLibraryCodeForTypes", true)
            .put("autoSearchPaths", true)
        val python = JSONObject()
            .put("analysis", analysis)
        if (pythonPath.isNotEmpty()) {
            python.put("pythonPath", pythonPath)
            python.put("defaultInterpreterPath", pythonPath)
        }
        return JSONObject().put("python", python)
    }

    fun settingsPayload(): JSONObject {
        return initializationOptions()
    }

    /** 响应 workspace/configuration 的单个 section。 */
    fun configurationForSection(section: String): Any {
        val pythonPath = resolvePythonExecutable().orEmpty()
        val analysis = JSONObject()
            .put("typeCheckingMode", "basic")
            .put("diagnosticMode", "openFilesOnly")
            .put("useLibraryCodeForTypes", true)
            .put("autoSearchPaths", true)
        return when (section) {
            "", "python" -> {
                val obj = JSONObject().put("analysis", analysis)
                if (pythonPath.isNotEmpty()) {
                    obj.put("pythonPath", pythonPath)
                    obj.put("defaultInterpreterPath", pythonPath)
                }
                obj
            }
            "python.analysis" -> analysis
            "python.pythonPath", "python.defaultInterpreterPath" ->
                if (pythonPath.isNotEmpty()) pythonPath else JSONObject.NULL
            else -> JSONObject.NULL
        }
    }
}
