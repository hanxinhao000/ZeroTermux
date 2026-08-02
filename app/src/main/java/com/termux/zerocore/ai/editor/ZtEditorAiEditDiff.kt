package com.termux.zerocore.ai.editor

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/**
 * 行级 diff，供 AI 改码确认弹窗展示（红删绿增）。
 */
object ZtEditorAiEditDiff {

    data class DiffResult(
        val body: CharSequence,
        val added: Int,
        val removed: Int,
        val unchanged: Int
    )

    private const val COLOR_ADD_FG = 0xFF81C784.toInt()
    private const val COLOR_ADD_BG = 0x332E7D32
    private const val COLOR_DEL_FG = 0xFFE57373.toInt()
    private const val COLOR_DEL_BG = 0x33C62828
    private const val COLOR_CTX_FG = 0xFFB0B0B0.toInt()
    private const val MAX_DP_CELLS = 1_800_000L
    private const val MAX_RENDER_LINES = 1200

    fun diff(oldText: String, newText: String): DiffResult {
        val oldLines = oldText.split('\n')
        val newLines = newText.split('\n')
        if (oldLines == newLines) {
            val body = SpannableStringBuilder()
            appendHeader(body, "（内容无变化）")
            return DiffResult(body, 0, 0, oldLines.size)
        }
        val ops = if (oldLines.size.toLong() * newLines.size.toLong() > MAX_DP_CELLS) {
            fallbackFullReplace(oldLines, newLines)
        } else {
            lcsDiff(oldLines, newLines)
        }
        return render(ops)
    }

    /** 整段视为新增（插入光标）。 */
    fun additionsOnly(text: String): DiffResult {
        val lines = text.split('\n')
        val ops = lines.map { DiffOp.Add(it) }
        return render(ops)
    }

    private sealed class DiffOp {
        data class Equal(val line: String) : DiffOp()
        data class Add(val line: String) : DiffOp()
        data class Del(val line: String) : DiffOp()
    }

    private fun lcsDiff(a: List<String>, b: List<String>): List<DiffOp> {
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (a[i] == b[j]) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }
        val out = ArrayList<DiffOp>(n + m)
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                a[i] == b[j] -> {
                    out.add(DiffOp.Equal(a[i]))
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    out.add(DiffOp.Del(a[i]))
                    i++
                }
                else -> {
                    out.add(DiffOp.Add(b[j]))
                    j++
                }
            }
        }
        while (i < n) {
            out.add(DiffOp.Del(a[i++]))
        }
        while (j < m) {
            out.add(DiffOp.Add(b[j++]))
        }
        return out
    }

    private fun fallbackFullReplace(a: List<String>, b: List<String>): List<DiffOp> {
        val out = ArrayList<DiffOp>(a.size + b.size)
        a.forEach { out.add(DiffOp.Del(it)) }
        b.forEach { out.add(DiffOp.Add(it)) }
        return out
    }

    private fun render(ops: List<DiffOp>): DiffResult {
        var added = 0
        var removed = 0
        var unchanged = 0
        ops.forEach {
            when (it) {
                is DiffOp.Add -> added++
                is DiffOp.Del -> removed++
                is DiffOp.Equal -> unchanged++
            }
        }
        val body = SpannableStringBuilder()
        appendHeader(body, "− 删除  + 新增")
        var rendered = 0
        var skippedEqual = 0
        // 大 diff 时压缩连续未改行，突出变更
        val compact = ops.size > 80
        var i = 0
        while (i < ops.size && rendered < MAX_RENDER_LINES) {
            val op = ops[i]
            if (compact && op is DiffOp.Equal) {
                var run = 0
                while (i + run < ops.size && ops[i + run] is DiffOp.Equal) run++
                if (run > 4) {
                    // 保留首尾各 1 行上下文
                    appendLine(body, DiffOp.Equal((ops[i] as DiffOp.Equal).line), rendered++)
                    val hidden = run - 2
                    if (hidden > 0 && rendered < MAX_RENDER_LINES) {
                        appendMeta(body, "··· 省略 $hidden 行未修改 ···")
                        rendered++
                        skippedEqual += hidden
                    }
                    if (run > 1 && rendered < MAX_RENDER_LINES) {
                        appendLine(body, DiffOp.Equal((ops[i + run - 1] as DiffOp.Equal).line), rendered++)
                    }
                    i += run
                    continue
                }
            }
            appendLine(body, op, rendered++)
            i++
        }
        if (i < ops.size) {
            appendMeta(body, "··· 其余 ${ops.size - i} 行已省略 ···")
        } else if (skippedEqual > 0) {
            // already noted inline
        }
        return DiffResult(body, added, removed, unchanged)
    }

    private fun appendHeader(sb: SpannableStringBuilder, text: String) {
        val start = sb.length
        sb.append(text).append('\n')
        sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF9E9E9E.toInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun appendMeta(sb: SpannableStringBuilder, text: String) {
        val start = sb.length
        sb.append(text).append('\n')
        sb.setSpan(ForegroundColorSpan(0xFF757575.toInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(Typeface.ITALIC), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun appendLine(sb: SpannableStringBuilder, op: DiffOp, @Suppress("UNUSED_PARAMETER") index: Int) {
        val start = sb.length
        val (prefix, line, fg, bg) = when (op) {
            is DiffOp.Add -> Quad("+ ", op.line, COLOR_ADD_FG, COLOR_ADD_BG)
            is DiffOp.Del -> Quad("- ", op.line, COLOR_DEL_FG, COLOR_DEL_BG)
            is DiffOp.Equal -> Quad("  ", op.line, COLOR_CTX_FG, 0)
        }
        sb.append(prefix).append(line).append('\n')
        sb.setSpan(ForegroundColorSpan(fg), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (bg != 0) {
            sb.setSpan(BackgroundColorSpan(bg), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private data class Quad(
        val prefix: String,
        val line: String,
        val fg: Int,
        val bg: Int
    )
}
