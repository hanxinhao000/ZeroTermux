package com.termux.zerocore.ai.agent

import com.example.xh_lib.utils.UUtils
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.zerocore.ai.config.ZtAiStrings

object ZtTerminalAiSnapshot {

    private const val DEFAULT_MAX_CHARS = 8000
    private const val MIN_MAX_CHARS = 500
    private const val MAX_MAX_CHARS = 12000
    private const val SCROLLBACK_TAIL_CHARS = 1800
    private const val PROMPT_LINE_REGEX = """^[\w@.:~/$\-\[\]()]+[$#]\s*$"""

    private val CONFIRMATION_REGEXES = listOf(
        Regex("""\([Yy]/[Nn]\)"""),
        Regex("""\[[Yy]/[Nn]\]"""),
        Regex("""(?i)\(yes/no\)"""),
        Regex("""(?i)\[yes/no\]"""),
        Regex("""(?i)do you want to continue"""),
        Regex("""(?i)proceed.*\?"""),
        Regex("""(?i)continue\s*\?\s*$"""),
        Regex("""(?i)are you sure"""),
        Regex("""是否继续"""),
        Regex("""[是否].*[Yy]/[Nn]"""),
        Regex("""(?i)press .* to continue"""),
        Regex("""(?i)\(y/N\)"""),
        Regex("""(?i)\(Y/n\)""")
    )

    fun isToolbarHidden(): Boolean {
        return TermuxAppSharedPreferences.build(UUtils.getContext(), false)
            ?.shouldShowTerminalToolbar() == false
    }

    fun toolbarHiddenNotice(): String? {
        if (!isToolbarHidden()) return null
        return ZtAiStrings.terminalToolbarHiddenNotice()
    }

    fun format(visible: String, full: String, maxChars: Int = DEFAULT_MAX_CHARS): String {
        val visibleText = visible.trim()
        val fullText = full.trim()
        if (visibleText.isEmpty() && fullText.isEmpty()) {
            return ZtAiStrings.str(com.termux.R.string.zt_ai_terminal_empty)
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
        val statusLine = resolveStatusLine(lastLine, visibleText)
        return buildString {
            appendLine(ZtAiStrings.terminalSnapshotHeader())
            toolbarHiddenNotice()?.let { appendLine(it) }
            appendLine(ZtAiStrings.terminalSnapshotLastLine(lastLine))
            appendLine(statusLine)
            appendLine(ZtAiStrings.terminalSnapshotVisibleHeader())
            appendLine(visibleBlock)
            if (scrollTail.isNotEmpty() && scrollTail != visibleBlock) {
                appendLine(ZtAiStrings.terminalSnapshotScrollbackHeader())
                appendLine(scrollTail)
            }
        }.trim()
    }

    fun resolveStatusLine(lastLine: String, visibleText: String): String {
        return when {
            isShellPrompt(lastLine) -> ZtAiStrings.terminalStatusIdle()
            isConfirmationPrompt(lastLine, visibleText) -> ZtAiStrings.terminalStatusAwaitingConfirmation()
            else -> ZtAiStrings.terminalStatusRunning()
        }
    }

    fun isConfirmationPrompt(lastLine: String, visibleText: String = ""): Boolean {
        val lines = buildList {
            add(lastLine.trim())
            visibleText.split('\n')
                .map { line -> line.trim() }
                .filter { line -> line.isNotBlank() }
                .takeLast(5)
                .forEach { line -> add(line) }
        }
        return lines.distinct().any { line ->
            CONFIRMATION_REGEXES.any { regex -> regex.containsMatchIn(line) }
        }
    }

    fun isShellPrompt(line: String): Boolean {
        if (line.isEmpty()) return false
        if (isConfirmationPrompt(line)) return false
        if (line.endsWith("$") || line.endsWith("#")) return true
        return line.matches(Regex(PROMPT_LINE_REGEX))
    }

    fun trimToMax(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        return ZtAiStrings.str(com.termux.R.string.zt_ai_terminal_truncated).format(maxChars) +
            text.substring(text.length - maxChars)
    }
}
