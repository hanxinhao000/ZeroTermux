package com.termux.zerocore.editor

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.termux.R
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * IDEA / Android Studio 风格补全列表：圆标 + 名称(参数灰) + 右侧类型/包名。
 * 兼容 sora-editor 0.24.x（无 detail/deprecated 字段）。
 */
class EditorIdeaCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            32f,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup, isCurrentCursorPosition: Boolean): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_editor_idea_completion, parent, false)
        val item = getItem(pos)
        val iconView = view.findViewById<ImageView>(R.id.idea_completion_icon)
        val labelView = view.findViewById<TextView>(R.id.idea_completion_label)
        val descView = view.findViewById<TextView>(R.id.idea_completion_desc)

        val kind = item.kind
        iconView.setImageDrawable(item.icon ?: EditorIdeaKindIcons.create(context, kind))

        val primary = getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY).let {
            if (it == 0 || android.graphics.Color.alpha(it) == 0) COLOR_PRIMARY else it
        }
        val secondary = getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY).let {
            if (it == 0 || android.graphics.Color.alpha(it) == 0) COLOR_SECONDARY else it
        }
        val matched = getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED).let {
            if (it == 0) COLOR_MATCHED else it
        }

        labelView.setTextColor(primary)
        labelView.text = styleLabel(item, primary, secondary, matched)
        labelView.isSelected = isCurrentCursorPosition

        val desc = item.desc?.toString()?.takeIf { it.isNotBlank() }
        descView.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE
        descView.text = desc
        descView.setTextColor(secondary)
        descView.isSelected = isCurrentCursorPosition

        view.setBackgroundColor(
            if (isCurrentCursorPosition) COLOR_SELECTION
            else 0
        )
        return view
    }

    private fun styleLabel(
        item: CompletionItem,
        primary: Int,
        secondary: Int,
        matched: Int
    ): CharSequence {
        val raw = item.label?.toString() ?: return ""
        val spannable = SpannableString(raw)
        // 参数列表变灰
        val open = raw.indexOf('(')
        val close = raw.lastIndexOf(')')
        if (open >= 0 && close > open) {
            spannable.setSpan(
                ForegroundColorSpan(secondary),
                open,
                close + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // 泛型参数略弱化
        val lt = raw.indexOf('<')
        val gt = raw.lastIndexOf('>')
        if (lt >= 0 && gt > lt && (open < 0 || lt < open)) {
            spannable.setSpan(
                ForegroundColorSpan(secondary),
                lt,
                gt + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // 匹配前缀高亮（若已有 spannable 匹配则保留 label 原样）
        if (item.label !is Spanned || (item.label as Spanned).getSpans(0, raw.length, Any::class.java).isEmpty()) {
            val prefixLen = item.prefixLength.coerceIn(0, raw.length)
            if (prefixLen > 0) {
                var end = prefixLen
                // 前缀落在标识符名上，不包含 '('
                if (open >= 0) end = end.coerceAtMost(open)
                if (end > 0) {
                    spannable.setSpan(
                        ForegroundColorSpan(matched),
                        0,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // 名称其余部分保持主色
                    if (end < (if (open >= 0) open else raw.length)) {
                        spannable.setSpan(
                            ForegroundColorSpan(primary),
                            end,
                            if (open >= 0) open else raw.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
        return spannable
    }

    companion object {
        // 贴近截图：深色底 + 蓝选中 + 灰次要信息
        private const val COLOR_PRIMARY = 0xFFE8E8E8.toInt()
        private const val COLOR_SECONDARY = 0xFF8A8A8A.toInt()
        private const val COLOR_MATCHED = 0xFF4DAAFC.toInt()
        private const val COLOR_SELECTION = 0xFF2F5F98.toInt()
    }
}
