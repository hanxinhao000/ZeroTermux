package com.termux.zerocore.ai.config

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.config.mainmenu.config.SnowflakeClickConfig
import com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder
import com.termux.zerocore.ftp.utils.UserSetManage

/**
 * AI 改 isSnowflakeShow / isRainShow 后，按 ZTUserBean 直接同步终端特效 UI（不再走 onClick 切换）。
 * setConfig 已写入 bean；此处只负责 firework / 雪花 View 与菜单状态一致。
 */
object ZtBeautifyUiEffects {

    @JvmStatic
    fun applyUserSetting(@Suppress("UNUSED_PARAMETER") key: String, @Suppress("UNUSED_PARAMETER") desired: Boolean) {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.getTermuxActivity()
            if (activity != null) {
                applyToActivity(activity)
            } else {
                ZtAiConfigSideEffects.requestBeautifyUiReload()
            }
        }
    }

    /** TermuxActivity 收到 reload_beautify_ui 或前台恢复时调用。 */
    @JvmStatic
    fun applyToActivity(activity: TermuxActivity) {
        syncBeautifyUiFromBean(activity)
        activity.refreshMainMenuPublic()
        scheduleResync(activity)
    }

    @JvmStatic
    fun syncBeautifyUiFromBean(activity: TermuxActivity) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isRainShow && bean.isSnowflakeShow) {
            bean.isSnowflakeShow = false
            UserSetManage.get().setZTUserBean(bean)
        }
        if (bean.isRainShow) {
            activity.xue_fragment?.removeAllViews()
            setFireworkVisible(activity, true)
        } else {
            setFireworkVisible(activity, false)
            if (bean.isSnowflakeShow) {
                SnowflakeClickConfig().initViewStatus(activity)
            } else {
                activity.xue_fragment?.removeAllViews()
            }
        }
    }

    private fun scheduleResync(activity: TermuxActivity) {
        val menuList = activity.findViewById<RecyclerView>(R.id.main_menu_list)
        val resync = Runnable { syncBeautifyUiFromBean(activity) }
        if (menuList != null) {
            menuList.post { menuList.post(resync) }
        } else {
            activity.window?.decorView?.post(resync)
        }
    }

    private fun setFireworkVisible(activity: TermuxActivity, visible: Boolean) {
        val firework = activity.firework_view ?: return
        if (visible) {
            firework.visibility = View.VISIBLE
            firework.onResume()
        } else {
            firework.onPause()
            firework.visibility = View.GONE
        }
    }
}
