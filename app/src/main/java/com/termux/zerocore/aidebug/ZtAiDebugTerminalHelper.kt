package com.termux.zerocore.aidebug

import com.termux.shared.shell.ShellUtils
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.terminal.TerminalSession

object ZtAiDebugTerminalHelper {

    fun snapshot(maxChars: Int = 12000): String {
        val session = currentSession() ?: return ""
        val text = ShellUtils.getTerminalSessionTranscriptText(session, false, true).orEmpty()
        if (text.length <= maxChars) return text
        return text.substring(text.length - maxChars)
    }

    fun sendCommand(command: String) {
        val session = currentSession() ?: return
        val normalized = if (command.endsWith("\n")) command else "$command\n"
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            session.write(normalized)
        }
    }

    fun execAndSnapshot(command: String, waitMs: Long): String {
        sendCommand(command)
        val wait = waitMs.coerceIn(200L, 30_000L)
        Thread.sleep(wait)
        return snapshot()
    }

    private fun currentSession(): TerminalSession? {
        val service = ZtAiDebugManager.termuxService ?: return null
        val context = com.example.xh_lib.utils.UUtils.getContext()
        val handle = TermuxAppSharedPreferences.build(context, false)?.currentSession
        if (!handle.isNullOrEmpty()) {
            service.getTerminalSessionForHandle(handle)?.let { return it }
        }
        return service.lastTermuxSession?.terminalSession
    }
}
