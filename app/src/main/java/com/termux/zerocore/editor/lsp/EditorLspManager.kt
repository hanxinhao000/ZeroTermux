package com.termux.zerocore.editor.lsp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import com.termux.R
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

    data class LspTextEdit(
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int,
        val newText: String
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
        val prefixLength: Int,
        /** 选中后额外编辑（如 import），可能需 resolve 后才有。 */
        val additionalEdits: List<LspTextEdit> = emptyList(),
        /** 原始 CompletionItem，用于 completionItem/resolve。 */
        val resolvePayload: JSONObject? = null,
        val languageId: String? = null,
        /** LSP CompletionItemKind（1=Text …），用于 IDEA 风格图标。 */
        val lspKind: Int? = null
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
    private val rawDiagnosticsByUri = ConcurrentHashMap<String, JSONArray>()
    /** languageKey → 上次启动失败时间；冷却后允许重试，避免整会话永久无补全。 */
    private val failedLanguages = LinkedHashMap<String, Long>()
    private val suppressedErrors = LinkedHashSet<String>()
    private val diagnosticIdSeq = AtomicLong(1)

    @Volatile
    private var diagnosticsListener: DiagnosticsListener? = null

    @Volatile
    private var settings = Settings(true, DEFAULT_TIMEOUT_MILLIS)

    fun setDiagnosticsListener(listener: DiagnosticsListener?) {
        diagnosticsListener = listener
    }

    /**
     * 由编辑器 Activity 实现：应用 WorkspaceEdit、弹出 CodeAction 选择等。
     */
    interface HostCallbacks {
        fun applyFileEdits(file: File, edits: List<LspTextEdit>)
        fun showCodeActionChoices(title: String, actions: List<EditorLspCodeAction>, onChosen: (EditorLspCodeAction) -> Unit)
        fun runOnUi(block: () -> Unit)
    }

    @Volatile
    private var hostCallbacks: HostCallbacks? = null

    fun setHostCallbacks(callbacks: HostCallbacks?) {
        hostCallbacks = callbacks
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
            "failed_languages" to synchronized(failedLanguages) { failedLanguages.keys.toList() }
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
                    clients[clientCacheKey(opened.languageId)]?.didClose(uri)
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
            clients[clientCacheKey(opened.languageId)]?.didClose(uri)
            clearDiagnostics(uri)
        }
    }

    fun completion(file: File, languageId: String, content: ContentReference, position: CharPosition): List<CompletionCandidate> {
        val text = contentToString(content)
        val createMethod = if (languageId == LANGUAGE_JAVA) {
            runCatching {
                EditorJavaCreateMethodCompletions.build(
                    text,
                    position,
                    context.getString(R.string.editor_java_create_method)
                )
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val postfix = if (languageId == LANGUAGE_JAVA) {
            runCatching { EditorJavaPostfixCompletions.build(text, position) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        if (!canUseLsp(languageId, text)) {
            return (createMethod + postfix).take(MAX_COMPLETION_ITEMS)
        }
        val uri = EditorLspUris.forFile(file)
        synchronized(this) {
            changeDocumentLocked(file, languageId, text, true)
        }
        val (triggerKind, triggerCharacter) = detectCompletionTrigger(text, position)
        val result = runCatching {
            clientFor(file, languageId)?.completion(
                uri,
                position.line,
                position.column,
                triggerKind,
                triggerCharacter
            )
        }.getOrNull()
        val lspItems = if (result != null) {
            parseCompletionResult(result, content, position, languageId)
        } else {
            emptyList()
        }
        val merged = mergeCompletionItems(postfix, lspItems, preferLspFirst = triggerCharacter == ".")
        // 快捷创建方法始终置顶
        return (createMethod + merged).take(MAX_COMPLETION_ITEMS)
    }

    /** 输入 `.` / `::` 等触发字符时走 TriggerCharacter，便于 jdt-ls 给出成员方法。 */
    private fun detectCompletionTrigger(text: String, position: CharPosition): Pair<Int, String?> {
        val lineText = lineAt(text, position.line) ?: return 1 to null
        val column = position.column.coerceIn(0, lineText.length)
        if (column <= 0) return 1 to null
        val ch = lineText[column - 1]
        // 仅在「触发符后尚无标识符前缀」时用 TriggerCharacter，例如 `a.` / `a.fo` 仍用 Invoked 过滤
        val prefixLen = computePrefixLength(lineText, column)
        if (prefixLen > 0) return 1 to null
        return when (ch) {
            '.', '(' -> 2 to ch.toString()
            else -> 1 to null
        }
    }

    private fun lineAt(text: String, line: Int): String? {
        if (line < 0) return null
        var current = 0
        var start = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '\n') {
                if (current == line) return text.substring(start, i)
                current++
                start = i + 1
            }
            i++
        }
        return if (current == line) text.substring(start) else null
    }

    private fun mergeCompletionItems(
        postfix: List<CompletionCandidate>,
        lspItems: List<CompletionCandidate>,
        preferLspFirst: Boolean
    ): List<CompletionCandidate> {
        if (postfix.isEmpty()) return lspItems
        if (lspItems.isEmpty()) return postfix
        val postfixLabels = postfix.map { it.label.lowercase(Locale.ROOT) }.toHashSet()
        val filteredLsp = lspItems.filter { item ->
            val label = item.label.lowercase(Locale.ROOT)
            // Prefer client loop templates when both sides offer for/fori/forr/…
            label !in postfixLabels && label !in EditorJavaPostfixCompletions.CLIENT_LABELS
        }
        // `a.` 成员补全：方法优先，postfix 放后面，避免只看到 for/fori/forr
        return if (preferLspFirst) filteredLsp + postfix else postfix + filteredLsp
    }

    /**
     * 选中补全项时解析并合并 additionalTextEdits（Java 自动 import）。
     * 返回用于实际插入的候选（已带上 import 编辑）。
     */
    fun resolveCompletionCandidate(candidate: CompletionCandidate): CompletionCandidate {
        val payload = candidate.resolvePayload ?: return candidate
        val languageId = candidate.languageId ?: return candidate
        if (candidate.additionalEdits.isNotEmpty()) return candidate
        val client = clients[clientCacheKey(languageId)] ?: return candidate
        if (!client.isRunning()) return candidate
        val resolved = client.resolveCompletionItem(payload) ?: return candidate
        val additional = parseAdditionalTextEdits(resolved)
        val textEdit = resolved.optJSONObject("textEdit")
        val editRange = textEdit?.optJSONObject("range") ?: textEdit?.optJSONObject("replace")
        val rangeStart = editRange?.optJSONObject("start")
        val rangeEnd = editRange?.optJSONObject("end")
        val format = resolved.optInt("insertTextFormat", 1)
        val rawInsert = when {
            textEdit?.has("newText") == true -> textEdit.optString("newText")
            resolved.optString("textEditText").isNotBlank() -> resolved.optString("textEditText")
            resolved.optString("insertText").isNotBlank() -> resolved.optString("insertText")
            else -> candidate.insertText
        }
        val merged = candidate.copy(
            insertText = sanitizeInsertText(rawInsert, format).ifBlank { candidate.insertText },
            startLine = rangeStart?.optInt("line") ?: candidate.startLine,
            startColumn = rangeStart?.optInt("character") ?: candidate.startColumn,
            endLine = rangeEnd?.optInt("line") ?: candidate.endLine,
            endColumn = rangeEnd?.optInt("character") ?: candidate.endColumn,
            detail = buildDetail(resolved) ?: candidate.detail,
            additionalEdits = additional.ifEmpty { candidate.additionalEdits },
            resolvePayload = null
        )
        return if (merged.label.lowercase(Locale.ROOT) in EditorJavaPostfixCompletions.CLIENT_LABELS) {
            mergePostfixDeletion(merged)
        } else {
            merged
        }
    }

    fun hover(file: File, languageId: String, line: Int, column: Int): String? {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return null
        val client = clientFor(file, languageId) ?: return null
        val uri = EditorLspUris.forFile(file)
        return EditorLspProtocol.parseHoverText(client.hover(uri, line, column))
    }

    fun definition(file: File, languageId: String, line: Int, column: Int): List<EditorLspLocation> {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return emptyList()
        val client = clientFor(file, languageId) ?: return emptyList()
        val uri = EditorLspUris.forFile(file)
        return EditorLspProtocol.parseLocations(client.definition(uri, line, column))
    }

    fun references(file: File, languageId: String, line: Int, column: Int): List<EditorLspLocation> {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return emptyList()
        val client = clientFor(file, languageId) ?: return emptyList()
        val uri = EditorLspUris.forFile(file)
        return EditorLspProtocol.parseLocations(client.references(uri, line, column))
    }

    /**
     * 普通路径直接返回；jdt:// / *.class 则拉取源码到本地缓存后再导航。
     * @param workspaceFile 当前工作区中的已打开文件，用于定位同一 jdt-ls 会话
     */
    fun resolveNavigationLocation(
        workspaceFile: File,
        languageId: String,
        location: EditorLspLocation
    ): EditorLspLocation? {
        if (!EditorJdtClassFileSupport.needsClassFileContents(location)) {
            return location.takeIf { it.file.isFile }
        }
        if (languageId != LANGUAGE_JAVA) return null
        val client = clientFor(workspaceFile, languageId) ?: return null
        return EditorJdtClassFileSupport.resolve(client, location)
    }

    fun codeActions(
        file: File,
        languageId: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        diagnostics: JSONArray = JSONArray(),
        only: List<String>? = null
    ): List<EditorLspCodeAction> {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return emptyList()
        val client = clientFor(file, languageId) ?: return emptyList()
        val uri = EditorLspUris.forFile(file)
        return EditorLspProtocol.parseCodeActions(
            client.codeAction(uri, startLine, startColumn, endLine, endColumn, diagnostics, only)
        )
    }

    fun organizeImports(file: File, languageId: String): Boolean {
        val uri = EditorLspUris.forFile(file)
        val text = openDocuments[uri]?.text
            ?: openDocuments.entries.firstOrNull { EditorLspUris.same(it.key, uri) }?.value?.text
            ?: runCatching { file.readText() }.getOrDefault("")
        val endLine = text.count { it == '\n' }
        val endCol = if (text.isEmpty()) 0 else text.length - text.lastIndexOf('\n') - 1
        val actions = codeActions(
            file,
            languageId,
            0,
            0,
            endLine,
            endCol.coerceAtLeast(0),
            only = listOf("source.organizeImports")
        )
        val action = actions.firstOrNull {
            it.kind?.contains("organizeImports", ignoreCase = true) == true ||
                it.title.contains("Organize Import", ignoreCase = true) ||
                it.title.contains("组织", ignoreCase = false)
        } ?: actions.firstOrNull() ?: return false
        return applyCodeAction(file, languageId, action)
    }

    fun applyCodeAction(file: File, languageId: String, action: EditorLspCodeAction): Boolean {
        var applied = false
        if (action.fileEdits.isNotEmpty()) {
            applyFileEdits(action.fileEdits)
            applied = true
        }
        val command = action.command ?: return applied
        val cmdName = command.optString("command").ifBlank {
            command.optJSONObject("command")?.optString("command").orEmpty()
        }
        val args = when (val raw = command.opt("arguments")) {
            is JSONArray -> raw
            else -> command.optJSONObject("command")?.optJSONArray("arguments")
        }
        if (cmdName.isBlank()) return applied
        val client = clients[clientCacheKey(languageId)] ?: return applied
        val result = client.executeCommand(cmdName, args)
        when (result) {
            is JSONObject -> {
                // 部分 jdt-ls command 直接返回 WorkspaceEdit
                if (result.has("changes") || result.has("documentChanges")) {
                    applyFileEdits(EditorLspProtocol.parseWorkspaceEdit(result))
                    applied = true
                }
            }
        }
        return applied
    }

    fun didSave(file: File, languageId: String, text: String) {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return
        val client = clients[clientCacheKey(languageId)] ?: return
        if (!client.isRunning()) return
        client.didSave(EditorLspUris.forFile(file), text)
    }

    fun handleServerApplyEdit(edit: JSONObject): Boolean {
        val fileEdits = EditorLspProtocol.parseWorkspaceEdit(edit)
        if (fileEdits.isEmpty()) return false
        applyFileEdits(fileEdits)
        return true
    }

    private fun applyFileEdits(fileEdits: List<Pair<File, List<LspTextEdit>>>) {
        val host = hostCallbacks
        if (host != null) {
            host.runOnUi {
                fileEdits.forEach { (file, edits) ->
                    host.applyFileEdits(file, edits)
                }
            }
            return
        }
        // 无 Host 时直接写盘（后台）
        fileEdits.forEach { (file, edits) ->
            runCatching {
                val original = if (file.isFile) file.readText() else ""
                file.writeText(applyEditsToString(original, edits))
            }
        }
    }

    fun applyEditsToString(original: String, edits: List<LspTextEdit>): String {
        if (edits.isEmpty()) return original
        // 转为行列表以便按行列替换；从后往前
        val lines = original.split('\n').toMutableList()
        val ordered = edits.sortedWith(
            compareByDescending<LspTextEdit> { it.startLine }.thenByDescending { it.startColumn }
        )
        for (edit in ordered) {
            val startLine = edit.startLine.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
            val endLine = edit.endLine.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
            val startCol = edit.startColumn.coerceIn(0, lines.getOrNull(startLine)?.length ?: 0)
            val endCol = edit.endColumn.coerceIn(0, lines.getOrNull(endLine)?.length ?: 0)
            if (lines.isEmpty()) {
                lines.add(edit.newText)
                continue
            }
            val prefix = lines[startLine].substring(0, startCol)
            val suffix = lines[endLine].substring(endCol)
            val inserted = edit.newText.split('\n')
            if (inserted.size == 1) {
                lines[startLine] = prefix + inserted[0] + suffix
                if (endLine > startLine) {
                    for (i in endLine downTo startLine + 1) lines.removeAt(i)
                }
            } else {
                val newLines = ArrayList<String>()
                newLines.add(prefix + inserted.first())
                for (i in 1 until inserted.lastIndex) newLines.add(inserted[i])
                newLines.add(inserted.last() + suffix)
                for (i in endLine downTo startLine) lines.removeAt(i)
                lines.addAll(startLine, newLines)
            }
        }
        return lines.joinToString("\n")
    }

    fun closeAll() {
        val uris: List<String>
        synchronized(this) {
            uris = diagnosticsByUri.keys.toList()
            openDocuments.clear()
            diagnosticsByUri.clear()
            rawDiagnosticsByUri.clear()
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

    fun ensureClangdInstalled(onFinished: ((Boolean) -> Unit)? = null) {
        lspInstaller.installPackage(EditorClangdSupport.PACKAGE_ID, quietIfInstalled = true) { success, _ ->
            if (success) {
                synchronized(failedLanguages) {
                    failedLanguages.remove(CLIENT_KEY_CLANGD)
                    failedLanguages.remove(LANGUAGE_C)
                    failedLanguages.remove(LANGUAGE_CPP)
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
        val cacheKey = clientCacheKey(languageId)
        if (!lspInstaller.isLanguageInstalled(languageId)) {
            // 不自动安装：统一走编辑器设置 → LSP 列表手动安装
            showErrorOnce(
                when (languageId) {
                    LANGUAGE_JAVA -> "Java LSP 未安装，请在编辑器设置 → LSP 中安装"
                    LANGUAGE_C, LANGUAGE_CPP -> "C/C++ LSP (clangd) 未安装，请在编辑器设置 → LSP 中安装"
                    LANGUAGE_PYTHON -> "Python LSP 未安装，请在编辑器设置 → LSP 中安装"
                    else -> "LSP 服务器未安装，请先在设置中安装对应语言包"
                }
            )
            return null
        }
        val projectRoot = when (languageId) {
            LANGUAGE_JAVA -> EditorJdtLsSupport.findProjectRoot(file)
            LANGUAGE_C, LANGUAGE_CPP -> EditorClangdSupport.findProjectRoot(file)
            LANGUAGE_PYTHON -> EditorPyrightSupport.findProjectRoot(file)
            else -> file.parentFile
        }
        val launchSpec = lspInstaller.launchSpecForLanguage(languageId, projectRoot)
        if (launchSpec == null) {
            showErrorOnce(
                when (languageId) {
                    LANGUAGE_JAVA -> "jdt-ls 启动失败：请确认已安装 openjdk-21，并重新安装 Java LSP"
                    LANGUAGE_C, LANGUAGE_CPP -> "clangd 启动失败：请确认已执行 pkg install clang，并重新安装 C/C++ LSP"
                    LANGUAGE_PYTHON -> "Pyright 启动失败：请确认已安装 node/npm，并重新安装 Python LSP"
                    else -> "LSP 服务器命令未找到，请重新安装对应语言包"
                }
            )
            return null
        }
        synchronized(failedLanguages) {
            val failedAt = failedLanguages[cacheKey]
            if (failedAt != null) {
                if (System.currentTimeMillis() - failedAt < FAILED_LANGUAGE_RETRY_MS) {
                    return null
                }
                failedLanguages.remove(cacheKey)
            }
        }
        clients[cacheKey]?.let { client ->
            if (client.isRunning()) return client
            clients.remove(cacheKey)
            val staleUris = openDocuments.filterValues {
                clientCacheKey(it.languageId) == cacheKey
            }.keys.toList()
            staleUris.forEach { uri ->
                openDocuments.remove(uri)
                clearDiagnostics(uri)
            }
        }
        // 补全等 RPC 用短超时；initialize 单独加长。勿用 maxOf(设置值)，否则会把补全拖成几十秒。
        val requestTimeout = when (languageId) {
            LANGUAGE_JAVA -> EditorJdtLsSupport.COMPLETION_TIMEOUT_MILLIS
            else -> settings.timeoutMillis
        }
        val initTimeout = when (languageId) {
            LANGUAGE_JAVA -> maxOf(settings.timeoutMillis, EditorJdtLsSupport.INIT_TIMEOUT_MILLIS)
            LANGUAGE_C, LANGUAGE_CPP -> maxOf(settings.timeoutMillis, EditorClangdSupport.INIT_TIMEOUT_MILLIS)
            LANGUAGE_PYTHON -> maxOf(settings.timeoutMillis, EditorPyrightSupport.INIT_TIMEOUT_MILLIS)
            else -> settings.timeoutMillis
        }
        val client = EditorLspClient(
            context.applicationContext,
            launchSpec,
            projectRoot,
            requestTimeout,
            ::showErrorOnce,
            EditorLspCommandResolver.environmentForLanguage(languageId),
            EditorLspCommandResolver.initializationOptionsForLanguage(languageId),
            ::handleServerNotification,
            initTimeoutMillis = initTimeout
        )
        return if (client.start()) {
            synchronized(failedLanguages) { failedLanguages.remove(cacheKey) }
            clients[cacheKey] = client
            client
        } else {
            synchronized(failedLanguages) {
                failedLanguages[cacheKey] = System.currentTimeMillis()
            }
            null
        }
    }

    /** C/C++ 共用一个 clangd 进程。 */
    private fun clientCacheKey(languageId: String): String {
        return when (languageId) {
            LANGUAGE_C, LANGUAGE_CPP -> CLIENT_KEY_CLANGD
            else -> languageId
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
        if (items.length() == 0) {
            rawDiagnosticsByUri.remove(uri)
        } else {
            rawDiagnosticsByUri[uri] = items
        }
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
        rawDiagnosticsByUri.remove(uri)
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

    private fun buildDiagnosticsContainer(
        text: String,
        items: JSONArray
    ): DiagnosticsContainer? {
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
            // QuickFix 类在部分 editor 依赖解析环境下不可见；修复入口走长按「代码操作」
            regions.add(
                DiagnosticRegion(
                    startIndex,
                    endIndex,
                    severity,
                    diagnosticIdSeq.getAndIncrement(),
                    DiagnosticDetail(brief, message, null, item)
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

    private fun parseCompletionResult(
        result: Any?,
        content: ContentReference,
        position: CharPosition,
        languageId: String
    ): List<CompletionCandidate> {
        val listObj = result as? JSONObject
        val items = when (result) {
            is JSONArray -> result
            is JSONObject -> result.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
        val itemDefaults = listObj?.optJSONObject("itemDefaults")
        val defaultFormat = itemDefaults?.optInt("insertTextFormat", 1) ?: 1
        val defaultRange = resolveDefaultEditRange(itemDefaults)
        val lineText = runCatching { content.getLine(position.line) }.getOrDefault("")
        val prefixLength = computePrefixLength(lineText, position.column)
        val candidates = ArrayList<CompletionCandidate>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            // 单条失败不影响其余成员方法
            val candidate = runCatching {
                parseOneCompletionItem(item, position, languageId, prefixLength, defaultFormat, defaultRange)
            }.getOrNull() ?: continue
            candidates.add(candidate)
        }
        return candidates.sortedWith(
            compareBy<CompletionCandidate> { it.sortText ?: it.label.lowercase(Locale.ROOT) }
                .thenBy { it.label }
        )
    }

    private fun parseOneCompletionItem(
        item: JSONObject,
        position: CharPosition,
        languageId: String,
        prefixLength: Int,
        defaultFormat: Int,
        defaultRange: JSONObject?
    ): CompletionCandidate? {
        val label = resolveCompletionLabel(item)
        if (label.isEmpty()) return null
        val textEdit = item.optJSONObject("textEdit")
        val editRange = textEdit?.optJSONObject("range")
            ?: textEdit?.optJSONObject("replace")
            ?: defaultRange
        val rangeStart = editRange?.optJSONObject("start")
        val rangeEnd = editRange?.optJSONObject("end")
        val rawInsert = when {
            textEdit?.has("newText") == true -> textEdit.optString("newText")
            item.optString("textEditText").isNotBlank() -> item.optString("textEditText")
            item.optString("insertText").isNotBlank() -> item.optString("insertText")
            else -> label
        }
        val format = when {
            item.has("insertTextFormat") -> item.optInt("insertTextFormat", 1)
            else -> defaultFormat
        }
        val insertText = sanitizeInsertText(rawInsert, format).ifBlank { label }
        val startLine = rangeStart?.optInt("line") ?: position.line
        val startColumn = rangeStart?.optInt("character")
            ?: (position.column - prefixLength).coerceAtLeast(0)
        val endLine = rangeEnd?.optInt("line") ?: position.line
        val endColumn = rangeEnd?.optInt("character") ?: position.column
        val additional = parseAdditionalTextEdits(item)
        // jdt-ls：import 常在 resolve 阶段才给出；有 data 时保留原文供 resolve
        val needsResolve = additional.isEmpty() && item.has("data")
        val lspKind = if (item.has("kind")) item.optInt("kind") else null
        val (displayLabel, displayDesc) = splitCompletionDisplay(label, buildDetail(item))
        val candidate = CompletionCandidate(
            label = displayLabel,
            detail = displayDesc,
            insertText = insertText,
            startLine = startLine,
            startColumn = startColumn,
            endLine = endLine,
            endColumn = endColumn,
            sortText = item.optString("sortText").takeIf { it.isNotBlank() },
            filterText = item.optString("filterText").takeIf { it.isNotBlank() } ?: label,
            prefixLength = prefixLength.coerceIn(0, position.column.coerceAtLeast(0)),
            additionalEdits = additional,
            resolvePayload = if (needsResolve) item else null,
            languageId = languageId,
            lspKind = lspKind
        )
        return if (label.lowercase(Locale.ROOT) in EditorJavaPostfixCompletions.CLIENT_LABELS) {
            mergePostfixDeletion(candidate).let { merged ->
                merged.copy(
                    prefixLength = merged.prefixLength.coerceIn(0, position.column.coerceAtLeast(0))
                )
            }
        } else {
            candidate
        }
    }

    private fun resolveCompletionLabel(item: JSONObject): String {
        val raw = item.opt("label") ?: return ""
        val base = when (raw) {
            is String -> raw.trim()
            is JSONObject -> raw.optString("label").trim().ifBlank {
                raw.optString("name").trim()
            }
            else -> raw.toString().trim()
        }
        // labelDetails.detail 常为参数表，拼到名称后更接近 IDEA
        val details = item.optJSONObject("labelDetails")
        val detailPart = details?.optString("detail")?.takeIf { it.isNotBlank() }
        return if (detailPart != null && !base.contains('(') && detailPart.startsWith("(")) {
            base + detailPart
        } else {
            base
        }
    }

    /**
     * 将 `method(args) : ReturnType` 拆成左侧签名 + 右侧类型；
     * 否则右侧用 detail（如包名 java.util）。
     */
    private fun splitCompletionDisplay(label: String, detail: String?): Pair<String, String?> {
        val sep = " : "
        val idx = label.lastIndexOf(sep)
        if (idx > 0) {
            val left = label.substring(0, idx).trim()
            val right = label.substring(idx + sep.length).trim()
            if (left.isNotEmpty() && right.isNotEmpty()) {
                return left to right
            }
        }
        return label to detail?.takeIf { it.isNotBlank() && it != label }
    }

    private fun resolveDefaultEditRange(itemDefaults: JSONObject?): JSONObject? {
        if (itemDefaults == null) return null
        val editRange = itemDefaults.opt("editRange") ?: return null
        return when (editRange) {
            is JSONObject -> editRange.optJSONObject("replace") ?: editRange
            else -> null
        }
    }

    /**
     * jdt-ls postfix completions insert the snippet at the cursor and delete `expr.postfix`
     * via an empty additionalTextEdit. Merge into a single replace so apply order stays correct.
     */
    private fun mergePostfixDeletion(candidate: CompletionCandidate): CompletionCandidate {
        if (candidate.additionalEdits.isEmpty()) return candidate
        val deletion = candidate.additionalEdits.firstOrNull { edit ->
            edit.newText.isEmpty() &&
                edit.startLine == candidate.endLine &&
                edit.endLine == candidate.endLine &&
                edit.endColumn >= candidate.endColumn &&
                edit.startColumn < candidate.startColumn
        } ?: return candidate
        // prefixLength 只能覆盖光标前前缀；过大会让 sora filterCompletionItems 越界，整表补全失败
        val span = (deletion.endColumn - deletion.startColumn).coerceAtLeast(0)
        return candidate.copy(
            startLine = deletion.startLine,
            startColumn = deletion.startColumn,
            endLine = deletion.endLine,
            endColumn = deletion.endColumn,
            prefixLength = span.coerceAtMost(candidate.endColumn.coerceAtLeast(0)),
            additionalEdits = candidate.additionalEdits.filterNot { it === deletion }
        )
    }

    private fun parseAdditionalTextEdits(item: JSONObject): List<LspTextEdit> {
        val array = item.optJSONArray("additionalTextEdits") ?: return emptyList()
        val edits = ArrayList<LspTextEdit>(array.length())
        for (i in 0 until array.length()) {
            val edit = array.optJSONObject(i) ?: continue
            val range = edit.optJSONObject("range") ?: continue
            val start = range.optJSONObject("start") ?: continue
            val end = range.optJSONObject("end") ?: continue
            edits.add(
                LspTextEdit(
                    startLine = start.optInt("line", 0),
                    startColumn = start.optInt("character", 0),
                    endLine = end.optInt("line", 0),
                    endColumn = end.optInt("character", 0),
                    newText = edit.optString("newText", "")
                )
            )
        }
        return edits
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
        // Expand LSP snippets to plain text (tab-stops / placeholders → defaults).
        var result = text
        // ${1|one,two|} choice → first option
        result = result.replace(Regex("\\$\\{\\d+\\|([^|}]+)\\|?}")) { match ->
            match.groupValues[1].substringBefore(',')
        }
        // ${1:default} / nested-ish simple form
        var previous: String
        do {
            previous = result
            result = result.replace(Regex("\\$\\{\\d+:([^{}]*)}"), "$1")
        } while (result != previous)
        result = result.replace(Regex("\\$\\{\\d+}"), "")
        result = result.replace(Regex("\\$\\d+"), "")
        return result
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
        const val LANGUAGE_C = "c"
        const val LANGUAGE_CPP = "cpp"
        private const val CLIENT_KEY_CLANGD = "clangd"
        const val DEFAULT_TIMEOUT_MILLIS = 3000L
        private const val MAX_LSP_TEXT_LENGTH = 1024 * 1024
        private const val MAX_COMPLETION_ITEMS = 120
        /** jdt-ls 等启动失败后的重试冷却，避免一次失败整会话无补全。 */
        private const val FAILED_LANGUAGE_RETRY_MS = 15_000L

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
                "c", "h" -> LANGUAGE_C
                "cpp", "cc", "cxx", "c++", "hpp", "hh", "hxx", "h++" -> LANGUAGE_CPP
                else -> null
            }
        }
    }
}
