package com.termux.zerocore.editor.lsp

/**
 * Java 补全插入增强：jdt-ls 在客户端未声明 snippetSupport 时，
 * `new HashMap` 常只插入类型名；即使开启 snippet，部分项仍可能缺括号。
 */
object EditorJavaCompletionEnrichment {
    /** LSP CompletionItemKind */
    private const val KIND_METHOD = 2
    private const val KIND_CONSTRUCTOR = 4
    private const val KIND_CLASS = 7
    private const val KIND_SNIPPET = 15

    /**
     * @param linePrefix 光标前本行文本（含将被替换的前缀）
     * @param lineSuffix 光标后本行文本
     * @return 可能追加了 `()` / `<>()` / `;` 的插入文本，以及建议光标落在左括号后的偏移（相对 insert 开头）；无括号时 offset=-1
     */
    fun enrichNewExpression(
        insertText: String,
        linePrefix: String,
        lineSuffix: String,
        lspKind: Int?
    ): Pair<String, Int> {
        val trimmedInsert = insertText
        if (trimmedInsert.isBlank()) return trimmedInsert to -1
        if (!isAfterNewKeyword(linePrefix)) {
            return trimmedInsert to findParenCursorOffset(trimmedInsert)
        }
        // 方法补全一般已带 ()；类/构造/纯类型名需要补
        val kindOk = lspKind == null ||
            lspKind == KIND_CLASS ||
            lspKind == KIND_CONSTRUCTOR ||
            lspKind == KIND_SNIPPET ||
            // 偶发以 Method 返回无参构造
            (lspKind == KIND_METHOD && !trimmedInsert.contains('('))
        if (!kindOk) {
            return trimmedInsert to findParenCursorOffset(trimmedInsert)
        }

        var text = trimmedInsert
        if (!text.contains('(')) {
            text = when {
                text.endsWith(">") -> text + "()"
                // 简单类型名：优先菱形（左侧已有泛型时更自然）
                looksLikeSimpleTypeName(text) -> text + "<>()"
                else -> text + "()"
            }
        }

        val suffix = lineSuffix.trimStart()
        val needsSemicolon = suffix.isEmpty() || suffix.startsWith("//") || suffix.startsWith("/*")
        if (needsSemicolon && !text.trimEnd().endsWith(";") && !suffix.startsWith(";")) {
            // 赋值语句或行首 new … 才自动加 ;
            if (isAssignmentNew(linePrefix) || isStatementNew(linePrefix)) {
                text = text.trimEnd() + ";"
            }
        }
        return text to findParenCursorOffset(text)
    }

    private fun isAfterNewKeyword(linePrefix: String): Boolean {
        // … = new Hash|   或   new Hash|
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
        // HashMap / java.util.HashMap / HashMap<>
        return Regex("""^[\w$.]+(<>)?$""").matches(t)
    }

    /** 光标落到第一个 `(` 之后，便于继续填参。 */
    fun findParenCursorOffset(insertText: String): Int {
        val open = insertText.indexOf('(')
        return if (open >= 0) open + 1 else -1
    }
}
