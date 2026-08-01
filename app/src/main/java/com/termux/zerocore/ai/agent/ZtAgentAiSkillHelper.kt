package com.termux.zerocore.ai.agent

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.R
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.utils.ZtLocaleStrings
import java.io.File
import java.util.Locale

/**
 * 本地 Skills：统一目录 ~/.skill/<skill-id>/SKILL.md
 */
object ZtAgentAiSkillHelper {

    const val SKILL_FILE_NAME = "SKILL.md"
    /** 内置默认可执行 Skill（随应用更新同步内容；默认关闭，需用户手动启用）。 */
    const val EXAMPLE_SKILL_ID = "termux-pkg-guide"
    private const val LEGACY_EXAMPLE_SKILL_ID = "termux-ops"
    private const val BUNDLED_EXAMPLE_VERSION = 3
    private const val MAX_BODY_PER_SKILL = 6000
    private const val MAX_TOTAL_SKILLS_CHARS = 16000
    private const val MAX_SAVE_CONTENT_CHARS = 12000

    private val gson = Gson()

    data class SkillEntry(
        val id: String,
        val name: String,
        val description: String,
        val file: File
    )

    data class SaveResult(
        val skillId: String,
        val path: String,
        val enabled: Boolean
    )

    fun skillsRoot(): File = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".skill")

    fun skillsRootDisplayPath(): String = "~/.skill"

    fun listAvailableSkills(): List<SkillEntry> {
        ensureSkillsRoot()
        ensureExampleSkillPresent()
        val root = skillsRoot()
        if (!root.isDirectory) return emptyList()
        return root.listFiles()?.orEmpty()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val skillFile = File(dir, SKILL_FILE_NAME)
                if (!skillFile.isFile) return@mapNotNull null
                val id = dir.name.trim()
                if (id.isEmpty()) return@mapNotNull null
                parseSkillFile(skillFile, id)
            }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            .orEmpty()
    }

    fun enabledSkillIds(): Set<String> {
        val json = UserSetManage.get().getZTUserBean().agentAiEnabledSkillsJson
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type).orEmpty().toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun saveEnabledSkillIds(ids: Set<String>) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.agentAiEnabledSkillsJson = if (ids.isEmpty()) null else gson.toJson(ids.sorted())
        UserSetManage.get().setZTUserBean(bean)
    }

    fun setSkillEnabled(skillId: String, enabled: Boolean) {
        val next = enabledSkillIds().toMutableSet()
        if (enabled) next.add(skillId) else next.remove(skillId)
        saveEnabledSkillIds(next)
    }

    fun sanitizeSkillId(raw: String): String? {
        val normalized = raw.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        if (normalized.isEmpty() || normalized.length > 64) return null
        return normalized
    }

    fun saveSkill(
        rawSkillId: String,
        displayName: String,
        description: String,
        content: String,
        enableAfterSave: Boolean = true
    ): Result<SaveResult> {
        val skillId = sanitizeSkillId(rawSkillId)
            ?: return Result.failure(IllegalArgumentException("invalid skill id"))
        val body = content.trim()
        if (body.isEmpty()) {
            return Result.failure(IllegalArgumentException("content is empty"))
        }
        if (body.length > MAX_SAVE_CONTENT_CHARS) {
            return Result.failure(IllegalArgumentException("content too long"))
        }
        val desc = description.trim()
        val name = displayName.trim().ifBlank { skillId }
        ensureSkillsRoot()
        val dir = File(skillsRoot(), skillId)
        val file = File(dir, SKILL_FILE_NAME)
        dir.mkdirs()
        file.writeText(buildSkillFile(name, desc, body))
        if (enableAfterSave) {
            setSkillEnabled(skillId, true)
        }
        return Result.success(
            SaveResult(
                skillId = skillId,
                path = file.absolutePath,
                enabled = enableAfterSave
            )
        )
    }

    fun listSkillsForTool(): String {
        val skills = listAvailableSkills()
        if (skills.isEmpty()) {
            return "No skills in ${skillsRootDisplayPath()}/. Use save_agent_skill to create one."
        }
        val enabled = enabledSkillIds()
        return buildString {
            append("Skills in ${skillsRootDisplayPath()}/:\n")
            skills.forEach { skill ->
                append("- ")
                append(skill.id)
                append(" | ")
                append(skill.name)
                append(" | enabled=")
                append(enabled.contains(skill.id))
                if (skill.description.isNotBlank()) {
                    append(" | ")
                    append(skill.description)
                }
                append('\n')
            }
        }.trim()
    }

    fun buildSkillsPrompt(enabledIds: Set<String>): String {
        if (enabledIds.isEmpty()) return ""
        val available = listAvailableSkills().associateBy { it.id }
        val header = ZtLocaleStrings.getString(R.string.zt_agent_ai_skills_prompt_header)
        val blocks = mutableListOf<String>()
        var total = 0
        for (id in enabledIds) {
            val entry = available[id] ?: continue
            val body = if (isBundledSkill(entry.id)) {
                exampleSkillBody().trim()
            } else {
                readSkillBody(entry).trim()
            }
            if (body.isEmpty()) continue
            val capped = truncate(body, MAX_BODY_PER_SKILL)
            val block = buildString {
                append("--- skill: ")
                append(displaySkillName(entry).ifBlank { entry.id })
                append(" (")
                append(entry.id)
                append(") ---\n")
                val desc = displaySkillDescription(entry)
                if (desc.isNotBlank()) {
                    append(desc.trim())
                    append('\n')
                }
                append(capped)
            }
            if (total + block.length > MAX_TOTAL_SKILLS_CHARS) break
            blocks.add(block)
            total += block.length
        }
        if (blocks.isEmpty()) return ""
        return buildString {
            append(header)
            append("\n\n")
            append(blocks.joinToString("\n\n"))
        }
    }

    fun skillsCapabilityPrompt(): String {
        return ZtLocaleStrings.getString(R.string.zt_agent_ai_skills_capability_prompt)
    }

    fun createExampleSkillIfNeeded(): File? {
        refreshBundledExampleSkill(preserveEnableState = true)
        return skillFile(EXAMPLE_SKILL_ID)
    }

    /** 若默认示例被删除则自动重建；已存在时同步为当前版本的内置内容。 */
    fun ensureExampleSkillPresent() {
        removeLegacyBundledExample()
        refreshBundledExampleSkill(preserveEnableState = true)
    }

    /**
     * 恢复为仅保留内置默认 Skill，删除其余目录（开关默认关闭，需用户手动启用）。
     */
    fun restoreDefaultSkills(): Result<Unit> {
        ensureSkillsRoot()
        val root = skillsRoot()
        root.listFiles()?.orEmpty()
            ?.filter { it.isDirectory }
            ?.forEach { dir ->
                if (dir.name != EXAMPLE_SKILL_ID) {
                    dir.deleteRecursively()
                }
            }
        removeLegacyBundledExample()
        refreshBundledExampleSkill(preserveEnableState = false)
        setSkillEnabled(EXAMPLE_SKILL_ID, false)
        return Result.success(Unit)
    }

    private fun refreshBundledExampleSkill(preserveEnableState: Boolean) {
        val file = File(File(skillsRoot(), EXAMPLE_SKILL_ID), SKILL_FILE_NAME)
        val existedBefore = file.isFile
        if (existedBefore && !needsBundledRefresh(file)) {
            return
        }
        val wasEnabled = enabledSkillIds().contains(EXAMPLE_SKILL_ID)
        saveSkill(
            rawSkillId = EXAMPLE_SKILL_ID,
            displayName = bundledExampleDisplayName(),
            description = bundledExampleDescription(),
            content = exampleSkillBody(),
            enableAfterSave = false
        ).onFailure { return }
        // 默认关闭；仅刷新已有文件时保留用户原先的开关状态
        setSkillEnabled(EXAMPLE_SKILL_ID, preserveEnableState && existedBefore && wasEnabled)
        markBundledExampleMeta()
    }

    private fun needsBundledRefresh(file: File): Boolean {
        if (!file.isFile) return true
        val raw = runCatching { file.readText() }.getOrDefault("")
        val (meta, _) = splitFrontmatter(raw)
        return meta["bundled_version"] != BUNDLED_EXAMPLE_VERSION.toString() ||
            meta["bundled_locale"] != currentLocaleTag()
    }

    private fun currentLocaleTag(): String =
        ZtLocaleStrings.context().resources.configuration.locales[0].toLanguageTag()

    private fun markBundledExampleMeta() {
        val file = skillFile(EXAMPLE_SKILL_ID) ?: return
        val raw = runCatching { file.readText() }.getOrDefault("")
        val (meta, body) = splitFrontmatter(raw)
        val updatedMeta = meta.toMutableMap().apply {
            put("name", bundledExampleDisplayName())
            put("description", bundledExampleDescription())
            put("bundled_version", BUNDLED_EXAMPLE_VERSION.toString())
            put("bundled", "true")
            put("bundled_locale", currentLocaleTag())
        }
        file.writeText(buildSkillFileFromMeta(updatedMeta, body))
    }

    private fun buildSkillFileFromMeta(meta: Map<String, String>, body: String): String {
        return buildString {
            appendLine("---")
            meta.forEach { (key, value) ->
                appendLine("$key: $value")
            }
            appendLine("---")
            appendLine()
            append(body.trim())
            if (!body.endsWith("\n")) appendLine()
        }
    }

    private fun removeLegacyBundledExample() {
        val legacyDir = File(skillsRoot(), LEGACY_EXAMPLE_SKILL_ID)
        if (legacyDir.exists()) {
            legacyDir.deleteRecursively()
        }
        val enabled = enabledSkillIds().toMutableSet()
        if (enabled.remove(LEGACY_EXAMPLE_SKILL_ID)) {
            enabled.add(EXAMPLE_SKILL_ID)
            saveEnabledSkillIds(enabled)
        }
    }

    private fun bundledExampleDisplayName(): String =
        ZtLocaleStrings.getString(R.string.zt_agent_skill_bundled_name)

    private fun bundledExampleDescription(): String =
        ZtLocaleStrings.getString(R.string.zt_agent_skill_bundled_description)

    fun createNewSkill(rawSkillId: String): Result<SaveResult> {
        val skillId = sanitizeSkillId(rawSkillId)
            ?: return Result.failure(IllegalArgumentException("invalid skill id"))
        val dir = File(skillsRoot(), skillId)
        if (dir.exists()) {
            return Result.failure(IllegalStateException("skill already exists"))
        }
        val displayName = skillId
        return saveSkill(
            rawSkillId = skillId,
            displayName = displayName,
            description = "",
            content = newSkillTemplateBody(displayName),
            enableAfterSave = true
        )
    }

    fun skillFile(skillId: String): File? {
        val file = File(File(skillsRoot(), skillId), SKILL_FILE_NAME)
        return if (file.isFile) file else null
    }

    fun isBundledSkill(skillId: String): Boolean = skillId == EXAMPLE_SKILL_ID

    fun displaySkillName(skill: SkillEntry): String {
        if (!isBundledSkill(skill.id)) return skill.name
        return ZtLocaleStrings.getString(R.string.zt_agent_skill_bundled_name)
    }

    fun displaySkillDescription(skill: SkillEntry): String {
        if (!isBundledSkill(skill.id)) return skill.description
        return ZtLocaleStrings.getString(R.string.zt_agent_skill_bundled_description)
    }

    fun deleteSkill(skillId: String): Result<Unit> {
        if (isBundledSkill(skillId)) {
            return Result.failure(IllegalStateException("bundled skill cannot be deleted"))
        }
        val dir = File(skillsRoot(), skillId)
        if (!dir.isDirectory) {
            return Result.failure(IllegalStateException("skill not found"))
        }
        if (!dir.deleteRecursively()) {
            return Result.failure(IllegalStateException("delete failed"))
        }
        val next = enabledSkillIds().toMutableSet()
        next.remove(skillId)
        saveEnabledSkillIds(next)
        return Result.success(Unit)
    }

    private fun newSkillTemplateBody(displayName: String): String =
        ZtLocaleStrings.getString(R.string.zt_agent_ai_skills_new_template, displayName)

    private fun ensureSkillsRoot() {
        skillsRoot().mkdirs()
    }

    private fun buildSkillFile(name: String, description: String, body: String): String {
        return buildString {
            appendLine("---")
            appendLine("name: $name")
            if (description.isNotBlank()) {
                appendLine("description: $description")
            }
            appendLine("---")
            appendLine()
            append(body)
            if (!body.endsWith("\n")) appendLine()
        }
    }

    private fun parseSkillFile(file: File, fallbackId: String): SkillEntry {
        val raw = runCatching { file.readText() }.getOrDefault("")
        val (meta, _) = splitFrontmatter(raw)
        val name = meta["name"]?.trim().orEmpty().ifBlank { fallbackId }
        val description = meta["description"]?.trim().orEmpty()
        return SkillEntry(
            id = fallbackId,
            name = name,
            description = description,
            file = file
        )
    }

    private fun readSkillBody(entry: SkillEntry): String {
        val raw = runCatching { entry.file.readText() }.getOrDefault("")
        return splitFrontmatter(raw).second
    }

    private fun splitFrontmatter(raw: String): Pair<Map<String, String>, String> {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("---")) {
            return emptyMap<String, String>() to trimmed
        }
        val end = trimmed.indexOf("\n---", 3)
        if (end < 0) {
            return emptyMap<String, String>() to trimmed
        }
        val front = trimmed.substring(3, end).trim()
        val bodyStart = end + 4
        val body = if (bodyStart < trimmed.length) trimmed.substring(bodyStart).trim() else ""
        val meta = linkedMapOf<String, String>()
        front.lineSequence().forEach { line ->
            val idx = line.indexOf(':')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim().lowercase(Locale.ROOT)
            val value = line.substring(idx + 1).trim().removeSurrounding("\"")
            if (key.isNotEmpty()) meta[key] = value
        }
        return meta to body
    }

    private fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        return text.substring(0, max) + "\n...[skill truncated]"
    }

    private fun exampleSkillBody(): String =
        ZtLocaleStrings.getString(R.string.zt_agent_skill_bundled_body)
}
