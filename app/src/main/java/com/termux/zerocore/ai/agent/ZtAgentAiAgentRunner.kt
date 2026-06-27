package com.termux.zerocore.ai.agent

import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.config.ZtAiStrings
import org.json.JSONObject

class ZtAgentAiAgentRunner(
    private val client: ZtAgentAiChatClient,
    private val terminalEnabled: Boolean,
    private val ztControlEnabled: Boolean
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
                LogUtils.e(TAG, "agent interrupted")
            } catch (e: Exception) {
                LogUtils.e(TAG, "agent error: $e")
                if (!callback.isCancelled()) {
                    post { callback.onError(e.message ?: "Agent error") }
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
        val tools = if (terminalEnabled || ztControlEnabled) {
            ZtAgentAiTools.definitions(terminalEnabled, ztControlEnabled)
        } else {
            null
        }
        var rounds = 0
        while (rounds < MAX_TOOL_ROUNDS) {
            if (callback.isCancelled()) return
            if (terminalEnabled) {
                appendFreshTerminalSnapshot(workingMessages)
            }
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
        post { callback.onError(UUtils.getString(R.string.zt_agent_ai_tool_limit)) }
    }

    private fun appendFreshTerminalSnapshot(messages: MutableList<ZtAgentAiChatClient.ChatMessage>) {
        messages.removeAll { it.role == ROLE_SYSTEM && it.content?.startsWith(SNAPSHOT_PREFIX) == true }
        messages.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = ZtAgentAiTerminalExecutor.captureSnapshot(3000)
            )
        )
    }

    private fun executeToolCallWithUi(
        toolCall: ZtAgentAiChatClient.ToolCall,
        callback: Callback,
        workingMessages: MutableList<ZtAgentAiChatClient.ChatMessage>
    ) {
        val label = ZtAgentAiToolExecutor.statusLabel(toolCall.name)
        val preview = toolCallPreview(toolCall)
        val toolResult = ZtAgentAiToolExecutor.execute(
            toolCall,
            terminalEnabled,
            ztControlEnabled
        )
        android.util.Log.i(TAG, "tool ${toolCall.name} result=$toolResult")

        when (toolCall.name) {
            "send_terminal_command", "run_zt_command" -> {
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
                "send_terminal_command" -> {
                    val cmd = args.optString("command", "").trim()
                    if (cmd.isEmpty()) "" else "→ $cmd"
                }
                "send_terminal_key" -> args.optString("key", "").trim()
                "run_zt_command" -> {
                    val cmd = args.optString("command", "").trim()
                    if (cmd.isEmpty()) "" else "→ zt $cmd"
                }
                "run_zerotermux_zt" -> {
                    val cmd = args.optString("command", "").trim()
                    if (cmd.isEmpty()) "" else "→ zt $cmd"
                }
                "set_zerotermux_config" -> {
                    val key = args.optString("key", "").trim()
                    val value = args.optString("value", "").trim()
                    if (key.isEmpty()) "" else "→ $key = $value"
                }
                "open_zerotermux_page" -> {
                    val page = args.optString("page_id", "").trim()
                    if (page.isEmpty()) "→ list pages" else "→ open $page"
                }
                "list_zerotermux_capabilities" -> {
                    val cat = args.optString("category", "all").trim()
                    "→ list $cat"
                }
                "get_zerotermux_config" -> {
                    val group = args.optString("group", "").trim()
                    if (group.isEmpty()) "→ read config" else "→ read $group"
                }
                "get_zerotermux_left_menu" -> "→ read left menu xml"
                "update_zerotermux_left_menu" -> {
                    val pkg = args.optString("create_menu_package", args.optString("create_tab_name", "")).trim()
                    when {
                        pkg.isNotEmpty() -> "→ Menu 菜单: $pkg"
                        args.optString("append_group_xml", "").isNotBlank() -> "→ edit AI menu package"
                        args.optString("xml_content", "").isNotBlank() -> "→ replace AI menu xml"
                        else -> "→ update Menu 菜单"
                    }
                }
                "list_zerotermux_pkg_sources" -> "→ list APT mirrors"
                "switch_zerotermux_pkg_source" -> {
                    val id = args.optString("source_id", "").trim()
                    if (id.isEmpty()) "→ switch APT source" else "→ switch to $id"
                }
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
        list.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = ZtAgentAiConfigHelper.resolveSystemPrompt(
                    config.systemPrompt,
                    terminalEnabled,
                    ztControlEnabled
                )
            )
        )

        val historyMessages = history.filter { it.role == ROLE_USER || it.role == ROLE_ASSISTANT }
        val limited = if (terminalEnabled) {
            historyMessages.takeLast(TERMINAL_HISTORY_LIMIT)
        } else {
            historyMessages
        }

        if (terminalEnabled && limited.isNotEmpty()) {
            list.add(
                ZtAgentAiChatClient.ChatMessage(
                    role = ROLE_SYSTEM,
                    content = ZtAiStrings.terminalHistoryNotice()
                )
            )
            val lastUserIndex = limited.indexOfLast { it.role == ROLE_USER }
            if (lastUserIndex >= 0) {
                if (lastUserIndex > 0) {
                    list.addAll(limited.subList(0, lastUserIndex))
                }
                val lastUser = limited[lastUserIndex]
                val snapshot = ZtAgentAiTerminalExecutor.captureSnapshot(3000)
                list.add(
                    ZtAgentAiChatClient.ChatMessage(
                        role = ROLE_USER,
                        content = buildString {
                            appendLine(lastUser.content.orEmpty())
                            appendLine()
                            appendLine("---")
                            append(snapshot)
                        }.trim()
                    )
                )
                if (lastUserIndex < limited.lastIndex) {
                    list.addAll(limited.subList(lastUserIndex + 1, limited.size))
                }
            } else {
                list.addAll(limited)
            }
        } else {
            list.addAll(limited)
        }

        return list
    }

    private fun post(block: () -> Unit) {
        UUtils.getHandler().post { block() }
    }

    companion object {
        private const val TAG = "ZtAgentAiAgentRunner"
        private const val MAX_TOOL_ROUNDS = 15
        private const val TERMINAL_HISTORY_LIMIT = 6
        private val SNAPSHOT_PREFIX: String
            get() = ZtAiStrings.terminalSnapshotPrefix()
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_TOOL = "tool"
    }
}
