package com.termux.zerocore.config.ztcommand.navigation

import android.app.Activity
import com.termux.app.TermuxActivity

object ZtForegroundActivityHolder {

    @Volatile
    private var termuxActivity: TermuxActivity? = null

    @JvmStatic
    fun set(activity: Activity?) {
        if (activity == null) {
            termuxActivity = null
            return
        }
        if (activity is TermuxActivity) {
            termuxActivity = activity
        }
    }

    @JvmStatic
    fun get(): Activity? = termuxActivity?.takeUnless { it.isFinishing || it.isDestroyed }

    @JvmStatic
    fun getTermuxActivity(): TermuxActivity? = get() as? TermuxActivity
}
