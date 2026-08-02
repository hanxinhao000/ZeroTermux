package com.termux.zerocore.editor.lsp

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class EditorLspLocation(
    val file: File,
    val line: Int,
    val column: Int,
    /** 原始 LSP URI；jdt:// 或 *.class 时用于拉取反编译/附着源码。 */
    val uri: String = ""
)

data class EditorLspCodeAction(
    val title: String,
    val kind: String?,
    /** 按文件分组的文本编辑；可能为空（仅 command）。 */
    val fileEdits: List<Pair<File, List<EditorLspManager.LspTextEdit>>>,
    val command: JSONObject?
)

object EditorLspProtocol {
    fun parseHoverText(result: Any?): String? {
        val obj = result as? JSONObject ?: return null
        val contents = obj.opt("contents") ?: return null
        val text = when (contents) {
            is String -> contents
            is JSONObject -> {
                contents.optString("value").ifBlank {
                    contents.optString("language").let { lang ->
                        val value = contents.optString("value")
                        if (lang.isNotBlank() && value.isNotBlank()) "```$lang\n$value\n```" else value
                    }
                }
            }
            is JSONArray -> {
                buildString {
                    for (i in 0 until contents.length()) {
                        val part = contents.opt(i) ?: continue
                        val piece = when (part) {
                            is String -> part
                            is JSONObject -> part.optString("value")
                            else -> part.toString()
                        }.trim()
                        if (piece.isEmpty()) continue
                        if (isNotEmpty()) append("\n\n")
                        append(piece)
                    }
                }
            }
            else -> contents.toString()
        }.trim()
        return text.takeIf { it.isNotEmpty() }?.let { stripSimpleMarkdown(it) }
    }

    fun parseLocations(result: Any?): List<EditorLspLocation> {
        if (result == null || result == JSONObject.NULL) return emptyList()
        val out = ArrayList<EditorLspLocation>()
        when (result) {
            is JSONObject -> parseOneLocation(result)?.let { out.add(it) }
            is JSONArray -> {
                for (i in 0 until result.length()) {
                    parseOneLocation(result.optJSONObject(i))?.let { out.add(it) }
                }
            }
        }
        return out
    }

    fun parseCodeActions(result: Any?): List<EditorLspCodeAction> {
        val array = when (result) {
            is JSONArray -> result
            null, JSONObject.NULL -> return emptyList()
            else -> return emptyList()
        }
        val out = ArrayList<EditorLspCodeAction>()
        for (i in 0 until array.length()) {
            val item = array.opt(i) ?: continue
            when (item) {
                is String -> continue
                is JSONObject -> {
                    // Command-only: { title, command, arguments }
                    if (item.has("command") && item.opt("command") is String) {
                        out.add(
                            EditorLspCodeAction(
                                title = item.optString("title").ifBlank { item.optString("command") },
                                kind = item.optString("kind").takeIf { it.isNotBlank() },
                                fileEdits = emptyList(),
                                command = item
                            )
                        )
                        continue
                    }
                    val title = item.optString("title").ifBlank { "Code Action" }
                    val kind = item.optString("kind").takeIf { it.isNotBlank() }
                    val edit = item.optJSONObject("edit")
                    val fileEdits = if (edit != null) parseWorkspaceEdit(edit) else emptyList()
                    val command = when (val cmd = item.opt("command")) {
                        is JSONObject -> cmd
                        is String -> JSONObject().put("command", cmd).put("title", title)
                        else -> null
                    }
                    if (fileEdits.isEmpty() && command == null) continue
                    out.add(EditorLspCodeAction(title, kind, fileEdits, command))
                }
            }
        }
        return out
    }

    fun parseWorkspaceEdit(edit: JSONObject): List<Pair<File, List<EditorLspManager.LspTextEdit>>> {
        val byFile = LinkedHashMap<String, ArrayList<EditorLspManager.LspTextEdit>>()
        val changes = edit.optJSONObject("changes")
        if (changes != null) {
            val keys = changes.keys()
            while (keys.hasNext()) {
                val uri = keys.next()
                val edits = parseTextEditArray(changes.optJSONArray(uri))
                if (edits.isNotEmpty()) {
                    byFile.getOrPut(uri) { ArrayList() }.addAll(edits)
                }
            }
        }
        val documentChanges = edit.optJSONArray("documentChanges")
        if (documentChanges != null) {
            for (i in 0 until documentChanges.length()) {
                val change = documentChanges.optJSONObject(i) ?: continue
                val doc = change.optJSONObject("textDocument")
                val uri = doc?.optString("uri").orEmpty().ifBlank { change.optString("uri") }
                if (uri.isBlank()) continue
                val edits = parseTextEditArray(change.optJSONArray("edits"))
                if (edits.isNotEmpty()) {
                    byFile.getOrPut(uri) { ArrayList() }.addAll(edits)
                }
            }
        }
        return byFile.mapNotNull { (uri, edits) ->
            val path = EditorLspUris.pathOf(uri)
            if (path.isEmpty()) null else File(path) to edits
        }
    }

    fun parseTextEditArray(array: JSONArray?): List<EditorLspManager.LspTextEdit> {
        if (array == null) return emptyList()
        val out = ArrayList<EditorLspManager.LspTextEdit>(array.length())
        for (i in 0 until array.length()) {
            val edit = array.optJSONObject(i) ?: continue
            val range = edit.optJSONObject("range") ?: continue
            val start = range.optJSONObject("start") ?: continue
            val end = range.optJSONObject("end") ?: continue
            out.add(
                EditorLspManager.LspTextEdit(
                    startLine = start.optInt("line", 0),
                    startColumn = start.optInt("character", 0),
                    endLine = end.optInt("line", 0),
                    endColumn = end.optInt("character", 0),
                    newText = edit.optString("newText", "")
                )
            )
        }
        return out
    }

    private fun parseOneLocation(obj: JSONObject?): EditorLspLocation? {
        if (obj == null) return null
        // LocationLink: targetUri + targetSelectionRange / targetRange
        val uri = obj.optString("uri").ifBlank {
            obj.optString("targetUri")
        }
        if (uri.isBlank()) return null
        val range = obj.optJSONObject("range")
            ?: obj.optJSONObject("targetSelectionRange")
            ?: obj.optJSONObject("targetRange")
            ?: return null
        val start = range.optJSONObject("start") ?: return null
        val line = start.optInt("line", 0)
        val column = start.optInt("character", 0)
        // jdt:// 或 jar 内 .class：保留 URI，稍后经 java/classFileContents 打开源码
        if (EditorJdtClassFileSupport.isClassContentUri(uri)) {
            return EditorLspLocation(
                file = EditorJdtClassFileSupport.cacheFileFor(uri),
                line = line,
                column = column,
                uri = uri
            )
        }
        val path = EditorLspUris.pathOf(uri)
        if (path.isEmpty()) return null
        return EditorLspLocation(
            file = File(path),
            line = line,
            column = column,
            uri = uri
        )
    }

    private fun stripSimpleMarkdown(text: String): String {
        return text
            .replace(Regex("```[a-zA-Z0-9_+-]*\\n?"), "")
            .replace("```", "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .trim()
    }
}
