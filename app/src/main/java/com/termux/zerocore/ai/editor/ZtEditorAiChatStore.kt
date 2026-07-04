package com.termux.zerocore.ai.editor

import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ftp.utils.UserSetManage

/**
 * 编辑器 AI 对话历史（与主界面 [com.termux.zerocore.ai.agent.ZtAgentAiChatStore] 完全独立）。
 * 使用独立 SaveData 键，避免 ZTUserBean 并发读写覆盖历史。
 */
object ZtEditorAiChatStore {

    const val MAX_MESSAGES = 100
    const val TRIM_BATCH = 50

    /** 独立存储键，不与 agentAiChatHistoryJson 共用 */
    private const val STORAGE_KEY = "zt_editor_ai_chat_history_v1"

    private val gson = Gson()

    data class StoredMessage(
        val role: String,
        val content: String
    )

    fun load(): MutableList<ZtAgentAiChatClient.ChatMessage> {
        val json = SaveData.getStringOther(STORAGE_KEY)?.trim().orEmpty()
        if (json.isNotBlank() && json != "def") {
            return parseMessages(json)
        }
        // 从旧版 ZTUserBean 字段迁移
        val legacy = UserSetManage.get().getZTUserBean().editorAiChatHistoryJson?.trim().orEmpty()
        if (legacy.isBlank()) return mutableListOf()
        val migrated = parseMessages(legacy)
        if (migrated.isNotEmpty()) {
            save(migrated)
        }
        return migrated
    }

    fun save(messages: List<ZtAgentAiChatClient.ChatMessage>) {
        val trimmed = messages
            .filter { (it.role == ROLE_USER || it.role == ROLE_ASSISTANT) && !it.content.isNullOrBlank() }
            .toMutableList()
        trimIfNeeded(trimmed)
        val json = gson.toJson(trimmed.map { StoredMessage(it.role, it.content!!) })
        SaveData.saveStringOther(STORAGE_KEY, json)
    }

    fun clear() {
        SaveData.saveStringOther(STORAGE_KEY, "")
        val bean = UserSetManage.get().getZTUserBean()
        bean.editorAiChatHistoryJson = null
        UserSetManage.get().setZTUserBean(bean)
    }

    fun trimIfNeeded(messages: MutableList<ZtAgentAiChatClient.ChatMessage>) {
        while (messages.size > MAX_MESSAGES) {
            val removeCount = TRIM_BATCH.coerceAtMost(messages.size)
            repeat(removeCount) {
                if (messages.isNotEmpty()) messages.removeAt(0)
            }
        }
    }

    private fun parseMessages(json: String): MutableList<ZtAgentAiChatClient.ChatMessage> {
        return try {
            val type = object : TypeToken<List<StoredMessage>>() {}.type
            val stored: List<StoredMessage>? = gson.fromJson(json, type)
            stored?.mapNotNull { item ->
                if (item.role.isBlank() || item.content.isBlank()) return@mapNotNull null
                if (item.role != ROLE_USER && item.role != ROLE_ASSISTANT) return@mapNotNull null
                ZtAgentAiChatClient.ChatMessage(item.role, item.content)
            }?.toMutableList() ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private const val ROLE_USER = "user"
    private const val ROLE_ASSISTANT = "assistant"
}
