package com.termux.zerocore.editor.lsp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class EditorLspManager(private val context: Context) {
    fun interface DiagnosticsListener {
        fun onDiagnosticsChanged(uri: String, diagnostics: DiagnosticsContainer?)
    }

    init {
        activeInstance = this
    }

    data class Settings(
        val enabled: Boolean,
        val timeoutMillis: Long
    )

    data class CompletionCandidate(
        val label: String,
        val detail: String?,
        val insertText: String,
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int,
        val sortText: String?,
        val filterText: String?,
        val prefixLength: Int
    )

    private data class OpenDocument(
        val uri: String,
        val languageId: String,
        var version: Int,
        var text: String
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lspInstaller = EditorLspInstaller(context.applicationContext)
    private val clients = ConcurrentHashMap<String, EditorLspClient>()
    private val openDocuments = ConcurrentHashMap<String, OpenDocument>()
    private val diagnosticsByUri = ConcurrentHashMap<String, DiagnosticsContainer>()
    private val failedLanguages = LinkedHashSet<String>()
    private val suppressedErrors = LinkedHashSet<String>()
    private val diagnosticIdSeq = AtomicLong(1)

    @Volatile
    private var diagnosticsListener: DiagnosticsListener? = null

    @Volatile
    private var settings = Settings(true, DEFAULT_TIMEOUT_MILLIS)

    fun setDiagnosticsListener(listener: DiagnosticsListener?) {
        diagnosticsListener = listener
    }

    fun diagnosticsFor(file: File): DiagnosticsContainer? {
        val uri = EditorLspUris.forFile(file)
        diagnosticsByUri[uri]?.let { return it }
        return diagnosticsByUri.entries.firstOrNull { EditorLspUris.same(it.key, uri) }?.value
    }

    fun debugStatus(): Map<String, Any?> {
        return mapOf(
            "active" to true,
            "enabled" to settings.enabled,
            "timeout_ms" to settings.timeoutMillis,
            "open_documents" to openDocuments.keys.toList(),
            "clients" to clients.mapValues { (_, client) -> client.isRunning() },
            "diagnostics_uris" to diagnosticsByUri.keys.toList(),
            "failed_languages" to synchronized(failedLanguages) { failedLanguages.toList() }
        )
    }

    fun releaseActiveIfMine() {
        if (activeInstance === this) {
            activeInstance = null
        }
    }

    fun updateSettings(newSettings: Settings) {
        val shouldRestart = settings.timeoutMillis != newSettings.timeoutMillis
        settings = newSettings
        if (!newSettings.enabled || shouldRestart) {
            closeAll()
            synchronized(failedLanguages) {
                failedLanguages.clear()
            }
        }
    }

    fun isEnabledFor(languageId: String): Boolean {
        val currentSettings = settings
        return currentSettings.enabled && lspInstaller.isLanguageInstalled(languageId)
    }

    fun isLanguageInstalled(languageId: String): Boolean {
        return lspInstaller.isLanguageInstalled(languageId)
    }

    fun openDocument(file: File, languageId: String, text: String) {
        if (!canUseLsp(languageId, text)) return
        val uri = EditorLspUris.forFile(file)
        synchronized(this) {
            val opened = openDocuments[uri]
            if (opened != null) {
                if (opened.languageId == languageId) {
                    changeDocumentLocked(file, languageId, text, true)
                } else {
                    clients[opened.languageId]?.didClose(uri)
                    openDocuments.remove(uri)
                    openDocumentLocked(file, languageId, text)
                }
            } else {
                openDocumentLocked(file, languageId, text)
            }
        }
    }

    fun changeDocument(file: File, languageId: String, text: String) {
        if (!canUseLsp(languageId, text)) return
        synchronized(this) {
            changeDocumentLocked(file, languageId, text, false)
        }
    }

    fun closeDocument(file: File) {
        val uri = EditorLspUris.forFile(file)
        synchronized(this) {
            val opened = openDocuments.remove(uri) ?: return
            clients[opened.languageId]?.didClose(uri)
            clearDiagnostics(uri)
        }
    }

    fun completion(file: File, languageId: String, content: ContentReference, position: CharPosition): List<CompletionCandidate> {
        val text = contentToString(content)
        if (!canUseLsp(languageId, text)) return emptyList()
        val uri = EditorLspUris.forFile(file)
        synchronized(this) {
            changeDocumentLocked(file, languageId, text, true)
        }
        val result = clientFor(file, languageId)?.completion(uri, position.line, position.column) ?: return emptyList()
        return parseCompletionResult(result, content, position).take(MAX_COMPLETION_ITEMS)
    }

    fun closeAll() {
        val uris: List<String>
        synchronized(this) {
            uris = diagnosticsByUri.keys.toList()
            openDocuments.clear()
            diagnosticsByUri.clear()
            EditorLspDebugStore.clearAllDiagnostics()
            clients.values.forEach { it.shutdown() }
            clients.clear()
        }
        uris.forEach { uri ->
            mainHandler.post { diagnosticsListener?.onDiagnosticsChanged(uri, null) }
        }
    }

    fun ensureBasicShellInstalled(onFinished: ((Boolean) -> Unit)? = null) {
        lspInstaller.ensureBasicShellInstalled(onFinished)
    }

    fun ensureJavaJdtLsInstalled(onFinished: ((Boolean) -> Unit)? = null) {
        lspInstaller.installPackage(EditorJdtLsSupport.PACKAGE_ID, quietIfInstalled = true) { success, _ ->
            if (success) {
                synchronized(failedLanguages) {
                    failedLanguages.remove(LANGUAGE_JAVA)
                }
            }
            onFinished?.invoke(success)
        }
    }

    fun availablePackages(): List<EditorLspInstaller.ServerPackage> {
        return lspInstaller.availablePackages()
    }

    fun installPackage(
        packageId: String,
        quietIfInstalled: Boolean = false,
        onFinished: ((Boolean, String) -> Unit)? = null
    ) {
        lspInstaller.installPackage(packageId, quietIfInstalled, onFinished)
    }

    fun isPackageInstalled(packageId: String): Boolean {
        return lspInstaller.isPackageInstalled(packageId)
    }

    fun isPackageInstalling(packageId: String): Boolean {
        return lspInstaller.isInstalling(packageId)
    }

    fun isNpmInstalled(): Boolean {
        return lspInstaller.isNpmInstalled()
    }

    private fun openDocumentLocked(file: File, languageId: String, text: String) {
        val client = clientFor(file, languageId) ?: return
        val uri = EditorLspUris.forFile(file)
        val version = 1
        client.didOpen(uri, languageId, text, version)
        openDocuments[uri] = OpenDocument(uri, languageId, version, text)
    }

    private fun changeDocumentLocked(file: File, languageId: String, text: String, openIfMissing: Boolean) {
        val uri = EditorLspUris.forFile(file)
        val opened = openDocuments[uri]
        if (opened == null) {
            if (openIfMissing) openDocumentLocked(file, languageId, text)
            return
        }
        if (opened.text == text) return
        val client = clientFor(file, languageId) ?: return
        opened.version++
        opened.text = text
        client.didChange(uri, text, opened.version)
    }

    private fun clientFor(file: File, languageId: String): EditorLspClient? {
        if (!settings.enabled) return null
        if (!lspInstaller.isLanguageInstalled(languageId)) {
            when (languageId) {
                LANGUAGE_SHELL -> lspInstaller.ensureBasicShellInstalled()
                LANGUAGE_JAVA -> ensureJavaJdtLsInstalled()
            }
            showErrorOnce(
                if (languageId == LANGUAGE_JAVA) {
                    "Java LSP (jdt-ls) 未安装，正在尝试安装；也可在编辑器设置 → LSP 中手动安装"
                } else {
                    "LSP 服务器未安装，请先在设置中安装对应语言包"
                }
            )
            return null
        }
        val projectRoot = if (languageId == LANGUAGE_JAVA) {
            EditorJdtLsSupport.findProjectRoot(file)
        } else {
            file.parentFile
        }
        val launchSpec = lspInstaller.launchSpecForLanguage(languageId, projectRoot)
        if (launchSpec == null) {
            showErrorOnce(
                if (languageId == LANGUAGE_JAVA) {
                    "jdt-ls 启动失败：请确认已安装 openjdk-21，并重新安装 Java LSP"
                } else {
                    "LSP 服务器命令未找到，请重新安装对应语言包"
                }
            )
            return null
        }
        synchronized(failedLanguages) {
            if (failedLanguages.contains(languageId)) return null
        }
        clients[languageId]?.let { client ->
            if (client.isRunning()) return client
            clients.remove(languageId)
            val staleUris = openDocuments.filterValues { it.languageId == languageId }.keys.toList()
            staleUris.forEach { uri ->
                openDocuments.remove(uri)
                clearDiagnostics(uri)
            }
        }
        val timeout = if (languageId == LANGUAGE_JAVA) {
            maxOf(settings.timeoutMillis, EditorJdtLsSupport.INIT_TIMEOUT_MILLIS)
        } else {
            settings.timeoutMillis
        }
        val client = EditorLspClient(
            context.applicationContext,
            launchSpec,
            projectRoot,
            timeout,
            ::showErrorOnce,
            EditorLspCommandResolver.environmentForLanguage(languageId),
            EditorLspCommandResolver.initializationOptionsForLanguage(languageId),
            ::handleServerNotification
        )
        return if (client.start()) {
            clients[languageId] = client
            client
        } else {
            synchronized(failedLanguages) {
                failedLanguages.add(languageId)
            }
            null
        }
    }

    private fun handleServerNotification(method: String, params: Any?) {
        if (method != "textDocument/publishDiagnostics") return
        val payload = params as? JSONObject ?: return
        val rawUri = payload.optString("uri")
        if (rawUri.isBlank()) return
        val uri = EditorLspUris.normalize(rawUri)
        val items = payload.optJSONArray("diagnostics") ?: JSONArray()
        val text = openDocuments[uri]?.text
            ?: openDocuments.entries.firstOrNull { EditorLspUris.same(it.key, uri) }?.value?.text
            ?: ""
        val summary = buildDiagnosticSummary(items)
        EditorLspDebugStore.setDiagnosticsSnapshot(uri, summary)
        EditorLspDebugStore.recordEvent(
            "diagnostics",
            "publishDiagnostics count=${items.length()} textLen=${text.length}",
            mapOf("uri" to uri, "count" to items.length(), "text_len" to text.length)
        )
        val container = buildDiagnosticsContainer(text, items)
        if (container == null) {
            diagnosticsByUri.remove(uri)
        } else {
            diagnosticsByUri[uri] = container
        }
        mainHandler.post {
            diagnosticsListener?.onDiagnosticsChanged(uri, container)
        }
    }

    private fun clearDiagnostics(uri: String) {
        diagnosticsByUri.remove(uri)
        EditorLspDebugStore.clearDiagnostics(uri)
        mainHandler.post {
            diagnosticsListener?.onDiagnosticsChanged(uri, null)
        }
    }

    private fun buildDiagnosticSummary(items: JSONArray): List<Map<String, Any?>> {
        val out = ArrayList<Map<String, Any?>>(items.length())
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val range = item.optJSONObject("range")
            val start = range?.optJSONObject("start")
            out.add(
                mapOf(
                    "severity" to item.optInt("severity", 1),
                    "message" to item.optString("message"),
                    "source" to item.optString("source"),
                    "line" to (start?.optInt("line") ?: -1),
                    "character" to (start?.optInt("character") ?: -1)
                )
            )
        }
        return out
    }

    private fun buildDiagnosticsContainer(text: String, items: JSONArray): DiagnosticsContainer? {
        if (items.length() == 0) return null
        val container = DiagnosticsContainer(true)
        val regions = ArrayList<DiagnosticRegion>(items.length())
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val range = item.optJSONObject("range") ?: continue
            val start = range.optJSONObject("start") ?: continue
            val end = range.optJSONObject("end") ?: continue
            val startLine = start.optInt("line", 0)
            val startChar = start.optInt("character", 0)
            val endLine = end.optInt("line", startLine)
            val endChar = end.optInt("character", startChar)
            var startIndex = lspPositionToIndex(text, startLine, startChar)
            var endIndex = lspPositionToIndex(text, endLine, endChar)
            if (endIndex <= startIndex) {
                endIndex = (startIndex + 1).coerceAtMost(text.length.coerceAtLeast(1))
            }
            if (text.isNotEmpty() && startIndex >= text.length) {
                startIndex = text.length - 1
                endIndex = text.length
            }
            val severity = mapLspSeverity(item.optInt("severity", 1))
            val message = item.optString("message").trim().ifEmpty { "Issue" }
            val source = item.optString("source").trim()
            val brief = if (source.isNotEmpty()) "$source: $message" else message
            regions.add(
                DiagnosticRegion(
                    startIndex,
                    endIndex,
                    severity,
                    diagnosticIdSeq.getAndIncrement(),
                    DiagnosticDetail(brief, message)
                )
            )
        }
        if (regions.isEmpty()) return null
        container.addDiagnostics(regions)
        return container
    }

    /** LSP DiagnosticSeverity: 1 Error, 2 Warning, 3 Information, 4 Hint */
    private fun mapLspSeverity(severity: Int): Short {
        return when (severity) {
            1 -> DiagnosticRegion.SEVERITY_ERROR
            2 -> DiagnosticRegion.SEVERITY_WARNING
            3, 4 -> DiagnosticRegion.SEVERITY_TYPO
            else -> DiagnosticRegion.SEVERITY_WARNING
        }
    }

    private fun lspPositionToIndex(text: String, line: Int, character: Int): Int {
        if (text.isEmpty()) return 0
        var index = 0
        var currentLine = 0
        val targetLine = line.coerceAtLeast(0)
        while (currentLine < targetLine && index < text.length) {
            if (text[index] == '\n') {
                currentLine++
            }
            index++
        }
        val lineStart = index
        var column = 0
        val targetColumn = character.coerceAtLeast(0)
        while (column < targetColumn && index < text.length && text[index] != '\n') {
            index++
            column++
        }
        // 若跨行字符数异常，至少落在行首
        if (index < lineStart) return lineStart.coerceIn(0, text.length)
        return index.coerceIn(0, text.length)
    }

    private fun canUseLsp(languageId: String, text: String): Boolean {
        val currentSettings = settings
        return currentSettings.enabled &&
            lspInstaller.isLanguageInstalled(languageId) &&
            text.length <= MAX_LSP_TEXT_LENGTH
    }

    private fun parseCompletionResult(result: Any?, content: ContentReference, position: CharPosition): List<CompletionCandidate> {
        val items = when (result) {
            is JSONArray -> result
            is JSONObject -> result.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
        val lineText = runCatching { content.getLine(position.line) }.getOrDefault("")
        val prefixLength = computePrefixLength(lineText, position.column)
        val candidates = ArrayList<CompletionCandidate>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val label = item.optString("label").trim()
            if (label.isEmpty()) continue
            val textEdit = item.optJSONObject("textEdit")
            val editRange = textEdit?.optJSONObject("range")
            val rangeStart = editRange?.optJSONObject("start")
            val rangeEnd = editRange?.optJSONObject("end")
            val editText = if (textEdit?.has("newText") == true) textEdit.optString("newText") else null
            val insertText = sanitizeInsertText(editText ?: item.optString("insertText", label), item.optInt("insertTextFormat", 1))
            val startLine = rangeStart?.optInt("line") ?: position.line
            val startColumn = rangeStart?.optInt("character") ?: (position.column - prefixLength).coerceAtLeast(0)
            val endLine = rangeEnd?.optInt("line") ?: position.line
            val endColumn = rangeEnd?.optInt("character") ?: position.column
            candidates.add(
                CompletionCandidate(
                    label = label,
                    detail = buildDetail(item),
                    insertText = insertText,
                    startLine = startLine,
                    startColumn = startColumn,
                    endLine = endLine,
                    endColumn = endColumn,
                    sortText = item.optString("sortText").takeIf { it.isNotBlank() },
                    filterText = item.optString("filterText").takeIf { it.isNotBlank() },
                    prefixLength = prefixLength
                )
            )
        }
        return candidates.sortedWith(compareBy<CompletionCandidate> { it.sortText ?: it.label.lowercase(Locale.ROOT) }.thenBy { it.label })
    }

    private fun buildDetail(item: JSONObject): String? {
        val detail = item.optString("detail").trim()
        if (detail.isNotEmpty()) return detail
        val documentation = item.opt("documentation") ?: return null
        return when (documentation) {
            is String -> documentation.lineSequence().firstOrNull()?.trim()
            is JSONObject -> documentation.optString("value").lineSequence().firstOrNull()?.trim()
            else -> null
        }?.takeIf { it.isNotEmpty() }
    }

    private fun sanitizeInsertText(text: String, insertTextFormat: Int): String {
        if (insertTextFormat != 2) return text
        return text
            .replace(Regex("\\$\\{\\d+:([^}]*)}"), "$1")
            .replace(Regex("\\$\\d+"), "")
    }

    private fun computePrefixLength(lineText: String, column: Int): Int {
        var index = column.coerceIn(0, lineText.length)
        val end = index
        while (index > 0 && isPrefixChar(lineText[index - 1])) {
            index--
        }
        return end - index
    }

    private fun isPrefixChar(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_' || char == '-' || char == '$'
    }

    private fun contentToString(content: ContentReference): String {
        val builder = StringBuilder()
        for (line in 0 until content.lineCount) {
            if (line > 0) builder.append('\n')
            builder.append(content.getLine(line))
        }
        return builder.toString()
    }

    private fun showErrorOnce(message: String) {
        val normalized = message.trim().take(180)
        if (normalized.isEmpty()) return
        // Eclipse/jdt-ls 日志与瞬时 RPC 错误不弹 Toast
        if (looksLikeServerLogNoise(normalized)) {
            EditorLspDebugStore.appendStderr(normalized)
            return
        }
        EditorLspDebugStore.recordEvent("toast", normalized)
        synchronized(suppressedErrors) {
            if (!suppressedErrors.add(normalized)) return
            if (suppressedErrors.size > 12) suppressedErrors.remove(suppressedErrors.first())
        }
        mainHandler.post {
            UUtils.showMsg("LSP: $normalized")
        }
    }

    private fun looksLikeServerLogNoise(message: String): Boolean {
        val m = message.lowercase()
        return m.startsWith("!") ||
            m.contains("!entry") ||
            m.contains("!message") ||
            m.contains("!session") ||
            m.contains("accessdeniedexception") ||
            m.contains("scan of file failed") ||
            m.contains("org.eclipse.") ||
            m.contains("reconciled ") ||
            m.contains("begin problem") ||
            m.contains("problems reported") ||
            m.contains("validated ") ||
            m.contains("completion request")
    }

    companion object {
        @Volatile
        var activeInstance: EditorLspManager? = null
            private set

        const val LANGUAGE_JSON = "json"
        const val LANGUAGE_JSONC = "jsonc"
        const val LANGUAGE_JAVASCRIPT = "javascript"
        const val LANGUAGE_TYPESCRIPT = "typescript"
        const val LANGUAGE_PYTHON = "python"
        const val LANGUAGE_SHELL = "shellscript"
        const val LANGUAGE_YAML = "yaml"
        const val LANGUAGE_JAVA = "java"
        const val DEFAULT_TIMEOUT_MILLIS = 3000L
        private const val MAX_LSP_TEXT_LENGTH = 1024 * 1024
        private const val MAX_COMPLETION_ITEMS = 120

        fun languageIdForExtension(extension: String): String? {
            return when (extension.lowercase(Locale.ROOT)) {
                "json", "webmanifest", "sublime-settings", "sublime-keymap", "sublime-menu", "sublime-theme", "sublime-build" -> LANGUAGE_JSON
                "jsonc", "json5", "code-workspace" -> LANGUAGE_JSONC
                "js", "mjs", "cjs", "jsx" -> LANGUAGE_JAVASCRIPT
                "ts", "tsx" -> LANGUAGE_TYPESCRIPT
                "py", "python", "pyw" -> LANGUAGE_PYTHON
                "sh", "bash", "zsh", "fish", "profile", "bashrc", "zshrc" -> LANGUAGE_SHELL
                "yaml", "yml" -> LANGUAGE_YAML
                "java" -> LANGUAGE_JAVA
                else -> null
            }
        }
    }
}
