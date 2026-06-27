package com.termux.zerocore.ai.config

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/** 经本地 Socket 直接执行 zt 命令（不依赖终端）。 */
object ZtAiZtSocketClient {

    private const val HOST = "127.0.0.1"
    private const val PORT = 19951
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 30_000

    fun send(command: String): String {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return errorJson("command is empty")
        }
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(trimmed)
                val response = reader.readLine().orEmpty()
                if (response.isBlank()) {
                    errorJson("empty response from zt socket (service may be starting)")
                } else {
                    response
                }
            }
        } catch (e: Exception) {
            errorJson(e.message ?: "zt socket failed")
        }
    }

    private fun errorJson(message: String): String {
        return """{"code":1,"msg":${org.json.JSONObject.quote(message)}}"""
    }
}
