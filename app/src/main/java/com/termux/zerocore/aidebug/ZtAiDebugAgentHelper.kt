package com.termux.zerocore.aidebug

import com.google.gson.Gson
import com.termux.zerocore.ai.agent.ZtAgentAiAgentRunner
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtAgentAiConfigHelper
import com.termux.zerocore.ai.agent.ZtAgentAiSkillHelper
import com.termux.zerocore.ai.agent.ZtAgentAiTools
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 调试 API：查看智能体解析后的系统提示（含 Skills），并发起一轮真实 Agent 对话。
 */
object ZtAiDebugAgentHelper {

    private val gson = Gson()

    fun contextJson(): String {
        val terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled()
        val ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled()
        val filesystemEnabled = ZtAgentAiConfigHelper.isFilesystemEnabled()
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        val enabledIds = ZtAgentAiSkillHelper.enabledSkillIds()
        val skillsBlock = ZtAgentAiSkillHelper.buildSkillsPrompt(enabledIds)
        val capabilityPrompt = ZtAgentAiSkillHelper.skillsCapabilityPrompt()
        val systemPrompt = ZtAgentAiConfigHelper.resolveSystemPrompt(
            config.systemPrompt,
            terminalEnabled,
            ztControlEnabled,
            filesystemEnabled
        )
        val tools = ZtAgentAiTools.definitions(terminalEnabled, ztControlEnabled, filesystemEnabled)
        val toolNames = buildList {
            for (i in 0 until tools.length()) {
                add(
                    tools.getJSONObject(i)
                        .getJSONObject("function")
                        .getString("name")
                )
            }
        }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "configured" to ZtAgentAiConfigHelper.isConfigured(),
                "provider" to config.provider,
                "model" to config.model,
                "should_use_agent_runner" to ZtAgentAiConfigHelper.shouldUseAgentRunner(),
                "enabled_skill_ids" to enabledIds.sorted(),
                "skills_prompt_block" to skillsBlock,
                "skills_capability_prompt" to capabilityPrompt,
                "skills_in_system_prompt" to systemPrompt.contains(skillsBlock.trim().take(40)),
                "capability_in_system_prompt" to systemPrompt.contains("list_agent_skills"),
                "skill_tools" to toolNames.filter { it.contains("skill") },
                "tool_count" to toolNames.size,
                "system_prompt_length" to systemPrompt.length,
                "system_prompt" to systemPrompt
            )
        )
    }

    fun chatJson(body: JSONObject): String {
        if (!ZtAgentAiConfigHelper.isConfigured()) {
            return errorJson("agent AI not configured (api url / key / model)")
        }
        val message = body.optString("message", "").trim()
        if (message.isEmpty()) {
            return errorJson("message is required")
        }
        val timeoutSec = body.optInt("timeout_sec", 120).coerceIn(15, 300)
        val terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled()
        val ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled()
        val filesystemEnabled = ZtAgentAiConfigHelper.isFilesystemEnabled()
        val client = ZtAgentAiChatClient(ZtAgentAiConfigHelper.loadActiveConfig())
        val runner = ZtAgentAiAgentRunner(
            client,
            terminalEnabled,
            ztControlEnabled,
            filesystemEnabled
        )
        val latch = CountDownLatch(1)
        val replyRef = AtomicReference<String?>(null)
        val errorRef = AtomicReference<String?>(null)
        val toolSteps = Collections.synchronizedList(mutableListOf<Map<String, String>>())
        val history = listOf(
            ZtAgentAiChatClient.ChatMessage(role = "user", content = message)
        )
        runner.run(
            history,
            object : ZtAgentAiAgentRunner.Callback {
                override fun onToolStep(label: String, detail: String) {
                    toolSteps.add(
                        mapOf(
                            "label" to label,
                            "detail" to detail
                        )
                    )
                }

                override fun onComplete(content: String) {
                    replyRef.set(content)
                    latch.countDown()
                }

                override fun onError(message: String) {
                    errorRef.set(message)
                    latch.countDown()
                }

                override fun isCancelled(): Boolean = false
            }
        )
        val finished = latch.await(timeoutSec.toLong(), TimeUnit.SECONDS)
        if (!finished) {
            runner.cancel()
            client.cancel()
            return errorJson("timeout after ${timeoutSec}s")
        }
        val error = errorRef.get()
        if (error != null) {
            return gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to error,
                    "tool_steps" to toolSteps
                )
            )
        }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "reply" to replyRef.get().orEmpty(),
                "tool_steps" to toolSteps,
                "enabled_skill_ids" to ZtAgentAiSkillHelper.enabledSkillIds().sorted()
            )
        )
    }

    private fun errorJson(message: String): String =
        gson.toJson(mapOf("ok" to false, "error" to message))
}
