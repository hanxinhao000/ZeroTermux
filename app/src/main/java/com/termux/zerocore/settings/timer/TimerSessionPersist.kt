package com.termux.zerocore.settings.timer

import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import com.termux.zerocore.ftp.utils.TimerSetManage

/** 持久化「始终允许」模式下的定时任务运行状态，供 APP 重启后恢复。 */
object TimerSessionPersist {

    private const val STORAGE_KEY = "zt_timer_session_v1"
    private val gson = Gson()

    data class Snapshot(
        val active: Boolean,
        val nextFireAtMillis: Long,
        val executionCount: Int
    )

    /** 运行中且开启「始终允许」时写入；条件不满足时**不**清除已有快照。 */
    fun saveIfAllowed() {
        val bean = TimerSetManage.get().getZTTimerBean()
        if (!bean.isAlwaysAllowTimer || !TimerRuntimeState.isRunning()) {
            return
        }
        val nextAt = TimerRuntimeState.getNextFireAtMillis()
        if (nextAt <= 0L) {
            return
        }
        writeSnapshot(nextAt, TimerRuntimeState.getExecutionCount())
    }

    /** APP 被系统回收前强制保存（不要求内存中 isRunning 仍为 true）。 */
    fun saveForBackgroundResume() {
        val bean = TimerSetManage.get().getZTTimerBean()
        if (!bean.isAlwaysAllowTimer) {
            return
        }
        val nextAt = TimerRuntimeState.getNextFireAtMillis()
        if (nextAt <= 0L) {
            return
        }
        writeSnapshot(nextAt, TimerRuntimeState.getExecutionCount())
    }

    private fun writeSnapshot(nextFireAtMillis: Long, executionCount: Int) {
        val snapshot = Snapshot(
            active = true,
            nextFireAtMillis = nextFireAtMillis,
            executionCount = executionCount
        )
        SaveData.saveStringOther(STORAGE_KEY, gson.toJson(snapshot))
        TimerDiagLog.i(
            "TimerSessionPersist",
            "save next=$nextFireAtMillis count=$executionCount"
        )
    }

    fun load(): Snapshot? {
        val json = SaveData.getStringOther(STORAGE_KEY)?.trim().orEmpty()
        if (json.isBlank() || json == "def") return null
        return try {
            gson.fromJson(json, Snapshot::class.java)
        } catch (e: Exception) {
            TimerDiagLog.e("TimerSessionPersist", "load FAIL: ${e.message}")
            null
        }
    }

    /** 仅用户主动停止定时任务时调用。 */
    fun clear() {
        SaveData.saveStringOther(STORAGE_KEY, "")
        TimerDiagLog.i("TimerSessionPersist", "clear")
    }
}
