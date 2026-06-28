package com.termux.zerocore.aidebug

import com.google.gson.Gson

object ZtAiDebugInputHelper {

    private val gson = Gson()

    fun tapJson(x: Int, y: Int): String {
        val result = ZtAiDebugRootHelper.execRoot("input tap $x $y", 4000)
        return actionJson("tap", result, mapOf("x" to x, "y" to y))
    }

    fun swipeJson(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int): String {
        val d = durationMs.coerceIn(50, 5000)
        val result = ZtAiDebugRootHelper.execRoot("input swipe $x1 $y1 $x2 $y2 $d", 5000)
        return actionJson(
            "swipe",
            result,
            mapOf("x1" to x1, "y1" to y1, "x2" to x2, "y2" to y2, "durationMs" to d)
        )
    }

    fun textJson(text: String): String {
        val escaped = text.replace(" ", "%s").replace("'", "\\'")
        val result = ZtAiDebugRootHelper.execRoot("input text '$escaped'", 5000)
        return actionJson("text", result, mapOf("text" to text))
    }

    fun keyeventJson(code: Int): String {
        val result = ZtAiDebugRootHelper.execRoot("input keyevent $code", 4000)
        return actionJson("keyevent", result, mapOf("code" to code))
    }

    fun launchJson(packageName: String, activity: String?): String {
        val cmd = if (activity.isNullOrBlank()) {
            "monkey -p ${quote(packageName)} -c android.intent.category.LAUNCHER 1"
        } else {
            "am start -n ${quote("$packageName/$activity")}"
        }
        val result = ZtAiDebugRootHelper.execRoot(cmd, 6000)
        return actionJson("launch", result, mapOf("package" to packageName, "activity" to (activity ?: "")))
    }

    fun forceStopJson(packageName: String): String {
        val result = ZtAiDebugRootHelper.execRoot("am force-stop ${quote(packageName)}", 5000)
        return actionJson("force_stop", result, mapOf("package" to packageName))
    }

    private fun actionJson(action: String, result: ZtAiDebugRootHelper.ExecResult, extra: Map<String, Any>): String {
        val map = mutableMapOf<String, Any>(
            "ok" to result.ok,
            "action" to action,
            "stdout" to result.stdout,
            "stderr" to result.stderr
        )
        map.putAll(extra)
        return gson.toJson(map)
    }

    private fun quote(s: String): String = "'${s.replace("'", "'\\''")}'"
}
