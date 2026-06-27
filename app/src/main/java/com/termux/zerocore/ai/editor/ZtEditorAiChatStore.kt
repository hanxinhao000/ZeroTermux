package com.termux.zerocore.ai.editor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtEditorAiChatStore {

    const val MAX_MESSAGES = 100
    const val TRIM_BATCH = 50

    private val gson = Gson()

    data class StoredMessage(
        val role: String,
        val content: String
    )

    fun load(): MutableList<ZtAgentAiChatClient.ChatMessage> {
        val json = UserSetManage.get().getZTUserBean().editorAiChatHistoryJson
        if (json.isNullOrBlank()) return mutableListOf()
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

    fun save(messages: List<ZtAgentAiChatClient.ChatMessage>) {
        val trimmed = messages
            .filter { (it.role == ROLE_USER || it.role == ROLE_ASSISTANT) && !it.content.isNullOrBlank() }
            .toMutableList()
        trimIfNeeded(trimmed)
        val bean = UserSetManage.get().getZTUserBean()
        bean.editorAiChatHistoryJson = gson.toJson(
            trimmed.map { StoredMessage(it.role, it.content!!) }
        )
        UserSetManage.get().setZTUserBean(bean)
    }

    fun clear() {
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

    private const val ROLE_USER = "user"
    private const val ROLE_ASSISTANT = "assistant"
}
