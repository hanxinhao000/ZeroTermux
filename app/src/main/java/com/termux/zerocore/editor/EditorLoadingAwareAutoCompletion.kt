package com.termux.zerocore.editor

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion

/**
 * 补全请求较慢时（如 JDT 首次 completion）先弹出悬浮窗并显示加载条，
 * 等 Language 推送结果后再填入列表。
 */
class EditorLoadingAwareAutoCompletion(
    private val codeEditor: CodeEditor
) : EditorAutoCompletion(codeEditor) {

    override fun requireCompletion() {
        super.requireCompletion()
        // super 已 setLoading(true) 并启动分析线程，但窗口要等首批 item 才 show。
        // 慢 LSP 场景先亮窗，让 ProgressBar 可见。
        codeEditor.postInLifecycle {
            if (!isEnabled || !isCompletionInProgress || isShowing) return@postInLifecycle
            updateCompletionWindowPosition()
            val minHeight = (codeEditor.dpUnit * 56f).toInt()
            if (height < minHeight) {
                val w = if (width > 0) width else (codeEditor.dpUnit * 240f).toInt()
                setSize(w, minHeight.coerceAtMost((codeEditor.dpUnit * 200f).toInt()))
            }
            show()
        }
    }
}
