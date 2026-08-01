package com.termux.zerocore.editor.lsp

import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * LSP 调试缓冲：stderr / 最近错误 / 诊断摘要，供 AI Debug API 读取，避免 Toast 刷屏。
 */
object EditorLspDebugStore {
    private const val MAX_STDERR_LINES = 400
    private const val MAX_EVENTS = 80

    private val stderrLock = Any()
    private val stderrLines = ArrayDeque<String>()
    private val eventsLock = Any()
    private val events = ArrayDeque<Map<String, Any?>>()
    private val diagnosticSnapshots = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    @Volatile
    var lastFatalError: String? = null
        private set

    fun appendStderr(line: String) {
        val text = line.trimEnd()
        if (text.isEmpty()) return
        synchronized(stderrLock) {
            stderrLines.addLast(text)
            while (stderrLines.size > MAX_STDERR_LINES) stderrLines.removeFirst()
        }
    }

    fun recordEvent(type: String, detail: String, extra: Map<String, Any?> = emptyMap()) {
        val item = LinkedHashMap<String, Any?>()
        item["ts"] = System.currentTimeMillis()
        item["type"] = type
        item["detail"] = detail.take(500)
        if (extra.isNotEmpty()) item.putAll(extra)
        synchronized(eventsLock) {
            events.addLast(item)
            while (events.size > MAX_EVENTS) events.removeFirst()
        }
        if (type == "fatal" || type == "error") {
            lastFatalError = detail.take(300)
        }
    }

    fun setDiagnosticsSnapshot(uri: String, items: List<Map<String, Any?>>) {
        if (items.isEmpty()) {
            diagnosticSnapshots.remove(uri)
        } else {
            diagnosticSnapshots[uri] = items
        }
    }

    fun clearDiagnostics(uri: String) {
        diagnosticSnapshots.remove(uri)
    }

    fun clearAllDiagnostics() {
        diagnosticSnapshots.clear()
    }

    fun stderrTail(limit: Int = 120): List<String> {
        synchronized(stderrLock) {
            return stderrLines.toList().takeLast(limit.coerceIn(1, MAX_STDERR_LINES))
        }
    }

    fun recentEvents(limit: Int = 40): List<Map<String, Any?>> {
        synchronized(eventsLock) {
            return events.toList().takeLast(limit.coerceIn(1, MAX_EVENTS))
        }
    }

    fun diagnosticsSnapshot(): Map<String, List<Map<String, Any?>>> {
        return diagnosticSnapshots.toMap()
    }

    fun clear() {
        synchronized(stderrLock) { stderrLines.clear() }
        synchronized(eventsLock) { events.clear() }
        diagnosticSnapshots.clear()
        lastFatalError = null
    }
}
