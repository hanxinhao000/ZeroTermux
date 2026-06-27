package com.termux.zerocore.ai.agent

import com.termux.zerocore.ai.config.ZtAiStrings

/**
 * 等待终端命令完成：在 shell 提示符、确认提示或达到 maxWaitMs 时返回。
 */
object ZtTerminalWaitHelper {

    /** 普通命令默认最长等待 10 分钟 */
    const val DEFAULT_COMMAND_MAX_WAIT_MS = 600_000L

    /** zt 命令默认最长等待 2 分钟 */
    const val DEFAULT_ZT_COMMAND_MAX_WAIT_MS = 120_000L

    /** 按键默认最长等待 30 秒 */
    const val DEFAULT_KEY_MAX_WAIT_MS = 30_000L

    const val MIN_MAX_WAIT_MS = 3_000L
    const val ABSOLUTE_MAX_WAIT_MS = 1_800_000L // 30 分钟上限

    data class WaitResult(
        val snapshot: String,
        val settled: Boolean,
        val timedOut: Boolean,
        val waitedMs: Long,
        val awaitingConfirmation: Boolean = false
    )

    fun resolveMaxWaitMs(requestedMs: Long?, defaultMs: Long): Long {
        val value = requestedMs ?: defaultMs
        return value.coerceIn(MIN_MAX_WAIT_MS, ABSOLUTE_MAX_WAIT_MS)
    }

    fun waitForTerminalSettle(
        initialWaitMs: Long = 400,
        pollIntervalMs: Long = 500,
        maxWaitMs: Long = DEFAULT_COMMAND_MAX_WAIT_MS,
        captureSnapshot: () -> String
    ): WaitResult {
        val cappedMax = maxWaitMs.coerceIn(MIN_MAX_WAIT_MS, ABSOLUTE_MAX_WAIT_MS)
        Thread.sleep(initialWaitMs)
        var waited = initialWaitMs
        while (waited < cappedMax) {
            val snap = captureSnapshot()
            if (isSnapshotAwaitingConfirmation(snap) && waited >= initialWaitMs + pollIntervalMs) {
                return WaitResult(
                    snapshot = snap,
                    settled = true,
                    timedOut = false,
                    waitedMs = waited,
                    awaitingConfirmation = true
                )
            }
            if (isSnapshotIdle(snap) && waited >= initialWaitMs + pollIntervalMs) {
                return WaitResult(
                    snapshot = snap,
                    settled = true,
                    timedOut = false,
                    waitedMs = waited
                )
            }
            Thread.sleep(pollIntervalMs)
            waited += pollIntervalMs
        }
        val finalSnap = captureSnapshot()
        val awaitingConfirmation = isSnapshotAwaitingConfirmation(finalSnap)
        val settled = isSnapshotIdle(finalSnap) || awaitingConfirmation
        return WaitResult(
            snapshot = finalSnap,
            settled = settled,
            timedOut = !settled,
            waitedMs = waited,
            awaitingConfirmation = awaitingConfirmation
        )
    }

    fun isSnapshotIdle(formattedSnapshot: String): Boolean {
        return formattedSnapshot.contains(ZtAiStrings.terminalStatusIdle())
    }

    fun isSnapshotAwaitingConfirmation(formattedSnapshot: String): Boolean {
        return formattedSnapshot.contains(ZtAiStrings.terminalStatusAwaitingConfirmation())
    }

    fun formatTimedOutNotice(waitedMs: Long): String {
        val seconds = waitedMs / 1000
        return ZtAiStrings.str(com.termux.R.string.zt_ai_terminal_wait_timeout).format(seconds)
    }

    fun formatConfirmationNotice(): String {
        return ZtAiStrings.str(com.termux.R.string.zt_ai_terminal_wait_confirm)
    }

    fun formatCommandResult(
        header: String,
        result: WaitResult
    ): String = buildString {
        appendLine(header)
        appendLine()
        if (result.awaitingConfirmation) {
            appendLine(formatConfirmationNotice())
            appendLine()
        } else if (result.timedOut) {
            appendLine(formatTimedOutNotice(result.waitedMs))
            appendLine()
        }
        append(result.snapshot)
    }.trim()
}
