package com.termux.zerocore.settings.timer

import android.content.Context
import android.content.Intent
import com.termux.zerocore.settings.TimerActivity

object TimerNotificationHelper {
    const val EXTRA_FROM_NOTIFICATION = TimerActivity.EXTRA_FROM_NOTIFICATION

    fun buildOpenTimerIntent(context: Context): Intent {
        return Intent(context, TimerActivity::class.java).apply {
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}
