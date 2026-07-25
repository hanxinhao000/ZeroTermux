package com.termux.zerocore.ai.agent

import com.termux.R
import com.termux.zerocore.utils.ZtLocaleStrings
import org.json.JSONObject

object ZtAgentAiSkillExecutor {

    fun execute(toolCall: ZtAgentAiChatClient.ToolCall): String {
        return when (toolCall.name) {
            "list_agent_skills" -> ZtAgentAiSkillHelper.listSkillsForTool()
            "save_agent_skill" -> saveSkill(toolCall)
            else -> "Error: unknown skill tool `${toolCall.name}`"
        }
    }

    fun statusLabel(toolName: String): String {
        return when (toolName) {
            "list_agent_skills" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_list_skills)
            "save_agent_skill" -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_save_skill)
            else -> ZtLocaleStrings.getString(R.string.zt_agent_ai_tool_running)
        }
    }

    private fun saveSkill(toolCall: ZtAgentAiChatClient.ToolCall): String {
        val args = JSONObject(toolCall.arguments.ifBlank { "{}" })
        val rawId = args.optString("skill_id", "").trim()
        if (rawId.isEmpty()) {
            return "Error: skill_id is required"
        }
        val description = args.optString("description", "").trim()
        val content = args.optString("content", "").trim()
        if (description.isEmpty()) {
            return "Error: description is required"
        }
        if (content.isEmpty()) {
            return "Error: content is required"
        }
        val displayName = args.optString("name", "").trim()
        val enableAfterSave = if (args.has("enable_after_save")) {
            args.optBoolean("enable_after_save", true)
        } else {
            true
        }
        return ZtAgentAiSkillHelper.saveSkill(
            rawSkillId = rawId,
            displayName = displayName,
            description = description,
            content = content,
            enableAfterSave = enableAfterSave
        ).fold(
            onSuccess = { result ->
                val state = ZtLocaleStrings.getString(
                    if (result.enabled) {
                        R.string.zt_agent_ai_skill_saved_enabled
                    } else {
                        R.string.zt_agent_ai_skill_saved_disabled
                    }
                )
                ZtLocaleStrings.format(
                    R.string.zt_agent_ai_skill_saved,
                    result.skillId,
                    result.path,
                    state
                )
            },
            onFailure = { error ->
                "Error: ${error.message ?: "failed to save skill"}"
            }
        )
    }
}
