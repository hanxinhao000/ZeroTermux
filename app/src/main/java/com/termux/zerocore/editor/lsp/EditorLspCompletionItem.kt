package com.termux.zerocore.editor.lsp

import com.termux.zerocore.editor.EditorIdeaKindIcons
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class EditorLspCompletionItem(
    private val candidate: EditorLspManager.CompletionCandidate,
    private val lspManager: EditorLspManager
) : CompletionItem(candidate.label, candidate.detail) {

    init {
        prefixLength = candidate.prefixLength
        sortText = candidate.sortText
        filterText = candidate.filterText
        val mapped = when {
            EditorJavaCreateMethodCompletions.isCreateMethodCandidate(candidate) ->
                CompletionItemKind.Method
            candidate.label in EditorJavaPostfixCompletions.CLIENT_LABELS ||
                candidate.sortText?.startsWith("0_postfix_") == true -> CompletionItemKind.Snippet
            else -> EditorIdeaKindIcons.mapLspKind(candidate.lspKind)
        }
        if (mapped != null) {
            kind(mapped)
        }
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        // 延迟生成图标，避免无 Context 时创建
        if (icon == null && kind != null) {
            icon(EditorIdeaKindIcons.create(editor.context, kind))
        }
        val resolved = runCatching { lspManager.resolveCompletionCandidate(candidate) }.getOrDefault(candidate)
        val mainEdit = EditorLspManager.LspTextEdit(
            startLine = resolved.startLine,
            startColumn = resolved.startColumn,
            endLine = resolved.endLine,
            endColumn = resolved.endColumn,
            newText = resolved.insertText
        )
        // 从后往前应用，避免前面的 import 行位移打乱后面的光标编辑
        val edits = (resolved.additionalEdits + mainEdit).sortedWith(
            compareByDescending<EditorLspManager.LspTextEdit> { it.startLine }
                .thenByDescending { it.startColumn }
        )
        for (edit in edits) {
            applyEdit(text, edit)
        }
        // Postfix / 新建方法：光标落到方法体空行
        if (resolved.sortText?.startsWith("0_postfix_") == true ||
            EditorJavaCreateMethodCompletions.isCreateMethodCandidate(resolved)
        ) {
            placeCursorInsideBlock(editor, resolved)
        }
    }

    private fun placeCursorInsideBlock(editor: CodeEditor, resolved: EditorLspManager.CompletionCandidate) {
        val insert = resolved.insertText
        val open = insert.indexOf("{\n")
        if (open < 0) return
        val prefix = insert.substring(0, open + 2)
        val lineOffset = prefix.count { it == '\n' }
        // 空行上通常已有一个 tab
        val bodyLine = insert.substring(open + 2).substringBefore('\n')
        val targetLine = (resolved.startLine + lineOffset).coerceAtMost(editor.text.lineCount - 1)
        val targetCol = bodyLine.length.coerceAtMost(editor.text.getColumnCount(targetLine))
        editor.setSelection(targetLine, targetCol)
    }

    private fun applyEdit(text: Content, edit: EditorLspManager.LspTextEdit) {
        val startLine = edit.startLine.coerceIn(0, text.lineCount - 1)
        var endLine = edit.endLine.coerceIn(0, text.lineCount - 1)
        val startColumn = edit.startColumn.coerceIn(0, text.getColumnCount(startLine))
        var endColumn = edit.endColumn.coerceIn(0, text.getColumnCount(endLine))
        if (endLine < startLine || (endLine == startLine && endColumn < startColumn)) {
            endLine = startLine
            endColumn = startColumn
        }
        text.replace(startLine, startColumn, endLine, endColumn, edit.newText)
    }
}
