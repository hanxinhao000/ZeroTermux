package com.termux.zerocore.aidebug

import com.google.gson.Gson
import com.termux.zerocore.ai.agent.ZtAgentAiSkillHelper
import org.json.JSONObject

object ZtAiDebugSkillHelper {

    private val gson = Gson()

    fun listJson(): String {
        val skills = ZtAgentAiSkillHelper.listAvailableSkills()
        val enabled = ZtAgentAiSkillHelper.enabledSkillIds()
        return gson.toJson(
            mapOf(
                "ok" to true,
                "skills_root" to ZtAgentAiSkillHelper.skillsRootDisplayPath(),
                "enabled_ids" to enabled.sorted(),
                "skills" to skills.map { skill ->
                    mapOf(
                        "id" to skill.id,
                        "name" to skill.name,
                        "description" to skill.description,
                        "path" to skill.file.absolutePath,
                        "enabled" to enabled.contains(skill.id)
                    )
                }
            )
        )
    }

    fun createJson(body: JSONObject): String {
        val rawId = body.optString("skill_id", body.optString("name", "")).trim()
        if (rawId.isEmpty()) {
            return errorJson("skill_id or name is required")
        }
        return ZtAgentAiSkillHelper.createNewSkill(rawId).fold(
            onSuccess = { result ->
                gson.toJson(
                    mapOf(
                        "ok" to true,
                        "skill_id" to result.skillId,
                        "path" to result.path,
                        "enabled" to result.enabled
                    )
                )
            },
            onFailure = { error ->
                errorJson(error.message ?: "create failed")
            }
        )
    }

    fun saveJson(body: JSONObject): String {
        val rawId = body.optString("skill_id", "").trim()
        val description = body.optString("description", "").trim()
        val content = body.optString("content", "").trim()
        if (rawId.isEmpty()) return errorJson("skill_id is required")
        if (description.isEmpty()) return errorJson("description is required")
        if (content.isEmpty()) return errorJson("content is required")
        val displayName = body.optString("name", "").trim()
        val enableAfterSave = if (body.has("enable_after_save")) {
            body.optBoolean("enable_after_save", true)
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
                gson.toJson(
                    mapOf(
                        "ok" to true,
                        "skill_id" to result.skillId,
                        "path" to result.path,
                        "enabled" to result.enabled
                    )
                )
            },
            onFailure = { error ->
                errorJson(error.message ?: "save failed")
            }
        )
    }

    fun setEnabledJson(body: JSONObject): String {
        val skillId = body.optString("skill_id", "").trim()
        if (skillId.isEmpty()) return errorJson("skill_id is required")
        if (!body.has("enabled")) return errorJson("enabled is required")
        val enabled = body.optBoolean("enabled", false)
        ZtAgentAiSkillHelper.setSkillEnabled(skillId, enabled)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "skill_id" to skillId,
                "enabled" to enabled,
                "enabled_ids" to ZtAgentAiSkillHelper.enabledSkillIds().sorted()
            )
        )
    }

    fun ensureExampleJson(): String {
        val file = ZtAgentAiSkillHelper.createExampleSkillIfNeeded()
        return gson.toJson(
            mapOf(
                "ok" to true,
                "example_skill_id" to ZtAgentAiSkillHelper.EXAMPLE_SKILL_ID,
                "path" to file?.absolutePath,
                "created_or_present" to (file != null)
            )
        )
    }

    fun restoreDefaultJson(): String {
        return ZtAgentAiSkillHelper.restoreDefaultSkills().fold(
            onSuccess = {
                gson.toJson(
                    mapOf(
                        "ok" to true,
                        "example_skill_id" to ZtAgentAiSkillHelper.EXAMPLE_SKILL_ID,
                        "enabled_ids" to ZtAgentAiSkillHelper.enabledSkillIds().sorted(),
                        "skills" to ZtAgentAiSkillHelper.listAvailableSkills().map { skill ->
                            mapOf(
                                "id" to skill.id,
                                "name" to skill.name,
                                "enabled" to ZtAgentAiSkillHelper.enabledSkillIds().contains(skill.id)
                            )
                        }
                    )
                )
            },
            onFailure = { error ->
                errorJson(error.message ?: "restore failed")
            }
        )
    }

    private fun errorJson(message: String): String =
        gson.toJson(mapOf("ok" to false, "error" to message))
}
