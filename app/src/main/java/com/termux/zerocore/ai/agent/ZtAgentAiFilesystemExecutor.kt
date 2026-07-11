package com.termux.zerocore.ai.agent

import com.termux.R
import com.termux.zerocore.utils.ZtLocaleStrings
import com.termux.zerocore.workstation.ZtWorkstationFileHelper
import org.json.JSONObject

object ZtAgentAiFilesystemExecutor {

    val toolNames: Set<String> = setOf(
        "list_directory",
        "read_file",
        "write_file",
        "create_file",
        "mkdir",
        "delete_path",
        "stat_path"
    )

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall): String {
        val args = try {
            JSONObject(toolCall.arguments.ifBlank { "{}" })
        } catch (_: Exception) {
            return errorJson("invalid arguments JSON")
        }
        return when (toolCall.name) {
            "list_directory" -> {
                val path = args.optString("path", "").trim().ifEmpty { null }
                ZtWorkstationFileHelper.listDirectory(path)
            }
            "read_file" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                val maxBytes = args.optInt("max_bytes", 512 * 1024).coerceIn(1024, 2 * 1024 * 1024)
                ZtWorkstationFileHelper.readText(path, maxBytes)
            }
            "write_file" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                if (!args.has("content")) return errorJson("content is required")
                val content = args.optString("content", "")
                ZtWorkstationFileHelper.writeText(path, content)
            }
            "create_file" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                ZtWorkstationFileHelper.createFile(path)
            }
            "mkdir" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                ZtWorkstationFileHelper.mkdir(path)
            }
            "delete_path" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                if (!args.optBoolean("user_confirmed", false)) {
                    return errorJson("user_confirmed must be true after the user explicitly agrees to delete")
                }
                ZtWorkstationFileHelper.delete(path)
            }
            "stat_path" -> {
                val path = args.optString("path", "").trim()
                if (path.isEmpty()) return errorJson("path is required")
                ZtWorkstationFileHelper.stat(path)
            }
            else -> errorJson("unknown filesystem tool: ${toolCall.name}")
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "list_directory" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_list_directory)
            "read_file" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_read_file)
            "write_file" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_write_file)
            "create_file" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_create_file)
            "mkdir" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_mkdir)
            "delete_path" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_delete_path)
            "stat_path" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_stat_path)
            else -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun errorJson(message: String): String =
        """{"ok":false,"error":${JSONObject.quote(message)}}"""
}
