package com.termux.zerocore.ai.agent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.settings.BaseTitleActivity

class ZtAgentAiSettingsActivity : BaseTitleActivity() {

    private val statusView by lazy { findViewById<TextView>(R.id.agent_ai_status) }
    private val providerDeepSeek by lazy { findViewById<CardView>(R.id.agent_provider_deepseek) }
    private val providerOpenAi by lazy { findViewById<CardView>(R.id.agent_provider_openai) }
    private val providerGoogle by lazy { findViewById<CardView>(R.id.agent_provider_google) }
    private val providerCustom by lazy { findViewById<CardView>(R.id.agent_provider_custom) }
    private val apiUrlEdit by lazy { findViewById<EditText>(R.id.agent_ai_api_url) }
    private val apiKeyEdit by lazy { findViewById<EditText>(R.id.agent_ai_api_key) }
    private val apiKeyToggle by lazy { findViewById<ImageButton>(R.id.agent_ai_api_key_toggle) }
    private val modelEdit by lazy { findViewById<EditText>(R.id.agent_ai_model) }
    private val systemPromptEdit by lazy { findViewById<EditText>(R.id.agent_ai_system_prompt) }
    private val terminalSwitch by lazy { findViewById<SwitchCompat>(R.id.agent_ai_terminal_switch) }
    private val ztControlSwitch by lazy { findViewById<SwitchCompat>(R.id.agent_ai_zt_control_switch) }
    private val resetCard by lazy { findViewById<CardView>(R.id.agent_ai_reset_card) }
    private val aiAgentPanelSwitch by lazy { findViewById<SwitchCompat>(R.id.ai_agent_panel_switch) }
    private val aiAgentPanelLl by lazy { findViewById<LinearLayout>(R.id.ai_agent_panel_ll) }

    private var suppressSave = false
    private var apiKeyVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_agent_ai_settings)
        setBaseTitle(UUtils.getString(R.string.zt_agent_ai_settings_title))
        initProviderCards()
        initAiAgentPanelSwitch()
        initApiKeyVisibilityToggle()
        initTerminalSwitch()
        initZtControlSwitch()
        initResetEntry()
        loadFieldsFromConfig()
        bindAutoSave()
        refreshProviderHighlight()
        refreshStatus()
    }

    private fun initAiAgentPanelSwitch() {
        aiAgentPanelSwitch.isChecked = UserSetManage.get().getZTUserBean().isAiAgentPanelEnabled
        aiAgentPanelLl.setOnClickListener {
            aiAgentPanelSwitch.isChecked = !aiAgentPanelSwitch.isChecked
        }
        aiAgentPanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            val bean = UserSetManage.get().getZTUserBean()
            bean.isAiAgentPanelEnabled = isChecked
            UserSetManage.get().setZTUserBean(bean)
        }
    }

    private fun initApiKeyVisibilityToggle() {
        updateApiKeyVisibility(false)
        apiKeyToggle.setOnClickListener {
            updateApiKeyVisibility(!apiKeyVisible)
        }
    }

    private fun updateApiKeyVisibility(visible: Boolean) {
        apiKeyVisible = visible
        val selection = apiKeyEdit.selectionEnd
        if (visible) {
            apiKeyEdit.transformationMethod = SingleLineTransformationMethod.getInstance()
            apiKeyToggle.setImageResource(R.drawable.ic_agent_eye_off)
        } else {
            apiKeyEdit.transformationMethod = PasswordTransformationMethod.getInstance()
            apiKeyToggle.setImageResource(R.drawable.ic_agent_eye_on)
        }
        apiKeyEdit.setSelection(selection.coerceAtLeast(0).coerceAtMost(apiKeyEdit.text?.length ?: 0))
    }

    private fun initProviderCards() {
        providerDeepSeek.setOnClickListener { selectProvider(ZtAgentAiProvider.DEEPSEEK) }
        providerOpenAi.setOnClickListener { selectProvider(ZtAgentAiProvider.OPENAI) }
        providerGoogle.setOnClickListener { selectProvider(ZtAgentAiProvider.GOOGLE) }
        providerCustom.setOnClickListener { selectProvider(ZtAgentAiProvider.CUSTOM) }
    }

    private fun selectProvider(provider: String) {
        if (provider == ZtAgentAiConfigHelper.activeProvider()) {
            refreshProviderHighlight()
            return
        }
        persistFields()
        ZtAgentAiConfigHelper.switchProvider(provider)
        loadFieldsFromConfig()
        refreshProviderHighlight()
        refreshStatus()
    }

    private fun initTerminalSwitch() {
        terminalSwitch.setOnCheckedChangeListener { _, isChecked ->
            ZtAgentAiConfigHelper.saveTerminalEnabled(isChecked)
        }
    }

    private fun initZtControlSwitch() {
        ztControlSwitch.setOnCheckedChangeListener { _, isChecked ->
            ZtAgentAiConfigHelper.saveZtControlEnabled(isChecked)
        }
    }

    private fun initResetEntry() {
        resetCard.setOnClickListener {
            ZtAgentAiResetHelper.showResetConfirmDialog(this) {
                loadFieldsFromConfig()
                refreshStatus()
            }
        }
    }

    private fun loadFieldsFromConfig() {
        suppressSave = true
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        apiUrlEdit.setText(config.apiUrl)
        apiKeyEdit.setText(config.apiKey)
        modelEdit.setText(config.model)
        systemPromptEdit.setText(config.systemPrompt)
        terminalSwitch.isChecked = ZtAgentAiConfigHelper.isTerminalEnabled()
        ztControlSwitch.isChecked = ZtAgentAiConfigHelper.isZtControlEnabled()
        suppressSave = false
    }

    private fun bindAutoSave() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressSave) return
                persistFields()
                refreshStatus()
            }
        }
        apiUrlEdit.addTextChangedListener(watcher)
        apiKeyEdit.addTextChangedListener(watcher)
        modelEdit.addTextChangedListener(watcher)
        systemPromptEdit.addTextChangedListener(watcher)
    }

    private fun persistFields() {
        ZtAgentAiConfigHelper.saveActiveFields(
            apiUrl = apiUrlEdit.text?.toString().orEmpty(),
            apiKey = apiKeyEdit.text?.toString().orEmpty(),
            model = modelEdit.text?.toString().orEmpty(),
            systemPrompt = systemPromptEdit.text?.toString().orEmpty()
        )
    }

    private fun refreshProviderHighlight() {
        val active = ZtAgentAiConfigHelper.activeProvider()
        val normal = getColor(R.color.color_55000000)
        val selected = getColor(R.color.color_5548baf3)
        providerDeepSeek.setCardBackgroundColor(
            if (active == ZtAgentAiProvider.DEEPSEEK) selected else normal
        )
        providerOpenAi.setCardBackgroundColor(
            if (active == ZtAgentAiProvider.OPENAI) selected else normal
        )
        providerGoogle.setCardBackgroundColor(
            if (active == ZtAgentAiProvider.GOOGLE) selected else normal
        )
        providerCustom.setCardBackgroundColor(
            if (active == ZtAgentAiProvider.CUSTOM) selected else normal
        )
    }

    private fun refreshStatus() {
        val config = ZtAgentAiConfigHelper.loadActiveConfig()
        val providerLabel = providerLabel(config.provider)
        statusView.text = if (ZtAgentAiConfigHelper.isConfigured()) {
            getString(
                R.string.zt_agent_ai_status_configured,
                providerLabel,
                config.model
            )
        } else {
            getString(R.string.zt_agent_ai_status_incomplete)
        }
    }

    private fun providerLabel(provider: String): String = when (provider) {
        ZtAgentAiProvider.OPENAI -> UUtils.getString(R.string.zt_agent_ai_provider_openai)
        ZtAgentAiProvider.GOOGLE -> UUtils.getString(R.string.zt_agent_ai_provider_google)
        ZtAgentAiProvider.CUSTOM -> UUtils.getString(R.string.zt_agent_ai_provider_custom)
        else -> UUtils.getString(R.string.zt_agent_ai_provider_deepseek)
    }
}
