package com.termux.zerocore.editor.lsp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class EditorLspManager(private val context: Context) {
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
    private val failedLanguages = LinkedHashSet<String>()
    private val suppressedErrors = LinkedHashSet<String>()

    @Volatile
    private var settings = Settings(true, DEFAULT_TIMEOUT_MILLIS)

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

    fun openDocument(file: File, languageId: String, text: String) {
        if (!canUseLsp(languageId, text)) return
        val uri = file.toURI().toString()
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
        val uri = file.toURI().toString()
        synchronized(this) {
            val opened = openDocuments.remove(uri) ?: return
            clients[opened.languageId]?.didClose(uri)
        }
    }

    fun completion(file: File, languageId: String, content: ContentReference, position: CharPosition): List<CompletionCandidate> {
        val text = contentToString(content)
        if (!canUseLsp(languageId, text)) return emptyList()
        val uri = file.toURI().toString()
        synchronized(this) {
            changeDocumentLocked(file, languageId, text, true)
        }
        val result = clientFor(file, languageId)?.completion(uri, position.line, position.column) ?: return emptyList()
        return parseCompletionResult(result, content, position).take(MAX_COMPLETION_ITEMS)
    }

    fun closeAll() {
        synchronized(this) {
            openDocuments.clear()
            clients.values.forEach { it.shutdown() }
            clients.clear()
        }
    }

    fun ensureBasicShellInstalled(onFinished: ((Boolean) -> Unit)? = null) {
        lspInstaller.ensureBasicShellInstalled(onFinished)
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
        val uri = file.toURI().toString()
        val version = 1
        client.didOpen(uri, languageId, text, version)
        openDocuments[uri] = OpenDocument(uri, languageId, version, text)
    }

    private fun changeDocumentLocked(file: File, languageId: String, text: String, openIfMissing: Boolean) {
        val uri = file.toURI().toString()
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
        val command = lspInstaller.commandForLanguage(languageId)?.trim().orEmpty()
        if (!settings.enabled || command.isEmpty()) return null
        synchronized(failedLanguages) {
            if (failedLanguages.contains(languageId)) return null
        }
        clients[languageId]?.let { client ->
            if (client.isRunning()) return client
            clients.remove(languageId)
            openDocuments.filterValues { it.languageId == languageId }.keys.forEach { openDocuments.remove(it) }
        }
        val client = EditorLspClient(
            context.applicationContext,
            command,
            file.parentFile,
            settings.timeoutMillis,
            ::showErrorOnce
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
        synchronized(suppressedErrors) {
            if (!suppressedErrors.add(normalized)) return
            if (suppressedErrors.size > 8) suppressedErrors.remove(suppressedErrors.first())
        }
        mainHandler.post {
            UUtils.showMsg("LSP: $normalized")
        }
    }

    companion object {
        const val LANGUAGE_JSON = "json"
        const val LANGUAGE_JSONC = "jsonc"
        const val LANGUAGE_JAVASCRIPT = "javascript"
        const val LANGUAGE_TYPESCRIPT = "typescript"
        const val LANGUAGE_PYTHON = "python"
        const val LANGUAGE_SHELL = "shellscript"
        const val LANGUAGE_YAML = "yaml"
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
                else -> null
            }
        }
    }
}
