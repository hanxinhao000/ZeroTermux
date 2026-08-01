package com.termux.zerocore.ai.agent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.view.TerminalView
import com.termux.zerocore.utils.SingletonCommunicationUtils
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TableAwareMovementMethod

/**
 * AI 智能体面板控制器：嵌入右侧栏 AI 选项卡。
 * [panelHost] 为右侧栏内的面板根视图；[isDrawerOpen] 用于判断顶栏打断条是否显示。
 */
class ZtAiAgentPanelHelper(
    private val panelHost: View,
    private val hostActivity: Activity,
    private val onCloseDrawer: Runnable? = null,
    private val onOpenAiTab: Runnable? = null,
    private val isDrawerOpen: (() -> Boolean)? = null,
    topRunningBanner: View? = null
) {
    private val panelCard: View = panelHost
    private val contextLabel: TextView = panelHost.findViewById(R.id.ai_agent_panel_context_label)
    private val contextText: TextView = panelHost.findViewById(R.id.ai_agent_panel_context_text)
    private val emptyView: TextView = panelHost.findViewById(R.id.ai_agent_panel_empty)
    private val messagesContainer: LinearLayout = panelHost.findViewById(R.id.ai_agent_panel_messages)
    private val scrollView: ScrollView = panelHost.findViewById(R.id.ai_agent_panel_scroll)
    private val input: EditText = panelHost.findViewById(R.id.ai_agent_panel_input)
    private val sendButton: TextView = panelHost.findViewById(R.id.ai_agent_panel_send)
    private val panelStopBar: View? = panelHost.findViewById(R.id.ai_agent_panel_stop_bar)

    private val conversationHistory = ZtAgentAiChatStore.load()
    private var chatClient: ZtAgentAiChatClient? = null
    private var agentRunner: ZtAgentAiAgentRunner? = null
    private var isSending = false
    private var agentCancelled = false
    /** 当前工具调用轮次 / 上限（与设置「工具调用轮数上限」一致）。 */
    private var toolRoundCurrent = 0
    private var toolRoundMax = ZtAgentAiConfigHelper.DEFAULT_TOOL_ROUNDS
    /** 右侧栏 AI 选项卡是否正在展示本面板。 */
    private var isPanelShown = false
    private var lastPanelHeight = 0
    private var lastImePad = -1
    private var keyboardListenerAttached = false

    private var pendingAssistantRow: View? = null

    /** 抬起目标：右侧栏整体（含底部选项卡），避免键盘挡住输入框。 */
    private val imeLiftTarget: View by lazy {
        (panelHost.parent as? View)?.parent as? View ?: panelHost
    }

    private val scrollBottomRunnable1 = Runnable { performScrollToBottom() }
    private val scrollBottomRunnable2 = Runnable { performScrollToBottom() }

    private val keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        updateImeLiftPadding()
        onPanelLayoutChanged()
    }

    private val markwon: Markwon by lazy { ZtAgentMarkwon.get(panelCard.context) }

    private val runningBanner: View? =
        topRunningBanner ?: hostActivity.findViewById(R.id.ai_agent_running_banner)

    init {
        panelHost.findViewById<View>(R.id.ai_agent_panel_skills).setOnClickListener {
            openSkillsPage()
        }
        panelHost.findViewById<View>(R.id.ai_agent_panel_reset).setOnClickListener {
            ZtAgentAiResetHelper.showResetConfirmDialog(hostActivity)
        }
        panelHost.findViewById<View>(R.id.ai_agent_panel_settings).setOnClickListener {
            openAgentSettings()
        }
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
                updateImeLiftPadding()
                scrollToBottomDelayed()
            } else if (!hasFocus) {
                panelCard.post { updateImeLiftPadding() }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(imeLiftTarget) { _, insets ->
            updateImeLiftPadding(insets)
            insets
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

    /** 打断 AI 自动执行（不关闭面板）：停请求 + 向终端发 Ctrl+C。 */
    fun stopAgentExecution() {
        if (!isSending) return
        agentCancelled = true
        chatClient?.cancel()
        agentRunner?.cancel()
        sendTerminalInterrupt()
        pendingAssistantRow?.findViewById<TextView>(R.id.agent_message_content)?.let { content ->
            renderMarkdown(content, panelCard.context.getString(R.string.zt_ai_agent_stopped), scroll = false)
        }
        pendingAssistantRow = null
        setSending(false)
    }

    /** 与迁到侧栏前一致：始终向终端发送 Ctrl+C（不依赖终端工具开关）。 */
    private fun sendTerminalInterrupt() {
        val sendOnce = Runnable {
            try {
                val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
                if (listener != null) {
                    listener.sendTextToTerminalCtrl("c", true)
                    return@Runnable
                }
                val terminal = (hostActivity as? TermuxActivity)?.getTerminalView()
                    ?: hostActivity.findViewById<TerminalView>(R.id.terminal_view)
                terminal?.sendTextToTerminalCtrl("c", true)
            } catch (_: Exception) {
            }
        }
        val main = Handler(Looper.getMainLooper())
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sendOnce.run()
            // 部分长命令需连发两次才能中断
            main.postDelayed(sendOnce, 120)
        } else {
            main.post(sendOnce)
            main.postDelayed(sendOnce, 120)
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
        ViewCompat.requestApplyInsets(imeLiftTarget)
        updateImeLiftPadding()
    }

    private fun detachKeyboardListener() {
        if (!keyboardListenerAttached) return
        panelCard.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutListener)
        keyboardListenerAttached = false
        lastPanelHeight = 0
        applyImePad(0)
    }

    private fun onPanelLayoutChanged() {
        if (!isVisible()) return
        val height = panelCard.height
        if (height == lastPanelHeight) return
        lastPanelHeight = height
        scrollToBottomDelayed()
    }

    private fun updateImeLiftPadding(insets: WindowInsetsCompat? = null) {
        if (!isPanelShown || panelHost.visibility != View.VISIBLE || isDrawerOpen?.invoke() == false) {
            applyImePad(0)
            return
        }
        val visible = Rect()
        imeLiftTarget.rootView.getWindowVisibleDisplayFrame(visible)
        val loc = IntArray(2)
        imeLiftTarget.getLocationOnScreen(loc)
        // 外框底边与可见区域底边的差值 = 被键盘挡住的高度
        val overlap = (loc[1] + imeLiftTarget.height - visible.bottom).coerceAtLeast(0)
        if (overlap > 0) {
            applyImePad(overlap)
            return
        }
        val resolved = insets ?: ViewCompat.getRootWindowInsets(imeLiftTarget)
        val imeBottom = resolved?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        applyImePad(imeBottom.coerceAtLeast(0))
    }

    private fun applyImePad(pad: Int) {
        if (lastImePad == pad) return
        lastImePad = pad
        val target = imeLiftTarget
        if (target.paddingBottom != pad) {
            target.setPadding(target.paddingLeft, target.paddingTop, target.paddingRight, pad)
        }
        if (pad > 0 && isVisible()) {
            scrollToBottomDelayed()
        }
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
        resetToolRoundProgress(
            show = ZtAgentAiConfigHelper.shouldUseAgentRunner(),
            max = ZtAgentAiConfigHelper.maxToolRounds()
        )

        chatClient?.cancel()
        agentRunner?.cancel()
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        chatClient = ZtAgentAiChatClient(config)

        if (ZtAgentAiConfigHelper.shouldUseAgentRunner()) {
            runAgentLoop(assistantView)
        } else {
            runStreamChat(assistantView)
        }
    }

    private fun runAgentLoop(assistantView: TextView) {
        agentRunner = ZtAgentAiAgentRunner(
            chatClient!!,
            terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled(),
            ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled(),
            filesystemEnabled = ZtAgentAiConfigHelper.isFilesystemEnabled()
        )
        agentRunner!!.run(conversationHistory, object : ZtAgentAiAgentRunner.Callback {
            override fun onToolStep(label: String, detail: String) {
                if (agentCancelled || !isSending) return
                appendToolStep(label, detail)
            }

            override fun onToolRoundProgress(current: Int, max: Int) {
                if (agentCancelled || !isSending) return
                updateToolRoundProgress(current, max)
            }

            override fun onComplete(content: String) {
                // 用户已打断：UI 已由 stopAgentExecution 处理，忽略迟到回调
                if (agentCancelled || !isSending) return
                finishAssistantReply(assistantView, content)
            }

            override fun onError(message: String) {
                if (agentCancelled || !isSending) return
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
                if (agentCancelled || !isSending) return
                renderMarkdown(assistantView, text)
                scrollToBottomDelayed()
            }

            override fun onError(message: String) {
                if (agentCancelled || !isSending) return
                renderMarkdown(assistantView, message)
                assistantView.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
                setSending(false)
                updateStopBarsVisibility()
            }

            override fun onComplete(fullText: String) {
                if (agentCancelled || !isSending) return
                finishAssistantReply(assistantView, fullText)
            }
        })
    }

    private fun finishAssistantReply(assistantView: TextView, fullText: String) {
        if (agentCancelled) return
        setSending(false)
        updateStopBarsVisibility()
        pendingAssistantRow = null
        if (fullText.isNotBlank()) {
            renderMarkdown(assistantView, fullText)
            conversationHistory.add(ZtAgentAiChatClient.ChatMessage(ROLE_ASSISTANT, fullText))
            ZtAgentAiChatStore.save(conversationHistory)
            // Markdown 排版后再滚到底，避免最新回复底部被裁切
            scrollToBottomDelayed()
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
                    ztControlEnabled = ZtAgentAiConfigHelper.isZtControlEnabled(),
                    filesystemEnabled = ZtAgentAiConfigHelper.isFilesystemEnabled()
                )
            )
        )
        val terminalEnabled = ZtAgentAiConfigHelper.isTerminalEnabled()
        val lastUserIndex = conversationHistory.indexOfLast { it.role == ROLE_USER }
        conversationHistory.forEachIndexed { index, message ->
            if (terminalEnabled && message.role == ROLE_USER) {
                val snapshot = if (index == lastUserIndex) {
                    ZtAgentAiTerminalExecutor.captureSnapshot(3000)
                } else {
                    message.terminalSnapshot
                }
                list.add(
                    ZtAgentAiChatClient.ChatMessage(
                        role = ROLE_USER,
                        content = ZtAgentAiChatStore.contentWithSnapshot(
                            message.content.orEmpty(),
                            snapshot
                        )
                    )
                )
            } else {
                list.add(message)
            }
        }
        return list
    }

    private fun appendUserMessage(text: String, persist: Boolean) {
        showMessagesArea()
        val itemView = inflateMessageItem(isUser = true)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        renderMarkdown(content, text)
        messagesContainer.addView(itemView)
        if (persist) {
            val snapshot = if (ZtAgentAiConfigHelper.isTerminalEnabled()) {
                ZtAgentAiTerminalExecutor.captureSnapshot(3000)
            } else {
                null
            }
            conversationHistory.add(
                ZtAgentAiChatClient.ChatMessage(
                    role = ROLE_USER,
                    content = text,
                    terminalSnapshot = snapshot
                )
            )
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

    private fun renderMarkdown(textView: TextView, markdown: String, scroll: Boolean = true) {
        // Markwon 原生渲染 + 表格/链接点击；可选中以便长按复制（系统复制菜单）
        markwon.setMarkdown(textView, markdown)
        enableMessageTextCopy(textView)
        textView.movementMethod = TableAwareMovementMethod.create()
        if (scroll) {
            textView.post { performScrollToBottom() }
        }
    }

    /** 长按选中复制；手柄沿用原 drawable，无其它自定义选区效果。 */
    private fun enableMessageTextCopy(textView: TextView) {
        textView.setTextIsSelectable(true)
        textView.setHorizontallyScrolling(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.setTextSelectHandle(R.drawable.ai_agent_text_select_handle_middle)
            textView.setTextSelectHandleLeft(R.drawable.ai_agent_text_select_handle_left)
            textView.setTextSelectHandleRight(R.drawable.ai_agent_text_select_handle_right)
        }
    }

    private fun inflateMessageItem(isUser: Boolean): View {
        val itemView = LayoutInflater.from(panelCard.context)
            .inflate(R.layout.view_agent_ai_message_item, messagesContainer, false)
        val content = itemView.findViewById<TextView>(R.id.agent_message_content)
        val lp = content.layoutParams as LinearLayout.LayoutParams
        val maxBubble = (260 * panelCard.resources.displayMetrics.density).toInt()
        if (isUser) {
            content.setBackgroundResource(R.drawable.shape_agent_msg_user)
            lp.gravity = Gravity.END
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
            content.maxWidth = maxBubble
        } else {
            content.setBackgroundResource(R.drawable.shape_agent_msg_assistant)
            lp.gravity = Gravity.START
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            content.maxWidth = Int.MAX_VALUE
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
        scrollView.removeCallbacks(scrollBottomRunnable1)
        scrollView.removeCallbacks(scrollBottomRunnable2)
        scrollToBottom()
        scrollView.postDelayed(scrollBottomRunnable1, 80)
        scrollView.postDelayed(scrollBottomRunnable2, 220)
        // 再补一次，覆盖 Markdown/图片异步撑高后的高度
        scrollView.postDelayed({ performScrollToBottom() }, 480)
    }

    private fun performScrollToBottom() {
        val child = scrollView.getChildAt(0) ?: return
        child.measure(
            View.MeasureSpec.makeMeasureSpec(scrollView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val contentHeight = child.measuredHeight.coerceAtLeast(child.height)
        val target = (contentHeight - scrollView.height + scrollView.paddingBottom).coerceAtLeast(0)
        // 不用 fullScroll(FOCUS_DOWN)：会把焦点抢到可选中消息 TextView，导致输入框偶发无法输入
        scrollView.scrollTo(0, target)
    }

    private fun setSending(sending: Boolean) {
        isSending = sending
        sendButton.isEnabled = !sending
        sendButton.alpha = if (sending) 0.5f else 1f
        if (sending) {
            input.isEnabled = false
        } else {
            restoreInputReady()
            hideToolRoundProgress()
        }
        updateStopBarsVisibility()
    }

    private fun resetToolRoundProgress(show: Boolean, max: Int) {
        toolRoundCurrent = 0
        toolRoundMax = max.coerceAtLeast(1)
        if (show) {
            updateToolRoundProgress(0, toolRoundMax)
        } else {
            hideToolRoundProgress()
        }
    }

    private fun updateToolRoundProgress(current: Int, max: Int) {
        toolRoundCurrent = current.coerceAtLeast(0)
        toolRoundMax = max.coerceAtLeast(1)
        val text = panelCard.context.getString(
            R.string.zt_ai_agent_tool_rounds_progress,
            toolRoundCurrent,
            toolRoundMax
        )
        applyToolRoundProgress(panelStopBar, text, visible = true)
        applyToolRoundProgress(runningBanner, text, visible = true)
    }

    private fun hideToolRoundProgress() {
        applyToolRoundProgress(panelStopBar, null, visible = false)
        applyToolRoundProgress(runningBanner, null, visible = false)
    }

    private fun applyToolRoundProgress(bar: View?, text: String?, visible: Boolean) {
        val tv = bar?.findViewById<TextView>(R.id.ai_agent_tool_rounds_progress) ?: return
        if (visible && !text.isNullOrBlank()) {
            tv.text = text
            tv.visibility = View.VISIBLE
        } else {
            tv.visibility = View.GONE
        }
    }

    /** 打断/结束后恢复输入框可聚焦状态（部分机型 disable 后再 enable 会丢 focusable）。 */
    private fun restoreInputReady() {
        input.isEnabled = true
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.isClickable = true
    }

    fun isVisible(): Boolean = isPanelShown && panelHost.visibility == View.VISIBLE

    /** 标记 AI 选项卡已展示/隐藏（由右侧栏切换调用）。 */
    fun setPanelTabVisible(visible: Boolean) {
        isPanelShown = visible
        if (visible) {
            panelHost.visibility = View.VISIBLE
            attachKeyboardListener()
            updateStopBarsVisibility()
            scrollToBottomDelayed()
        } else {
            hideInputKeyboard()
            detachKeyboardListener()
            if (!isSending) {
                pendingAssistantRow = null
            }
            panelHost.visibility = View.GONE
            updateStopBarsVisibility()
        }
    }

    /** 抽屉开合变化时刷新打断条（运行中且抽屉关闭/正在关闭时显示顶栏）。 */
    fun onDrawerVisibilityChanged() {
        updateStopBarsVisibility()
        updateImeLiftPadding()
    }

    fun show(selectedText: String?) {
        applySelectedText(selectedText)
        onOpenAiTab?.run()
        setPanelTabVisible(true)
        input.clearFocus()
        updateStopBarsVisibility()
        scrollToBottomDelayed()
    }

    /** 收起面板展示（切走 AI 选项卡）；若仍在执行则显示主界面顶部提示条。 */
    fun dismissPanel() {
        if (!isPanelShown) return
        setPanelTabVisible(false)
    }

    /** 强制停止 AI 并收起面板（重置等场景使用）。 */
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
            setPanelTabVisible(false)
        }
    }

    /**
     * - 侧栏打开且 AI 页可见 → 显示面板内打断条
     * - 侧栏关闭（或未在 AI 页）且仍在执行 → 显示主界面顶栏
     * 顶栏挂在根 RelativeLayout 最后一层，切换抽屉时用 ensureTopBannerLayer 保持置顶。
     */
    private fun updateStopBarsVisibility() {
        val drawerOpen = isDrawerOpen?.invoke() == true
        val showPanelBar = isSending && isPanelShown && drawerOpen
        val showTopBar = isSending && (!isPanelShown || !drawerOpen)
        panelStopBar?.visibility = if (showPanelBar) View.VISIBLE else View.GONE
        val bar = runningBanner ?: return
        val apply = Runnable {
            if (showTopBar) {
                bar.animate().cancel()
                bar.alpha = 1f
                bar.translationY = 0f
                val lp = bar.layoutParams
                if (lp != null && lp.height == 0) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    bar.layoutParams = lp
                }
                applyBannerStatusBarInset(bar)
                ensureTopBannerLayer(bar)
                bar.visibility = View.VISIBLE
            } else {
                bar.animate().cancel()
                bar.visibility = View.GONE
                bar.alpha = 1f
                bar.translationY = 0f
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply.run()
        } else {
            bar.post(apply)
        }
    }

    /** 顶栏避开状态栏（根布局 overlay 不会自动 fitsSystemWindows）。 */
    private fun applyBannerStatusBarInset(bar: View) {
        val insets = ViewCompat.getRootWindowInsets(bar)
            ?: ViewCompat.getRootWindowInsets(panelCard)
        val statusTop = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val extra = (2f * bar.resources.displayMetrics.density).toInt()
        val targetTop = statusTop + extra
        val lp = bar.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (lp.topMargin != targetTop) {
            lp.topMargin = targetTop
            bar.layoutParams = lp
        }
    }

    /**
     * 保证顶栏是根 RelativeLayout/FrameLayout 的最后一个子 View（绘制最上层）。
     * 注意：不要对竖直 LinearLayout 调用 bringChildToFront，会把条挪到屏幕底部。
     */
    private fun ensureTopBannerLayer(bar: View) {
        val parent = bar.parent as? ViewGroup ?: return
        if (parent !is RelativeLayout && parent !is FrameLayout) return
        val last = parent.childCount - 1
        if (last < 0 || parent.getChildAt(last) === bar) {
            bar.elevation = 32f * bar.resources.displayMetrics.density
            return
        }
        parent.removeView(bar)
        parent.addView(bar)
        bar.elevation = 32f * bar.resources.displayMetrics.density
    }

    /** 打开 App 页面时仅收起软键盘，不中断进行中的 AI 对话 */
    fun minimizeForNavigation() {
        hideInputKeyboard()
    }

    fun toggle(selectedText: String?) {
        if (isPanelShown && isDrawerOpen?.invoke() == true) {
            onCloseDrawer?.run()
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

    private fun showInputKeyboard() {
        if (!isVisible()) return
        if (!isSending) {
            restoreInputReady()
        }
        if (!input.isEnabled) return
        clearTerminalFocus()
        if (!input.hasFocus()) {
            input.requestFocus()
        }
        if (isSoftKeyboardVisible()) {
            updateImeLiftPadding()
            return
        }
        val imm = panelCard.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        input.post { updateImeLiftPadding() }
    }

    private fun isSoftKeyboardVisible(): Boolean {
        val insets = ViewCompat.getRootWindowInsets(panelCard) ?: return false
        if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
            return true
        }
        val visible = Rect()
        panelCard.rootView.getWindowVisibleDisplayFrame(visible)
        val screenHeight = panelCard.rootView.height
        return screenHeight - visible.bottom > screenHeight / 6
    }

    private fun hideInputKeyboard() {
        val imm = panelCard.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
        applyImePad(0)
    }

    private fun openAgentSettings() {
        hideInputKeyboard()
        hostActivity.startActivity(
            Intent(hostActivity, ZtAgentAiSettingsActivity::class.java)
        )
    }

    private fun openSkillsPage() {
        hideInputKeyboard()
        hostActivity.startActivity(
            Intent(hostActivity, ZtAgentAiSkillsActivity::class.java)
        )
    }

    private fun clearTerminalFocus() {
        panelCard.rootView.findViewById<View>(R.id.terminal_view)?.clearFocus()
        hostActivity.currentFocus?.takeIf { it.id != R.id.ai_agent_panel_input }?.clearFocus()
    }

    companion object {
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_SYSTEM = "system"
    }
}
