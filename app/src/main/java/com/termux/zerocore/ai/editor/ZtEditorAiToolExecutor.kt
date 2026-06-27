package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ZtEditorAiToolExecutor {

    private val editTools = setOf("read_editor", "insert_at_cursor", "replace_range", "replace_all")
    private val terminalTools = setOf("read_terminal", "send_terminal_command", "send_terminal_key")

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        if (toolCall.name in terminalTools) {
            return ZtEditorAiTerminalExecutor.execute(toolCall, host)
        }
        if (toolCall.name in editTools && !host.isEditorReady()) {
            return UUtils.getString(R.string.zt_editor_ai_unavailable)
        }
        val result = AtomicReference<String>()
        val latch = CountDownLatch(1)
        UUtils.getHandler().post {
            try {
                result.set(executeOnUi(toolCall, host))
            } catch (e: Exception) {
                result.set("Error: ${e.message ?: "tool failed"}")
            } finally {
                latch.countDown()
            }
        }
        latch.await(8, TimeUnit.SECONDS)
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
            else -> UUtils.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun executeOnUi(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        return when (toolCall.name) {
            "read_editor" -> host.captureSnapshot(args.optInt("max_chars", 12000))
            "insert_at_cursor" -> {
                val text = args.optString("text", "")
                if (text.isEmpty()) "Error: text is required" else host.insertAtCursor(text)
            }
            "replace_range" -> {
                if (!args.has("start") || !args.has("end")) {
                    return "Error: start and end are required"
                }
                host.replaceRange(args.getInt("start"), args.getInt("end"), args.optString("text", ""))
            }
            "replace_all" -> {
                val text = args.optString("text", "")
                if (text.isEmpty()) "Error: text is required" else host.replaceAll(text)
            }
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
            else -> "Error: unknown tool `${toolCall.name}`"
        }
    }
}
