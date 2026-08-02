package com.termux.zerocore.editor.lsp

import io.github.rosemoe.sora.text.CharPosition

/**
 * 仅在类/接口/枚举/匿名类成员层输入方法名时，提供「快捷创建方法」补全（置顶）。
 * 方法体、控制块内不提示。例如输入 `testfun` → 生成：
 * ```
 * public void testfun() {
 * }
 * ```
 */
object EditorJavaCreateMethodCompletions {

    const val SORT_PREFIX = "00_create_method"

    private val LINE_PATTERN = Regex("""^(\s*)([A-Za-z_$][\w$]*)$""")

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new",
        "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while", "true", "false", "null",
        "var", "record", "sealed", "permits", "non-sealed", "yield"
    )

    fun build(
        fullText: String,
        position: CharPosition,
        createMethodDetail: String
    ): List<EditorLspManager.CompletionCandidate> {
        val ctx = detectContext(fullText, position) ?: return emptyList()
        val indent = ctx.indent
        val name = ctx.name
        val bodyIndent = indent + "\t"
        val insertText = buildString {
            append("public void ")
            append(name)
            append("() {\n")
            append(bodyIndent)
            append('\n')
            append(indent)
            append('}')
        }
        return listOf(
            EditorLspManager.CompletionCandidate(
                label = "$name()",
                detail = createMethodDetail,
                insertText = insertText,
                startLine = position.line,
                startColumn = ctx.startColumn,
                endLine = position.line,
                endColumn = ctx.endColumn,
                sortText = "${SORT_PREFIX}_$name",
                filterText = name,
                prefixLength = name.length,
                languageId = EditorLspManager.LANGUAGE_JAVA,
                lspKind = 2 // Method
            )
        )
    }

    fun isCreateMethodCandidate(candidate: EditorLspManager.CompletionCandidate): Boolean {
        return candidate.sortText?.startsWith(SORT_PREFIX) == true
    }

    private data class Context(
        val name: String,
        val indent: String,
        val startColumn: Int,
        val endColumn: Int
    )

    private fun detectContext(fullText: String, position: CharPosition): Context? {
        val lineText = lineAt(fullText, position.line) ?: return null
        val column = position.column.coerceIn(0, lineText.length)
        val before = lineText.substring(0, column)
        val match = LINE_PATTERN.matchEntire(before) ?: return null
        val indent = match.groupValues[1]
        val name = match.groupValues[2]
        if (name.length < 1) return null
        if (name in JAVA_KEYWORDS) return null
        // 仅类/接口/枚举/匿名类体；方法、if/for、静态块等内部不提示
        if (!isInTypeBody(fullText, position)) return null
        // 行尾若已有代码（光标后非空白），不打扰
        val after = lineText.substring(column)
        if (after.any { !it.isWhitespace() }) return null
        return Context(
            name = name,
            indent = indent,
            startColumn = indent.length,
            endColumn = column
        )
    }

    private enum class BlockKind {
        /** class / interface / enum / record / 匿名类 */
        TYPE,
        /** 方法、构造器 */
        METHOD,
        /** if/for/try/static{} / lambda 等 */
        OTHER
    }

    private fun isInTypeBody(fullText: String, position: CharPosition): Boolean {
        val stack = ArrayDeque<BlockKind>()
        var line = 0
        var col = 0
        var i = 0
        var inLineComment = false
        var inBlockComment = false
        var inString = false
        var inChar = false
        fun advance(chars: Int = 1) {
            repeat(chars) {
                if (i >= fullText.length) return
                val ch = fullText[i]
                if (ch == '\n') {
                    line++
                    col = 0
                } else {
                    col++
                }
                i++
            }
        }
        while (i < fullText.length) {
            if (line > position.line || (line == position.line && col >= position.column)) break
            val c = fullText[i]
            val next = fullText.getOrNull(i + 1)
            when {
                inLineComment -> {
                    if (c == '\n') inLineComment = false
                    advance()
                }
                inBlockComment -> {
                    if (c == '*' && next == '/') {
                        inBlockComment = false
                        advance(2)
                    } else {
                        advance()
                    }
                }
                inString -> {
                    when {
                        c == '\\' && next != null -> advance(2)
                        c == '"' -> {
                            inString = false
                            advance()
                        }
                        else -> advance()
                    }
                }
                inChar -> {
                    when {
                        c == '\\' && next != null -> advance(2)
                        c == '\'' -> {
                            inChar = false
                            advance()
                        }
                        else -> advance()
                    }
                }
                c == '/' && next == '/' -> {
                    inLineComment = true
                    advance(2)
                }
                c == '/' && next == '*' -> {
                    inBlockComment = true
                    advance(2)
                }
                c == '"' -> {
                    inString = true
                    advance()
                }
                c == '\'' -> {
                    inChar = true
                    advance()
                }
                c == '{' -> {
                    stack.addLast(classifyOpeningBrace(fullText, i))
                    advance()
                }
                c == '}' -> {
                    if (stack.isNotEmpty()) stack.removeLast()
                    advance()
                }
                else -> advance()
            }
        }
        return stack.lastOrNull() == BlockKind.TYPE
    }

    private val TYPE_DECL = Regex("""\b(class|interface|enum|record)\b""")
    private val ANONYMOUS_NEW = Regex("""\bnew\b[^;{}]*\)\s*$""")
    private val METHOD_SIG = Regex("""\)\s*(throws\s+[\w.\s,]+)?\s*$""")

    private fun classifyOpeningBrace(fullText: String, braceIndex: Int): BlockKind {
        // 只看「当前声明」头部，避免 240 字符窗口里残留外层 class 把方法体误判成类体
        val sig = currentDeclarationHead(signatureWindowBefore(fullText, braceIndex))
        if (TYPE_DECL.containsMatchIn(sig)) return BlockKind.TYPE
        // new Foo(...) { ... }
        if (ANONYMOUS_NEW.containsMatchIn(sig)) return BlockKind.TYPE
        val trimmed = sig.trimEnd()
        if (trimmed.endsWith("->")) return BlockKind.OTHER
        if (METHOD_SIG.containsMatchIn(trimmed)) return BlockKind.METHOD
        return BlockKind.OTHER
    }

    private fun currentDeclarationHead(window: String): String {
        var cut = -1
        for (i in window.indices.reversed()) {
            when (window[i]) {
                '{', '}', ';' -> {
                    cut = i
                    break
                }
            }
        }
        return if (cut >= 0) window.substring(cut + 1) else window
    }

    /** 取 `{` 前一小段代码，去掉行注释，便于分类。 */
    private fun signatureWindowBefore(fullText: String, braceIndex: Int): String {
        val end = braceIndex.coerceIn(0, fullText.length)
        val start = (end - 240).coerceAtLeast(0)
        val raw = fullText.substring(start, end)
        return buildString(raw.length) {
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                val next = raw.getOrNull(i + 1)
                when {
                    c == '/' && next == '/' -> {
                        i += 2
                        while (i < raw.length && raw[i] != '\n') i++
                    }
                    c == '/' && next == '*' -> {
                        i += 2
                        while (i < raw.length - 1 && !(raw[i] == '*' && raw[i + 1] == '/')) i++
                        i = (i + 2).coerceAtMost(raw.length)
                    }
                    else -> {
                        append(c)
                        i++
                    }
                }
            }
        }
    }

    private fun lineAt(fullText: String, line: Int): String? {
        if (line < 0) return null
        var current = 0
        var start = 0
        var i = 0
        while (i < fullText.length) {
            if (fullText[i] == '\n') {
                if (current == line) return fullText.substring(start, i)
                current++
                start = i + 1
            }
            i++
        }
        return if (current == line) fullText.substring(start) else null
    }
}
