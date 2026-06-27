package com.termux.zerocore.ai.config

import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.config.ztcommand.navigation.ZtNavigationHelper
import org.json.JSONArray
import org.json.JSONObject

object ZtAiConfigExecutor {

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        return try {
            when (toolCall.name) {
                "list_zerotermux_capabilities" -> {
                    ZtAiConfigRegistry.listCapabilities(args.optString("category", "all"))
                }
                "get_zerotermux_config" -> {
                    val keysArg = args.optJSONArray("keys")
                    ZtAiConfigRegistry.getConfig(
                        group = args.optString("group").takeIf { it.isNotBlank() },
                        keys = keysArg
                    )
                }
                "set_zerotermux_config" -> {
                    val key = args.optString("key", "").trim()
                    if (key.isEmpty()) return "Error: key is required"
                    ZtAiConfigRegistry.setConfig(key, args.optString("value", ""))
                }
                "open_zerotermux_page" -> {
                    openPage(args)
                }
                "run_zerotermux_zt" -> {
                    val command = args.optString("command", "").trim()
                    if (command.isEmpty()) return "Error: command is required"
                    ZtAiZtSocketClient.send(command)
                }
                "get_zerotermux_left_menu" -> ZtAiLeftMenuHelper.getMenuInfo()
                "update_zerotermux_left_menu" -> ZtAiLeftMenuHelper.updateMenu(args)
                "list_zerotermux_pkg_sources" -> ZtAiPkgSourceHelper.listSources()
                "switch_zerotermux_pkg_source" -> ZtAiPkgSourceHelper.switchSource(args)
                else -> "Error: unknown config tool `${toolCall.name}`"
            }
        } catch (e: Exception) {
            "Error: ${e.message ?: "config tool failed"}"
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "list_zerotermux_capabilities" -> ZtAiStrings.statusListCapabilities()
            "get_zerotermux_config" -> ZtAiStrings.statusGetConfig()
            "set_zerotermux_config" -> ZtAiStrings.statusSetConfig()
            "open_zerotermux_page" -> ZtAiStrings.statusOpenPage()
            "run_zerotermux_zt" -> ZtAiStrings.statusRunZt()
            "get_zerotermux_left_menu" -> ZtAiStrings.statusGetLeftMenu()
            "update_zerotermux_left_menu" -> ZtAiStrings.statusUpdateLeftMenu()
            "list_zerotermux_pkg_sources" -> ZtAiStrings.statusListPkgSources()
            "switch_zerotermux_pkg_source" -> ZtAiStrings.statusSwitchPkgSource()
            else -> ZtAiStrings.statusConfigDefault()
        }
    }

    private fun openPage(args: JSONObject): String {
        val pageId = args.optString("page_id", "").trim()
        if (pageId.isEmpty()) {
            return ZtNavigationHelper.listPagesJson()
        }
        val extras = args.optJSONObject("extras")
        val context = com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder.get()
            ?: return """{"code":1,"msg":"Termux main screen is not active. Return to terminal first."}"""
        return ZtNavigationHelper.openPage(context, pageId, extras)
    }
}
