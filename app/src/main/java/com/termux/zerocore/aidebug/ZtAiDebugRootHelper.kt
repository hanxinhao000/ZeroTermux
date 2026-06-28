package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.scottyab.rootbeer.RootBeer
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.shell.ExeCommand
import com.topjohnwu.superuser.Shell
import java.io.File

object ZtAiDebugRootHelper {

    private val gson = Gson()

    fun isRootModeEnabled(): Boolean {
        return UserSetManage.get().getZTUserBean().isZtAiDebugRootEnabled
    }

    fun isRootAvailable(context: Context): Boolean {
        return try {
            if (!RootBeer(context).isRooted) return false
            Shell.getShell().isRoot
        } catch (_: Exception) {
            false
        }
    }

    fun statusJson(context: Context): String {
        val rootMode = isRootModeEnabled()
        val rooted = isRootAvailable(context)
        val adbPort = readAdbTcpPort()
        return gson.toJson(
            mapOf(
                "ok" to true,
                "root_mode_enabled" to rootMode,
                "root_available" to rooted,
                "root_shell" to rooted,
                "adb_tcp_port" to adbPort,
                "adb_tcp_listening" to (adbPort > 0),
                "hint_for_ai" to when {
                    !rootMode ->
                        "Root API locked. User must enable Root full debug in ZeroTermux Settings → External AI debug."
                    !rooted ->
                        "Root mode enabled but su unavailable. Ask user to grant ZeroTermux root in Magisk/SU manager."
                    else ->
                        "Root mode active. Use /api/root/exec, /api/input/*, /api/system/*, /api/adb/*, GET /api/screenshot?source=root"
                }
            )
        )
    }

    fun readAdbTcpPort(): Int {
        val out = exec("getprop service.adb.tcp.port", 3000, asRoot = false).stdout.trim()
        return out.toIntOrNull()?.takeIf { it > 0 } ?: -1
    }

    data class ExecResult(
        val ok: Boolean,
        val stdout: String,
        val stderr: String = "",
        val exitCode: Int = if (ok) 0 else 1,
        val timedOut: Boolean = false
    )

    fun exec(command: String, timeoutMs: Long, asRoot: Boolean): ExecResult {
        if (command.isBlank()) {
            return ExecResult(ok = false, stdout = "", exitCode = 1)
        }
        val clamped = timeoutMs.coerceIn(200L, 120_000L).toInt()
        return execInternal(command, clamped, asRoot)
    }

    fun execRoot(command: String, timeoutMs: Long): ExecResult {
        return exec(command, timeoutMs, asRoot = true)
    }

    private fun execInternal(command: String, timeoutMs: Int, asRoot: Boolean): ExecResult {
        return try {
            if (asRoot) {
                val result = Shell.cmd("sh", "-c", command).exec()
                ExecResult(
                    ok = result.isSuccess,
                    stdout = result.out.joinToString("\n"),
                    stderr = result.err.joinToString("\n"),
                    exitCode = result.code
                )
            } else {
                val cmd = ExeCommand(true)
                cmd.run(command, timeoutMs, false)
                ExecResult(
                    ok = true,
                    stdout = cmd.getResult().trim(),
                    exitCode = 0
                )
            }
        } catch (e: Exception) {
            ExecResult(ok = false, stdout = "", stderr = e.message.orEmpty(), exitCode = 1)
        }
    }

    fun execResultJson(result: ExecResult, command: String, timeoutMs: Long): String {
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "command" to command,
                "timeoutMs" to timeoutMs,
                "exitCode" to result.exitCode,
                "stdout" to result.stdout,
                "stderr" to result.stderr,
                "timedOut" to result.timedOut
            )
        )
    }

    fun screencapPng(context: Context): ByteArray? {
        val tmp = File(context.cacheDir, "zt_ai_screencap_${System.currentTimeMillis()}.png")
        return try {
            val path = tmp.absolutePath
            val result = execRoot("screencap -p \"$path\"", 8000)
            if (!result.ok || !tmp.exists() || tmp.length() == 0L) {
                tmp.delete()
                null
            } else {
                tmp.readBytes().also { tmp.delete() }
            }
        } catch (_: Exception) {
            tmp.delete()
            null
        }
    }

    fun enableAdbTcp(port: Int): ExecResult {
        val p = port.coerceIn(1024, 65535)
        execRoot("setprop service.adb.tcp.port $p", 5000)
        execRoot("stop adbd", 5000)
        return execRoot("start adbd", 8000)
    }

    fun disableAdbTcp(): ExecResult {
        execRoot("setprop service.adb.tcp.port -1", 5000)
        execRoot("stop adbd", 5000)
        return execRoot("start adbd", 8000)
    }
}
