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
        /** 在宿主编辑器打开 LSP 位置（含 jdt 缓存的 JDK 源码）。 */
        fun openLspLocation(location: EditorLspLocation) {}
    }

    @Volatile
    private var hostCallbacks: HostCallbacks? = null

    fun setHostCallbacks(callbacks: HostCallbacks?) {
        hostCallbacks = callbacks
    }

    /** AI Debug：在前台编辑器打开已解析的 LSP 位置。 */
    fun openLocationInHost(location: EditorLspLocation): Boolean {
        val host = hostCallbacks ?: return false
        host.runOnUi { host.openLspLocation(location) }
        return true
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
        }.onFailure { err ->
            EditorLspDebugStore.recordEvent(
                "completion_error",
                "${err.javaClass.simpleName}: ${err.message?.take(120)}"
            )
        }.getOrNull()
        if (result == null) {
            EditorLspDebugStore.recordEvent(
                "completion_empty",
                "no result lang=$languageId trigger=$triggerKind ch=$triggerCharacter"
            )
        }
        val lspItems = if (result != null) {
            parseCompletionResult(result, content, position, languageId)
        } else {
            emptyList()
        }
        EditorLspDebugStore.recordEvent(
            "completion",
            "lang=$languageId lsp=${lspItems.size} postfix=${postfix.size} create=${createMethod.size}"
        )
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
        return definitionDetailed(file, languageId, line, column).locations
    }

    data class DefinitionProbe(
        val locations: List<EditorLspLocation>,
        val source: String,
        val elapsedMs: Long,
        val preferType: Boolean,
        val rawDefinition: String?,
        val rawTypeDefinition: String?,
        val rawNull: Boolean
    )

    fun definitionDetailed(
        file: File,
        languageId: String,
        line: Int,
        column: Int
    ): DefinitionProbe {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) {
            return DefinitionProbe(emptyList(), "disabled", 0, false, null, null, true)
        }
        val client = clientFor(file, languageId)
            ?: return DefinitionProbe(emptyList(), "no_client", 0, false, null, null, true)
        val uri = EditorLspUris.forFile(file)
        val timeout = navigationTimeout(languageId)
        val started = System.currentTimeMillis()
        var locations = emptyList<EditorLspLocation>()
        var source = "definition"
        var rawDef: Any? = null
        var rawType: Any? = null

        // 仅对「简单名.」且后面不是方法调用的类型限定（System.xxx）优先 typeDefinition。
        // println( / out. 中的成员应用 definition 跳到方法/字段。
        val preferType = languageId == LANGUAGE_JAVA && isTypeQualifierName(file, line, column)
        if (preferType) {
            rawType = client.typeDefinition(uri, line, column, timeout)
            locations = EditorLspProtocol.parseLocations(rawType)
            source = "typeDefinition_first"
        }
        if (locations.isEmpty()) {
            rawDef = client.definition(uri, line, column, timeout)
            locations = EditorLspProtocol.parseLocations(rawDef)
            source = "definition"
        }
        // 方法重载：definition 有时返回 LocationLink 列表；若仍空，再试 declaration
        if (locations.isEmpty() && languageId == LANGUAGE_JAVA) {
            val rawDecl = client.declaration(uri, line, column, timeout)
            val declLocs = EditorLspProtocol.parseLocations(rawDecl)
            if (declLocs.isNotEmpty()) {
                locations = declLocs
                source = "declaration"
                if (rawDef == null) rawDef = rawDecl
            }
        }
        // JDK 方法（println 等）偶发 definition 为空：经接收者类型定位后在源码中搜方法
        if (locations.isEmpty() && languageId == LANGUAGE_JAVA) {
            val viaReceiver = resolveMethodViaReceiver(client, file, line, column, timeout)
            if (viaReceiver != null) {
                locations = listOf(viaReceiver)
                source = "method_via_receiver"
            }
        }
        // 类型名（System）等：definition/declaration 都空时回退 typeDefinition
        if (locations.isEmpty() && languageId == LANGUAGE_JAVA && source != "typeDefinition_first") {
            rawType = client.typeDefinition(uri, line, column, timeout)
            locations = EditorLspProtocol.parseLocations(rawType)
            source = "typeDefinition"
        }
        val elapsed = System.currentTimeMillis() - started
        EditorLspDebugStore.recordEvent(
            "definition",
            "goto $source line=$line col=$column count=${locations.size} ${elapsed}ms",
            mapOf(
                "uri" to uri,
                "line" to line,
                "column" to column,
                "count" to locations.size,
                "elapsed_ms" to elapsed,
                "source" to source,
                "prefer_type" to preferType,
                "raw_null" to (rawDef == null && !preferType),
                "raw_def_kind" to (rawDef?.javaClass?.simpleName ?: "null"),
                "raw_def_preview" to rawPreview(rawDef),
                "first_uri" to locations.firstOrNull()?.uri.orEmpty(),
                "first_file" to (locations.firstOrNull()?.file?.absolutePath.orEmpty()),
                "first_line" to (locations.firstOrNull()?.line ?: -1),
                "first_column" to (locations.firstOrNull()?.column ?: -1)
            )
        )
        return DefinitionProbe(
            locations = locations,
            source = source,
            elapsedMs = elapsed,
            preferType = preferType,
            rawDefinition = rawPreview(rawDef),
            rawTypeDefinition = rawPreview(rawType),
            rawNull = rawDef == null
        )
    }

    private fun rawPreview(raw: Any?): String? {
        if (raw == null) return null
        val text = raw.toString()
        return if (text.length <= 1500) text else text.take(1500) + "…"
    }

    /**
     * 光标在「类型限定名.」上，且该段不是方法调用名。
     * System.out → true；out.println → false（out 是字段）；println( → false。
     */
    private fun isTypeQualifierName(file: File, line: Int, column: Int): Boolean {
        val uri = EditorLspUris.forFile(file)
        val text = openDocuments[uri]?.text
            ?: openDocuments.entries.firstOrNull { EditorLspUris.same(it.key, uri) }?.value?.text
            ?: return false
        val lineText = text.lineSequence().elementAtOrNull(line) ?: return false
        if (lineText.isEmpty()) return false
        var index = column.coerceIn(0, lineText.length)
        if (index == lineText.length) index -= 1
        if (index < 0) return false
        if (!lineText[index].isJavaIdentPart()) {
            if (index > 0 && lineText[index - 1].isJavaIdentPart()) index -= 1 else return false
        }
        var start = index
        while (start > 0 && lineText[start - 1].isJavaIdentPart()) start--
        var end = index + 1
        while (end < lineText.length && lineText[end].isJavaIdentPart()) end++
        var after = end
        while (after < lineText.length && lineText[after].isWhitespace()) after++
        if (after >= lineText.length || lineText[after] != '.') return false
        // 标识符前若还有「.」，多半是成员链中间（out / println），不要优先 typeDefinition
        var before = start - 1
        while (before >= 0 && lineText[before].isWhitespace()) before--
        if (before >= 0 && lineText[before] == '.') return false
        // 后跟 '.' 且名字首字母大写（类型约定）或已知常见类型别名
        val name = lineText.substring(start, end)
        return name.isNotEmpty() && name[0].isUpperCase()
    }

    private fun Char.isJavaIdentPart(): Boolean {
        return this.isLetterOrDigit() || this == '_' || this == '$'
    }

    /**
     * 当 textDocument/definition 对方法名返回空时：
     * 取前一段接收者（out / System）→ typeDefinition/definition → 打开类源码 → 定位 methodName。
     */
    private fun resolveMethodViaReceiver(
        client: EditorLspClient,
        file: File,
        line: Int,
        column: Int,
        timeout: Long
    ): EditorLspLocation? {
        val uri = EditorLspUris.forFile(file)
        val text = openDocuments[uri]?.text
            ?: openDocuments.entries.firstOrNull { EditorLspUris.same(it.key, uri) }?.value?.text
            ?: return null
        val lineText = text.lineSequence().elementAtOrNull(line) ?: return null
        val method = findIdentSpan(lineText, column) ?: return null
        // 方法调用：println(
        var afterMethod = method.second
        while (afterMethod < lineText.length && lineText[afterMethod].isWhitespace()) afterMethod++
        if (afterMethod >= lineText.length || lineText[afterMethod] != '(') return null
        // 接收者：.out. 或 System.
        var before = method.first - 1
        while (before >= 0 && lineText[before].isWhitespace()) before--
        if (before < 0 || lineText[before] != '.') return null
        before--
        while (before >= 0 && lineText[before].isWhitespace()) before--
        if (before < 0 || !lineText[before].isJavaIdentPart()) return null
        val receiver = findIdentSpan(lineText, before) ?: return null
        val receiverCol = receiver.first + (receiver.second - receiver.first).coerceAtLeast(1) / 2
        var typeLocs = EditorLspProtocol.parseLocations(
            client.typeDefinition(uri, line, receiverCol, timeout)
        )
        if (typeLocs.isEmpty()) {
            typeLocs = EditorLspProtocol.parseLocations(
                client.definition(uri, line, receiverCol, timeout)
            )
        }
        val typeLoc = typeLocs.firstOrNull() ?: return null
        val resolved = resolveNavigationLocation(file, LANGUAGE_JAVA, typeLoc) ?: return null
        if (!resolved.file.isFile) return null
        val methodName = lineText.substring(method.first, method.second)
        val anchor = findMethodInJavaSource(resolved.file, methodName) ?: return null
        return EditorLspLocation(
            file = resolved.file,
            line = anchor.first,
            column = anchor.second,
            uri = resolved.uri.ifBlank { typeLoc.uri }
        )
    }

    private fun findIdentSpan(lineText: String, column: Int): Pair<Int, Int>? {
        if (lineText.isEmpty()) return null
        var index = column.coerceIn(0, lineText.length)
        if (index == lineText.length) index -= 1
        if (index < 0) return null
        if (!lineText[index].isJavaIdentPart()) {
            if (index > 0 && lineText[index - 1].isJavaIdentPart()) index -= 1 else return null
        }
        var start = index
        while (start > 0 && lineText[start - 1].isJavaIdentPart()) start--
        var end = index + 1
        while (end < lineText.length && lineText[end].isJavaIdentPart()) end++
        return start to end
    }

    /** 在反编译/源码中定位方法名，尽量匹配声明行。 */
    private fun findMethodInJavaSource(sourceFile: File, methodName: String): Pair<Int, Int>? {
        val lines = runCatching { sourceFile.readLines() }.getOrNull() ?: return null
        val decl = Regex(
            """(?:^|[\s;.{}])(?:public|protected|private|static|final|native|synchronized|default|abstract|\s)+""" +
                """[\w.<>,\[\]\s]+\s+${Regex.escape(methodName)}\s*\("""
        )
        val loose = Regex("""\b${Regex.escape(methodName)}\s*\(""")
        var fallback: Pair<Int, Int>? = null
        for (i in lines.indices) {
            val text = lines[i]
            if (decl.containsMatchIn(text)) {
                val col = text.indexOf(methodName).coerceAtLeast(0)
                return i to col
            }
            if (fallback == null) {
                val at = text.indexOf(methodName)
                if (at >= 0 && loose.containsMatchIn(text) && !text.trimStart().startsWith("//")) {
                    fallback = i to at
                }
            }
        }
        return fallback
    }

    fun references(file: File, languageId: String, line: Int, column: Int): List<EditorLspLocation> {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return emptyList()
        val client = clientFor(file, languageId) ?: return emptyList()
        val uri = EditorLspUris.forFile(file)
        return EditorLspProtocol.parseLocations(
            client.references(uri, line, column, navigationTimeout(languageId))
        )
    }

    private fun navigationTimeout(languageId: String): Long {
        return when (languageId) {
            LANGUAGE_JAVA -> EditorJdtLsSupport.NAVIGATION_TIMEOUT_MILLIS
            else -> maxOf(settings.timeoutMillis, 8_000L)
        }
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
            ?: location.takeIf { it.file.isFile }
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

    /** 整文件格式化（LSP textDocument/formatting）。无改动时也返回 true。 */
    fun formatDocument(
        file: File,
        languageId: String,
        tabSize: Int = 4,
        insertSpaces: Boolean = true
    ): Boolean {
        if (!settings.enabled || !lspInstaller.isLanguageInstalled(languageId)) return false
        val client = clientFor(file, languageId) ?: return false
        val uri = EditorLspUris.forFile(file)
        val result = client.formatting(uri, tabSize, insertSpaces) ?: return false
        val edits = when (result) {
            is JSONArray -> EditorLspProtocol.parseTextEditArray(result)
            else -> emptyList()
        }
        if (edits.isEmpty()) return true
        applyFileEdits(listOf(file to edits))
        return true
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
        // ${1:default} / nested-ish simple form（多轮以处理浅层嵌套）
        var previous: String
        do {
            previous = result
            result = result.replace(Regex("\\$\\{\\d+:([^{}]*)}"), "$1")
        } while (result != previous)
        // ${1} / $1 / $0 → 空（光标位）
        result = result.replace(Regex("\\$\\{\\d+}"), "")
        result = result.replace(Regex("\\$\\d+"), "")
        // 常见残留：HashMap< > → HashMap<> 
        result = result.replace(Regex("<\\s+>"), "<>")
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
