package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.termux.zerocore.editor.lsp.EditorJdtLsSupport
import com.termux.zerocore.editor.lsp.EditorLspDebugStore
import com.termux.zerocore.editor.lsp.EditorLspInstaller
import com.termux.zerocore.editor.lsp.EditorLspManager

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
                    "arch" to (System.getProperty("os.arch") ?: "unknown")
                ),
                "manager" to (manager?.debugStatus() ?: mapOf("active" to false)),
                "last_fatal_error" to EditorLspDebugStore.lastFatalError,
                "diagnostics" to EditorLspDebugStore.diagnosticsSnapshot(),
                "recent_events" to EditorLspDebugStore.recentEvents(50),
                "stderr_tail" to EditorLspDebugStore.stderrTail(80),
                "hint_zh" to "打开编辑器并启用 LSP 后，diagnostics 会随 publishDiagnostics 更新；stderr_tail 为 jdt-ls 日志（不再 Toast）。"
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
}
