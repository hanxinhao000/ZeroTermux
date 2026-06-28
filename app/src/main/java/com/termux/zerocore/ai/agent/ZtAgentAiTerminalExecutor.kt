package com.termux.zerocore.ai.agent

import com.termux.R
import com.termux.zerocore.utils.SingletonCommunicationUtils
import com.termux.zerocore.utils.ZtLocaleStrings
import org.json.JSONObject

object ZtAgentAiTerminalExecutor {

    private const val DEFAULT_MAX_CHARS = 8000

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return ZtLocaleStrings.getString(R.string.zt_agent_ai_terminal_unavailable)
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
        } catch (e: InterruptedException) {
            throw e
        } catch (e: Exception) {
            "Error: ${e.message ?: "tool execution failed"}"
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "read_terminal" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_read_terminal)
            "send_terminal_command" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_send_command)
            "send_terminal_key" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_send_key)
            else -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    fun captureSnapshot(maxChars: Int = DEFAULT_MAX_CHARS): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return ZtLocaleStrings.getString(R.string.zt_agent_ai_terminal_unavailable)
        }
        val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
        val visible = listener.getVisibleTerminalText().trim()
        val full = listener.getTextToTerminal()?.trim().orEmpty()
        return ZtTerminalAiSnapshot.format(visible, full, maxChars)
    }

    fun runZtCommand(command: String): String {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return ZtLocaleStrings.getString(R.string.zt_agent_ai_terminal_unavailable)
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
            JSONObject()
                .put("command", ztLine)
                .put("append_newline", true),
            listener,
            defaultMaxWaitMs = ZtTerminalWaitHelper.DEFAULT_ZT_COMMAND_MAX_WAIT_MS
        )
    }

    private fun sendCommand(
        args: JSONObject,
        listener: SingletonCommunicationUtils.SingletonCommunicationListener,
        defaultMaxWaitMs: Long = ZtTerminalWaitHelper.DEFAULT_COMMAND_MAX_WAIT_MS
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
        val maxWaitMs = ZtTerminalWaitHelper.resolveMaxWaitMs(
            args.optLong("max_wait_ms").takeIf { args.has("max_wait_ms") },
            defaultMaxWaitMs
        )
        listener.sendTextToTerminal(toSend)
        val result = ZtTerminalWaitHelper.waitForTerminalSettle(
            maxWaitMs = maxWaitMs
        ) { captureSnapshot(2500) }
        return ZtTerminalWaitHelper.formatCommandResult("Command sent: $command", result)
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
        val result = ZtTerminalWaitHelper.waitForTerminalSettle(
            initialWaitMs = 200,
            pollIntervalMs = 250,
            maxWaitMs = ZtTerminalWaitHelper.DEFAULT_KEY_MAX_WAIT_MS
        ) { captureSnapshot(2500) }
        return ZtTerminalWaitHelper.formatCommandResult(
            "Key sent: ${args.optString("key")}",
            result
        )
    }
}
