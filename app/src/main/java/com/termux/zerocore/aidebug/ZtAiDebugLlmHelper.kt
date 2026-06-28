package com.termux.zerocore.aidebug

import com.google.gson.Gson
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtAgentAiToolExecutor
import com.termux.zerocore.ai.config.ZtAiConfigRegistry
import com.termux.zerocore.ai.config.ZtAiZtSocketClient
import com.termux.zerocore.ai.config.ZtBeautifyClearHelper
import com.termux.zerocore.ai.config.ZtBeautifyColorHelper
import com.termux.zerocore.ai.config.ZtAiContainerHelper
import com.termux.zerocore.command.ZtCommandDefHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * 调试 API（19998）代理主界面 AI 智能体的全部 LLM 工具，便于外部 Cursor 等远程调试。
 * 鉴权由调试匹配码负责，此处不再检查 agentAiZtControlEnabled。
 */
object ZtAiDebugLlmHelper {

    private val gson = Gson()

    val toolNames: List<String> = listOf(
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
        "run_zerotermux_command_def",
        "read_terminal",
        "send_terminal_command",
        "send_terminal_key",
        "run_zt_command"
    )

    fun listToolsJson(): String {
        return gson.toJson(
            mapOf(
                "ok" to true,
                "tools" to toolNames,
                "hint" to "POST /api/llm/tool with {\"tool\":\"set_zerotermux_config\",\"arguments\":{\"key\":\"font_color\",\"value\":\"#00FF00\"}}",
                "beautify_shortcut" to mapOf(
                    "get" to "GET /api/beautify/colors",
                    "set" to "POST /api/beautify/colors {\"font_color\":\"#FFFFFF\",\"back_color\":\"#000000\"}",
                    "reset" to "POST /api/beautify/clear"
                ),
                "container_shortcut" to mapOf(
                    "list" to "GET /api/containers",
                    "create" to "POST /api/containers/create {\"container_name\":\"MyLinux\",\"switch_after_create\":\"true\",\"restart_app\":\"true\"}",
                    "switch" to "POST /api/containers/switch {\"container_id\":\"files1\",\"restart_app\":\"true\"}",
                    "delete" to "POST /api/containers/delete {\"container_id\":\"files1\",\"user_confirmed\":\"true\"}"
                ),
                "command_def_shortcut" to mapOf(
                    "list" to "GET /api/commands",
                    "add" to "POST /api/commands/add {\"name\":\"更新\",\"command\":\"pkg update\",\"append_newline\":\"true\"}",
                    "run" to "POST /api/commands/run {\"name\":\"更新\"}",
                    "delete" to "POST /api/commands/delete {\"name\":\"更新\",\"user_confirmed\":\"true\"}"
                )
            )
        )
    }

    fun listCommandDefsJson(): String = ZtCommandDefHelper.listJson()

    fun addCommandDefJson(body: JSONObject): String = ZtCommandDefHelper.addCommand(body)

    fun updateCommandDefJson(body: JSONObject): String = ZtCommandDefHelper.updateCommand(body)

    fun deleteCommandDefJson(body: JSONObject): String = ZtCommandDefHelper.deleteCommand(body)

    fun runCommandDefJson(body: JSONObject): String = ZtCommandDefHelper.runCommand(body)

    fun listContainersJson(): String = ZtAiContainerHelper.listContainersJson()

    fun createContainerJson(body: JSONObject): String = ZtAiContainerHelper.createContainer(body)

    fun switchContainerJson(body: JSONObject): String = ZtAiContainerHelper.switchContainer(body)

    fun deleteContainerJson(body: JSONObject): String = ZtAiContainerHelper.deleteContainer(body)

    fun executeTool(tool: String, arguments: JSONObject): String {
        val name = tool.trim()
        if (name.isEmpty()) {
            return errorJson("tool is required")
        }
        if (name !in toolNames) {
            return errorJson("unknown tool: $name. GET /api/llm/tools for list.")
        }
        val toolCall = ZtAgentAiChatClient.ToolCall(
            id = "debug-api",
            name = name,
            arguments = arguments.toString()
        )
        val result = ZtAgentAiToolExecutor.execute(
            toolCall,
            terminalEnabled = true,
            ztControlEnabled = true
        )
        val ok = !result.startsWith("Error:") &&
            !result.contains("\"ok\":false") &&
            !result.contains("\"code\":1")
        return gson.toJson(
            mapOf(
                "ok" to ok,
                "tool" to name,
                "result" to result
            )
        )
    }

    fun getConfig(group: String?, keys: JSONArray?): String =
        ZtAiConfigRegistry.getConfig(group, keys)

    fun setConfig(key: String, value: String): String =
        ZtAiConfigRegistry.setConfig(key.trim(), value)

    fun runZt(command: String): String {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return errorJson("command is required")
        val response = ZtAiZtSocketClient.send(trimmed)
        return gson.toJson(mapOf("ok" to response.contains("\"code\":0"), "command" to trimmed, "response" to response))
    }

    fun getBeautifyColorsJson(): String = ZtBeautifyColorHelper.snapshotJson()

    fun setBeautifyColors(body: JSONObject): String = ZtBeautifyColorHelper.applyFromDebugBody(body)

    fun clearBeautifyJson(): String = ZtBeautifyClearHelper.clearAndApplyUi()

    private fun errorJson(message: String): String =
        gson.toJson(mapOf("ok" to false, "error" to message))
}
