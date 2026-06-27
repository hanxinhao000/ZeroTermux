package com.termux.zerocore.ai.agent

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtAgentAiConfigHelper {

    private val gson = Gson()

    data class ProviderConfig(
        var apiUrl: String = "",
        var apiKey: String = "",
        var model: String = ""
    )

    data class ActiveConfig(
        val provider: String,
        val apiUrl: String,
        val apiKey: String,
        val model: String,
        val systemPrompt: String,
        val apiFormat: String
    )

    fun activeProvider(): String {
        val bean = UserSetManage.get().getZTUserBean()
        val provider = bean.agentAiActiveProvider
        return if (provider.isNullOrBlank()) ZtAgentAiProvider.DEEPSEEK else provider
    }

    fun loadActiveConfig(): ActiveConfig {
        val bean = UserSetManage.get().getZTUserBean()
        val provider = activeProvider()
        val defaults = ZtAgentAiProvider.defaults(provider)
        return ActiveConfig(
            provider = provider,
            apiUrl = bean.agentAiApiUrl?.takeIf { it.isNotBlank() } ?: defaults.apiUrl,
            apiKey = bean.agentAiApiKey.orEmpty(),
            model = bean.agentAiModel?.takeIf { it.isNotBlank() } ?: defaults.model,
            systemPrompt = bean.agentAiSystemPrompt.orEmpty(),
            apiFormat = defaults.apiFormat
        )
    }

    fun loadProviderConfig(provider: String): ProviderConfig {
        val cache = readCache()
        val cached = cache[provider]
        val defaults = ZtAgentAiProvider.defaults(provider)
        return ProviderConfig(
            apiUrl = cached?.apiUrl?.takeIf { it.isNotBlank() } ?: defaults.apiUrl,
            apiKey = cached?.apiKey.orEmpty(),
            model = cached?.model?.takeIf { it.isNotBlank() } ?: defaults.model
        )
    }

    fun switchProvider(newProvider: String) {
        val bean = UserSetManage.get().getZTUserBean()
        val currentProvider = activeProvider()
        if (currentProvider != newProvider) {
            saveCurrentToCache(bean, currentProvider)
        }
        bean.agentAiActiveProvider = newProvider
        applyCachedProvider(bean, newProvider)
        UserSetManage.get().setZTUserBean(bean)
    }

    fun saveActiveFields(apiUrl: String, apiKey: String, model: String, systemPrompt: String) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.agentAiApiUrl = apiUrl.trim()
        bean.agentAiApiKey = apiKey.trim()
        bean.agentAiModel = model.trim()
        bean.agentAiSystemPrompt = systemPrompt.trim()
        saveCurrentToCache(bean, activeProvider())
        UserSetManage.get().setZTUserBean(bean)
    }

    fun isTerminalEnabled(): Boolean {
        return UserSetManage.get().getZTUserBean().isAgentAiTerminalEnabled
    }

    fun saveTerminalEnabled(enabled: Boolean) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.setAgentAiTerminalEnabled(enabled)
        UserSetManage.get().setZTUserBean(bean)
    }

    fun isZtControlEnabled(): Boolean {
        return UserSetManage.get().getZTUserBean().isAgentAiZtControlEnabled
    }

    fun saveZtControlEnabled(enabled: Boolean) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.setAgentAiZtControlEnabled(enabled)
        UserSetManage.get().setZTUserBean(bean)
    }

    fun resolveSystemPrompt(
        rawPrompt: String,
        terminalEnabled: Boolean,
        ztControlEnabled: Boolean = isZtControlEnabled()
    ): String {
        val base = rawPrompt.trim().ifBlank {
            com.example.xh_lib.utils.UUtils.getString(com.termux.R.string.zt_agent_ai_default_system_prompt)
        }
        val parts = mutableListOf(base)
        if (terminalEnabled) {
            parts.add(
                com.example.xh_lib.utils.UUtils.getString(
                    com.termux.R.string.zt_agent_ai_terminal_system_prompt
                )
            )
        } else {
            parts.add(
                com.example.xh_lib.utils.UUtils.getString(
                    com.termux.R.string.zt_agent_ai_terminal_disabled_prompt
                )
            )
        }
        if (ztControlEnabled) {
            parts.add(
                com.example.xh_lib.utils.UUtils.getString(
                    com.termux.R.string.zt_agent_ai_zt_control_system_prompt
                )
            )
        } else {
            parts.add(
                com.example.xh_lib.utils.UUtils.getString(
                    com.termux.R.string.zt_agent_ai_zt_control_disabled_prompt
                )
            )
        }
        if (terminalEnabled || ztControlEnabled) {
            parts.add(
                com.example.xh_lib.utils.UUtils.getString(
                    com.termux.R.string.zt_agent_ai_tool_usage_prompt
                )
            )
        }
        return parts.joinToString("\n\n")
    }

    fun isAgentToolsEnabled(): Boolean {
        return isTerminalEnabled() || isZtControlEnabled()
    }

    fun isConfigured(): Boolean {
        val config = loadActiveConfig()
        return config.apiUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()
    }

    private fun applyCachedProvider(bean: ZTUserBean, provider: String) {
        val config = loadProviderConfig(provider)
        bean.agentAiApiUrl = config.apiUrl
        bean.agentAiApiKey = config.apiKey
        bean.agentAiModel = config.model
    }

    private fun saveCurrentToCache(bean: ZTUserBean, provider: String) {
        if (provider.isBlank()) return
        val cache = readCache()
        cache[provider] = ProviderConfig(
            apiUrl = bean.agentAiApiUrl.orEmpty(),
            apiKey = bean.agentAiApiKey.orEmpty(),
            model = bean.agentAiModel.orEmpty()
        )
        bean.agentAiProviderCacheJson = gson.toJson(cache)
    }

    private fun readCache(): MutableMap<String, ProviderConfig> {
        val json = UserSetManage.get().getZTUserBean().agentAiProviderCacheJson
        if (json.isNullOrBlank()) return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, ProviderConfig>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }
}
