package com.termux.zerocore.editor.lsp

/**
 * Java 补全插入增强：jdt-ls 在客户端未声明 snippetSupport 时，
 * 常只插入裸标识符（`HashMap` / `println`），缺少 `()` 与 `;`。
 */
object EditorJavaCompletionEnrichment {
    /** LSP CompletionItemKind */
    private const val KIND_METHOD = 2
    private const val KIND_FUNCTION = 3
    private const val KIND_CONSTRUCTOR = 4
    private const val KIND_FIELD = 5
    private const val KIND_VARIABLE = 6
    private const val KIND_CLASS = 7
    private const val KIND_SNIPPET = 15

    /**
     * @param linePrefix 光标前本行文本
     * @param lineSuffix textEdit 终点之后的本行文本
     * @return 增强后的插入文本，以及建议光标相对 insert 开头的偏移（落在第一个 `(` 后；无括号为 -1）
     */
    fun enrichInsert(
        insertText: String,
        linePrefix: String,
        lineSuffix: String,
        lspKind: Int?
    ): Pair<String, Int> {
        val raw = insertText
        if (raw.isBlank()) return raw to -1

        if (isAfterNewKeyword(linePrefix)) {
            return enrichNewExpression(raw, linePrefix, lineSuffix, lspKind)
        }
        if (isMethodLikeCompletion(raw, lspKind)) {
            return enrichMethodCall(raw, linePrefix, lineSuffix)
        }
        return raw to findParenCursorOffset(raw)
    }

    /** @deprecated 使用 [enrichInsert] */
    fun enrichNewExpression(
        insertText: String,
        linePrefix: String,
        lineSuffix: String,
        lspKind: Int?
    ): Pair<String, Int> {
        val trimmedInsert = insertText
        if (trimmedInsert.isBlank()) return trimmedInsert to -1

        val kindOk = lspKind == null ||
            lspKind == KIND_CLASS ||
            lspKind == KIND_CONSTRUCTOR ||
            lspKind == KIND_SNIPPET ||
            (lspKind == KIND_METHOD && !trimmedInsert.contains('('))
        if (!kindOk) {
            return trimmedInsert to findParenCursorOffset(trimmedInsert)
        }

        var text = trimmedInsert
        if (!text.contains('(')) {
            text = when {
                text.endsWith(">") -> text + "()"
                looksLikeSimpleTypeName(text) -> text + "<>()"
                else -> text + "()"
            }
        }
        text = maybeAppendSemicolon(text, linePrefix, lineSuffix, forNewExpression = true)
        return text to findParenCursorOffset(text)
    }

    private fun enrichMethodCall(
        insertText: String,
        linePrefix: String,
        lineSuffix: String
    ): Pair<String, Int> {
        var text = insertText.trim()
        // 若 insert 误带了签名参数表 println(String) → 只保留到方法名再加 ()
        if ('(' in text && !text.contains("()")) {
            val nameOnly = text.substringBefore('(').trim()
            if (looksLikeSimpleIdentifier(nameOnly)) {
                text = nameOnly
            }
        }
        if (!text.contains('(')) {
            text += "()"
        }
        text = maybeAppendSemicolon(text, linePrefix, lineSuffix, forNewExpression = false)
        return text to findParenCursorOffset(text)
    }

    private fun isMethodLikeCompletion(
        insertText: String,
        lspKind: Int?
    ): Boolean {
        // 仅在明确是方法/函数时补 ()；Field（如 System.out）绝不能当成方法。
        // kind 缺失时也不要猜：成员访问里既有 out/in 字段，也有 println 方法。
        return when (lspKind) {
            KIND_METHOD, KIND_FUNCTION -> looksLikeSimpleIdentifier(
                insertText.trim().substringBefore('(')
            )
            else -> false
        }
    }

    private fun maybeAppendSemicolon(
        text: String,
        linePrefix: String,
        lineSuffix: String,
        forNewExpression: Boolean
    ): String {
        if (text.trimEnd().endsWith(";")) return text
        val suffix = lineSuffix.trimStart()
        if (suffix.startsWith(";")) return text
        val needsSemicolon = suffix.isEmpty() || suffix.startsWith("//") || suffix.startsWith("/*")
        if (!needsSemicolon) return text
        // 仍在外层未闭合的 ( 内（如 foo(bar.println|））不加 ;
        if (unclosedParenCount(linePrefix) > 0) return text
        if (forNewExpression) {
            if (!(isAssignmentNew(linePrefix) || isStatementNew(linePrefix))) return text
        }
        return text.trimEnd() + ";"
    }

    private fun unclosedParenCount(linePrefix: String): Int {
        var n = 0
        for (c in linePrefix) {
            when (c) {
                '(' -> n++
                ')' -> n = (n - 1).coerceAtLeast(0)
            }
        }
        return n
    }

    private fun isAfterNewKeyword(linePrefix: String): Boolean {
        return Regex("""\bnew\s+[\w$.]*$""").containsMatchIn(linePrefix)
    }

    private fun isAssignmentNew(linePrefix: String): Boolean {
        return Regex("""=\s*new\s+[\w$.]*$""").containsMatchIn(linePrefix)
    }

    private fun isStatementNew(linePrefix: String): Boolean {
        val trimmed = linePrefix.trimStart()
        return Regex("""^new\s+[\w$.]*$""").containsMatchIn(trimmed)
    }

    private fun looksLikeSimpleTypeName(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty() || t.contains('(') || t.contains('{') || t.contains(';')) return false
        return Regex("""^[\w$.]+(<>)?$""").matches(t)
    }

    private fun looksLikeSimpleIdentifier(text: String): Boolean {
        val t = text.trim()
        return t.isNotEmpty() && Regex("""^[\w$]+$""").matches(t)
    }

    /** 光标落到第一个 `(` 之后，便于继续填参。 */
    fun findParenCursorOffset(insertText: String): Int {
        val open = insertText.indexOf('(')
        return if (open >= 0) open + 1 else -1
    }
}
