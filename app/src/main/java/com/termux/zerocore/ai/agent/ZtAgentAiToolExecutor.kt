package com.termux.zerocore.ai.agent

import com.example.xh_lib.utils.UUtils
import com.termux.R
import org.json.JSONObject

object ZtAgentAiToolExecutor {

    fun execute(
        toolCall: ZtAgentAiChatClient.ToolCall,
        terminalEnabled: Boolean,
        ztControlEnabled: Boolean
    ): String {
        return when (toolCall.name) {
            "read_terminal", "send_terminal_command", "send_terminal_key" -> {
                if (!terminalEnabled) {
                    return UUtils.getString(R.string.zt_agent_ai_tool_disabled)
                }
                ZtAgentAiTerminalExecutor.execute(toolCall)
            }
            "run_zt_command" -> {
                if (!ztControlEnabled) {
                    return UUtils.getString(R.string.zt_agent_ai_tool_disabled)
                }
                if (!terminalEnabled) {
                    return UUtils.getString(R.string.zt_agent_ai_zt_requires_terminal)
                }
                runZtCommand(toolCall)
            }
            else -> "Error: unknown tool `${toolCall.name}`"
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "read_terminal" -> UUtils.getString(R.string.zt_agent_ai_tool_read_terminal)
            "send_terminal_command" -> UUtils.getString(R.string.zt_agent_ai_tool_send_command)
            "send_terminal_key" -> UUtils.getString(R.string.zt_agent_ai_tool_send_key)
            "run_zt_command" -> UUtils.getString(R.string.zt_agent_ai_tool_run_command)
            else -> UUtils.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun runZtCommand(toolCall: ZtAgentAiChatClient.ToolCall): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        val command = args.optString("command", "").trim()
        if (command.isEmpty()) {
            return "Error: command is required. Run help first. Examples: help, openpage zt_settings, openleft, toast hello"
        }
        android.util.Log.i("ZT_CMD", "run_zt_command via terminal: zt $command")
        return ZtAgentAiTerminalExecutor.runZtCommand(command)
    }
}
