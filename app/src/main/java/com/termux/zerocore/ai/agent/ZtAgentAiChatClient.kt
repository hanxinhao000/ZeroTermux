package com.termux.zerocore.ai.agent

import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ZtAgentAiChatClient(
    private val config: ZtAgentAiConfigHelper.ActiveConfig
) {
    interface Listener {
        fun onChunk(text: String)
        fun onError(message: String)
        fun onComplete(fullText: String)
    }

    data class ChatMessage(
        val role: String,
        val content: String? = null,
        val toolCalls: List<ToolCall>? = null,
        val toolCallId: String? = null
    )

    data class ToolCall(
        val id: String,
        val name: String,
        val arguments: String
    )

    data class CompletionResult(
        val content: String?,
        val toolCalls: List<ToolCall>,
        val error: String?
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeCall: Call? = null

    fun cancel() {
        activeCall?.cancel()
        activeCall = null
    }

    fun chatCompletionSync(
        messages: List<ChatMessage>,
        tools: JSONArray?
    ): CompletionResult {
        if (config.apiUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            return CompletionResult(
                content = null,
                toolCalls = emptyList(),
                error = UUtils.getString(com.termux.R.string.zt_ai_agent_not_configured)
            )
        }
        return try {
            val bodyJson = buildRequestBody(messages, stream = false, tools = tools)
            val body = bodyJson.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(config.apiUrl.trim())
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val call = client.newCall(request)
            activeCall = call
            call.execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return CompletionResult(
                        content = null,
                        toolCalls = emptyList(),
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
                parseCompletionResponse(responseBody)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "chatCompletionSync error: $e")
            CompletionResult(content = null, toolCalls = emptyList(), error = e.message ?: "Request error")
        } finally {
            activeCall = null
        }
    }

    fun chat(messages: List<ChatMessage>, stream: Boolean, listener: Listener) {
        if (config.apiUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            UUtils.getHandler().post {
                listener.onError(UUtils.getString(com.termux.R.string.zt_ai_agent_not_configured))
                listener.onComplete("")
            }
            return
        }

        try {
            val bodyJson = buildRequestBody(messages, stream, tools = null)
            val body = bodyJson.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(config.apiUrl.trim())
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            activeCall = client.newCall(request)
            activeCall!!.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (call.isCanceled()) return
                    LogUtils.e(TAG, "chat onFailure: $e")
                    postError(listener, e.message ?: "Network error")
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (call.isCanceled()) {
                        response.close()
                        return
                    }
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            val errBody = resp.body?.string().orEmpty()
                            postError(listener, "HTTP ${resp.code}: $errBody")
                            return
                        }
                        val source: BufferedSource = resp.body?.source()
                            ?: run {
                                postError(listener, "Empty response")
                                return
                            }
                        val full = StringBuilder()
                        try {
                            var line: String?
                            while (source.readUtf8Line().also { line = it } != null) {
                                val chunk = parseStreamLine(line!!) ?: continue
                                if (chunk == STREAM_DONE) break
                                val content = extractStreamContent(chunk) ?: continue
                                full.append(content)
                                val text = full.toString()
                                UUtils.getHandler().post { listener.onChunk(text) }
                            }
                            val result = full.toString()
                            UUtils.getHandler().post { listener.onComplete(result) }
                        } catch (e: Exception) {
                            LogUtils.e(TAG, "chat parse error: $e")
                            postError(listener, e.message ?: "Parse error")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "chat error: $e")
            postError(listener, e.message ?: "Request error")
        }
    }

    private fun postError(listener: Listener, message: String) {
        UUtils.getHandler().post {
            listener.onError(message)
            listener.onComplete("")
        }
    }

    private fun buildRequestBody(
        messages: List<ChatMessage>,
        stream: Boolean,
        tools: JSONArray?
    ): String {
        val arr = JSONArray()
        messages.forEach { msg -> arr.put(messageToJson(msg)) }
        val body = JSONObject()
            .put("model", config.model.trim())
            .put("messages", arr)
            .put("stream", stream)
        if (tools != null && tools.length() > 0) {
            body.put("tools", tools)
            body.put("tool_choice", "auto")
        }
        return body.toString()
    }

    private fun messageToJson(msg: ChatMessage): JSONObject {
        val obj = JSONObject().put("role", msg.role)
        when (msg.role) {
            ROLE_TOOL -> obj.put("content", msg.content ?: "")
            ROLE_ASSISTANT -> {
                if (msg.content != null) {
                    obj.put("content", msg.content)
                } else {
                    obj.put("content", JSONObject.NULL)
                }
            }
            else -> obj.put("content", msg.content ?: "")
        }
        if (!msg.toolCalls.isNullOrEmpty()) {
            val toolArr = JSONArray()
            msg.toolCalls.forEach { tool ->
                toolArr.put(
                    JSONObject()
                        .put("id", tool.id)
                        .put("type", "function")
                        .put(
                            "function",
                            JSONObject()
                                .put("name", tool.name)
                                .put("arguments", tool.arguments)
                        )
                )
            }
            obj.put("tool_calls", toolArr)
        }
        if (!msg.toolCallId.isNullOrBlank()) {
            obj.put("tool_call_id", msg.toolCallId)
        }
        return obj
    }

    private fun parseCompletionResponse(responseBody: String): CompletionResult {
        return try {
            val message = JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
            val content = if (message.isNull("content")) {
                null
            } else {
                message.optString("content", null)
            }
            val toolCalls = parseToolCalls(message.optJSONArray("tool_calls"))
            CompletionResult(content = content, toolCalls = toolCalls, error = null)
        } catch (e: Exception) {
            CompletionResult(
                content = null,
                toolCalls = emptyList(),
                error = "Parse error: ${e.message}"
            )
        }
    }

    private fun parseToolCalls(array: JSONArray?): List<ToolCall> {
        if (array == null || array.length() == 0) return emptyList()
        val list = mutableListOf<ToolCall>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val function = item.getJSONObject("function")
            list.add(
                ToolCall(
                    id = item.getString("id"),
                    name = function.getString("name"),
                    arguments = function.optString("arguments", "{}")
                )
            )
        }
        return list
    }

    private fun parseStreamLine(line: String): String? {
        val clean = line.removePrefix("data: ").trim()
        if (clean.isEmpty()) return null
        if (clean == STREAM_DONE) return STREAM_DONE
        if (clean.first() != '{') return null
        return clean
    }

    private fun extractStreamContent(json: String): String? {
        return try {
            val choices = JSONObject(json).getJSONArray("choices")
            if (choices.length() == 0) return null
            val delta = choices.getJSONObject(0).getJSONObject("delta")
            if (!delta.has("content")) return null
            delta.getString("content")
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "ZtAgentAiChatClient"
        private const val STREAM_DONE = "[DONE]"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_TOOL = "tool"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
