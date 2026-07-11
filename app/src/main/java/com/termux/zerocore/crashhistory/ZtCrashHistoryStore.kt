package com.termux.zerocore.crashhistory

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ZtCrashHistoryStore {

    private const val DIR_NAME = "zt_crash_history"
    private const val INDEX_FILE = "index.json"
    private const val MAX_ENTRIES = 100

    private val gson = Gson()
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun save(context: Context, record: ZtCrashRecord) {
        synchronized(lock) {
            val dir = historyDir(context)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val bodyFile = File(dir, "${record.id}.json")
            bodyFile.writeText(gson.toJson(record))
            val index = loadIndexLocked(dir).toMutableList()
            index.removeAll { it.id == record.id }
            index.add(0, toSummary(record))
            while (index.size > MAX_ENTRIES) {
                val removed = index.removeAt(index.lastIndex)
                File(dir, "${removed.id}.json").delete()
            }
            saveIndexLocked(dir, index)
        }
    }

    fun listSummaries(context: Context): List<ZtCrashSummary> {
        synchronized(lock) {
            return loadIndexLocked(historyDir(context))
        }
    }

    fun get(context: Context, id: String): ZtCrashRecord? {
        synchronized(lock) {
            val file = File(historyDir(context), "$id.json")
            if (!file.isFile) return null
            return try {
                gson.fromJson(file.readText(), ZtCrashRecord::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun delete(context: Context, id: String): Boolean {
        synchronized(lock) {
            val dir = historyDir(context)
            val deleted = File(dir, "$id.json").delete()
            val index = loadIndexLocked(dir).filter { it.id != id }
            saveIndexLocked(dir, index)
            return deleted
        }
    }

    fun clearAll(context: Context): Int {
        synchronized(lock) {
            val dir = historyDir(context)
            val count = loadIndexLocked(dir).size
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            saveIndexLocked(dir, emptyList())
            return count
        }
    }

    fun formatTime(timestampMs: Long): String {
        return timeFormat.format(Date(timestampMs))
    }

    private fun historyDir(context: Context): File {
        return File(context.filesDir, DIR_NAME)
    }

    private fun loadIndexLocked(dir: File): List<ZtCrashSummary> {
        if (!dir.exists()) {
            return emptyList()
        }
        val indexFile = File(dir, INDEX_FILE)
        if (!indexFile.isFile) {
            return rebuildIndexFromFiles(dir)
        }
        return try {
            val type = object : TypeToken<List<ZtCrashSummary>>() {}.type
            gson.fromJson<List<ZtCrashSummary>>(indexFile.readText(), type) ?: emptyList()
        } catch (_: Exception) {
            rebuildIndexFromFiles(dir)
        }
    }

    private fun rebuildIndexFromFiles(dir: File): List<ZtCrashSummary> {
        val summaries = dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") && it.name != INDEX_FILE }
            ?.mapNotNull { file ->
                try {
                    val record = gson.fromJson(file.readText(), ZtCrashRecord::class.java)
                    toSummary(record)
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.timestampMs }
            ?: emptyList()
        saveIndexLocked(dir, summaries)
        return summaries
    }

    private fun saveIndexLocked(dir: File, summaries: List<ZtCrashSummary>) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, INDEX_FILE).writeText(gson.toJson(summaries))
    }

    private fun toSummary(record: ZtCrashRecord): ZtCrashSummary {
        return ZtCrashSummary(
            id = record.id,
            timestampMs = record.timestampMs,
            threadName = record.threadName,
            exceptionClass = record.exceptionClass,
            message = record.message,
            summary = record.summaryTitle(),
            appVersion = record.appVersion
        )
    }
}

data class ZtCrashSummary(
    val id: String,
    val timestampMs: Long,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val summary: String,
    val appVersion: String
)
