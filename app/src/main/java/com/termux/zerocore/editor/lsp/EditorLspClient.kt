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
    private val timeoutMillis: Long,
    private val onError: (String) -> Unit,
    private val environmentExtras: Map<String, String> = emptyMap(),
    private val initializationOptions: JSONObject? = null,
    private val onServerNotification: ((method: String, params: Any?) -> Unit)? = null
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

    fun completion(uri: String, line: Int, column: Int): Any? {
        if (!ensureRunning()) return null
        return request(
            "textDocument/completion",
            JSONObject()
                .put("textDocument", JSONObject().put("uri", uri))
                .put("position", JSONObject().put("line", line).put("character", column))
                .put("context", JSONObject().put("triggerKind", 1))
        )
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
                JSONObject().put(
                    "completionItem",
                    JSONObject()
                        .put("snippetSupport", false)
                        .put("documentationFormat", org.json.JSONArray().put("markdown").put("plaintext"))
                )
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
                    .put("didSave", false)
            )
        val capabilities = JSONObject()
            .put("textDocument", textDocument)
            .put("workspace", JSONObject().put("workspaceFolders", true))
        val safeRoot = rootDirectory?.takeIf {
            it.isDirectory && it.absolutePath != "/" && it.canRead()
        } ?: TermuxConstants.TERMUX_HOME_DIR
        val rootUri = safeRoot.toURI().toString()
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
        val response = request("initialize", params) ?: return false
        notify("initialized", JSONObject())
        return response is JSONObject
    }

    private fun request(method: String, params: Any?, timeout: Long = timeoutMillis): Any? {
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
                respondToServerRequest(id.toInt())
            }
        }
    }

    private fun respondToServerRequest(id: Int) {
        try {
            writeMessage(
                JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("result", JSONObject.NULL)
            )
        } catch (_: Exception) {
        }
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

    private fun shouldIgnoreStderrLine(line: String): Boolean {
        val normalized = line.lowercase()
        return normalized.contains("shellcheck") ||
            normalized.contains("shfmt") ||
            normalized.contains("explainshell")
    }
}
