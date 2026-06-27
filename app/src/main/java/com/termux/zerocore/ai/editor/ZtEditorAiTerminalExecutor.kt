package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtTerminalWaitHelper
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ZtEditorAiTerminalExecutor {

    private const val DEFAULT_MAX_CHARS = 8000

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall, host: ZtEditorAiHost): String {
        if (!host.isTerminalAvailable()) {
            return UUtils.getString(R.string.zt_editor_ai_terminal_unavailable)
        }
        return try {
            val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
            when (toolCall.name) {
                "read_terminal" -> host.captureTerminalSnapshot(args.optInt("max_chars", DEFAULT_MAX_CHARS))
                "send_terminal_command" -> sendCommand(host, args)
                "send_terminal_key" -> sendKey(host, args)
                else -> "Error: unknown tool `${toolCall.name}`"
            }
        } catch (e: InterruptedException) {
            throw e
        } catch (e: Exception) {
            "Error: ${e.message ?: "terminal tool failed"}"
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "read_terminal" -> UUtils.getString(R.string.zt_agent_ai_tool_read_terminal)
            "send_terminal_command" -> UUtils.getString(R.string.zt_agent_ai_tool_send_command)
            "send_terminal_key" -> UUtils.getString(R.string.zt_agent_ai_tool_send_key)
            else -> UUtils.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun sendCommand(host: ZtEditorAiHost, args: JSONObject): String {
        val command = args.optString("command", "").trim()
        if (command.isEmpty()) {
            return "Error: command is empty"
        }
        val appendNewline = if (args.has("append_newline")) {
            args.optBoolean("append_newline", true)
        } else {
            true
        }
        val toSend = if (appendNewline && !command.endsWith("\n")) {
            "$command\n"
        } else {
            command
        }
        val maxWaitMs = ZtTerminalWaitHelper.resolveMaxWaitMs(
            args.optLong("max_wait_ms").takeIf { args.has("max_wait_ms") },
            ZtTerminalWaitHelper.DEFAULT_COMMAND_MAX_WAIT_MS
        )
        runOnUi { host.sendTerminalText(toSend) }
        val result = ZtTerminalWaitHelper.waitForTerminalSettle(
            maxWaitMs = maxWaitMs
        ) { host.captureTerminalSnapshot(2500) }
        return ZtTerminalWaitHelper.formatCommandResult("Command sent: $command", result)
    }

    private fun sendKey(host: ZtEditorAiHost, args: JSONObject): String {
        val key = args.optString("key", "").lowercase()
        if (key.isEmpty()) {
            return "Error: key is required"
        }
        runOnUi { host.sendTerminalKey(key) }
        val result = ZtTerminalWaitHelper.waitForTerminalSettle(
            initialWaitMs = 200,
            pollIntervalMs = 250,
            maxWaitMs = ZtTerminalWaitHelper.DEFAULT_KEY_MAX_WAIT_MS
        ) { host.captureTerminalSnapshot(2500) }
        return ZtTerminalWaitHelper.formatCommandResult("Key sent: $key", result)
    }

    private fun runOnUi(block: () -> Unit) {
        val latch = CountDownLatch(1)
        UUtils.getHandler().post {
            try {
                block()
            } finally {
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
    }
}
