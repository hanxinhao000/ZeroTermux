package com.termux.zerocore.workstation

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.terminal.TerminalSession
import java.nio.charset.StandardCharsets

object ZtWorkstationTerminalHelper {

    private const val MSG_INPUT: Int = 0
    private const val MSG_RESIZE: Int = 1
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastCols = 0
    private var lastRows = 0

    fun handleTtyMessage(data: ByteArray) {
        if (!ZtWorkstationPermissionHelper.isTerminalAllowed()) return
        when (data[0].toInt()) {
            MSG_INPUT -> {
                if (data.size <= 1) return
                sendInput(String(data, 1, data.size - 1, StandardCharsets.UTF_8))
            }
            MSG_RESIZE -> {
                if (data.size < 5) return
                val cols = ((data[1].toInt() and 0xff) shl 8) or (data[2].toInt() and 0xff)
                val rows = ((data[3].toInt() and 0xff) shl 8) or (data[4].toInt() and 0xff)
                if (cols > 0 && rows > 0) {
                    updateSize(cols, rows)
                }
            }
        }
    }

    fun sendInput(text: String) {
        val session = currentSession() ?: return
        mainHandler.post { session.write(text) }
    }

    fun updateSize(cols: Int, rows: Int) {
        if (cols == lastCols && rows == lastRows) return
        lastCols = cols
        lastRows = rows
        val session = currentSession() ?: return
        mainHandler.post { session.updateSize(cols, rows, 10, 20) }
    }

    private fun currentSession(): TerminalSession? {
        val service = ZtWorkstationManager.termuxService ?: return null
        val context = com.example.xh_lib.utils.UUtils.getContext()
        val handle = TermuxAppSharedPreferences.build(context, false)?.currentSession
        if (!handle.isNullOrEmpty()) {
            service.getTerminalSessionForHandle(handle)?.let { return it }
        }
        return service.lastTermuxSession?.terminalSession
    }
}
