package com.termux.zerocore.ai.config

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.config.mainmenu.config.ParticleClickConfig
import com.termux.zerocore.config.mainmenu.config.SnowflakeClickConfig
import com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder
import com.termux.zerocore.ftp.utils.UserSetManage

/**
 * AI 改 isSnowflakeShow / isRainShow 后，走菜单 Config 的 onClick / initViewStatus，不修改 Config 本身。
 *
 * setConfig 会先写入目标值，因此这里先置反 bean 再 onClick，与菜单点击同一套切换逻辑。
 * 粒子为 GLSurfaceView，VISIBLE 后须 onResume 才会渲染；rain 开启时跳过 Snowflake.initViewStatus，
 * 避免菜单 bind 顺序（雪花→粒子）或 bean 残留 snow=true 时把 xue_fragment 恢复成雪花。
 */
object ZtBeautifyUiEffects {

    fun applyUserSetting(key: String, desired: Boolean) {
        when (key) {
            "isSnowflakeShow" -> applyViaSnowflakeConfig(desired)
            "isRainShow" -> applyViaParticleConfig(desired)
        }
    }

    private fun applyViaSnowflakeConfig(desired: Boolean) {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.getTermuxActivity() ?: return@post
            invertIfMatchesDesired { working ->
                if (working.isSnowflakeShow == desired) {
                    working.isSnowflakeShow = !desired
                    true
                } else {
                    false
                }
            }
            val config = SnowflakeClickConfig()
            config.setTextView(TextView(activity))
            config.onClick(View(activity), activity)
            finishBeautifyChange(activity)
        }
    }

    private fun applyViaParticleConfig(desired: Boolean) {
        UUtils.getHandler().post {
            val activity = ZtForegroundActivityHolder.getTermuxActivity() ?: return@post
            invertIfMatchesDesired { working ->
                if (working.isRainShow == desired) {
                    working.isRainShow = !desired
                    true
                } else {
                    false
                }
            }
            ParticleClickConfig().onClick(View(activity), activity)
            finishBeautifyChange(activity)
        }
    }

    private inline fun invertIfMatchesDesired(block: (com.termux.zerocore.bean.ZTUserBean) -> Boolean) {
        val working = UserSetManage.get().getZTUserBean()
        if (block(working)) {
            UserSetManage.get().setZTUserBean(working)
        }
    }

    private fun finishBeautifyChange(activity: TermuxActivity) {
        syncBeautifyUiFromBean(activity)
        activity.refreshMainMenuPublic()
        val menuList = activity.findViewById<RecyclerView>(R.id.main_menu_list)
        val resync = Runnable { syncBeautifyUiFromBean(activity) }
        if (menuList != null) {
            // 双层 post：等外层与嵌套菜单 item bind 完成后再同步，避免 bind 覆盖粒子状态
            menuList.post { menuList.post(resync) }
        } else {
            activity.window?.decorView?.post(resync)
        }
    }

    private fun syncBeautifyUiFromBean(activity: TermuxActivity) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isRainShow && bean.isSnowflakeShow) {
            bean.isSnowflakeShow = false
            UserSetManage.get().setZTUserBean(bean)
        }
        ParticleClickConfig().initViewStatus(activity)
        if (bean.isRainShow) {
            activity.xue_fragment.removeAllViews()
            setFireworkVisible(activity, true)
        } else {
            setFireworkVisible(activity, false)
            SnowflakeClickConfig().initViewStatus(activity)
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
