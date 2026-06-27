package com.termux.zerocore.ai.agent

object ZtTerminalAiSnapshot {

    private const val DEFAULT_MAX_CHARS = 8000
    private const val MIN_MAX_CHARS = 500
    private const val MAX_MAX_CHARS = 12000
    private const val SCROLLBACK_TAIL_CHARS = 1800
    private const val PROMPT_LINE_REGEX = """^[\w@.:~/$\-\[\]()]+[$#]\s*$"""

    fun format(visible: String, full: String, maxChars: Int = DEFAULT_MAX_CHARS): String {
        val visibleText = visible.trim()
        val fullText = full.trim()
        if (visibleText.isEmpty() && fullText.isEmpty()) {
            return "(terminal is empty)"
        }
        val limit = maxChars.coerceIn(MIN_MAX_CHARS, MAX_MAX_CHARS)
        val visibleBlock = if (visibleText.isNotEmpty()) {
            trimToMax(visibleText, limit / 2)
        } else {
            trimToMax(fullText, limit / 2)
        }
        val scrollTail = if (fullText.isNotEmpty()) {
            trimToMax(fullText, SCROLLBACK_TAIL_CHARS)
        } else {
            ""
        }
        val lastLine = visibleText.lineSequence().lastOrNull { it.isNotBlank() }?.trim()
            ?: fullText.lineSequence().lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        val idle = isShellPrompt(lastLine)
        return buildString {
            appendLine("=== 终端快照（必须以此为准，禁止编造；忽略对话历史里的旧终端描述） ===")
            appendLine("末行: $lastLine")
            appendLine(
                if (idle) {
                    "状态: 空闲（shell 提示符已出现）"
                } else {
                    "状态: 可能有命令在运行，或尚未回到提示符"
                }
            )
            appendLine("--- 当前可见屏幕（与用户肉眼所见一致） ---")
            appendLine(visibleBlock)
            if (scrollTail.isNotEmpty() && scrollTail != visibleBlock) {
                appendLine("--- scrollback 末尾（参考，以可见屏幕为准） ---")
                appendLine(scrollTail)
            }
        }.trim()
    }

    fun isShellPrompt(line: String): Boolean {
        if (line.isEmpty()) return false
        if (line.endsWith("$") || line.endsWith("#")) return true
        return line.matches(Regex(PROMPT_LINE_REGEX))
    }

    fun trimToMax(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        return "...(truncated, showing last $maxChars chars)\n" +
            text.substring(text.length - maxChars)
    }
}
