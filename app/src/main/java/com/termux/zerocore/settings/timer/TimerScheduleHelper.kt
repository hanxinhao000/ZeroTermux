package com.termux.zerocore.settings.timer

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.ZtLocaleStrings
import java.util.Calendar
import java.util.Locale

object TimerScheduleHelper {

    /** 计算距离下次触发还需等待的毫秒数（至少 1 秒）。 */
    fun computeNextDelayMs(bean: TimerBean, afterMillis: Long = System.currentTimeMillis()): Long {
        return (computeNextFireAtMillis(bean, afterMillis) - afterMillis).coerceAtLeast(1_000L)
    }

    /** 计算下一次触发的绝对时间戳（毫秒）。 */
    fun computeNextFireAtMillis(bean: TimerBean, afterMillis: Long = System.currentTimeMillis()): Long {
        return if (bean.timerMode == TimerBean.MODE_DAILY_TIME) {
            computeNextDailyFireAtMillis(bean.scheduledHour, bean.scheduledMinute, afterMillis)
        } else {
            afterMillis + intervalDelayMs(bean)
        }
    }

    /** 每日定时：返回 afterMillis 之后的下一个目标时刻（绝对时间戳）。 */
    fun computeNextDailyFireAtMillis(
        hour: Int,
        minute: Int,
        afterMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val target = Calendar.getInstance().apply {
            timeInMillis = afterMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
        }
        if (target.timeInMillis <= afterMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    fun computeDelayUntilDailyTime(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        return (computeNextDailyFireAtMillis(hour, minute, now) - now).coerceAtLeast(1_000L)
    }

    fun formatScheduleLabel(bean: TimerBean): String {
        return if (bean.timerMode == TimerBean.MODE_DAILY_TIME) {
            ZtLocaleStrings.getString(
                R.string.zt_timer_daily_at,
                formatClock(bean.scheduledHour, bean.scheduledMinute)
            )
        } else if (bean.timerNumber == TimerBean.TIMER_OTHER) {
            formatCustomIntervalLabel(bean.timerOtherNumber)
        } else {
            when (bean.timerNumber) {
                TimerBean.TIMER_30_SECOND -> UUtils.getString(R.string.zt_timer_30_second)
                TimerBean.TIMER_1_MINUTE -> UUtils.getString(R.string.zt_timer_1_minute)
                TimerBean.TIMER_10_MINUTE -> UUtils.getString(R.string.zt_timer_10_minute)
                TimerBean.TIMER_30_MINUTE -> UUtils.getString(R.string.zt_timer_30_minute)
                else -> formatCustomIntervalLabel(bean.timerNumber.toLong())
            }
        }
    }

    fun formatClock(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun intervalDelayMs(bean: TimerBean): Long {
        val raw = if (bean.timerNumber == TimerBean.TIMER_OTHER) {
            bean.timerOtherNumber
        } else {
            bean.timerNumber.toLong()
        }
        return raw.coerceAtLeast(1_000L)
    }

    private fun formatCustomIntervalLabel(millis: Long): String {
        return if (millis >= 60 * 60 * 1000) {
            "${millis / 60 / 60 / 1000} ${UUtils.getString(R.string.zt_timer_1_hour)}"
        } else if (millis >= 60 * 1000) {
            "${millis / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
        } else if (millis >= 1000) {
            "${millis / 1000} ${UUtils.getString(R.string.zt_timer_second_unit)}"
        } else {
            "< 1 ${UUtils.getString(R.string.zt_timer_second_unit)}"
        }
    }
}
