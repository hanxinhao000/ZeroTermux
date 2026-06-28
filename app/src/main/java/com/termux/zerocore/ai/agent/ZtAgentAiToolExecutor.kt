package com.termux.zerocore.ai.agent

import com.termux.R
import com.termux.zerocore.ai.config.ZtAiConfigExecutor
import com.termux.zerocore.utils.ZtLocaleStrings
import org.json.JSONObject

object ZtAgentAiToolExecutor {

    private val configToolNames = setOf(
        "list_zerotermux_capabilities",
        "get_zerotermux_config",
        "set_zerotermux_config",
        "open_zerotermux_page",
        "run_zerotermux_zt",
        "get_zerotermux_left_menu",
        "update_zerotermux_left_menu",
        "list_zerotermux_pkg_sources",
        "switch_zerotermux_pkg_source",
        "reset_zerotermux_beautify",
        "list_zerotermux_containers",
        "create_zerotermux_container",
        "switch_zerotermux_container",
        "delete_zerotermux_container",
        "list_zerotermux_command_defs",
        "add_zerotermux_command_def",
        "update_zerotermux_command_def",
        "delete_zerotermux_command_def",
        "run_zerotermux_command_def"
    )

    fun execute(
        toolCall: ZtAgentAiChatClient.ToolCall,
        terminalEnabled: Boolean,
        ztControlEnabled: Boolean
    ): String {
        if (toolCall.name in configToolNames) {
            if (!ztControlEnabled) {
                return ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_disabled)
            }
            return ZtAiConfigExecutor.execute(toolCall)
        }
        return when (toolCall.name) {
            "read_terminal", "send_terminal_command", "send_terminal_key" -> {
                if (!terminalEnabled) {
                    return ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_disabled)
                }
                ZtAgentAiTerminalExecutor.execute(toolCall)
            }
            "run_zt_command" -> {
                if (!ztControlEnabled) {
                    return ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_disabled)
                }
                if (!terminalEnabled) {
                    return ZtLocaleStrings.getString(R.string.zt_agent_ai_zt_requires_terminal)
                }
                runZtCommand(toolCall)
            }
            else -> "Error: unknown tool `${toolCall.name}`"
        }
    }

    fun statusLabel(toolName: String): String {
        if (toolName in configToolNames) {
            return ZtAiConfigExecutor.statusLabel(toolName)
        }
        return when (toolName) {
            "read_terminal" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_read_terminal)
            "send_terminal_command" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_send_command)
            "send_terminal_key" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_send_key)
            "run_zt_command" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_run_command)
            else -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_running)
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
