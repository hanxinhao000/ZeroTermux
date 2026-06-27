package com.termux.zerocore.ai.editor

interface ZtEditorAiHost {
    fun isEditorReady(): Boolean
    fun captureSnapshot(maxChars: Int): String
    fun insertAtCursor(text: String): String
    fun replaceRange(start: Int, end: Int, text: String): String
    fun replaceAll(text: String): String
    fun createEditorFile(path: String, content: String, open: Boolean): String
    fun openEditorFile(path: String): String
    fun saveCurrentEditorFile(): String
    fun listOpenEditorFiles(): String
    /** AI 面板输入前释放 CodeEditor 焦点，避免 IME 被编辑器占用 */
    fun releaseEditorInputForAiPanel()
    /** AI 面板关闭后恢复 CodeEditor 软键盘能力 */
    fun restoreEditorInputAfterAiPanel()

    fun isTerminalAvailable(): Boolean
    fun captureTerminalSnapshot(maxChars: Int): String
    fun sendTerminalText(text: String)
    fun sendTerminalKey(key: String)
}
