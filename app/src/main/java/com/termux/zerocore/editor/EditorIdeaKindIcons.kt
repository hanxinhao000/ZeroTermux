package com.termux.zerocore.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import kotlin.math.min

/**
 * IDEA / Android Studio 风格补全图标：彩色圆底 + 字母。
 */
object EditorIdeaKindIcons {

    fun mapLspKind(lspKind: Int?): CompletionItemKind? {
        if (lspKind == null || lspKind < 1) return null
        // LSP CompletionItemKind（1-based）→ Sora enum
        return when (lspKind) {
            1 -> CompletionItemKind.Text
            2 -> CompletionItemKind.Method
            3 -> CompletionItemKind.Function
            4 -> CompletionItemKind.Constructor
            5 -> CompletionItemKind.Field
            6 -> CompletionItemKind.Variable
            7 -> CompletionItemKind.Class
            8 -> CompletionItemKind.Interface
            9 -> CompletionItemKind.Module
            10 -> CompletionItemKind.Property
            11 -> CompletionItemKind.Unit
            12 -> CompletionItemKind.Value
            13 -> CompletionItemKind.Enum
            14 -> CompletionItemKind.Keyword
            15 -> CompletionItemKind.Snippet
            16 -> CompletionItemKind.Color
            17 -> CompletionItemKind.File
            18 -> CompletionItemKind.Reference
            19 -> CompletionItemKind.Folder
            20 -> CompletionItemKind.EnumMember
            21 -> CompletionItemKind.Constant
            22 -> CompletionItemKind.Struct
            23 -> CompletionItemKind.Event
            24 -> CompletionItemKind.Operator
            25 -> CompletionItemKind.TypeParameter
            else -> CompletionItemKind.Text
        }
    }

    fun create(context: Context, kind: CompletionItemKind?): Drawable {
        val k = kind ?: CompletionItemKind.Text
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            context.resources.displayMetrics
        ).toInt().coerceAtLeast(14)
        return KindBadgeDrawable(size, badgeColor(k), badgeLetter(k))
    }

    private fun badgeLetter(kind: CompletionItemKind): String {
        return when (kind) {
            CompletionItemKind.Method, CompletionItemKind.Function, CompletionItemKind.Constructor -> "m"
            CompletionItemKind.Field, CompletionItemKind.Property, CompletionItemKind.EnumMember -> "f"
            CompletionItemKind.Variable, CompletionItemKind.Value, CompletionItemKind.Constant -> "v"
            CompletionItemKind.Class, CompletionItemKind.Struct, CompletionItemKind.Enum -> "C"
            CompletionItemKind.Interface -> "I"
            CompletionItemKind.Keyword -> "k"
            CompletionItemKind.Snippet -> "S"
            CompletionItemKind.Module, CompletionItemKind.Folder -> "P"
            CompletionItemKind.File -> "F"
            else -> kind.getDisplayChar()
        }
    }

    private fun badgeColor(kind: CompletionItemKind): Int {
        val argb = kind.defaultDisplayBackgroundColor
        if (argb != 0L) return (argb and 0xffffffffL).toInt()
        return when (kind) {
            CompletionItemKind.Snippet -> 0xFF7A7A7A.toInt()
            CompletionItemKind.File, CompletionItemKind.Folder -> 0xFFABB6BD.toInt()
            else -> 0xFFABB6BD.toInt()
        }
    }

    private class KindBadgeDrawable(
        private val sizePx: Int,
        private val fillColor: Int,
        private val letter: String
    ) : Drawable() {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            val r = min(b.width(), b.height()) / 2f
            canvas.drawCircle(cx, cy, r, fillPaint)
            textPaint.textSize = r * 1.15f
            val fm = textPaint.fontMetrics
            val textY = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(letter, cx, textY, textPaint)
        }

        override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            textPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            textPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = sizePx
        override fun getIntrinsicHeight(): Int = sizePx
    }
}
