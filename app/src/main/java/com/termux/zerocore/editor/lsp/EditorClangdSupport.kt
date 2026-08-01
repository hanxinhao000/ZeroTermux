package com.termux.zerocore.editor.lsp

import com.termux.shared.termux.TermuxConstants
import java.io.File

/**
 * clangd（C/C++ Language Server，stdio）。
 * Termux 中一般由 `pkg install clang` 提供 `clangd` 可执行文件。
 */
object EditorClangdSupport {
    const val PACKAGE_ID = "c-clangd"
    const val INIT_TIMEOUT_MILLIS = 30_000L

    fun isInstalled(): Boolean = resolveClangdExecutable() != null

    fun resolveClangdExecutable(): String? {
        return EditorLspCommandResolver.resolveExecutablePath("clangd")
    }

    fun resolveLaunchSpec(projectRoot: File?): EditorLspLaunchSpec? {
        val clangd = resolveClangdExecutable() ?: return null
        // Termux/LLVM clangd 默认走 stdio，不要传 --stdio（会直接报 Unknown argument）
        val args = mutableListOf(
            "--background-index=false",
            "--clang-tidy=false",
            "--header-insertion=never",
            "--limit-results=80",
            "--pch-storage=memory",
            "--log=error"
        )
        projectRoot?.takeIf { it.isDirectory && File(it, "compile_commands.json").isFile }?.let { root ->
            args.add("--compile-commands-dir=${root.absolutePath}")
        }
        // Termux 的 clang 驱动：让 clangd 能抽出系统头路径
        resolveQueryDriverGlob()?.let { args.add("--query-driver=$it") }
        return EditorLspLaunchSpec(clangd, args)
    }

    private fun resolveQueryDriverGlob(): String? {
        val bin = TermuxConstants.TERMUX_BIN_PREFIX_DIR
        val drivers = listOf("clang", "clang++", "gcc", "g++")
            .map { File(bin, it) }
            .filter { it.canExecute() || it.isFile }
        if (drivers.isEmpty()) return null
        return drivers.joinToString(",") { it.absolutePath }
    }

    /**
     * 向上查找工程根：compile_commands.json / .clangd / CMakeLists.txt / Makefile / .git
     * 绝不返回 `/`。
     */
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
            if (File(current, "compile_commands.json").isFile ||
                File(current, ".clangd").exists() ||
                File(current, "CMakeLists.txt").isFile ||
                File(current, "Makefile").isFile ||
                File(current, "makefile").isFile ||
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

    fun installShellScript(): String {
        return """
            set -e
            export HOME=${shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)}
            export PREFIX=${shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)}
            export PATH=${shellQuote(EditorLspCommandResolver.buildPath(null))}
            echo '[ZeroTermux Editor] Installing clang (provides clangd)...'
            if command -v pkg >/dev/null 2>&1; then
              pkg install -y clang || pkg install -y clangd || true
            fi
            if ! command -v clangd >/dev/null 2>&1; then
              echo 'clangd not found after pkg install'
              exit 127
            fi
            echo '[ZeroTermux Editor] clangd install done'
            clangd --version
        """.trimIndent()
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
