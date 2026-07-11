package com.termux.zerocore.crashhistory

import android.content.Context
import com.google.gson.Gson

object ZtAiDebugCrashHelper {

    private val gson = Gson()

    fun listJson(context: Context): String {
        val items = ZtCrashHistoryStore.listSummaries(context).map { summary ->
            mapOf(
                "id" to summary.id,
                "timestampMs" to summary.timestampMs,
                "time" to ZtCrashHistoryStore.formatTime(summary.timestampMs),
                "thread" to summary.threadName,
                "exceptionClass" to summary.exceptionClass,
                "message" to summary.message,
                "summary" to summary.summary,
                "appVersion" to summary.appVersion
            )
        }
        return gson.toJson(mapOf("ok" to true, "count" to items.size, "items" to items))
    }

    fun detailJson(context: Context, id: String): String {
        val record = ZtCrashHistoryStore.get(context, id)
            ?: return gson.toJson(mapOf("ok" to false, "error" to "not found"))
        return gson.toJson(
            mapOf(
                "ok" to true,
                "item" to mapOf(
                    "id" to record.id,
                    "timestampMs" to record.timestampMs,
                    "time" to ZtCrashHistoryStore.formatTime(record.timestampMs),
                    "thread" to record.threadName,
                    "exceptionClass" to record.exceptionClass,
                    "message" to record.message,
                    "summary" to record.summaryTitle(),
                    "appVersion" to record.appVersion,
                    "stackTrace" to record.stackTrace
                )
            )
        )
    }

    fun deleteJson(context: Context, id: String): String {
        if (id.isBlank()) {
            return gson.toJson(mapOf("ok" to false, "error" to "missing id"))
        }
        val deleted = ZtCrashHistoryStore.delete(context, id)
        return gson.toJson(mapOf("ok" to deleted, "deleted" to deleted, "id" to id))
    }

    fun clearJson(context: Context): String {
        val count = ZtCrashHistoryStore.clearAll(context)
        return gson.toJson(mapOf("ok" to true, "cleared" to count))
    }
}
