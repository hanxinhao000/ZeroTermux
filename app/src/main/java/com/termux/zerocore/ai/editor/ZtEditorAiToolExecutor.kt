package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.utils.ZtLocaleStrings
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ZtEditorAiToolExecutor {

    private val editTools = setOf("read_editor", "insert_at_cursor", "replace_range", "replace_all")
    private val confirmEditTools = setOf("insert_at_cursor", "replace_range", "replace_all")
    private val terminalTools = setOf("read_terminal", "send_terminal_command", "send_terminal_key")

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        if (toolCall.name in terminalTools) {
            return ZtEditorAiTerminalExecutor.execute(toolCall, host)
        }
        if (toolCall.name in confirmEditTools) {
            return executeConfirmEditTool(toolCall, host)
        }
        return executeOnUiThread(toolCall, host)
    }

    /** 后台线程等待；UI 线程只做校验、弹窗与写入，避免 ANR。 */
    private fun executeConfirmEditTool(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        val latch = CountDownLatch(1)
        val result = AtomicReference<String>()

        UUtils.getHandler().post {
            try {
                when (toolCall.name) {
                    "insert_at_cursor" -> {
                        val text = args.optString("text", "")
                        if (text.isEmpty()) {
                            result.set("Error: text is required")
                            latch.countDown()
                            return@post
                        }
                        if (!ensureEditableCurrentFile(host)) {
                            result.set(currentFileEditError(host))
                            latch.countDown()
                            return@post
                        }
                        val preview = buildInsertPreview(text)
                        val label = statusLabel(toolCall.name)
                        host.requestCodeEditConfirmation(label, preview.summary, preview.body) { approved ->
                            if (!approved) {
                                result.set(UUtils.getString(R.string.zt_editor_ai_edit_rejected))
                            } else {
                                result.set(host.insertAtCursor(text))
                            }
                            latch.countDown()
                        }
                    }
                    "replace_range" -> {
                        if (!args.has("start") || !args.has("end")) {
                            result.set("Error: start and end are required")
                            latch.countDown()
                            return@post
                        }
                        if (!ensureEditableCurrentFile(host)) {
                            result.set(currentFileEditError(host))
                            latch.countDown()
                            return@post
                        }
                        val start = args.getInt("start")
                        val end = args.getInt("end")
                        val text = args.optString("text", "")
                        val full = host.getCurrentEditorText().orEmpty()
                        if (start < 0 || end < start || end > full.length) {
                            result.set("Error: invalid range $start..$end (length ${full.length})")
                            latch.countDown()
                            return@post
                        }
                        val oldSlice = full.substring(start, end)
                        val preview = buildReplaceRangePreview(start, end, oldSlice, text)
                        val label = statusLabel(toolCall.name)
                        host.requestCodeEditConfirmation(label, preview.summary, preview.body) { approved ->
                            if (!approved) {
                                result.set(UUtils.getString(R.string.zt_editor_ai_edit_rejected))
                            } else {
                                result.set(host.replaceRange(start, end, text))
                            }
                            latch.countDown()
                        }
                    }
                    "replace_all" -> {
                        val text = args.optString("text", "")
                        if (text.isEmpty()) {
                            result.set("Error: text is required")
                            latch.countDown()
                            return@post
                        }
                        if (!ensureEditableCurrentFile(host)) {
                            result.set(currentFileEditError(host))
                            latch.countDown()
                            return@post
                        }
                        val old = host.getCurrentEditorText().orEmpty()
                        val preview = buildReplaceAllPreview(old, text)
                        val label = statusLabel(toolCall.name)
                        host.requestCodeEditConfirmation(label, preview.summary, preview.body) { approved ->
                            if (!approved) {
                                result.set(UUtils.getString(R.string.zt_editor_ai_edit_rejected))
                            } else {
                                result.set(host.replaceAll(text))
                            }
                            latch.countDown()
                        }
                    }
                    else -> {
                        result.set("Error: unknown tool `${toolCall.name}`")
                        latch.countDown()
                    }
                }
            } catch (e: Exception) {
                result.set("Error: ${e.message ?: "tool failed"}")
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.MINUTES)
        return result.get() ?: "Error: editor tool timeout"
    }

    private fun executeOnUiThread(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        val result = AtomicReference<String>()
        val latch = CountDownLatch(1)
        UUtils.getHandler().post {
            try {
                if (toolCall.name in editTools && !host.isEditorReady()) {
                    result.set(UUtils.getString(R.string.zt_editor_ai_unavailable))
                } else {
                    result.set(executeOnUi(toolCall, host))
                }
            } catch (e: Exception) {
                result.set("Error: ${e.message ?: "tool failed"}")
            } finally {
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.MINUTES)
        return result.get() ?: "Error: editor tool timeout"
    }

    fun statusLabel(toolName: String): String {
        if (toolName in terminalTools) {
            return ZtEditorAiTerminalExecutor.statusLabel(toolName)
        }
        return when (toolName) {
            "read_editor" -> UUtils.getString(R.string.zt_editor_ai_tool_read)
            "insert_at_cursor" -> UUtils.getString(R.string.zt_editor_ai_tool_insert)
            "replace_range" -> UUtils.getString(R.string.zt_editor_ai_tool_replace)
            "replace_all" -> UUtils.getString(R.string.zt_editor_ai_tool_replace_all)
            "create_file" -> UUtils.getString(R.string.zt_editor_ai_tool_create)
            "open_file" -> UUtils.getString(R.string.zt_editor_ai_tool_open)
            "save_current_file" -> UUtils.getString(R.string.zt_editor_ai_tool_save)
            "list_open_files" -> UUtils.getString(R.string.zt_editor_ai_tool_list)
            "run_build_script" -> UUtils.getString(R.string.zt_editor_ai_tool_run_build)
            "switch_editor_dock_tab" -> UUtils.getString(R.string.zt_editor_ai_tool_switch_dock)
            else -> UUtils.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun executeOnUi(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        return when (toolCall.name) {
            "read_editor" -> host.captureSnapshot(args.optInt("max_chars", 12000))
            "create_file" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return "Error: path is required"
                host.createEditorFile(
                    path,
                    args.optString("content", ""),
                    args.optBoolean("open", true)
                )
            }
            "open_file" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return "Error: path is required"
                host.openEditorFile(path)
            }
            "save_current_file" -> host.saveCurrentEditorFile()
            "list_open_files" -> host.listOpenEditorFiles()
            "run_build_script" -> host.runBuildScriptForAi()
            "switch_editor_dock_tab" -> {
                val tab = args.optString("tab", "").trim()
                if (tab.isEmpty()) return "Error: tab is required (gui or terminal)"
                host.switchEditorDockTab(tab)
            }
            else -> "Error: unknown tool `${toolCall.name}`"
        }
    }

    private fun ensureEditableCurrentFile(host: ZtEditorAiHost): Boolean {
        return host.isEditorReady() && !host.getCurrentEditorFilePath().isNullOrBlank()
    }

    private fun currentFileEditError(host: ZtEditorAiHost): String {
        return if (!host.isEditorReady()) {
            UUtils.getString(R.string.zt_editor_ai_unavailable)
        } else {
            UUtils.getString(R.string.zt_editor_ai_edit_no_file)
        }
    }

    private data class Preview(val summary: String, val body: CharSequence)

    private fun buildInsertPreview(text: String): Preview {
        val diff = ZtEditorAiEditDiff.additionsOnly(text)
        val summary = ZtLocaleStrings.getString(
            R.string.zt_editor_ai_edit_preview_insert_diff,
            diff.added
        )
        return Preview(summary, diff.body)
    }

    private fun buildReplaceRangePreview(
        start: Int,
        end: Int,
        oldSlice: String,
        newText: String
    ): Preview {
        val diff = ZtEditorAiEditDiff.diff(oldSlice, newText)
        val summary = ZtLocaleStrings.getString(
            R.string.zt_editor_ai_edit_preview_range_diff,
            start,
            end,
            diff.added,
            diff.removed
        )
        return Preview(summary, diff.body)
    }

    private fun buildReplaceAllPreview(oldText: String, newText: String): Preview {
        val diff = ZtEditorAiEditDiff.diff(oldText, newText)
        val lineCount = newText.lines().size
        val summary = ZtLocaleStrings.getString(
            R.string.zt_editor_ai_edit_preview_replace_all_diff,
            lineCount,
            diff.added,
            diff.removed
        )
        return Preview(summary, diff.body)
    }
}
