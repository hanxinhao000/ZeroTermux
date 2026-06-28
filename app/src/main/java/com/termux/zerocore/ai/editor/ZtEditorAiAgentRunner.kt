package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtAgentAiConfigHelper
import org.json.JSONObject

class ZtEditorAiAgentRunner(
    private val client: ZtAgentAiChatClient,
    private val host: ZtEditorAiHost
) {
    interface Callback {
        fun onToolStep(label: String, detail: String)
        fun onComplete(content: String)
        fun onError(message: String)
        fun isCancelled(): Boolean
    }

    @Volatile
    private var worker: Thread? = null

    fun cancel() {
        worker?.interrupt()
        worker = null
    }

    fun run(history: List<ZtAgentAiChatClient.ChatMessage>, callback: Callback) {
        cancel()
        val thread = Thread {
            try {
                runInternal(history, callback)
            } catch (e: InterruptedException) {
                LogUtils.e(TAG, "editor ai interrupted")
            } catch (e: Exception) {
                LogUtils.e(TAG, "editor ai error: $e")
                if (!callback.isCancelled()) {
                    post { callback.onError(e.message ?: "Editor AI error") }
                }
            }
        }
        worker = thread
        thread.start()
    }

    private fun runInternal(
        history: List<ZtAgentAiChatClient.ChatMessage>,
        callback: Callback
    ) {
        val workingMessages = buildWorkingMessages(history)
        val tools = ZtEditorAiTools.definitions()
        var rounds = 0
        while (rounds < MAX_TOOL_ROUNDS) {
            if (callback.isCancelled()) return
            appendFreshEditorSnapshot(workingMessages)
            appendFreshTerminalSnapshot(workingMessages)
            val result = client.chatCompletionSync(workingMessages, tools)
            if (callback.isCancelled()) return
            if (result.error != null) {
                post { callback.onError(result.error) }
                return
            }
            if (result.toolCalls.isNotEmpty()) {
                workingMessages.add(
                    ZtAgentAiChatClient.ChatMessage(
                        role = ROLE_ASSISTANT,
                        content = result.content,
                        toolCalls = result.toolCalls
                    )
                )
                for (toolCall in result.toolCalls) {
                    if (callback.isCancelled()) return
                    executeToolCallWithUi(toolCall, callback, workingMessages)
                }
                rounds++
                continue
            }
            val reply = result.content?.trim().orEmpty()
            post { callback.onComplete(reply) }
            return
        }
        post {
            callback.onError(
                String.format(
                    UUtils.getString(R.string.zt_agent_ai_tool_limit),
                    MAX_TOOL_ROUNDS
                )
            )
        }
    }

    private fun appendFreshEditorSnapshot(messages: MutableList<ZtAgentAiChatClient.ChatMessage>) {
        messages.removeAll { it.role == ROLE_SYSTEM && it.content?.startsWith(EDITOR_SNAPSHOT_PREFIX) == true }
        messages.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = host.captureSnapshot(8000)
            )
        )
    }

    private fun appendFreshTerminalSnapshot(messages: MutableList<ZtAgentAiChatClient.ChatMessage>) {
        if (!host.isTerminalAvailable()) return
        messages.removeAll { it.role == ROLE_SYSTEM && it.content?.startsWith(TERMINAL_SNAPSHOT_PREFIX) == true }
        messages.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = host.captureTerminalSnapshot(3000)
            )
        )
    }

    private fun executeToolCallWithUi(
        toolCall: ZtAgentAiChatClient.ToolCall,
        callback: Callback,
        workingMessages: MutableList<ZtAgentAiChatClient.ChatMessage>
    ) {
        val label = ZtEditorAiToolExecutor.statusLabel(toolCall.name)
        val preview = toolCallPreview(toolCall)
        val toolResult = ZtEditorAiToolExecutor.execute(toolCall, host)

        when (toolCall.name) {
            "send_terminal_command" -> {
                post { callback.onToolStep(label, preview) }
                post {
                    callback.onToolStep(
                        UUtils.getString(R.string.zt_agent_ai_tool_read_after_send),
                        ""
                    )
                }
            }
            "read_terminal", "send_terminal_key" -> {
                post { callback.onToolStep(label, preview) }
            }
            else -> {
                val detail = when {
                    preview.isNotBlank() && toolResult.isNotBlank() -> "$preview\n\n$toolResult"
                    toolResult.isNotBlank() -> toolResult
                    else -> preview
                }
                post { callback.onToolStep(label, detail) }
            }
        }
        workingMessages.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_TOOL,
                content = toolResult,
                toolCallId = toolCall.id
            )
        )
    }

    private fun toolCallPreview(toolCall: ZtAgentAiChatClient.ToolCall): String {
        return try {
            val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
            when (toolCall.name) {
                "insert_at_cursor" -> args.optString("text", "").take(80)
                "replace_all" -> "→ replace all"
                "create_file" -> args.optString("path", "")
                "open_file" -> args.optString("path", "")
                "save_current_file" -> "→ save"
                "list_open_files" -> "→ list tabs"
                "replace_range" -> {
                    val start = args.optInt("start", -1)
                    val end = args.optInt("end", -1)
                    if (start >= 0 && end >= 0) "→ $start..$end" else ""
                }
                "send_terminal_command" -> {
                    val cmd = args.optString("command", "").trim()
                    if (cmd.isEmpty()) "" else "→ $cmd"
                }
                "send_terminal_key" -> args.optString("key", "").trim()
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildWorkingMessages(
        history: List<ZtAgentAiChatClient.ChatMessage>
    ): MutableList<ZtAgentAiChatClient.ChatMessage> {
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        val list = mutableListOf<ZtAgentAiChatClient.ChatMessage>()
        val basePrompt = config.systemPrompt.ifBlank {
            UUtils.getString(R.string.zt_agent_ai_default_system_prompt)
        }
        list.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = basePrompt + "\n\n" + UUtils.getString(R.string.zt_editor_ai_system_prompt)
            )
        )
        list.addAll(history.filter { it.role == ROLE_USER || it.role == ROLE_ASSISTANT }.takeLast(HISTORY_LIMIT))
        return list
    }

    private fun post(block: () -> Unit) {
        UUtils.getHandler().post { block() }
    }

    companion object {
        private const val TAG = "ZtEditorAiAgentRunner"
        private const val MAX_TOOL_ROUNDS = 30
        private const val HISTORY_LIMIT = 12
        private const val EDITOR_SNAPSHOT_PREFIX = "=== 编辑器快照"
        private const val TERMINAL_SNAPSHOT_PREFIX = "=== 终端快照"
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_TOOL = "tool"
    }
}
