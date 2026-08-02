package com.termux.zerocore.editor

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow

/**
 * 文档被整文件替换（如 AI replace_all）后行数骤减时，
 * 原版诊断气泡仍用旧 memorizedPosition 算坐标，会 IndexOutOfBounds 崩溃。
 */
class SafeEditorDiagnosticTooltipWindow(
    editor: CodeEditor
) : EditorDiagnosticTooltipWindow(editor) {

    init {
        eventManager.subscribeEvent<ContentChangeEvent> { _, _ ->
            // 文本变更后旧诊断坐标立刻失效
            runCatching { updateDiagnostic(null, null, null) }
        }
    }

    override fun isSelectionVisible(): Boolean {
        val selection = editor.cursor.left()
        if (!isPositionInDocument(selection.line, selection.column)) {
            return false
        }
        return try {
            super.isSelectionVisible()
        } catch (_: IndexOutOfBoundsException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun updateWindowPosition() {
        val selection = memorizedPosition
        if (selection == null || !isPositionInDocument(selection.line, selection.column)) {
            dismiss()
            return
        }
        try {
            super.updateWindowPosition()
        } catch (_: IndexOutOfBoundsException) {
            dismiss()
        } catch (_: Exception) {
            dismiss()
        }
    }

    private fun isPositionInDocument(line: Int, column: Int): Boolean {
        val text = editor.text ?: return false
        val lineCount = text.lineCount
        if (lineCount <= 0 || line < 0 || line >= lineCount) return false
        val colCount = runCatching { text.getColumnCount(line) }.getOrDefault(0)
        return column in 0..colCount
    }
}
