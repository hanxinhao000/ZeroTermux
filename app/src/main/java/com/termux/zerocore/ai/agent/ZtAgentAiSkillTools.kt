package com.termux.zerocore.ai.agent

import com.termux.zerocore.ai.config.ZtAiStrings
import org.json.JSONArray
import org.json.JSONObject

object ZtAgentAiSkillTools {

    val toolNames = setOf(
        "list_agent_skills",
        "save_agent_skill"
    )

    fun addSkillTools(tools: JSONArray) {
        tools.put(tool(
            "list_agent_skills",
            ZtAiStrings.toolListAgentSkills(),
            JSONObject().put("type", "object").put("properties", JSONObject())
                .put("required", JSONArray())
        ))
        tools.put(tool(
            "save_agent_skill",
            ZtAiStrings.toolSaveAgentSkill(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "skill_id",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_skill_id))
                    )
                    .put(
                        "name",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_skill_name))
                    )
                    .put(
                        "description",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_skill_description))
                    )
                    .put(
                        "content",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_skill_content))
                    )
                    .put(
                        "enable_after_save",
                        JSONObject()
                            .put("type", "boolean")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_skill_enable))
                    )
            ).put(
                "required",
                JSONArray().put("skill_id").put("description").put("content")
            )
        ))
    }

    private fun tool(name: String, description: String, parameters: JSONObject): JSONObject {
        return JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("parameters", parameters)
            )
    }
}
