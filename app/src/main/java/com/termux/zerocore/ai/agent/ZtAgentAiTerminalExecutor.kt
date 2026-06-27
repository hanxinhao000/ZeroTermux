package com.termux.zerocore.ai.agent

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.SingletonCommunicationUtils
import org.json.JSONObject

object ZtAgentAiTerminalExecutor {

    private const val DEFAULT_MAX_CHARS = 8000
    private const val MIN_MAX_CHARS = 500
    private const val MAX_MAX_CHARS = 12000

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return UUtils.getString(R.string.zt_agent_ai_terminal_unavailable)
        }
        val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
        return try {
            val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
            when (toolCall.name) {
                "read_terminal" -> captureSnapshot(args.optInt("max_chars", DEFAULT_MAX_CHARS))
                "send_terminal_command" -> sendCommand(args, listener)
                "send_terminal_key" -> sendKey(args, listener)
                else -> "Error: unknown tool `${toolCall.name}`"
            }
        } catch (e: Exception) {
            "Error: ${e.message ?: "tool execution failed"}"
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

    fun captureSnapshot(maxChars: Int = DEFAULT_MAX_CHARS): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return UUtils.getString(R.string.zt_agent_ai_terminal_unavailable)
        }
        val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
        val visible = listener.getVisibleTerminalText().trim()
        val full = listener.getTextToTerminal()?.trim().orEmpty()
        return ZtTerminalAiSnapshot.format(visible, full, maxChars)
    }

    fun runZtCommand(command: String): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return UUtils.getString(R.string.zt_agent_ai_terminal_unavailable)
        }
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return "Error: command is empty"
        }
        val ztLine = when {
            trimmed == "zt" -> "zt help"
            trimmed.startsWith("zt ") -> trimmed
            else -> "zt $trimmed"
        }
        val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
        return sendCommand(
            JSONObject().put("command", ztLine).put("append_newline", true),
            listener,
            initialWaitMs = 500,
            maxWaitMs = 6000
        )
    }

    private fun sendCommand(
        args: JSONObject,
        listener: SingletonCommunicationUtils.SingletonCommunicationListener,
        initialWaitMs: Long = 400,
        pollIntervalMs: Long = 350,
        maxWaitMs: Long = 4000
    ): String {
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
        listener.sendTextToTerminal(toSend)
        val tail = waitForTerminalSettle(
            initialWaitMs = initialWaitMs,
            pollIntervalMs = pollIntervalMs,
            maxWaitMs = maxWaitMs
        )
        return buildString {
            appendLine("Command sent: $command")
            appendLine()
            append(tail)
        }.trim()
    }

    private fun sendKey(
        args: JSONObject,
        listener: SingletonCommunicationUtils.SingletonCommunicationListener
    ): String {
        when (args.optString("key", "").lowercase()) {
            "enter" -> listener.onTerminalExtraKeyButtonClick("ENTER")
            "tab" -> listener.onTerminalExtraKeyButtonClick("TAB")
            "escape" -> listener.onTerminalExtraKeyButtonClick("ESC")
            "backspace" -> listener.onTerminalExtraKeyButtonClick("BKSP")
            "up" -> listener.onTerminalExtraKeyButtonClick("UP")
            "down" -> listener.onTerminalExtraKeyButtonClick("DOWN")
            "left" -> listener.onTerminalExtraKeyButtonClick("LEFT")
            "right" -> listener.onTerminalExtraKeyButtonClick("RIGHT")
            "ctrl_c" -> listener.sendTextToTerminalCtrl("c", true)
            "ctrl_d" -> listener.sendTextToTerminalCtrl("d", true)
            "ctrl_l" -> listener.sendTextToTerminalCtrl("l", true)
            "ctrl_z" -> listener.sendTextToTerminalCtrl("z", true)
            else -> return "Error: unsupported key `${args.optString("key")}`"
        }
        val tail = waitForTerminalSettle(
            initialWaitMs = 200,
            pollIntervalMs = 250,
            maxWaitMs = 2000
        )
        return buildString {
            appendLine("Key sent: ${args.optString("key")}")
            appendLine()
            append(tail)
        }.trim()
    }

    private fun waitForTerminalSettle(
        initialWaitMs: Long,
        pollIntervalMs: Long,
        maxWaitMs: Long
    ): String {
        Thread.sleep(initialWaitMs)
        var waited = initialWaitMs
        var lastContent = ""
        var stableCount = 0
        while (waited < maxWaitMs) {
            val snap = captureSnapshot(2500)
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
        return captureSnapshot(2500)
    }
}
