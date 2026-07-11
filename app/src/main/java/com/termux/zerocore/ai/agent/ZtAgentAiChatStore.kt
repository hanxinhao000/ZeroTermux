package com.termux.zerocore.ai.agent

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtAgentAiChatStore {

    const val MAX_MESSAGES = 100
    const val TRIM_BATCH = 50

    private val gson = Gson()

    data class StoredMessage(
        val role: String,
        val content: String,
        val terminalSnapshot: String? = null
    )

    fun load(): MutableList<ZtAgentAiChatClient.ChatMessage> {
        val json = UserSetManage.get().getZTUserBean().agentAiChatHistoryJson
        if (json.isNullOrBlank()) return mutableListOf()
        return try {
            val type = object : TypeToken<List<StoredMessage>>() {}.type
            val stored: List<StoredMessage>? = gson.fromJson(json, type)
            stored?.mapNotNull { item ->
                if (item.role.isBlank() || item.content.isBlank()) return@mapNotNull null
                if (item.role != ROLE_USER && item.role != ROLE_ASSISTANT) return@mapNotNull null
                ZtAgentAiChatClient.ChatMessage(
                    role = item.role,
                    content = item.content,
                    terminalSnapshot = item.terminalSnapshot?.takeIf { it.isNotBlank() }
                )
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
        bean.agentAiChatHistoryJson = gson.toJson(
            trimmed.map {
                StoredMessage(
                    role = it.role,
                    content = it.content!!,
                    terminalSnapshot = it.terminalSnapshot?.takeIf { snap -> snap.isNotBlank() }
                )
            }
        )
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

    /** 将用户原文与终端快照拼成发给模型的 content。 */
    fun contentWithSnapshot(userText: String, snapshot: String?): String {
        val text = userText.trim()
        val snap = snapshot?.trim().orEmpty()
        if (snap.isEmpty()) return text
        return buildString {
            appendLine(text)
            appendLine()
            appendLine("---")
            append(snap)
        }.trim()
    }

    private const val ROLE_USER = "user"
    private const val ROLE_ASSISTANT = "assistant"
}
