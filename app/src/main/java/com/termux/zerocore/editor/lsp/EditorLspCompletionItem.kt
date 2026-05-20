package com.termux.zerocore.editor.lsp

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class EditorLspCompletionItem(
    private val candidate: EditorLspManager.CompletionCandidate
) : CompletionItem(candidate.label, candidate.detail) {

    init {
        prefixLength = candidate.prefixLength
        sortText = candidate.sortText
        filterText = candidate.filterText
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        val startLine = candidate.startLine.coerceIn(0, text.lineCount - 1)
        var endLine = candidate.endLine.coerceIn(0, text.lineCount - 1)
        val startColumn = candidate.startColumn.coerceIn(0, text.getColumnCount(startLine))
        var endColumn = candidate.endColumn.coerceIn(0, text.getColumnCount(endLine))
        if (endLine < startLine || (endLine == startLine && endColumn < startColumn)) {
            endLine = startLine
            endColumn = startColumn
        }
        text.replace(startLine, startColumn, endLine, endColumn, candidate.insertText)
    }
}
