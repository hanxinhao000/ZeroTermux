package com.termux.zerocore.crashhistory

import android.content.Context
import com.example.xh_lib.utils.UUtils
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID

object ZtCrashHistoryRecorder {

    @JvmStatic
    fun record(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val record = ZtCrashRecord(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                threadName = thread.name,
                exceptionClass = throwable.javaClass.name,
                message = throwable.message ?: "",
                stackTrace = stackTraceString(throwable),
                appVersion = UUtils.getVersionName(context)
            )
            ZtCrashHistoryStore.save(context.applicationContext, record)
        } catch (_: Exception) {
        }
    }

    private fun stackTraceString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
