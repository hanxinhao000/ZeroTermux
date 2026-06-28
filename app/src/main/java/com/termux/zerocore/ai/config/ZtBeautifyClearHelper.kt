package com.termux.zerocore.ai.config

import android.graphics.Color
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.view.TerminalRenderer
import com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder
import com.termux.zerocore.utils.FileIOUtils

/**
 * 与左侧菜单「清空美化」相同：清除 SaveData 美化项、背景图/视频，并重置终端 UI。
 * 手动菜单、美化对话框、AI、调试 API 共用此入口。
 */
object ZtBeautifyClearHelper {

    private val gson = Gson()

    /** 清空美化并刷新 UI；返回 JSON 供 AI / 调试 API 使用。 */
    fun clearAndApplyUi(): String {
        FileIOUtils.clearStyle()
        UUtils.getHandler().post { applyUiReset() }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "message" to "cleared beautify settings (same as menu 清空美化)"
            )
        )
    }

    private fun applyUiReset() {
        val activity = ZtForegroundActivityHolder.getTermuxActivity()
        if (activity != null) {
            activity.clearBeautifyStyle()
            return
        }
        applyStaticDefaults()
        ZtAiConfigSideEffects.applyBeautify(ZtBeautifyColorHelper.KEY_FONT_COLOR)
    }

    private fun applyStaticDefaults() {
        TerminalRenderer.COLOR_TEXT = Color.WHITE
        TerminalRenderer.TEXT_SHADOW_PROGRESS = 0
        ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR = Color.WHITE
    }
}
