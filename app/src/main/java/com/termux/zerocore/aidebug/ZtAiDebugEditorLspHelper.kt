package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.editor.lsp.EditorClangdSupport
import com.termux.zerocore.editor.lsp.EditorJdtClassFileSupport
import com.termux.zerocore.editor.lsp.EditorJdtLsSupport
import com.termux.zerocore.editor.lsp.EditorLspDebugStore
import com.termux.zerocore.editor.lsp.EditorLspInstaller
import com.termux.zerocore.editor.lsp.EditorLspManager
import com.termux.zerocore.editor.lsp.EditorLspUris
import java.io.File

object ZtAiDebugEditorLspHelper {
    private val gson = Gson()

    fun statusJson(context: Context): String {
        val prefs = context.getSharedPreferences("zero_editor_settings", Context.MODE_PRIVATE)
        val lspEnabled = prefs.getBoolean("lsp_enabled", false)
        val manager = EditorLspManager.activeInstance
        val installer = EditorLspInstaller(context.applicationContext)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "editor_open" to (manager != null),
                "lsp_enabled_pref" to lspEnabled,
                "java_jdtls" to mapOf(
                    "package_id" to EditorJdtLsSupport.PACKAGE_ID,
                    "installed" to EditorJdtLsSupport.isInstalled(),
                    "installer_marks_installed" to installer.isPackageInstalled(EditorJdtLsSupport.PACKAGE_ID),
                    "java_home" to EditorJdtLsSupport.resolveJavaHome(),
                    "java_exec" to EditorJdtLsSupport.resolveJavaExecutable(),
                    "launcher" to EditorJdtLsSupport.findLauncherJar()?.absolutePath,
                    "config_template" to EditorJdtLsSupport.findConfigTemplateDir()?.absolutePath,
                    "config_runtime" to EditorJdtLsSupport.ensureRuntimeConfigDir()?.absolutePath,
                    "arch" to (System.getProperty("os.arch") ?: "unknown"),
                    "navigation_timeout_ms" to EditorJdtLsSupport.NAVIGATION_TIMEOUT_MILLIS
                ),
                "c_clangd" to mapOf(
                    "package_id" to EditorClangdSupport.PACKAGE_ID,
                    "installed" to EditorClangdSupport.isInstalled(),
                    "installer_marks_installed" to installer.isPackageInstalled(EditorClangdSupport.PACKAGE_ID),
                    "clangd_exec" to EditorClangdSupport.resolveClangdExecutable()
                ),
                "python_pyright" to mapOf(
                    "package_id" to EditorLspInstaller.PYTHON_PACKAGE_ID,
                    "installed" to installer.isPackageInstalled(EditorLspInstaller.PYTHON_PACKAGE_ID),
                    "command" to "pyright-langserver --stdio"
                ),
                "manager" to (manager?.debugStatus() ?: mapOf("active" to false)),
                "last_fatal_error" to EditorLspDebugStore.lastFatalError,
                "diagnostics" to EditorLspDebugStore.diagnosticsSnapshot(),
                "recent_events" to EditorLspDebugStore.recentEvents(50),
                "stderr_tail" to EditorLspDebugStore.stderrTail(80),
                "hint_zh" to "打开编辑器并启用 LSP 后可用 POST /api/editor/lsp/definition 探测转到定义。"
            )
        )
    }

    fun diagnosticsJson(): String {
        return gson.toJson(
            mapOf(
                "ok" to true,
                "diagnostics" to EditorLspDebugStore.diagnosticsSnapshot(),
                "manager" to (EditorLspManager.activeInstance?.debugStatus() ?: mapOf("active" to false))
            )
        )
    }

    fun stderrJson(lines: Int): String {
        return gson.toJson(
            mapOf(
                "ok" to true,
                "lines" to lines,
                "stderr" to EditorLspDebugStore.stderrTail(lines),
                "events" to EditorLspDebugStore.recentEvents(40)
            )
        )
    }

    /**
     * 探测 textDocument/definition（及 typeDefinition 回退）与 class 源码解析。
     * JSON: {path?, line?, column?, word?, occurrence?}
     */
    fun definitionJson(
        path: String?,
        line: Int?,
        column: Int?,
        word: String?,
        occurrence: Int = 0,
        navigate: Boolean = false
    ): String {
        val manager = EditorLspManager.activeInstance
            ?: return gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to "editor_lsp_inactive",
                    "hint_zh" to "先 POST /api/editor/open 打开 Java 文件并等待 LSP 就绪（GET /api/editor/lsp/status）"
                )
            )
        val file = resolveProbeFile(path, manager)
            ?: return gson.toJson(mapOf("ok" to false, "error" to "file_not_found", "path" to path))
        val languageId = EditorLspManager.languageIdForExtension(file.extension)
            ?: return gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to "unsupported_language",
                    "file" to file.absolutePath
                )
            )
        val content = runCatching { file.readText() }.getOrElse {
            return gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to "read_failed",
                    "detail" to (it.message ?: "")
                )
            )
        }
        val pos = resolveProbePosition(content, line, column, word, occurrence)
            ?: return gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to "position_not_found",
                    "word" to word,
                    "line" to line,
                    "column" to column
                )
            )
        runCatching { manager.openDocument(file, languageId, content) }
        val started = System.currentTimeMillis()
        val probe = manager.definitionDetailed(file, languageId, pos.first, pos.second)
        val locations = probe.locations
        val resolved = locations.map { loc ->
            val target = manager.resolveNavigationLocation(file, languageId, loc)
            mapOf(
                "raw_uri" to loc.uri,
                "raw_file" to loc.file.absolutePath,
                "raw_line" to loc.line,
                "raw_column" to loc.column,
                "needs_class_contents" to EditorJdtClassFileSupport.needsClassFileContents(loc),
                "resolved_file" to target?.file?.absolutePath,
                "resolved_exists" to (target?.file?.isFile == true),
                "resolved_size" to (target?.file?.takeIf { it.isFile }?.length() ?: -1L),
                "resolved_line" to target?.line,
                "resolved_column" to target?.column
            )
        }
        val elapsed = System.currentTimeMillis() - started
        val snippetLine = content.lineSequence().elementAtOrNull(pos.first).orEmpty()
        var navigated = false
        var navigateError: String? = null
        if (navigate) {
            val target = locations.firstOrNull()?.let { loc ->
                manager.resolveNavigationLocation(file, languageId, loc)
            }
            if (target != null && target.file.isFile) {
                navigated = manager.openLocationInHost(target)
                if (!navigated) navigateError = "host_open_failed"
            } else {
                navigateError = "no_resolved_target"
            }
        }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "file" to file.absolutePath,
                "language_id" to languageId,
                "query" to mapOf(
                    "line" to pos.first,
                    "column" to pos.second,
                    "word" to word,
                    "line_text" to snippetLine,
                    "uri" to EditorLspUris.forFile(file)
                ),
                "count" to locations.size,
                "elapsed_ms" to elapsed,
                "probe_source" to probe.source,
                "prefer_type" to probe.preferType,
                "raw_definition" to probe.rawDefinition,
                "raw_typeDefinition" to probe.rawTypeDefinition,
                "locations" to resolved,
                "navigate" to navigate,
                "navigated" to navigated,
                "navigate_error" to navigateError,
                "hint_zh" to if (locations.isEmpty()) {
                    "定义为空：看 raw_definition；方法名应走 definition，类型名才走 typeDefinition"
                } else {
                    "resolved_line/column 即为方法在源码中的位置；navigate=true 可在编辑器打开"
                },
                "recent_events" to EditorLspDebugStore.recentEvents(15)
            )
        )
    }

    private fun resolveProbeFile(path: String?, manager: EditorLspManager): File? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            val direct = File(trimmed)
            if (direct.isFile) return direct
            val underHome = File(TermuxConstants.TERMUX_HOME_DIR, trimmed.removePrefix("/"))
            if (underHome.isFile) return underHome
            return null
        }
        val open = manager.debugStatus()["open_documents"] as? List<*>
        val firstUri = open?.firstOrNull()?.toString().orEmpty()
        if (firstUri.isBlank()) return null
        val p = EditorLspUris.pathOf(firstUri)
        return p.takeIf { it.isNotEmpty() }?.let { File(it) }?.takeIf { it.isFile }
    }

    private fun resolveProbePosition(
        content: String,
        line: Int?,
        column: Int?,
        word: String?,
        occurrence: Int
    ): Pair<Int, Int>? {
        val w = word?.trim().orEmpty()
        if (w.isNotEmpty()) {
            var seen = 0
            content.lineSequence().forEachIndexed { idx, text ->
                var from = 0
                while (true) {
                    val at = text.indexOf(w, from)
                    if (at < 0) break
                    if (seen == occurrence.coerceAtLeast(0)) {
                        val mid = at + w.length / 2
                        return idx to mid
                    }
                    seen++
                    from = at + w.length
                }
            }
            return null
        }
        if (line == null || column == null) return null
        return line.coerceAtLeast(0) to column.coerceAtLeast(0)
    }
}
