package com.termux.zerocore.aidebug

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.termux.BuildConfig
import com.termux.zerocore.ftp.new_ftp.utils.NetworkEnvironmentUtil

object ZtAiDebugSystemHelper {

    private val gson = Gson()

    fun statusJson(context: Context): String {
        val root = ZtAiDebugRootHelper.isRootAvailable(context)
        val mem = if (root) {
            ZtAiDebugRootHelper.execRoot("cat /proc/meminfo", 4000).stdout
        } else {
            ZtAiDebugRootHelper.exec("cat /proc/meminfo", 4000, false).stdout
        }
        val load = if (root) {
            ZtAiDebugRootHelper.execRoot("cat /proc/loadavg", 3000).stdout.trim()
        } else ""
        val disk = if (root) {
            ZtAiDebugRootHelper.execRoot("df -h /data /sdcard 2>/dev/null || df -h", 5000).stdout
        } else ""
        val uptime = if (root) {
            ZtAiDebugRootHelper.execRoot("uptime 2>/dev/null || cat /proc/uptime", 3000).stdout.trim()
        } else ""
        return gson.toJson(
            mapOf(
                "ok" to true,
                "androidVersion" to (Build.VERSION.RELEASE ?: "unknown"),
                "androidSdk" to Build.VERSION.SDK_INT,
                "deviceModel" to (Build.MODEL ?: "unknown"),
                "deviceManufacturer" to (Build.MANUFACTURER ?: "unknown"),
                "ztVersion" to (BuildConfig.VERSION_NAME ?: "unknown"),
                "lanIps" to NetworkEnvironmentUtil.getLocalIpv4Addresses(),
                "root_mode" to ZtAiDebugRootHelper.isRootModeEnabled(),
                "root_available" to root,
                "meminfo" to mem,
                "loadavg" to load,
                "disk" to disk,
                "uptime" to uptime,
                "adb_tcp_port" to ZtAiDebugRootHelper.readAdbTcpPort()
            )
        )
    }

    fun dumpsysJson(service: String): String {
        val name = service.trim().ifEmpty { "activity" }
        val result = ZtAiDebugRootHelper.execRoot("dumpsys $name", 15_000)
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "service" to name,
                "output" to result.stdout.ifEmpty { result.stderr }
            )
        )
    }

    fun getpropJson(key: String?): String {
        val cmd = if (key.isNullOrBlank()) "getprop" else "getprop ${shellQuote(key)}"
        val result = ZtAiDebugRootHelper.exec(cmd, 5000, asRoot = false)
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "key" to (key ?: "*"),
                "value" to result.stdout.trim()
            )
        )
    }

    fun setpropJson(key: String, value: String): String {
        val result = ZtAiDebugRootHelper.execRoot(
            "setprop ${shellQuote(key)} ${shellQuote(value)}",
            5000
        )
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "key" to key,
                "value" to value,
                "stdout" to result.stdout,
                "stderr" to result.stderr
            )
        )
    }

    fun packagesJson(filter: String?): String {
        val cmd = if (filter.isNullOrBlank()) {
            "pm list packages -f"
        } else {
            "pm list packages -f | grep -i ${shellQuote(filter)} || true"
        }
        val result = ZtAiDebugRootHelper.execRoot(cmd, 10_000)
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "filter" to (filter ?: ""),
                "packages" to result.stdout.lines().filter { it.isNotBlank() }
            )
        )
    }

    fun processesJson(): String {
        val result = ZtAiDebugRootHelper.execRoot("ps -A -o PID,USER,NAME 2>/dev/null || ps", 8000)
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "processes" to result.stdout
            )
        )
    }

    fun dmesgJson(lines: Int): String {
        val n = lines.coerceIn(50, 2000)
        val result = ZtAiDebugRootHelper.execRoot("dmesg 2>/dev/null | tail -n $n", 8000)
        return gson.toJson(
            mapOf(
                "ok" to result.ok,
                "lines" to n,
                "content" to result.stdout.ifEmpty { result.stderr }
            )
        )
    }

    private fun shellQuote(s: String): String {
        return "'" + s.replace("'", "'\\''") + "'"
    }
}
