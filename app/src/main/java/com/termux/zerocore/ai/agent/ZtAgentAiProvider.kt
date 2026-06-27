package com.termux.zerocore.ai.agent

object ZtAgentAiProvider {
    const val DEEPSEEK = "deepseek"
    const val OPENAI = "openai"
    const val GOOGLE = "google"
    const val CUSTOM = "custom"

    val ALL = listOf(DEEPSEEK, OPENAI, GOOGLE, CUSTOM)

    data class Defaults(
        val apiUrl: String,
        val model: String,
        val apiFormat: String
    )

    fun defaults(provider: String): Defaults = when (provider) {
        OPENAI -> Defaults(
            apiUrl = "https://api.openai.com/v1/chat/completions",
            model = "gpt-4o-mini",
            apiFormat = "openai"
        )
        GOOGLE -> Defaults(
            apiUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            model = "gemini-2.0-flash",
            apiFormat = "openai"
        )
        CUSTOM -> Defaults(
            apiUrl = "",
            model = "",
            apiFormat = "openai"
        )
        else -> Defaults(
            apiUrl = "https://api.deepseek.com/chat/completions",
            model = "deepseek-chat",
            apiFormat = "openai"
        )
    }
}
