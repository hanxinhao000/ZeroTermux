package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtTerminalAiSnapshot
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
        runOnUi { host.sendTerminalText(toSend) }
        val tail = waitForTerminalSettle(host, initialWaitMs = 400, pollIntervalMs = 350, maxWaitMs = 6000)
        return buildString {
            appendLine("Command sent: $command")
            appendLine()
            append(tail)
        }.trim()
    }

    private fun sendKey(host: ZtEditorAiHost, args: JSONObject): String {
        val key = args.optString("key", "").lowercase()
        if (key.isEmpty()) {
            return "Error: key is required"
        }
        runOnUi { host.sendTerminalKey(key) }
        val tail = waitForTerminalSettle(host, initialWaitMs = 200, pollIntervalMs = 250, maxWaitMs = 2000)
        return buildString {
            appendLine("Key sent: $key")
            appendLine()
            append(tail)
        }.trim()
    }

    private fun waitForTerminalSettle(
        host: ZtEditorAiHost,
        initialWaitMs: Long,
        pollIntervalMs: Long,
        maxWaitMs: Long
    ): String {
        Thread.sleep(initialWaitMs)
        var waited = initialWaitMs
        var lastContent = ""
        var stableCount = 0
        while (waited < maxWaitMs) {
            val snap = host.captureTerminalSnapshot(2500)
            if (snap == lastContent) {
                stableCount++
                if (stableCount >= 2) break
            } else {
                stableCount = 0
                lastContent = snap
            }
            val lastLine = snap.lineSequence().lastOrNull { it.isNotBlank() }?.trim().orEmpty()
            if (ZtTerminalAiSnapshot.isShellPrompt(lastLine) && waited >= initialWaitMs + pollIntervalMs) {
                break
            }
            Thread.sleep(pollIntervalMs)
            waited += pollIntervalMs
        }
        return host.captureTerminalSnapshot(2500)
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
        latch.await(5, TimeUnit.SECONDS)
    }
}
