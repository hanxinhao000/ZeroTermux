package com.termux.zerocore.editor.lsp

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import com.termux.R
import com.termux.zerocore.editor.EditorIdeaKindIcons
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.concurrent.atomic.AtomicInteger

/**
 * 编辑器旁悬浮窗：查找引用（可加载/取消）与长按 Hover 信息。
 */
class EditorLspSymbolPopup(
    private val editor: CodeEditor
) {
    private var popup: PopupWindow? = null
    private val loadToken = AtomicInteger(0)
    private var onDismissExtra: (() -> Unit)? = null

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun dismiss() {
        loadToken.incrementAndGet()
        val p = popup
        popup = null
        if (p?.isShowing == true) {
            p.dismiss()
        }
        onDismissExtra?.invoke()
        onDismissExtra = null
    }

    fun showLoadingNear(
        line: Int,
        column: Int,
        title: CharSequence,
        onDismiss: (() -> Unit)? = null
    ): Int {
        dismiss()
        onDismissExtra = onDismiss
        val token = loadToken.incrementAndGet()
        val content = inflateContent()
        content.titleView.text = title
        content.progress.visibility = View.VISIBLE
        content.messageView.visibility = View.GONE
        content.listView.visibility = View.GONE
        showNear(content.root, line, column)
        return token
    }

    fun isTokenActive(token: Int): Boolean {
        return token == loadToken.get() && isShowing
    }

    fun showMessage(token: Int, title: CharSequence, message: CharSequence) {
        if (!isTokenActive(token)) return
        val content = currentContent() ?: return
        content.titleView.text = title
        content.progress.visibility = View.GONE
        content.listView.visibility = View.GONE
        content.messageView.visibility = View.VISIBLE
        content.messageView.text = message
        content.messageView.maxHeight = (editor.resources.displayMetrics.heightPixels * 0.45f).toInt()
            .coerceAtLeast((editor.dpUnit * 280).toInt())
        content.messageView.movementMethod = ScrollingMovementMethod.getInstance()
        content.messageView.setOnTouchListener { v, event ->
            v.parent?.requestDisallowInterceptTouchEvent(true)
            v.onTouchEvent(event)
            true
        }
        resizeToContent(content.root)
    }

    fun showReferenceList(
        token: Int,
        title: CharSequence,
        items: List<ReferenceItem>,
        onItemClick: (Int) -> Unit
    ) {
        if (!isTokenActive(token)) return
        val content = currentContent() ?: return
        content.titleView.text = title
        content.progress.visibility = View.GONE
        content.messageView.visibility = View.GONE
        content.listView.visibility = View.VISIBLE
        content.listView.adapter = ReferenceListAdapter(items)
        content.listView.setOnItemClickListener { _, _, position, _ ->
            onItemClick(position)
            dismiss()
        }
        // ListView 在 Popup 里用 WRAP_CONTENT 往往只算出一行高，按条目估算高度
        val screenH = editor.resources.displayMetrics.heightPixels
        val maxListHeight = (screenH * 0.55f).toInt().coerceAtLeast((editor.dpUnit * 320).toInt())
        val itemHeight = (editor.dpUnit * 40).toInt()
        val desired = (items.size * itemHeight + (editor.dpUnit * 8).toInt())
            .coerceAtMost(maxListHeight)
            .coerceAtLeast(itemHeight)
        content.listView.layoutParams = content.listView.layoutParams.apply {
            height = desired
        }
        resizeToContent(content.root)
    }

    data class ReferenceItem(
        val fileName: String,
        val lineNumber: Int,
        val snippet: CharSequence
    )

    private class ReferenceListAdapter(
        private val items: List<ReferenceItem>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): ReferenceItem = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_editor_lsp_reference, parent, false)
            val item = items[position]
            view.findViewById<ImageView>(R.id.lsp_ref_icon).setImageDrawable(
                EditorIdeaKindIcons.create(parent.context, CompletionItemKind.Reference)
            )
            view.findViewById<TextView>(R.id.lsp_ref_file).text = item.fileName
            view.findViewById<TextView>(R.id.lsp_ref_line).text = item.lineNumber.toString()
            view.findViewById<TextView>(R.id.lsp_ref_snippet).text = item.snippet
            return view
        }
    }

    fun showEmpty(token: Int, title: CharSequence, emptyMessage: CharSequence) {
        showMessage(token, title, emptyMessage)
    }

    private fun inflateContent(): ContentViews {
        val root = LayoutInflater.from(editor.context)
            .inflate(R.layout.popup_editor_lsp_symbol, FrameLayout(editor.context), false)
        return ContentViews(
            root = root,
            titleView = root.findViewById(R.id.lsp_popup_title),
            progress = root.findViewById(R.id.lsp_popup_progress),
            messageView = root.findViewById(R.id.lsp_popup_message),
            listView = root.findViewById(R.id.lsp_popup_list)
        ).also { root.tag = it }
    }

    private fun currentContent(): ContentViews? {
        return popup?.contentView?.tag as? ContentViews
    }

    private fun showNear(content: View, line: Int, column: Int) {
        val width = (editor.width * 0.86f).toInt().coerceIn(
            (editor.dpUnit * 220).toInt(),
            (editor.dpUnit * 360).toInt()
        )
        content.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWindow = PopupWindow(
            content,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(0x00000000))
            elevation = editor.dpUnit * 8
            setOnDismissListener {
                if (popup === this) {
                    loadToken.incrementAndGet()
                    popup = null
                    onDismissExtra?.invoke()
                    onDismissExtra = null
                }
            }
        }
        popup = popupWindow
        val loc = IntArray(2)
        editor.getLocationInWindow(loc)
        val anchorX = editor.getCharOffsetX(line, column)
        val anchorY = editor.getCharOffsetY(line, column)
        val rowBottom: Float = try {
            val pos = editor.text.indexer.getCharPosition(line, column)
            val rowIndex = editor.layout.getRowIndexForPosition(pos.index)
            editor.getRowBottomOfText(rowIndex).toFloat() - editor.offsetY.toFloat()
        } catch (_: Exception) {
            anchorY + editor.dpUnit * 18f
        }
        content.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupH = content.measuredHeight
        val popupW = width
        var x = (loc[0] + anchorX - popupW / 4f).toInt()
        var y = (loc[1] + rowBottom + editor.dpUnit * 4f).toInt()
        val screenW = editor.resources.displayMetrics.widthPixels
        val screenH = editor.resources.displayMetrics.heightPixels
        x = x.coerceIn(8, (screenW - popupW - 8).coerceAtLeast(8))
        // 下方空间不足则翻到文字上方，避免跑到屏幕最底部
        if (y + popupH > screenH - 24) {
            y = (loc[1] + anchorY - popupH - editor.dpUnit * 8f).toInt()
        }
        y = y.coerceIn(24, (screenH - popupH - 24).coerceAtLeast(24))
        popupWindow.showAtLocation(editor, Gravity.NO_GRAVITY, x, y)
    }

    private fun resizeToContent(content: View) {
        val p = popup ?: return
        val width = p.width
        content.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        p.update(width, content.measuredHeight)
    }

    private data class ContentViews(
        val root: View,
        val titleView: TextView,
        val progress: ProgressBar,
        val messageView: TextView,
        val listView: ListView
    )

    companion object {
        data class IdentifierHit(
            val word: String,
            val line: Int,
            val startColumn: Int,
            val endColumn: Int
        )

        fun findIdentifierAt(editor: CodeEditor, line: Int, column: Int): IdentifierHit? {
            if (line < 0 || line >= editor.lineCount) return null
            val lineText = editor.text.getLineString(line)
            if (lineText.isEmpty()) return null
            var index = column.coerceIn(0, lineText.length)
            if (index == lineText.length) index -= 1
            if (index < 0 || !isIdentChar(lineText[index])) {
                if (index > 0 && isIdentChar(lineText[index - 1])) {
                    index -= 1
                } else {
                    return null
                }
            }
            var start = index
            while (start > 0 && isIdentChar(lineText[start - 1])) start--
            var end = index + 1
            while (end < lineText.length && isIdentChar(lineText[end])) end++
            val word = lineText.substring(start, end)
            if (word.isEmpty()) return null
            return IdentifierHit(word, line, start, end)
        }

        private fun isIdentChar(c: Char): Boolean {
            return c.isLetterOrDigit() || c == '_' || c == '$'
        }

        fun buildReferenceItem(
            location: EditorLspLocation,
            highlightWord: String?
        ): ReferenceItem {
            val lineNumber = location.line + 1
            val fromClass = EditorJdtClassFileSupport.needsClassFileContents(location)
            val fileName = if (fromClass && location.uri.isNotBlank()) {
                EditorJdtClassFileSupport.displayJavaName(location.uri)
            } else {
                location.file.name
            }
            val rawLine = when {
                location.file.isFile -> readLineSnippet(location.file, location.line)
                fromClass -> location.uri.substringBefore('?').substringAfterLast('/')
                else -> ""
            }
            val snippet = highlightSnippet(rawLine, highlightWord, location.column)
            return ReferenceItem(
                fileName = fileName,
                lineNumber = lineNumber,
                snippet = snippet
            )
        }

        private fun readLineSnippet(file: java.io.File, line: Int): String {
            if (line < 0 || !file.isFile) return ""
            return runCatching {
                file.useLines { lines ->
                    lines.drop(line).firstOrNull().orEmpty()
                }
            }.getOrDefault("").trim()
        }

        private fun highlightSnippet(
            rawLine: String,
            highlightWord: String?,
            columnHint: Int
        ): CharSequence {
            if (rawLine.isEmpty()) return ""
            val collapsed = rawLine.replace('\t', ' ').trim()
            if (highlightWord.isNullOrBlank()) return collapsed
            val spannable = SpannableString(collapsed)
            val lower = collapsed.lowercase()
            val target = highlightWord.lowercase()
            // 优先按列附近匹配，否则取第一个整词命中
            var start = -1
            val approx = columnHint.coerceIn(0, collapsed.length)
            val local = collapsed.indexOf(highlightWord, startIndex = (approx - highlightWord.length).coerceAtLeast(0), ignoreCase = true)
            if (local >= 0) {
                start = local
            } else {
                start = lower.indexOf(target)
            }
            if (start >= 0) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    (start + highlightWord.length).coerceAtMost(collapsed.length),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }
    }
}
