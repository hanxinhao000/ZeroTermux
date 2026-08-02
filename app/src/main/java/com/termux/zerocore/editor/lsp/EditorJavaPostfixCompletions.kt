package com.termux.zerocore.editor.lsp

import io.github.rosemoe.sora.text.CharPosition
import java.util.Locale

/**
 * Client-side Java postfix completions (Android Studio / IDEA style).
 * Triggers after `expr.` for arrays, collections, and maps.
 */
object EditorJavaPostfixCompletions {

    private enum class ExprKind {
        ARRAY,
        LIST,
        COLLECTION,
        MAP,
        UNKNOWN
    }

    private data class PostfixContext(
        val expression: String,
        val typedPrefix: String,
        val startColumn: Int,
        val endColumn: Int,
        val indent: String,
        val kind: ExprKind
    )

    private data class Template(
        val label: String,
        val detail: String,
        val build: (expr: String, indent: String) -> String
    )

    private val MAP_TYPES = setOf(
        "map", "hashmap", "linkedhashmap", "treemap", "concurrenthashmap",
        "weakhashmap", "identityhashmap", "hashtable", "enummap",
        "sortedmap", "navigablemap", "concurrentnavigablemap"
    )

    private val LIST_TYPES = setOf(
        "list", "arraylist", "linkedlist", "vector", "stack",
        "copyonwritearraylist", "abstractlist"
    )

    private val COLLECTION_TYPES = setOf(
        "collection", "iterable", "set", "hashset", "treeset", "linkedhashset",
        "queue", "deque", "arraydeque", "priorityqueue", "blockingqueue",
        "concurrentlinkedqueue", "copyonwritearrayset", "enumset", "sortedset",
        "navigableset"
    )

    /** Labels we also generate; used to hide duplicate jdt-ls postfix items. */
    val CLIENT_LABELS = setOf("for", "fori", "forr", "fork", "forv")

    fun build(
        fullText: String,
        position: CharPosition
    ): List<EditorLspManager.CompletionCandidate> {
        val ctx = detectContext(fullText, position) ?: return emptyList()
        val prefix = ctx.typedPrefix.lowercase(Locale.ROOT)
        return templatesFor(ctx.kind)
            .filter { prefix.isEmpty() || it.label.lowercase(Locale.ROOT).startsWith(prefix) }
            .map { template ->
                EditorLspManager.CompletionCandidate(
                    label = template.label,
                    detail = template.detail,
                    insertText = template.build(ctx.expression, ctx.indent),
                    startLine = position.line,
                    startColumn = ctx.startColumn,
                    endLine = position.line,
                    endColumn = ctx.endColumn,
                    sortText = "0_postfix_${template.label}",
                    filterText = template.label,
                    prefixLength = ctx.typedPrefix.length,
                    languageId = EditorLspManager.LANGUAGE_JAVA,
                    lspKind = 15 // Snippet
                )
            }
    }

    private fun detectContext(fullText: String, position: CharPosition): PostfixContext? {
        val lineText = lineAt(fullText, position.line) ?: return null
        val column = position.column.coerceIn(0, lineText.length)
        val before = lineText.substring(0, column)
        val parsed = parsePostfix(before) ?: return null
        val (expression, typedPrefix, exprStart) = parsed
        if (expression.isBlank()) return null
        // Avoid type-name postfix like System. / String.
        if (expression.first().isUpperCase() &&
            !expression.contains('(') &&
            !expression.contains('[') &&
            expression.all { it.isLetterOrDigit() || it == '_' || it == '$' }
        ) {
            return null
        }
        val indent = before.takeWhile { it == ' ' || it == '\t' }
        return PostfixContext(
            expression = expression,
            typedPrefix = typedPrefix,
            startColumn = exprStart,
            endColumn = column,
            indent = indent,
            kind = inferKind(fullText, expression)
        )
    }

    private fun lineAt(fullText: String, line: Int): String? {
        if (line < 0) return null
        var current = 0
        var start = 0
        var i = 0
        while (i < fullText.length) {
            if (fullText[i] == '\n') {
                if (current == line) {
                    return fullText.substring(start, i)
                }
                current++
                start = i + 1
            }
            i++
        }
        return if (current == line) fullText.substring(start) else null
    }

    /**
     * Parse `expression.typedPrefix` at end of [before].
     * Supports simple chains: `a`, `a.b`, `list.get(0)`.
     */
    private fun parsePostfix(before: String): Triple<String, String, Int>? {
        if (before.isEmpty()) return null
        var index = before.length
        while (index > 0 && isIdentChar(before[index - 1])) {
            index--
        }
        val typedPrefix = before.substring(index)
        if (index == 0 || before[index - 1] != '.') return null
        val dotIndex = index - 1
        var start = dotIndex
        var paren = 0
        var bracket = 0
        while (start > 0) {
            val c = before[start - 1]
            when {
                c == ')' -> {
                    paren++
                    start--
                }
                c == '(' -> {
                    if (paren == 0) break
                    paren--
                    start--
                }
                c == ']' -> {
                    bracket++
                    start--
                }
                c == '[' -> {
                    if (bracket == 0) break
                    bracket--
                    start--
                }
                paren > 0 || bracket > 0 -> start--
                isIdentChar(c) || c == '.' -> start--
                else -> break
            }
        }
        val expression = before.substring(start, dotIndex).trim()
        if (expression.isEmpty()) return null
        // Must end with an identifier (not a trailing dot).
        if (!expression.last().isLetterOrDigit() && expression.last() != '_' &&
            expression.last() != '$' && expression.last() != ')' && expression.last() != ']'
        ) {
            return null
        }
        return Triple(expression, typedPrefix, start)
    }

    private fun isIdentChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '$'
    }

    private fun inferKind(fullText: String, expression: String): ExprKind {
        val simpleName = expression
            .substringAfterLast('.')
            .substringBefore('(')
            .substringBefore('[')
            .trim()
        if (simpleName.isEmpty()) return ExprKind.UNKNOWN
        val declType = findDeclaredType(fullText, simpleName)
        if (declType != null) {
            return kindFromType(declType)
        }
        val lower = simpleName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith("map") || lower.contains("map") -> ExprKind.MAP
            lower.endsWith("list") || lower.endsWith("array") -> ExprKind.LIST
            lower.endsWith("set") || lower.endsWith("queue") || lower.endsWith("collection") ->
                ExprKind.COLLECTION
            else -> ExprKind.UNKNOWN
        }
    }

    private fun findDeclaredType(fullText: String, varName: String): String? {
        val escaped = Regex.escape(varName)
        val patterns = listOf(
            Regex(
                """(?:(?:public|private|protected|static|final|volatile|transient)\s+)*([\w.$]+(?:\s*<[^;={]*>)?(?:\s*\[\s*\])+)\s+$escaped\b"""
            ),
            Regex(
                """(?:(?:public|private|protected|static|final|volatile|transient)\s+)*([\w.$]+(?:\s*<[^;={]*>)?)\s+$escaped\s*[=;,)\[]"""
            )
        )
        for (pattern in patterns) {
            val match = pattern.findAll(fullText).lastOrNull() ?: continue
            val type = match.groupValues[1].replace(Regex("\\s+"), "")
            if (type.isNotEmpty() && type != "return" && type != "instanceof") {
                return type
            }
        }
        return null
    }

    private fun kindFromType(type: String): ExprKind {
        if (type.contains("[]")) return ExprKind.ARRAY
        val raw = type.substringBefore('<')
            .substringAfterLast('.')
            .lowercase(Locale.ROOT)
        return when {
            raw in MAP_TYPES || raw.endsWith("map") -> ExprKind.MAP
            raw in LIST_TYPES || raw.endsWith("list") -> ExprKind.LIST
            raw in COLLECTION_TYPES -> ExprKind.COLLECTION
            else -> ExprKind.UNKNOWN
        }
    }

    private fun templatesFor(kind: ExprKind): List<Template> {
        return when (kind) {
            ExprKind.ARRAY, ExprKind.UNKNOWN -> arrayTemplates()
            ExprKind.LIST -> listTemplates()
            ExprKind.COLLECTION -> collectionTemplates()
            ExprKind.MAP -> mapTemplates()
        }
    }

    private fun arrayTemplates(): List<Template> = listOf(
        loop("for", "Iterate elements") { expr, indent ->
            "for (var item : $expr) {\n$indent\t\n$indent}"
        },
        loop("fori", "Iterate with index") { expr, indent ->
            "for (int i = 0; i < $expr.length; i++) {\n$indent\t\n$indent}"
        },
        loop("forr", "Iterate in reverse") { expr, indent ->
            "for (int i = $expr.length - 1; i >= 0; i--) {\n$indent\t\n$indent}"
        }
    )

    private fun listTemplates(): List<Template> = listOf(
        loop("for", "Iterate elements") { expr, indent ->
            "for (var item : $expr) {\n$indent\t\n$indent}"
        },
        loop("fori", "Iterate with index") { expr, indent ->
            "for (int i = 0; i < $expr.size(); i++) {\n$indent\t\n$indent}"
        },
        loop("forr", "Iterate in reverse") { expr, indent ->
            "for (int i = $expr.size() - 1; i >= 0; i--) {\n$indent\t\n$indent}"
        }
    )

    private fun collectionTemplates(): List<Template> = listOf(
        loop("for", "Iterate elements") { expr, indent ->
            "for (var item : $expr) {\n$indent\t\n$indent}"
        }
    )

    private fun mapTemplates(): List<Template> = listOf(
        loop("for", "Iterate entries") { expr, indent ->
            "for (var entry : $expr.entrySet()) {\n$indent\t\n$indent}"
        },
        loop("fork", "Iterate keys") { expr, indent ->
            "for (var key : $expr.keySet()) {\n$indent\t\n$indent}"
        },
        loop("forv", "Iterate values") { expr, indent ->
            "for (var value : $expr.values()) {\n$indent\t\n$indent}"
        }
    )

    private fun loop(
        label: String,
        detail: String,
        build: (expr: String, indent: String) -> String
    ): Template = Template(label, detail, build)
}
