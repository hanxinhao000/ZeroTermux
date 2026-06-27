package com.termux.zerocore.ai.editor

import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.termux.R
import com.termux.zerocore.ai.agent.ZtAgentAiChatClient
import com.termux.zerocore.ai.agent.ZtAgentAiConfigHelper
import com.termux.zerocore.ai.deepseek.markdown.MarkDownAPI
import com.termux.zerocore.ai.deepseek.utils.SpannableTextUtil
import io.noties.markwon.Markwon
import kotlin.math.abs

class ZtEditorAiPanelHelper(
    private val overlay: View,
    panelRoot: View,
    private val host: ZtEditorAiHost,
    private val applyPanelHeight: (() -> Unit)? = null
) {
    private val panelCard: View = panelRoot
    private val dragHandle: View = panelRoot.findViewById(R.id.editor_ai_panel_drag_handle)
    private val resetButton: View = panelRoot.findViewById(R.id.editor_ai_panel_reset)
    private val closeButton: View = panelRoot.findViewById(R.id.editor_ai_panel_close)
    private val emptyView: TextView = panelRoot.findViewById(R.id.editor_ai_panel_empty)
    private val messagesContainer: LinearLayout = panelRoot.findViewById(R.id.editor_ai_panel_messages)
    private val scrollView: ScrollView = panelRoot.findViewById(R.id.editor_ai_panel_scroll)
    private val input: EditText = panelRoot.findViewById(R.id.editor_ai_panel_input)
    private val sendButton: TextView = panelRoot.findViewById(R.id.editor_ai_panel_send)

    private val conversationHistory = ZtEditorAiChatStore.load()
    private var chatClient: ZtAgentAiChatClient? = null
    private var agentRunner: ZtEditorAiAgentRunner? = null
    private var isSending = false
    private var cancelled = false
    private var pendingAssistantRow: View? = null

    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var dragStartTransX = 0f
    private var dragStartTransY = 0f
    private var dragging = false

    private val markwon: Markwon by lazy {
        Markwon.builder(panelCard.context)
            .usePlugin(MarkDownAPI.create(panelCard.context))
            .build()
    }

    init {
        panelCard.isFocusable = false
        panelCard.isFocusableInTouchMode = false
        panelRoot.findViewById<View>(R.id.editor_ai_panel_reset).setOnClickListener {
            ZtEditorAiResetHelper.showResetConfirmDialog(panelCard.context)
        }
        panelRoot.findViewById<View>(R.id.editor_ai_panel_close).setOnClickListener { hide() }
        sendButton.setOnClickListener { onSendClicked() }
        input.isFocusableInTouchMode = true
        input.setOnClickListener { showInputKeyboard() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSendClicked()
                true
            } else {
                false
            }
        }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isVisible()) {
                scrollToBottom()
            }
        }
        setupDrag()
        restoreConversationUi()
        ZtEditorAiResetHelper.registerUiRefreshCallback { clearUiAfterReset() }
    }

    private fun clearUiAfterReset() {
        cancelled = true
        chatClient?.cancel()
        agentRunner?.cancel()
        conversationHistory.clear()
        pendingAssistantRow = null
        messagesContainer.removeAllViews()
        emptyView.visibility = View.VISIBLE
        messagesContainer.visibility = View.GONE
        input.setText("")
        setSending(false)
        scrollToBottom()
    }

    private fun setupDrag() {
        dragHandle.setOnTouchListener { _, event ->
            if (isTouchOnView(event, resetButton) || isTouchOnView(event, closeButton)) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    dragStartRawX = event.rawX
                    dragStartRawY = event.rawY
                    dragStartTransX = panelCard.translationX
                    dragStartTransY = panelCard.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartRawX
                    val dy = event.rawY - dragStartRawY
                    if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) {
                        dragging = true
                    }
                    if (dragging) {
                        panelCard.translationX = dragStartTransX + dx
                        panelCard.translationY = dragStartTransY + dy
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun isTouchOnView(event: MotionEvent, view: View): Boolean {
        if (view.visibility != View.VISIBLE) return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        return x >= location[0] && x <= location[0] + view.width &&
            y >= location[1] && y <= location[1] + view.height
    }

    fun toggle() {
        if (isVisible()) hide() else show()
    }

    fun isVisible(): Boolean = overlay.visibility == View.VISIBLE

    fun hide() {
        if (!isVisible()) return
        hideInputKeyboard()
        host.restoreEditorInputAfterAiPanel()
        cancelled = true
        chatClient?.cancel()
        agentRunner?.cancel()
        setSending(false)
        pendingAssistantRow = null
        overlay.animate().alpha(0f).setDuration(180).withEndAction {
            overlay.visibility = View.GONE
            overlay.alpha = 1f
        }.start()
        panelCard.animate()
            .translationY(panelCard.height.toFloat())
            .setDuration(220)
            .start()
    }

    fun show() {
        if (isVisible()) return
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.requestLayout()
        panelCard.post {
            applyPanelHeight?.invoke()
            panelCard.requestLayout()
            panelCard.post { animateShowIn() }
        }
    }

    private fun animateShowIn() {
        val slideDistance = panelCard.height.takeIf { it > 0 }
            ?: (overlay.height * 0.45f).toInt().coerceAtLeast(200)
        panelCard.translationY = slideDistance.toFloat()
        overlay.animate().alpha(1f).setDuration(180).start()
        panelCard.animate()
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { scrollToBottom() }
            .start()
    }

    fun destroy() {
        ZtEditorAiResetHelper.registerUiRefreshCallback(null)
        hide()
        chatClient?.cancel()
        agentRunner?.cancel()
    }

    private fun onSendClicked() {
        if (isSending) return
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        if (!ZtAgentAiConfigHelper.isConfigured()) {
            appendAssistantMessage(
                panelCard.context.getString(R.string.zt_ai_agent_not_configured),
                persist = false
            )
            scrollToBottom()
            return
        }
        input.setText("")
        appendUserMessage(text, persist = true)
        scrollToBottom()
        val assistantView = appendAssistantMessage(
            panelCard.context.getString(R.string.zt_ai_agent_thinking),
            persist = false
        )
        setSending(true)
        cancelled = false
        chatClient?.cancel()
        agentRunner?.cancel()
        chatClient = ZtAgentAiChatClient(ZtAgentAiConfigHelper.loadActiveConfig())
        agentRunner = ZtEditorAiAgentRunner(chatClient!!, host)
        agentRunner!!.run(conversationHistory, object : ZtEditorAiAgentRunner.Callback {
            override fun onToolStep(label: String, detail: String) {
                appendToolStep(label, detail)
            }

            override fun onComplete(content: String) {
                finishAssistantReply(assistantView, content)
            }

            override fun onError(message: String) {
                renderMarkdown(assistantView, message)
                setSending(false)
            }

            override fun isCancelled(): Boolean = cancelled
        })
    }

    private fun finishAssistantReply(assistantView: TextView, fullText: String) {
        setSending(false)
        pendingAssistantRow = null
        if (fullText.isNotBlank()) {
            renderMarkdown(assistantView, fullText)
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_ASSISTANT, fullText))
            ZtEditorAiChatStore.save(conversationHistory)
            scrollToBottom()
        } else {
            removeMessageView(assistantView)
        }
    }

    private fun appendToolStep(label: String, detail: String) {
        showMessagesArea()
        val itemView = LayoutInflater.from(panelCard.context)
            .inflate(R.layout.view_agent_ai_tool_step, messagesContainer, false)
        itemView.findViewById<TextView>(R.id.agent_tool_step_label).text = label
        val content = itemView.findViewById<TextView>(R.id.agent_tool_step_content)
        if (detail.isNotBlank()) {
            content.visibility = View.VISIBLE
            content.text = detail
        } else {
            content.visibility = View.GONE
        }
        val anchor = pendingAssistantRow
        val insertIndex = if (anchor != null) {
            messagesContainer.indexOfChild(anchor).takeIf { it >= 0 } ?: messagesContainer.childCount
        } else {
            messagesContainer.childCount
        }
        messagesContainer.addView(itemView, insertIndex)
        scrollToBottom()
    }

    private fun restoreConversationUi() {
        if (conversationHistory.isEmpty()) return
        showMessagesArea()
        conversationHistory.forEach { message ->
            when (message.role) {
                ROLE_USER -> appendUserMessage(message.content.orEmpty(), persist = false)
                ROLE_ASSISTANT -> appendAssistantMessage(message.content.orEmpty(), persist = false)
            }
        }
        scrollToBottom()
    }

    private fun appendUserMessage(text: String, persist: Boolean) {
        showMessagesArea()
        val itemView = inflateMessageItem(isUser = true)
        renderMarkdown(itemView.findViewById(R.id.agent_message_content), text)
        messagesContainer.addView(itemView)
        if (persist) {
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_USER, text))
            ZtEditorAiChatStore.save(conversationHistory)
        }
    }

    private fun appendAssistantMessage(text: String, persist: Boolean): TextView {
        showMessagesArea()
        val itemView = inflateMessageItem(isUser = false)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        renderMarkdown(content, text)
        messagesContainer.addView(itemView)
        if (!persist) pendingAssistantRow = itemView
        if (persist) {
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_ASSISTANT, text))
            ZtEditorAiChatStore.save(conversationHistory)
        }
        return content
    }

    private fun inflateMessageItem(isUser: Boolean): View {
        val itemView = LayoutInflater.from(panelCard.context)
            .inflate(R.layout.view_agent_ai_message_item, messagesContainer, false)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        val lp = content.layoutParams as LinearLayout.LayoutParams
        if (isUser) {
            content.setBackgroundResource(R.drawable.shape_agent_msg_user)
            lp.gravity = Gravity.END
        } else {
            content.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
            lp.gravity = Gravity.START
        }
        content.layoutParams = lp
        return itemView
    }

    private fun renderMarkdown(textView: TextView, markdown: String) {
        val spanned = markwon.toMarkdown(markdown)
        val finalSpanned = SpannableTextUtil.createClickableSpannableString(spanned, panelCard.context)
        markwon.setParsedMarkdown(textView, finalSpanned)
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun removeMessageView(contentView: TextView) {
        val row = contentView.parent as? View ?: return
        messagesContainer.removeView(row)
    }

    private fun showMessagesArea() {
        emptyView.visibility = View.GONE
        messagesContainer.visibility = View.VISIBLE
    }

    private fun scrollToBottom() {
        scrollView.post {
            val child = scrollView.getChildAt(0) ?: return@post
            val target = (child.height - scrollView.height + scrollView.paddingBottom).coerceAtLeast(0)
            scrollView.scrollTo(0, target)
        }
    }

    private fun setSending(sending: Boolean) {
        isSending = sending
        sendButton.isEnabled = !sending
        sendButton.alpha = if (sending) 0.5f else 1f
        input.isEnabled = !sending
    }

    private fun hideInputKeyboard() {
        val imm = panelCard.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun showInputKeyboard() {
        if (!isVisible() || !input.isEnabled) return
        host.releaseEditorInputForAiPanel()
        val imm = panelCard.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
        imm.hideSoftInputFromWindow(panelCard.rootView.windowToken, 0)
        input.requestFocus()
        input.postDelayed({
            if (!isVisible()) return@postDelayed
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 120)
    }

    companion object {
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
    }
}
