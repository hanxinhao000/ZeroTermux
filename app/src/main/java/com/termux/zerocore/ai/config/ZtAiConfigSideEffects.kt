package com.termux.zerocore.ai.config

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.xh_lib.utils.UUtils
import com.termux.app.TermuxActivity
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.config.mainmenu.MainMenuPackageManager
import com.termux.zerocore.config.ztcommand.ZTSocketService
import com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder

/** 配置写入后的即时副作用（尽量与 UI 设置页行为一致）。 */
object ZtAiConfigSideEffects {

    /** LocalBroadcast → TermuxActivity.messageReceiver → initColorConfig() */
    const val RELOAD_BEAUTIFY_MESSAGE = "reload_beautify"

    /** LocalBroadcast → TermuxActivity.messageReceiver → ZtBeautifyUiEffects.applyToActivity() */
    const val RELOAD_BEAUTIFY_UI_MESSAGE = "reload_beautify_ui"

    fun requestBeautifyUiReload() {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.getTermuxActivity()
            if (activity != null) {
                ZtBeautifyUiEffects.applyToActivity(activity)
                return@post
            }
            val intent = Intent(ZTSocketService.ZT_COMMAND_ACTIVITY_ACTION)
            intent.putExtra("message", RELOAD_BEAUTIFY_UI_MESSAGE)
            LocalBroadcastManager.getInstance(UUtils.getContext()).sendBroadcast(intent)
        }
    }

    fun apply(key: String, bean: ZTUserBean) {
        when (key) {
            "isShowCommand" -> {
                val cmd = if (bean.isShowCommand) "x11commandshow" else "x11commandhide"
                ZtAiZtSocketClient.send(cmd)
            }
            "isInternalPassage" -> {
                // 完整切换需安装通道文件，提示用户重启；bean 已持久化供下次启动
            }
            "isSnowflakeShow", "isRainShow" -> {
                ZtBeautifyUiEffects.applyUserSetting(key, when (key) {
                    "isSnowflakeShow" -> bean.isSnowflakeShow
                    else -> bean.isRainShow
                })
            }
        }
    }

    fun applyBeautify(key: String) {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.get()
            if (activity is TermuxActivity) {
                activity.initColorConfig()
                activity.getTerminalView()?.invalidate()
            }
            val intent = Intent(ZTSocketService.ZT_COMMAND_ACTIVITY_ACTION)
            intent.putExtra("message", RELOAD_BEAUTIFY_MESSAGE)
            LocalBroadcastManager.getInstance(UUtils.getContext()).sendBroadcast(intent)
        }
    }

    /** AI 创建/更新可配置菜单包后刷新 Menu 列表；若已切换或正在编辑该包则同步刷新左侧菜单。 */
    fun refreshMenuPackagesAfterAiUpdate(switched: Boolean, targetPackageId: String? = null) {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.getTermuxActivity() ?: return@post
            val activeId = MainMenuPackageManager.getActivePackageId(activity)
            val refreshMain = switched || (targetPackageId != null && activeId == targetPackageId)
            if (refreshMain) {
                activity.refreshMainMenuPublic()
            }
            activity.refreshMenuPackageListPublic()
        }
    }
}
