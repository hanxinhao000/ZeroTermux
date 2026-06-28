package com.termux.zerocore.ai.config

import android.graphics.Color
import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import kotlin.math.roundToInt

/**
 * 美化设置：终端字体色 / 背景遮罩色。
 * 手动（ColorSeekBar）与 AI（set_zerotermux_config）共用同一套 SaveData 键与解析逻辑。
 */
object ZtBeautifyColorHelper {

    const val KEY_FONT_COLOR = "font_color"
    const val KEY_FONT_COLOR_PROGRESS = "font_color_progress"
    const val KEY_BACK_COLOR = "back_color"
    const val KEY_BACK_COLOR_PROGRESS = "back_color_progress"

    private const val MAX_PROGRESS = 100
    private val gson = Gson()

    /** 与 ColorSeekBar 默认 colorSeekBarColorSeeds 一致，便于 progress ↔ color 互转。 */
    private val COLOR_SEEDS = intArrayOf(
        0xFF000000.toInt(), 0xFF9900FF.toInt(), 0xFF0000FF.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(),
        0xFFFF0000.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6600.toInt(), 0xFFFFFF00.toInt(), 0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )

    fun isColorKey(key: String): Boolean =
        key == KEY_FONT_COLOR || key == KEY_FONT_COLOR_PROGRESS ||
            key == KEY_BACK_COLOR || key == KEY_BACK_COLOR_PROGRESS

    fun readFontColorProgress(): Int? = readProgress(KEY_FONT_COLOR_PROGRESS)

    fun readBackColorProgress(): Int? = readProgress(KEY_BACK_COLOR_PROGRESS)

    fun readFontColorArgb(): Int? = readStoredColor(KEY_FONT_COLOR)

    fun readBackColorArgb(): Int? = readStoredColor(KEY_BACK_COLOR)

    /** UI：ColorSeekBar 拖动时写入。 */
    fun saveFontColorFromUi(color: Int, progress: Int) {
        persistColorPair(KEY_FONT_COLOR, KEY_FONT_COLOR_PROGRESS, color, progress)
    }

    fun saveBackColorFromUi(color: Int, progress: Int) {
        persistColorPair(KEY_BACK_COLOR, KEY_BACK_COLOR_PROGRESS, color, progress)
        applyAfterChange(KEY_BACK_COLOR)
    }

    /** AI / Registry：按 key 写入；返回 JSON 结果，非颜色键返回 null。 */
    fun setConfigKey(key: String, value: String): String? {
        return when (key.trim()) {
            KEY_FONT_COLOR -> setColorValue(isFont = true, value)
            KEY_BACK_COLOR -> setColorValue(isFont = false, value)
            KEY_FONT_COLOR_PROGRESS -> setProgressValue(isFont = true, value)
            KEY_BACK_COLOR_PROGRESS -> setProgressValue(isFont = false, value)
            else -> null
        }
    }

    /** get_zerotermux_config：附带可读 hex，便于 AI 理解当前值。 */
    fun enrichForAiRead(key: String, raw: String?): Any? {
        if (raw.isNullOrBlank() || raw == "def") return raw
        if (key != KEY_FONT_COLOR && key != KEY_BACK_COLOR) return raw
        val color = raw.toIntOrNull() ?: return raw
        return org.json.JSONObject()
            .put("stored", raw)
            .put("hex", colorToHex(color))
    }

    private fun setColorValue(isFont: Boolean, raw: String): String {
        val color = parseColorValue(raw)
            ?: return err("invalid color: use #RRGGBB, #AARRGGBB, 0xAARRGGBB or Android color int")
        val progress = progressForColor(color)
        val colorKey = if (isFont) KEY_FONT_COLOR else KEY_BACK_COLOR
        val progressKey = if (isFont) KEY_FONT_COLOR_PROGRESS else KEY_BACK_COLOR_PROGRESS
        persistColorPair(colorKey, progressKey, color, progress)
        applyAfterChange(colorKey)
        return ok(
            "set $colorKey=${colorToHex(color)} progress=$progress",
            mapOf("color" to colorToHex(color), "progress" to progress)
        )
    }

    private fun setProgressValue(isFont: Boolean, raw: String): String {
        val progress = raw.toIntOrNull()
            ?: return err("invalid int for ${if (isFont) KEY_FONT_COLOR_PROGRESS else KEY_BACK_COLOR_PROGRESS}")
        if (progress !in 0..MAX_PROGRESS) {
            return err("progress must be 0..$MAX_PROGRESS")
        }
        val color = colorAtProgress(progress)
        val colorKey = if (isFont) KEY_FONT_COLOR else KEY_BACK_COLOR
        val progressKey = if (isFont) KEY_FONT_COLOR_PROGRESS else KEY_BACK_COLOR_PROGRESS
        persistColorPair(colorKey, progressKey, color, progress)
        applyAfterChange(colorKey)
        return ok(
            "set $progressKey=$progress color=${colorToHex(color)}",
            mapOf("color" to colorToHex(color), "progress" to progress)
        )
    }

    private fun persistColorPair(colorKey: String, progressKey: String, color: Int, progress: Int) {
        SaveData.saveStringOther(colorKey, color.toString())
        SaveData.saveStringOther(progressKey, progress.toString())
    }

    private fun applyAfterChange(colorKey: String) {
        ZtAiConfigSideEffects.applyBeautify(colorKey)
    }

    fun parseColorValue(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.equals("def", true)) return null
        if (trimmed.startsWith("#")) {
            return try {
                Color.parseColor(trimmed)
            } catch (_: Exception) {
                null
            }
        }
        if (trimmed.startsWith("0x", ignoreCase = true)) {
            return trimmed.substring(2).toLongOrNull(16)?.let { longToArgb(it) }
        }
        return trimmed.toIntOrNull()
    }

    fun colorAtProgress(progress: Int): Int {
        val clamped = progress.coerceIn(0, MAX_PROGRESS)
        val p = clamped / MAX_PROGRESS.toFloat()
        val segments = COLOR_SEEDS.size - 1
        val scaled = p * segments
        val index = scaled.toInt().coerceIn(0, segments - 1)
        val t = scaled - index
        return blendArgb(COLOR_SEEDS[index], COLOR_SEEDS[index + 1], t)
    }

    fun progressForColor(color: Int): Int {
        var bestProgress = 0
        var bestDistance = Long.MAX_VALUE
        for (progress in 0..MAX_PROGRESS) {
            val candidate = colorAtProgress(progress)
            val distance = colorDistance(color, candidate)
            if (distance < bestDistance) {
                bestDistance = distance
                bestProgress = progress
            }
        }
        return bestProgress
    }

    private fun readProgress(key: String): Int? {
        val raw = SaveData.getStringOther(key) ?: return null
        if (raw.isEmpty() || raw == "def") return null
        return raw.toIntOrNull()
    }

    private fun readStoredColor(key: String): Int? {
        val raw = SaveData.getStringOther(key) ?: return null
        if (raw.isEmpty() || raw == "def") return null
        return raw.toIntOrNull()
    }

    private fun colorToHex(color: Int): String {
        val rgb = color and 0xFFFFFF
        return String.format("#%06X", rgb)
    }

    private fun longToArgb(value: Long): Int {
        return when {
            value <= 0xFFFFFFL -> (0xFF000000L or value).toInt()
            else -> value.toInt()
        }
    }

    private fun blendArgb(from: Int, to: Int, t: Float): Int {
        val a = lerp(Color.alpha(from), Color.alpha(to), t).roundToInt()
        val r = lerp(Color.red(from), Color.red(to), t).roundToInt()
        val g = lerp(Color.green(from), Color.green(to), t).roundToInt()
        val b = lerp(Color.blue(from), Color.blue(to), t).roundToInt()
        return Color.argb(a, r, g, b)
    }

    private fun lerp(from: Int, to: Int, t: Float): Float = from + (to - from) * t

    private fun colorDistance(a: Int, b: Int): Long {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return (dr * dr + dg * dg + db * db).toLong()
    }

    private fun ok(message: String, extra: Map<String, Any> = emptyMap()): String {
        val map = mutableMapOf<String, Any>("ok" to true, "message" to message)
        map.putAll(extra)
        return gson.toJson(map)
    }

    private fun err(message: String): String =
        gson.toJson(mapOf("ok" to false, "error" to message))

    fun snapshotJson(): String {
        fun entry(colorKey: String, progressKey: String): Map<String, Any?> {
            val argb = readStoredColor(colorKey)
            val progress = readProgress(progressKey)
            return mapOf(
                "stored" to SaveData.getStringOther(colorKey),
                "hex" to argb?.let { colorToHex(it) },
                "progress" to progress
            )
        }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "font" to entry(KEY_FONT_COLOR, KEY_FONT_COLOR_PROGRESS),
                "back" to entry(KEY_BACK_COLOR, KEY_BACK_COLOR_PROGRESS),
                "hint" to "POST font_color/back_color as #RRGGBB or set *_progress 0-100"
            )
        )
    }

    /** 调试 API：{font_color?, back_color?, font_color_progress?, back_color_progress?} */
    fun applyFromDebugBody(body: org.json.JSONObject): String {
        var last: String? = null
        val font = body.optString("font_color", "").trim()
            .ifEmpty { body.optString("font", "").trim() }
        val back = body.optString("back_color", "").trim()
            .ifEmpty { body.optString("back", "").trim() }
        if (font.isNotEmpty()) {
            last = setConfigKey(KEY_FONT_COLOR, font)
        }
        if (back.isNotEmpty()) {
            last = setConfigKey(KEY_BACK_COLOR, back)
        }
        if (body.has("font_color_progress")) {
            last = setConfigKey(KEY_FONT_COLOR_PROGRESS, body.opt("font_color_progress").toString())
        }
        if (body.has("back_color_progress")) {
            last = setConfigKey(KEY_BACK_COLOR_PROGRESS, body.opt("back_color_progress").toString())
        }
        return last ?: err("provide font_color and/or back_color (#RRGGBB) or *_progress 0-100")
    }
}
