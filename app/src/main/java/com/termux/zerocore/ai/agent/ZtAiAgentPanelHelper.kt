package com.termux.zerocore.ai.agent

import android.content.Context
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.app.Activity
import android.content.Intent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.termux.R
import com.termux.zerocore.ai.deepseek.markdown.MarkDownAPI
import com.termux.zerocore.ai.deepseek.utils.SpannableTextUtil
import com.termux.zerocore.utils.SingletonCommunicationUtils
import io.noties.markwon.Markwon

class ZtAiAgentPanelHelper(
    private val overlay: View,
    panelRoot: View,
    private val onOpenRightMenu: Runnable? = null
) {
    private val panelCard: View = panelRoot
    private val contextLabel: TextView = panelRoot.findViewById(R.id.ai_agent_panel_context_label)
    private val contextText: TextView = panelRoot.findViewById(R.id.ai_agent_panel_context_text)
    private val emptyView: TextView = panelRoot.findViewById(R.id.ai_agent_panel_empty)
    private val messagesContainer: LinearLayout = panelRoot.findViewById(R.id.ai_agent_panel_messages)
    private val scrollView: ScrollView = panelRoot.findViewById(R.id.ai_agent_panel_scroll)
    private val input: EditText = panelRoot.findViewById(R.id.ai_agent_panel_input)
    private val sendButton: TextView = panelRoot.findViewById(R.id.ai_agent_panel_send)

    private val conversationHistory = ZtAgentAiChatStore.load()
    private var chatClient: ZtAgentAiChatClient? = null
    private var agentRunner: ZtAgentAiAgentRunner? = null
    private var isSending = false
    private var agentCancelled = false
    /** 悬浮窗是否处于打开状态（与 overlay 动画结束后的实际展示一致）。 */
    private var isPanelShown = false
    private var lastPanelHeight = 0
    private var keyboardListenerAttached = false

    private var pendingAssistantRow: View? = null

    private val scrollBottomRunnable1 = Runnable { performScrollToBottom() }
    private val scrollBottomRunnable2 = Runnable { performScrollToBottom() }

    private val keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        onPanelLayoutChanged()
    }

    private val markwon: Markwon by lazy {
        Markwon.builder(panelCard.context)
            .usePlugin(MarkDownAPI.create(panelCard.context))
            .build()
    }

    private val runningBanner: View? by lazy {
        findRunningBannerView()
    }

    private fun findRunningBannerView(): View? {
        var node: View? = overlay
        while (node != null) {
            node.findViewById<View>(R.id.ai_agent_running_banner)?.let { return it }
            node = node.parent as? View
        }
        return panelCard.rootView.findViewById(R.id.ai_agent_running_banner)
    }

    private val panelStopBar: View? by lazy {
        overlay.findViewById(R.id.ai_agent_panel_stop_bar)
    }

    init {
        panelRoot.findViewById<View>(R.id.ai_agent_panel_close).setOnClickListener { dismissPanel() }
        panelRoot.findViewById<View>(R.id.ai_agent_panel_reset).setOnClickListener {
            ZtAgentAiResetHelper.showResetConfirmDialog(panelRoot.context)
        }
        panelRoot.findViewById<View>(R.id.ai_agent_panel_settings).setOnClickListener {
            openAgentSettings()
        }
        panelRoot.findViewById<View>(R.id.ai_agent_panel_open_menu).setOnClickListener {
            dismissPanel()
            onOpenRightMenu?.run()
        }
        overlay.setOnClickListener { dismissPanel() }
        panelCard.isFocusable = false
        panelCard.isFocusableInTouchMode = false
        sendButton.setOnClickListener { onSendClicked() }
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
                scrollToBottomDelayed()
            }
        }
        restoreConversationUi()
        ZtAgentAiResetHelper.registerUiRefreshCallback { clearUiAfterReset() }
        bindStopBar(panelStopBar)
        bindStopBar(runningBanner, reopenOnLabelClick = true)
    }

    private fun bindStopBar(bar: View?, reopenOnLabelClick: Boolean = false) {
        bar ?: return
        bar.findViewById<View>(R.id.ai_agent_stop_button)?.setOnClickListener {
            stopAgentExecution()
        }
        if (reopenOnLabelClick) {
            bar.findViewById<View>(R.id.ai_agent_stop_bar_label)?.setOnClickListener {
                show(null)
            }
        }
    }

    /** 打断 AI 自动执行（不关闭悬浮窗）。 */
    fun stopAgentExecution() {
        if (!isSending) return
        agentCancelled = true
        chatClient?.cancel()
        agentRunner?.cancel()
        sendTerminalInterrupt()
        setSending(false)
        pendingAssistantRow?.findViewById<TextView>(R.id.agent_message_content)?.let { content ->
            renderMarkdown(content, panelCard.context.getString(R.string.zt_ai_agent_stopped))
        }
        pendingAssistantRow = null
    }

    private fun sendTerminalInterrupt() {
        if (!ZtAgentAiConfigHelper.isTerminalEnabled()) return
        val utils = SingletonCommunicationUtils.getInstance()
        if (!utils.hasTerminalListener()) return
        try {
            utils.getmSingletonCommunicationListener()?.sendTextToTerminalCtrl("c", true)
        } catch (_: Exception) {
        }
    }

    private fun clearUiAfterReset() {
        agentCancelled = true
        chatClient?.cancel()
        agentRunner?.cancel()
        conversationHistory.clear()
        pendingAssistantRow = null
        messagesContainer.removeAllViews()
        emptyView.visibility = View.VISIBLE
        messagesContainer.visibility = View.GONE
        contextLabel.visibility = View.GONE
        contextText.visibility = View.GONE
        contextText.text = ""
        input.setText("")
        setSending(false)
        updateStopBarsVisibility()
        scrollToBottom()
    }

    private fun attachKeyboardListener() {
        if (keyboardListenerAttached) return
        panelCard.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
        keyboardListenerAttached = true
    }

    private fun detachKeyboardListener() {
        if (!keyboardListenerAttached) return
        panelCard.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutListener)
        keyboardListenerAttached = false
        lastPanelHeight = 0
    }

    private fun onPanelLayoutChanged() {
        if (!isVisible()) return
        val height = panelCard.height
        if (height == lastPanelHeight) return
        lastPanelHeight = height
        scrollToBottomDelayed()
    }

    private fun restoreConversationUi() {
        if (conversationHistory.isEmpty()) return
        showMessagesArea()
        conversationHistory.forEach { message ->
            when (message.role) {
                ROLE_USER -> appendUserMessage(message.content.orEmpty(), persist = false)
                ROLE_ASSISTANT -> appendAssistantMessage(message.content.orEmpty(), isError = false, persist = false)
            }
        }
        scrollToBottom()
    }

    private fun onSendClicked() {
        if (isSending) return
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        if (!ZtAgentAiConfigHelper.isConfigured()) {
            appendAssistantMessage(
                panelCard.context.getString(R.string.zt_ai_agent_not_configured),
                isError = true,
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
            isError = false,
            persist = false
        )
        setSending(true)
        agentCancelled = false

        chatClient?.cancel()
        agentRunner?.cancel()
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        chatClient = ZtAgentAiChatClient(config)

        if (ZtAgentAiConfigHelper.isAgentToolsEnabled()) {
            runAgentLoop(assistantView)
        } else {
            runStreamChat(assistantView)
        }
    }

    private fun runAgentLoop(assistantView: TextView) {
        agentRunner = ZtAgentAiAgentRunner(
            chatClient!!,
            terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled(),
            ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled()
        )
        agentRunner!!.run(conversationHistory, object : ZtAgentAiAgentRunner.Callback {
            override fun onToolStep(label: String, detail: String) {
                appendToolStep(label, detail)
            }

            override fun onComplete(content: String) {
                finishAssistantReply(assistantView, content)
            }

            override fun onError(message: String) {
                renderMarkdown(assistantView, message)
                assistantView.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
                setSending(false)
                updateStopBarsVisibility()
            }

            override fun isCancelled(): Boolean = agentCancelled
        })
    }

    private fun runStreamChat(assistantView: TextView) {
        val requestMessages = buildRequestMessages()
        chatClient!!.chat(requestMessages, stream = true, listener = object : ZtAgentAiChatClient.Listener {
            override fun onChunk(text: String) {
                renderMarkdown(assistantView, text)
                scrollToBottom()
            }

            override fun onError(message: String) {
                renderMarkdown(assistantView, message)
                assistantView.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
                setSending(false)
                updateStopBarsVisibility()
            }

            override fun onComplete(fullText: String) {
                finishAssistantReply(assistantView, fullText)
            }
        })
    }

    private fun finishAssistantReply(assistantView: TextView, fullText: String) {
        setSending(false)
        updateStopBarsVisibility()
        pendingAssistantRow = null
        if (fullText.isNotBlank()) {
            renderMarkdown(assistantView, fullText)
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_ASSISTANT, fullText))
            ZtAgentAiChatStore.save(conversationHistory)
            scrollToBottom()
        } else {
            removeMessageView(assistantView)
        }
    }

    /** 工具步骤小窗：插入在「思考中/回复」气泡之前，完成后保留 */
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

    private fun removeMessageView(contentView: TextView) {
        val row = contentView.parent as? View ?: return
        messagesContainer.removeView(row)
    }

    private fun buildRequestMessages(): List<ZtAgentAiChatClient.ChatMessage> {
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        val list = mutableListOf<ZtAgentAiChatClient.ChatMessage>()
        list.add(
            ZtAgentAiChatClient.ChatMessage(
                role = ROLE_SYSTEM,
                content = ZtAgentAiConfigHelper.resolveSystemPrompt(
                    config.systemPrompt,
                    terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled(),
                    ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled()
                )
            )
        )
        list.addAll(conversationHistory)
        return list
    }

    private fun appendUserMessage(text: String, persist: Boolean) {
        showMessagesArea()
        val itemView = inflateMessageItem(isUser = true)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        renderMarkdown(content, text)
        messagesContainer.addView(itemView)
        if (persist) {
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_USER, text))
            ZtAgentAiChatStore.save(conversationHistory)
        }
    }

    private fun appendAssistantMessage(text: String, isError: Boolean, persist: Boolean): TextView {
        showMessagesArea()
        val itemView = inflateMessageItem(isUser = false)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        renderMarkdown(content, text)
        if (isError) {
            content.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
        }
        messagesContainer.addView(itemView)
        if (!persist) {
            pendingAssistantRow = itemView
        }
        if (persist) {
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_ASSISTANT, text))
            ZtAgentAiChatStore.save(conversationHistory)
        }
        return content
    }

    private fun renderMarkdown(textView: TextView, markdown: String) {
        val spanned = markwon.toMarkdown(markdown)
        val finalSpanned = SpannableTextUtil.createClickableSpannableString(spanned, panelCard.context)
        markwon.setParsedMarkdown(textView, finalSpanned)
        textView.movementMethod = LinkMovementMethod.getInstance()
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

    private fun showMessagesArea() {
        emptyView.visibility = View.GONE
        messagesContainer.visibility = View.VISIBLE
    }

    private fun scrollToBottom() {
        scrollView.post { performScrollToBottom() }
    }

    private fun scrollToBottomDelayed() {
        scrollToBottom()
        scrollView.removeCallbacks(scrollBottomRunnable1)
        scrollView.removeCallbacks(scrollBottomRunnable2)
        scrollView.postDelayed(scrollBottomRunnable1, 100)
        scrollView.postDelayed(scrollBottomRunnable2, 280)
    }

    private fun performScrollToBottom() {
        val child = scrollView.getChildAt(0) ?: return
        val target = (child.height - scrollView.height + scrollView.paddingBottom).coerceAtLeast(0)
        scrollView.scrollTo(0, target)
    }

    private fun setSending(sending: Boolean) {
        isSending = sending
        sendButton.isEnabled = !sending
        sendButton.alpha = if (sending) 0.5f else 1f
        input.isEnabled = !sending
        updateStopBarsVisibility()
    }

    fun isVisible(): Boolean = overlay.visibility == View.VISIBLE

    fun show(selectedText: String?) {
        applySelectedText(selectedText)
        attachKeyboardListener()
        input.clearFocus()
        if (isPanelShown && overlay.visibility == View.VISIBLE && panelCard.translationX == 0f) {
            updateStopBarsVisibility()
            scrollToBottomDelayed()
            return
        }
        panelCard.animate().cancel()
        overlay.animate().cancel()
        overlay.visibility = View.VISIBLE
        isPanelShown = true
        updateStopBarsVisibility()
        overlay.alpha = 0f
        panelCard.post {
            val slideDistance = slideDistance()
            panelCard.translationX = slideDistance
            overlay.animate()
                .alpha(1f)
                .setDuration(OVERLAY_DURATION_MS)
                .start()
            panelCard.animate()
                .translationX(0f)
                .setDuration(PANEL_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    scrollToBottomDelayed()
                    updateStopBarsVisibility()
                }
                .start()
        }
    }

    /** 收起悬浮窗；若 AI 仍在执行则转入后台并显示主界面顶部提示条。 */
    fun dismissPanel() {
        if (!isPanelShown) return
        hideInputKeyboard()
        if (!isSending) {
            pendingAssistantRow = null
        }
        animatePanelAway()
    }

    /** 强制停止 AI 并关闭面板（重置等场景使用）。 */
    fun hide() {
        if (!isPanelShown && !isSending) {
            updateStopBarsVisibility()
            return
        }
        hideInputKeyboard()
        if (isSending) {
            stopAgentExecution()
        } else {
            pendingAssistantRow = null
            updateStopBarsVisibility()
        }
        if (isPanelShown) {
            animatePanelAway()
        }
    }

    private fun updateStopBarsVisibility() {
        val showPanelBar = isSending && isPanelShown
        val showTopBar = isSending && !isPanelShown
        panelStopBar?.visibility = if (showPanelBar) View.VISIBLE else View.GONE
        val bar = runningBanner ?: return
        if (showTopBar) {
            ZtAiAgentTopBannerAnimator.show(bar)
        } else {
            ZtAiAgentTopBannerAnimator.hide(bar)
        }
    }

    /** 打开 App 页面时仅收起面板，不中断进行中的 AI 对话 */
    fun minimizeForNavigation() {
        dismissPanel()
    }

    private fun animatePanelAway() {
        scrollView.removeCallbacks(scrollBottomRunnable1)
        scrollView.removeCallbacks(scrollBottomRunnable2)
        detachKeyboardListener()
        panelCard.animate().cancel()
        overlay.animate().cancel()
        val slideDistance = slideDistance()
        overlay.animate()
            .alpha(0f)
            .setDuration(OVERLAY_DURATION_MS)
            .start()
        panelCard.animate()
            .translationX(slideDistance)
            .setDuration(PANEL_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                overlay.visibility = View.GONE
                panelCard.translationX = 0f
                overlay.alpha = 1f
                isPanelShown = false
                updateStopBarsVisibility()
            }
            .start()
    }

    fun toggle(selectedText: String?) {
        if (isPanelShown) {
            dismissPanel()
        } else {
            show(selectedText)
        }
    }

    private fun applySelectedText(selectedText: String?) {
        if (!TextUtils.isEmpty(selectedText)) {
            contextLabel.visibility = View.VISIBLE
            contextText.visibility = View.VISIBLE
            contextText.text = selectedText
            input.setText(selectedText)
            input.setSelection(selectedText!!.length)
        } else {
            contextLabel.visibility = View.GONE
            contextText.visibility = View.GONE
            contextText.text = ""
        }
    }

    private fun slideDistance(): Float {
        val width = panelCard.width
        if (width > 0) return width.toFloat()
        return panelCard.context.resources.displayMetrics.widthPixels * 0.45f
    }

    private fun showInputKeyboard() {
        if (!isVisible() || !input.isEnabled) return
        clearTerminalFocus()
        val imm = panelCard.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(panelCard.rootView.windowToken, 0)
        input.requestFocus()
        input.postDelayed({
            if (!isVisible()) return@postDelayed
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 120)
    }

    private fun hideInputKeyboard() {
        val imm = panelCard.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun openAgentSettings() {
        hideInputKeyboard()
        panelCard.context.startActivity(
            Intent(panelCard.context, ZtAgentAiSettingsActivity::class.java)
        )
    }

    private fun clearTerminalFocus() {
        panelCard.rootView.findViewById<View>(R.id.terminal_view)?.clearFocus()
        val activity = panelCard.context as? Activity
        activity?.currentFocus?.takeIf { it.id != R.id.ai_agent_panel_input }?.clearFocus()
    }

    companion object {
        private const val PANEL_DURATION_MS = 280L
        private const val OVERLAY_DURATION_MS = 220L
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_SYSTEM = "system"
    }
}
