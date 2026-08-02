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

    /** 等同编辑器「运行」按钮：准备 build.sh、启动嵌入式 VNC（如需）、执行 ./build.sh。 */
    fun runBuildScriptForAi(): String

    /** @param tab `gui` 或 `terminal` */
    fun switchEditorDockTab(tab: String): String

    /** 当前打开且可编辑的文件绝对路径；预览/不可编辑时返回 null。 */
    fun getCurrentEditorFilePath(): String?

    /** 当前编辑器全文；不可用时返回 null。 */
    fun getCurrentEditorText(): String?

    /**
     * 修改当前打开文件前请求用户确认（异步，不可阻塞主线程）。
     * @param summary 变更摘要（新增/删除行数等）
     * @param diffBody 可滚动的行级 diff（红删绿增）
     * @param onResult 在主线程回调，参数 true 表示用户同意
     */
    fun requestCodeEditConfirmation(
        actionLabel: String,
        summary: String,
        diffBody: CharSequence,
        onResult: (approved: Boolean) -> Unit
    )
}
