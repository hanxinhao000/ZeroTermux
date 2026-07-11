package com.termux.zerocore.crashhistory

data class ZtCrashRecord(
    val id: String,
    val timestampMs: Long,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val appVersion: String
) {
    fun summaryTitle(): String {
        val msg = message.trim()
        return if (msg.isNotEmpty()) {
            "$exceptionClass: $msg"
        } else {
            exceptionClass
        }
    }
}
