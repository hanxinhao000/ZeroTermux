package com.termux.zerocore.aidebug

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.editor.EditorVncEnvironment
import java.io.File

object ZtAiDebugVncHelper {

    private val gson = Gson()

    private const val DIAG_MARKER_START = "===ZT_VNC_DIAG_START==="
    private const val DIAG_MARKER_END = "===ZT_VNC_DIAG_END==="

    fun statusJson(): String {
        val raw = runDiagnosticScript(waitMs = 5000L)
        return gson.toJson(parseDiagnosticOutput(raw))
    }

    fun startJson(): String {
        val scriptPath = "\${HOME}/.zerotermux/ai_debug_vnc_start.sh"
        val script = buildString {
            append("#!/data/data/com.termux/files/usr/bin/bash\n")
            append("set +e\n")
            append(EditorVncEnvironment.coreVncShellFunctions())
            append("start_editor_vnc\n")
            append("wait_for_editor_vnc && echo ZT_VNC_START_OK || echo ZT_VNC_START_FAIL\n")
            append("pgrep -af Xvfb 2>/dev/null || true\n")
            append("pgrep -af x11vnc 2>/dev/null || true\n")
            append("editor_vnc_port_listening && echo PORT_OK || echo PORT_FAIL\n")
        }
        val writeCmd = "mkdir -p ~/.zerotermux && cat > $scriptPath <<'ZT_VNC_EOF'\n$script\nZT_VNC_EOF\nchmod +x $scriptPath"
        ZtAiDebugTerminalHelper.execAndSnapshot(writeCmd, 2000L)
        val snapshot = ZtAiDebugTerminalHelper.execAndSnapshot("bash $scriptPath", 10000L)
        val status = parseDiagnosticOutput(snapshot)
        val started = snapshot.contains("ZT_VNC_START_OK") || (status["port_listening"] == true)
        return gson.toJson(
            mapOf(
                "ok" to started,
                "started" to started,
                "snapshot" to snapshot.takeLast(4000),
                "status" to status
            )
        )
    }

    fun openEditorJson(context: Context, path: String, openX11Tab: Boolean = false, autoRun: Boolean = false): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) {
            return gson.toJson(mapOf("ok" to false, "error" to "path required"))
        }
        val resolved = resolvePath(trimmed)
        return try {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.termux.zerocore.activity.EditTextActivity")
                action = "com.termux.zerocore.activity.edittextactivity"
                putExtra("edit_path", resolved)
                putExtra(EXTRA_OPEN_X11_TAB, openX11Tab)
                putExtra(EXTRA_AUTO_RUN, autoRun)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "path" to resolved,
                    "open_x11_tab" to openX11Tab,
                    "auto_run" to autoRun,
                    "hint_for_ai" to "Editor opened. With open_x11_tab/auto_run the GUI dock runs build.sh automatically after rebuild."
                )
            )
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to (e.message ?: "start failed")))
        }
    }

    private fun runDiagnosticScript(waitMs: Long): String {
        val script = buildString {
            append("echo $DIAG_MARKER_START\n")
            append("export DISPLAY=${EditorVncEnvironment.DISPLAY}\n")
            append(EditorVncEnvironment.coreVncShellFunctions())
            append("echo XVFB_PROCESSES:\n")
            append("pgrep -af Xvfb 2>/dev/null || echo none\n")
            append("echo X11VNC_PROCESSES:\n")
            append("pgrep -af x11vnc 2>/dev/null || echo none\n")
            append("echo PORT_${EditorVncEnvironment.VNC_PORT}:\n")
            append("if editor_vnc_port_listening; then echo PORT_OK; else echo PORT_FAIL; fi\n")
            append("echo XDPYINFO:\n")
            append("xdpyinfo -display ${EditorVncEnvironment.DISPLAY} 2>&1 | head -3 || true\n")
            append("echo X11VNC_LOG_TAIL:\n")
            append("tail -n 15 ${EditorVncEnvironment.LOG_FILE} 2>/dev/null || echo no_log\n")
            append("echo $DIAG_MARKER_END\n")
        }
        return ZtAiDebugTerminalHelper.execAndSnapshot(script, waitMs)
    }

    private fun parseDiagnosticOutput(raw: String): Map<String, Any?> {
        val block = extractBetween(raw, DIAG_MARKER_START, DIAG_MARKER_END) ?: raw.takeLast(3000)
        val xvfbLines = sectionLines(block, "XVFB_PROCESSES:")
        val x11vncLines = sectionLines(block, "X11VNC_PROCESSES:")
        val portOk = block.contains("PORT_OK")
        val xdpyinfo = sectionText(block, "XDPYINFO:").trim()
        val logTail = sectionText(block, "X11VNC_LOG_TAIL:").trim()
        val displayUp = !xdpyinfo.contains("unable to open display", ignoreCase = true) &&
            xdpyinfo.isNotBlank() && xdpyinfo != "no_log"
        return mapOf(
            "display" to EditorVncEnvironment.DISPLAY,
            "vnc_port" to EditorVncEnvironment.VNC_PORT,
            "vnc_host" to EditorVncEnvironment.VNC_HOST,
            "xvfb_running" to xvfbLines.any { it.contains("Xvfb") },
            "x11vnc_running" to x11vncLines.any { it.contains("x11vnc") },
            "port_listening" to portOk,
            "display_up" to displayUp,
            "xvfb_processes" to xvfbLines.filter { it.isNotBlank() && it != "none" },
            "x11vnc_processes" to x11vncLines.filter { it.isNotBlank() && it != "none" },
            "xdpyinfo_head" to xdpyinfo.lines().take(3),
            "x11vnc_log_tail" to logTail.lines().takeLast(15),
            "hint_for_ai" to buildHint(portOk, displayUp, xvfbLines.any { it.contains("Xvfb") })
        )
    }

    private fun buildHint(portOk: Boolean, displayUp: Boolean, xvfbRunning: Boolean): String {
        return when {
            !xvfbRunning -> "Xvfb not running. POST /api/vnc/start or run build.sh from editor with GUI tab."
            !portOk -> "x11vnc port ${EditorVncEnvironment.VNC_PORT} not listening. Check .zerotermux/x11vnc.log; POST /api/vnc/start."
            !displayUp -> "Display ${EditorVncEnvironment.DISPLAY} not responding. Restart VNC stack."
            else -> "VNC stack looks healthy. If viewer is black, check logcat EditorVncPanel EmbeddedVncFragment for viewport/sync issues; GET /api/screenshot while GUI tab visible."
        }
    }

    private fun sectionLines(block: String, header: String): List<String> {
        val text = sectionText(block, header)
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun sectionText(block: String, header: String): String {
        val start = block.indexOf(header)
        if (start < 0) return ""
        val from = start + header.length
        val nextHeaders = listOf(
            "XVFB_PROCESSES:", "X11VNC_PROCESSES:", "PORT_", "XDPYINFO:", "X11VNC_LOG_TAIL:", DIAG_MARKER_END
        )
        var end = block.length
        for (h in nextHeaders) {
            if (h == header) continue
            val idx = block.indexOf(h, from)
            if (idx in from until end) end = idx
        }
        return block.substring(from, end)
    }

    private fun extractBetween(raw: String, start: String, end: String): String? {
        val s = raw.indexOf(start)
        val e = raw.indexOf(end)
        if (s < 0 || e <= s) return null
        return raw.substring(s + start.length, e)
    }

    private fun resolvePath(path: String): String {
        if (path.startsWith("/")) return path
        val home = TermuxConstants.TERMUX_HOME_DIR_PATH
        return File(home, path).absolutePath
    }

    const val EXTRA_OPEN_X11_TAB = "zt_open_x11_tab"
    const val EXTRA_AUTO_RUN = "zt_auto_run"
}
