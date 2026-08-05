package com.termux.zerocore.editor.lsp

import android.content.Context
import android.os.Process
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EditorLspClient(
    private val context: Context,
    private val launchSpec: EditorLspLaunchSpec,
    private val rootDirectory: File?,
    /** 普通 RPC（补全等）超时；不宜过长，否则补全面板进度条会一直转。 */
    private val requestTimeoutMillis: Long,
    private val onError: (String) -> Unit,
    private val environmentExtras: Map<String, String> = emptyMap(),
    private val initializationOptions: JSONObject? = null,
    private val onServerNotification: ((method: String, params: Any?) -> Unit)? = null,
    /** initialize 可单独加长（jdt-ls 冷启动很慢）。 */
    private val initTimeoutMillis: Long = requestTimeoutMillis
) {
    private data class PendingRequest(
        val latch: CountDownLatch = CountDownLatch(1),
        @Volatile var result: Any? = null,
        @Volatile var error: JSONObject? = null
    )

    private val nextRequestId = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, PendingRequest>()
    private val writeLock = Any()
    private var process: java.lang.Process? = null
    private var readerThread: Thread? = null
    private var stderrThread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var initialized = false

    fun start(): Boolean {
        if (running && initialized) return true
        return try {
            val command = ArrayList<String>(1 + launchSpec.arguments.size)
            command.add(launchSpec.executable)
            command.addAll(launchSpec.arguments)
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(rootDirectory?.takeIf { it.isDirectory } ?: TermuxConstants.TERMUX_HOME_DIR)
            processBuilder.environment().putAll(TermuxShellEnvironment().getEnvironment(context, false))
            processBuilder.environment()["PATH"] = buildPath(processBuilder.environment()["PATH"])
            environmentExtras.forEach { (key, value) ->
                processBuilder.environment()[key] = value
            }
            // 避免系统/VPN 注入的代理让 jdt-ls / Maven 相关逻辑挂起
            stripProxyEnvironment(processBuilder.environment())
            processBuilder.environment()["TMPDIR"] =
                processBuilder.environment()["TMPDIR"] ?: TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH
            process = processBuilder.start()
            running = true
            startReaderThread()
            startStderrThread()
            initialized = initializeServer()
            if (!initialized) {
                EditorLspDebugStore.recordEvent("fatal", "LSP initialize failed: ${launchSpec.executable}")
                onError("LSP initialize failed")
                shutdown()
            } else {
                EditorLspDebugStore.recordEvent("info", "LSP initialized: ${launchSpec.executable}")
            }
            initialized
        } catch (e: Exception) {
            val msg = e.message
                ?: "LSP server start failed: ${launchSpec.executable} ${launchSpec.arguments.joinToString(" ")}"
            EditorLspDebugStore.recordEvent("fatal", msg)
            onError(msg)
            shutdown()
            false
        }
    }

    fun isRunning(): Boolean {
        return ensureRunning()
    }

    fun didOpen(uri: String, languageId: String, text: String, version: Int) {
        if (!ensureRunning()) return
        notify(
            "textDocument/didOpen",
            JSONObject().put(
                "textDocument",
                JSONObject()
                    .put("uri", uri)
                    .put("languageId", languageId)
                    .put("version", version)
                    .put("text", text)
            )
        )
    }

    fun didChange(uri: String, text: String, version: Int) {
        if (!ensureRunning()) return
        notify(
            "textDocument/didChange",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri).put("version", version))
                .put("contentChanges", org.json.JSONArray().put(JSONObject().put("text", text)))
        )
    }

    fun didClose(uri: String) {
        if (!ensureRunning()) return
        notify(
            "textDocument/didClose",
            JSONObject().put("textDocument", JSONObject().put("uri", uri))
        )
    }

    /**
     * @param triggerKind 1=Invoked, 2=TriggerCharacter, 3=TriggerForIncompleteCompletions
     * @param triggerCharacter 如 "."；（仅 triggerKind=2 时有效）
     */
    fun completion(
        uri: String,
        line: Int,
        column: Int,
        triggerKind: Int = 1,
        triggerCharacter: String? = null
    ): Any? {
        if (!ensureRunning()) return null
        val context = JSONObject().put("triggerKind", triggerKind)
        if (triggerKind == 2 && !triggerCharacter.isNullOrEmpty()) {
            context.put("triggerCharacter", triggerCharacter)
        }
        return request(
            "textDocument/completion",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column))
                .put("context", context)
        )
    }

    /** 解析补全项（jdt-ls 在此填充 import 的 additionalTextEdits）。 */
    fun resolveCompletionItem(item: JSONObject): JSONObject? {
        if (!ensureRunning()) return null
        val result = request("completionItem/resolve", item) ?: return null
        return result as? JSONObject
    }

    fun hover(uri: String, line: Int, column: Int): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/hover",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column))
        )
    }

    fun definition(uri: String, line: Int, column: Int, timeout: Long = requestTimeoutMillis): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/definition",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column)),
            timeout = timeout
        )
    }

    fun typeDefinition(uri: String, line: Int, column: Int, timeout: Long = requestTimeoutMillis): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/typeDefinition",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column)),
            timeout = timeout
        )
    }

    fun declaration(uri: String, line: Int, column: Int, timeout: Long = requestTimeoutMillis): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/declaration",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column)),
            timeout = timeout
        )
    }

    /**
     * jdt-ls 扩展：根据 jdt:// 或 *.class URI 返回附着源码 / 反编译文本。
     * @see <a href="https://github.com/eclipse-jdtls/eclipse.jdt.ls">eclipse.jdt.ls</a>
     */
    fun classFileContents(uri: String): String? {
        if (!ensureRunning()) return null
        val result = request(
            "java/classFileContents",
            JSONObject().put("uri", uri),
            timeout = EditorJdtLsSupport.NAVIGATION_TIMEOUT_MILLIS
        ) ?: return null
        return when (result) {
            is String -> result
            is JSONObject -> result.optString("content").ifBlank {
                result.optString("value")
            }.takeIf { it.isNotBlank() }
            else -> result.toString().takeIf { it.isNotBlank() && it != "null" }
        }
    }

    fun references(uri: String, line: Int, column: Int, timeout: Long = requestTimeoutMillis): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/references",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column))
                .put("context", JSONObject().put("includeDeclaration", true)),
            timeout = timeout
        )
    }

    fun formatting(uri: String, tabSize: Int, insertSpaces: Boolean): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/formatting",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put(
                    "options",
                    JSONObject()
                        .put("tabSize", tabSize.coerceIn(1, 16))
                        .put("insertSpaces", insertSpaces)
                ),
            timeout = 30_000L
        )
    }

    fun codeAction(
        uri: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        diagnostics: org.json.JSONArray,
        only: List<String>? = null
    ): Any? {
        if (!ensureRunning()) return null
        val context = JSONObject()
            .put("diagnostics", diagnostics)
            .put("triggerKind", 1)
        if (!only.isNullOrEmpty()) {
            context.put("only", org.json.JSONArray(only))
        }
        return request(
            "textDocument/codeAction",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put(
                    "range",
                    JSONObject()
                        .put("start", JSONObject().put("line", startLine).put("character", startColumn))
                        .put("end", JSONObject().put("line", endLine).put("character", endColumn))
                )
                .put("context", context)
        )
    }

    fun executeCommand(command: String, arguments: org.json.JSONArray?): Any? {
        if (!ensureRunning()) return null
        val params = JSONObject().put("command", command)
        if (arguments != null) params.put("arguments", arguments)
        return request("workspace/executeCommand", params)
    }

    fun didSave(uri: String, text: String?) {
        if (!ensureRunning()) return
        val params = JSONObject().put("textDocument", JSONObject().put("uri", uri))
        if (text != null) params.put("text", text)
        notify("textDocument/didSave", params)
    }

    fun shutdown() {
        try {
            if (isProcessAlive()) {
                request("shutdown", null, 500)
                notify("exit", null)
            }
        } catch (_: Exception) {
        }
        running = false
        initialized = false
        failPendingRequests()
        try {
            process?.destroy()
        } catch (_: Exception) {
        }
        process = null
        readerThread = null
        stderrThread = null
    }

    private fun ensureRunning(): Boolean {
        if (!running || !isProcessAlive()) {
            running = false
            initialized = false
            return false
        }
        return initialized
    }

    private fun isProcessAlive(): Boolean {
        val currentProcess = process ?: return false
        return try {
            currentProcess.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    private fun initializeServer(): Boolean {
        val textDocument = JSONObject()
            .put(
                "completion",
                JSONObject()
                    .put("contextSupport", true)
                    .put(
                        "completionItem",
                        JSONObject()
                            // 保持 false：snippetSupport=true 时 jdt-ls 补全易变慢/异常，面板常空白。
                            // new HashMap 的 ()/; 由 EditorJavaCompletionEnrichment 本地补齐。
                            .put("snippetSupport", false)
                            .put("documentationFormat", org.json.JSONArray().put("markdown").put("plaintext"))
                            // jdt-ls 把 import 等放在 resolve 的 additionalTextEdits
                            .put(
                                "resolveSupport",
                                JSONObject().put(
                                    "properties",
                                    org.json.JSONArray()
                                        .put("documentation")
                                        .put("detail")
                                        .put("additionalTextEdits")
                                )
                            )
                    )
            )
            .put(
                "hover",
                JSONObject().put(
                    "contentFormat",
                    org.json.JSONArray().put("markdown").put("plaintext")
                )
            )
            .put("definition", JSONObject().put("linkSupport", true))
            .put("declaration", JSONObject().put("linkSupport", true))
            .put("typeDefinition", JSONObject().put("linkSupport", true))
            .put("references", JSONObject())
            .put("formatting", JSONObject().put("dynamicRegistration", false))
            .put("rangeFormatting", JSONObject().put("dynamicRegistration", false))
            .put(
                "codeAction",
                JSONObject()
                    .put("codeActionLiteralSupport", JSONObject().put(
                        "codeActionKind",
                        JSONObject().put(
                            "valueSet",
                            org.json.JSONArray()
                                .put("quickfix")
                                .put("quickfix.import")
                                .put("source")
                                .put("source.organizeImports")
                        )
                    ))
                    .put("resolveSupport", JSONObject().put(
                        "properties",
                        org.json.JSONArray().put("edit")
                    ))
            )
            .put(
                "publishDiagnostics",
                JSONObject()
                    .put("relatedInformation", false)
                    .put("versionSupport", false)
                    .put("tagSupport", JSONObject().put("valueSet", org.json.JSONArray().put(1).put(2)))
            )
            .put(
                "synchronization",
                JSONObject()
                    .put("dynamicRegistration", false)
                    .put("willSave", false)
                    .put("willSaveWaitUntil", false)
                    .put("didSave", true)
            )
        val capabilities = JSONObject()
            .put("textDocument", textDocument)
            .put(
                "workspace",
                JSONObject()
                    .put("workspaceFolders", true)
                    .put("configuration", true)
                    .put("didChangeConfiguration", JSONObject().put("dynamicRegistration", false))
                    .put(
                        "applyEdit",
                        true
                    )
                    .put(
                        "workspaceEdit",
                        JSONObject()
                            .put("documentChanges", true)
                    )
            )
        val safeRoot = rootDirectory?.takeIf {
            it.isDirectory && it.absolutePath != "/" && it.canRead()
        } ?: TermuxConstants.TERMUX_HOME_DIR
        val rootUri = EditorLspUris.forFile(safeRoot)
        val params = JSONObject()
            .put("processId", Process.myPid())
            .put("rootUri", rootUri)
            .put("rootPath", safeRoot.absolutePath)
            .put(
                "workspaceFolders",
                org.json.JSONArray().put(
                    JSONObject().put("uri", rootUri).put("name", safeRoot.name.ifEmpty { "workspace" })
                )
            )
            .put("capabilities", capabilities)
            .put("clientInfo", JSONObject().put("name", "ZeroTermux Editor"))
        initializationOptions?.let { params.put("initializationOptions", it) }
        val response = request("initialize", params, initTimeoutMillis) ?: return false
        notify("initialized", JSONObject())
        // Pyright 等依赖 workspace 配置；initialize 后主动推送一次
        initializationOptions?.let { opts ->
            if (opts.has("python") || opts.has("settings")) {
                val settings = opts.optJSONObject("settings") ?: opts
                notify("workspace/didChangeConfiguration", JSONObject().put("settings", settings))
            }
        }
        return response is JSONObject
    }

    private fun request(method: String, params: Any?, timeout: Long = requestTimeoutMillis): Any? {
        val id = nextRequestId.getAndIncrement()
        val pendingRequest = PendingRequest()
        pendingRequests[id] = pendingRequest
        val message = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
        if (params != null) message.put("params", params)
        return try {
            writeMessage(message)
            if (!pendingRequest.latch.await(timeout, TimeUnit.MILLISECONDS)) {
                pendingRequests.remove(id)
                EditorLspDebugStore.recordEvent("error", "LSP request timeout: $method")
                null
            } else {
                pendingRequest.error?.let { err ->
                    EditorLspDebugStore.recordEvent(
                        "rpc_error",
                        "$method: ${err.optString("message", err.toString())}"
                    )
                }
                pendingRequest.result
            }
        } catch (e: Exception) {
            pendingRequests.remove(id)
            EditorLspDebugStore.recordEvent("error", e.message ?: "LSP request failed: $method")
            null
        }
    }

    private fun notify(method: String, params: Any?) {
        val message = JSONObject()
            .put("jsonrpc", "2.0")
            .put("method", method)
        if (params != null) message.put("params", params)
        try {
            writeMessage(message)
        } catch (e: Exception) {
            EditorLspDebugStore.recordEvent("error", e.message ?: "LSP notify failed: $method")
        }
    }

    private fun writeMessage(message: JSONObject) {
        val outputStream = process?.outputStream ?: return
        val body = message.toString().toByteArray(Charsets.UTF_8)
        val header = "Content-Length: ${body.size}\r\n\r\n".toByteArray(Charsets.US_ASCII)
        synchronized(writeLock) {
            outputStream.write(header)
            outputStream.write(body)
            outputStream.flush()
        }
    }

    private fun startReaderThread() {
        val inputStream = process?.inputStream ?: return
        readerThread = Thread {
            val bufferedInputStream = BufferedInputStream(inputStream)
            while (running) {
                try {
                    val message = readMessage(bufferedInputStream) ?: break
                    handleMessage(message)
                } catch (e: Exception) {
                    if (running) {
                        EditorLspDebugStore.recordEvent("fatal", e.message ?: "LSP read failed")
                        onError("LSP connection lost")
                    }
                    break
                }
            }
            running = false
            initialized = false
            failPendingRequests()
        }.apply {
            name = "ZT-LSP-Reader"
            isDaemon = true
            start()
        }
    }

    private fun startStderrThread() {
        val errorStream = process?.errorStream ?: return
        stderrThread = Thread {
            try {
                BufferedReader(InputStreamReader(errorStream)).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank() || shouldIgnoreStderrLine(line)) return@forEach
                        // jdt-ls / Eclipse 日志会刷屏；只进调试缓冲，不 Toast
                        EditorLspDebugStore.appendStderr(line)
                    }
                }
            } catch (_: Exception) {
            }
        }.apply {
            name = "ZT-LSP-Stderr"
            isDaemon = true
            start()
        }
    }

    private fun handleMessage(message: JSONObject) {
        val method = message.optString("method", "")
        val id = message.opt("id")
        // 服务端通知（无 id）：如 textDocument/publishDiagnostics
        if (method.isNotEmpty() && (id == null || id == JSONObject.NULL)) {
            try {
                onServerNotification?.invoke(method, message.opt("params"))
            } catch (_: Exception) {
            }
            return
        }
        if (id is Number) {
            val pendingRequest = pendingRequests.remove(id.toInt())
            if (pendingRequest != null) {
                pendingRequest.error = message.optJSONObject("error")
                pendingRequest.result = if (message.has("result")) message.opt("result") else null
                pendingRequest.latch.countDown()
            } else if (method.isNotEmpty()) {
                respondToServerRequest(id.toInt(), method, message.opt("params"))
            }
        }
    }

    private fun respondToServerRequest(id: Int, method: String, params: Any?) {
        try {
            val result: Any? = when (method) {
                "workspace/configuration" -> resolveWorkspaceConfiguration(params)
                "workspace/applyEdit" -> {
                    val edit = (params as? JSONObject)?.optJSONObject("edit")
                    val applied = if (edit != null) {
                        EditorLspManager.activeInstance?.handleServerApplyEdit(edit) == true
                    } else {
                        false
                    }
                    JSONObject().put("applied", applied)
                }
                "workspace/workspaceFolders" -> {
                    val safeRoot = rootDirectory?.takeIf {
                        it.isDirectory && it.absolutePath != "/" && it.canRead()
                    } ?: TermuxConstants.TERMUX_HOME_DIR
                    val rootUri = EditorLspUris.forFile(safeRoot)
                    org.json.JSONArray().put(
                        JSONObject().put("uri", rootUri).put("name", safeRoot.name.ifEmpty { "workspace" })
                    )
                }
                "client/registerCapability",
                "client/unregisterCapability",
                "window/workDoneProgress/create" -> JSONObject()
                else -> JSONObject.NULL
            }
            writeMessage(
                JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("result", result ?: JSONObject.NULL)
            )
        } catch (_: Exception) {
        }
    }

    private fun resolveWorkspaceConfiguration(params: Any?): org.json.JSONArray {
        val items = (params as? JSONObject)?.optJSONArray("items") ?: org.json.JSONArray()
        val result = org.json.JSONArray()
        for (i in 0 until items.length()) {
            val section = items.optJSONObject(i)?.optString("section").orEmpty()
            result.put(
                when {
                    section.startsWith("python") ||
                        (section.isEmpty() && initializationOptions?.has("python") == true) ->
                        EditorPyrightSupport.configurationForSection(section.ifEmpty { "python" })
                    // 之前对 java.* 一律返回 null，jdt-ls 拿不到 JDK runtime，成员方法补全会空
                    section.startsWith("java") ||
                        (section.isEmpty() && initializationOptions?.optJSONObject("settings")?.has("java") == true) ->
                        EditorLspCommandResolver.javaConfigurationForSection(
                            if (section.isEmpty()) "java" else section
                        )
                    else -> JSONObject.NULL
                }
            )
        }
        return result
    }

    private fun readMessage(inputStream: InputStream): JSONObject? {
        var contentLength = -1
        while (true) {
            val line = readHeaderLine(inputStream) ?: return null
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0 && line.substring(0, separator).trim().equals("Content-Length", ignoreCase = true)) {
                contentLength = line.substring(separator + 1).trim().toIntOrNull() ?: -1
            }
        }
        if (contentLength <= 0) return null
        val body = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val read = inputStream.read(body, offset, contentLength - offset)
            if (read < 0) return null
            offset += read
        }
        return JSONObject(String(body, Charset.forName("UTF-8")))
    }

    private fun readHeaderLine(inputStream: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val value = inputStream.read()
            if (value < 0) return if (buffer.size() == 0) null else buffer.toString("UTF-8")
            if (value == '\n'.code) break
            if (value != '\r'.code) buffer.write(value)
        }
        return buffer.toString("UTF-8")
    }

    private fun failPendingRequests() {
        pendingRequests.values.forEach { it.latch.countDown() }
        pendingRequests.clear()
    }

    private fun buildPath(existingPath: String?): String {
        return EditorLspCommandResolver.buildPath(existingPath)
    }

    private fun stripProxyEnvironment(env: MutableMap<String, String>) {
        listOf(
            "http_proxy", "https_proxy", "all_proxy", "ftp_proxy", "no_proxy",
            "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "FTP_PROXY", "NO_PROXY"
        ).forEach { env.remove(it) }
        env["NO_PROXY"] = "*"
        env["no_proxy"] = "*"
    }

    private fun shouldIgnoreStderrLine(line: String): Boolean {
        val normalized = line.lowercase()
        return normalized.contains("shellcheck") ||
            normalized.contains("shfmt") ||
            normalized.contains("explainshell")
    }
}
