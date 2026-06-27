package com.termux.zerocore.ai.editor

import android.content.Context
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.SwitchDialog

object ZtEditorAiResetHelper {

    private var uiRefreshCallback: (() -> Unit)? = null

    fun registerUiRefreshCallback(callback: (() -> Unit)?) {
        uiRefreshCallback = callback
    }

    fun showResetConfirmDialog(context: Context, onReset: (() -> Unit)? = null) {
        val dialog = SwitchDialog(context)
        dialog.createSwitchDialog(context.getString(R.string.zt_editor_ai_reset_dialog_message))
        dialog.title?.text = context.getString(R.string.zt_editor_ai_reset_dialog_title)
        dialog.cancel?.setOnClickListener { dialog.dismiss() }
        dialog.ok?.setOnClickListener {
            resetChatHistory()
            UUtils.showMsg(context.getString(R.string.zt_editor_ai_reset_done))
            onReset?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }

    /** 仅清空编辑器 AI 对话历史，不影响智能体 AI 与 API 配置 */
    fun resetChatHistory() {
        ZtEditorAiChatStore.clear()
        uiRefreshCallback?.invoke()
    }
}
