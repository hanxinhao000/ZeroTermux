package com.termux.zerocore.ai.agent

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.EditTextActivity
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
    private val filesystemSwitch by lazy { findViewById<SwitchCompat>(R.id.agent_ai_filesystem_switch) }
    private val toolRoundCards by lazy {
        ZtAgentAiConfigHelper.toolRoundOptions().associateWith { rounds ->
            findViewById<CardView>(toolRoundCardId(rounds))
        }
    }
    private val resetCard by lazy { findViewById<CardView>(R.id.agent_ai_reset_card) }
    private val skillsPathView by lazy { findViewById<TextView>(R.id.agent_ai_skills_path) }
    private val skillsListContainer by lazy { findViewById<LinearLayout>(R.id.agent_skills_list) }
    private val skillsEmptyView by lazy { findViewById<TextView>(R.id.agent_ai_skills_empty) }
    private val skillsCreateNew by lazy { findViewById<TextView>(R.id.agent_ai_skills_create_new) }
    private val aiAgentPanelSwitch by lazy { findViewById<SwitchCompat>(R.id.ai_agent_panel_switch) }
    private val aiAgentPanelLl by lazy { findViewById<LinearLayout>(R.id.ai_agent_panel_ll) }

    private var suppressSave = false
    private var suppressSkillToggle = false
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
        initFilesystemSwitch()
        initToolRoundCards()
        initResetEntry()
        initSkillsSection()
        loadFieldsFromConfig()
        bindAutoSave()
        refreshProviderHighlight()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshSkillsList()
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

    private fun initFilesystemSwitch() {
        filesystemSwitch.setOnCheckedChangeListener { _, isChecked ->
            ZtAgentAiConfigHelper.saveFilesystemEnabled(isChecked)
        }
    }

    private fun initToolRoundCards() {
        toolRoundCards.forEach { (rounds, card) ->
            card.setOnClickListener { selectToolRounds(rounds) }
        }
        refreshToolRoundHighlight()
    }

    private fun toolRoundCardId(rounds: Int): Int = when (rounds) {
        10 -> R.id.agent_tool_rounds_10
        20 -> R.id.agent_tool_rounds_20
        40 -> R.id.agent_tool_rounds_40
        60 -> R.id.agent_tool_rounds_60
        80 -> R.id.agent_tool_rounds_80
        100 -> R.id.agent_tool_rounds_100
        else -> R.id.agent_tool_rounds_40
    }

    private fun selectToolRounds(rounds: Int) {
        if (rounds == ZtAgentAiConfigHelper.maxToolRounds()) {
            refreshToolRoundHighlight()
            return
        }
        ZtAgentAiConfigHelper.saveMaxToolRounds(rounds)
        refreshToolRoundHighlight()
    }

    private fun refreshToolRoundHighlight() {
        val active = ZtAgentAiConfigHelper.maxToolRounds()
        val normal = getColor(R.color.color_55000000)
        val selected = getColor(R.color.color_5548baf3)
        toolRoundCards.forEach { (rounds, card) ->
            card.setCardBackgroundColor(if (active == rounds) selected else normal)
        }
    }

    private fun initResetEntry() {
        resetCard.setOnClickListener {
            ZtAgentAiResetHelper.showResetConfirmDialog(this) {
                loadFieldsFromConfig()
                refreshSkillsList()
                refreshStatus()
            }
        }
    }

    private fun initSkillsSection() {
        skillsPathView.text = getString(
            R.string.zt_agent_ai_skills_path_format,
            ZtAgentAiSkillHelper.skillsRootDisplayPath()
        )
        skillsCreateNew.setOnClickListener { showCreateSkillDialog() }
        skillsEmptyView.text = getString(
            R.string.zt_agent_ai_skills_empty,
            getString(R.string.zt_agent_skill_bundled_name)
        )
        refreshSkillsList()
    }

    private fun showCreateSkillDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.zt_agent_ai_skills_create_name_hint)
            setSingleLine(true)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_agent_ai_skills_create_dialog_title)
            .setMessage(R.string.zt_agent_ai_skills_create_dialog_message)
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val rawName = input.text?.toString()?.trim().orEmpty()
                if (rawName.isEmpty()) {
                    UUtils.showMsg(getString(R.string.zt_agent_ai_skills_create_name_required))
                    return@setPositiveButton
                }
                ZtAgentAiSkillHelper.createNewSkill(rawName).fold(
                    onSuccess = {
                        UUtils.showMsg(getString(R.string.zt_agent_ai_skills_created, it.skillId))
                        refreshSkillsList()
                        openSkillEditor(it.skillId)
                    },
                    onFailure = { error ->
                        val message = when (error.message) {
                            "invalid skill id" -> getString(R.string.zt_agent_ai_skills_invalid_id)
                            "skill already exists" -> getString(R.string.zt_agent_ai_skills_already_exists)
                            else -> error.message ?: getString(R.string.zt_agent_ai_skills_create_failed)
                        }
                        UUtils.showMsg(message)
                    }
                )
            }
            .show()
    }

    private fun openSkillEditor(skillId: String) {
        val file = ZtAgentAiSkillHelper.skillFile(skillId) ?: return
        startActivity(
            Intent(this, EditTextActivity::class.java)
                .putExtra("edit_path", file.absolutePath)
        )
    }

    private fun showDeleteSkillDialog(skill: ZtAgentAiSkillHelper.SkillEntry) {
        if (ZtAgentAiSkillHelper.isBundledSkill(skill.id)) return
        val displayName = ZtAgentAiSkillHelper.displaySkillName(skill)
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_agent_ai_skills_delete_dialog_title)
            .setMessage(getString(R.string.zt_agent_ai_skills_delete_dialog_message, displayName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.zt_agent_ai_skills_delete) { _, _ ->
                ZtAgentAiSkillHelper.deleteSkill(skill.id).fold(
                    onSuccess = {
                        UUtils.showMsg(getString(R.string.zt_agent_ai_skills_deleted, displayName))
                        refreshSkillsList()
                    },
                    onFailure = { error ->
                        val message = when (error.message) {
                            "bundled skill cannot be deleted" ->
                                getString(R.string.zt_agent_ai_skills_delete_bundled)
                            "skill not found" ->
                                getString(R.string.zt_agent_ai_skills_delete_not_found)
                            else -> error.message ?: getString(R.string.zt_agent_ai_skills_delete_failed)
                        }
                        UUtils.showMsg(message)
                    }
                )
            }
            .show()
    }

    private fun refreshSkillsList() {
        val skills = ZtAgentAiSkillHelper.listAvailableSkills()
        val enabled = ZtAgentAiSkillHelper.enabledSkillIds()
        skillsListContainer.removeAllViews()
        skillsEmptyView.visibility = if (skills.isEmpty()) View.VISIBLE else View.GONE
        val dp = resources.displayMetrics.density
        val padV = (8 * dp).toInt()
        val padH = (4 * dp).toInt()
        val editPadH = (10 * dp).toInt()
        val editPadV = (4 * dp).toInt()
        skills.forEach { skill ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, padV, 0, padV)
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val title = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = ZtAgentAiSkillHelper.displaySkillName(skill)
                setTextColor(ContextCompat.getColor(this@ZtAgentAiSettingsActivity, R.color.color_ffffff))
                textSize = 14f
            }
            val editButton = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(R.drawable.shape_agent_skill_edit_button)
                setPadding(editPadH, editPadV, editPadH, editPadV)
                text = getString(R.string.zt_agent_ai_skills_edit)
                setTextColor(ContextCompat.getColor(this@ZtAgentAiSettingsActivity, R.color.color_ffffff))
                textSize = 12f
                setOnClickListener { openSkillEditor(skill.id) }
            }
            header.addView(title)
            header.addView(editButton)
            if (!ZtAgentAiSkillHelper.isBundledSkill(skill.id)) {
                val deleteButton = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = (8 * dp).toInt()
                    }
                    setBackgroundResource(R.drawable.shape_agent_skill_delete_button)
                    setPadding(editPadH, editPadV, editPadH, editPadV)
                    text = getString(R.string.zt_agent_ai_skills_delete)
                    setTextColor(ContextCompat.getColor(this@ZtAgentAiSettingsActivity, R.color.color_ffffff))
                    textSize = 12f
                    setOnClickListener { showDeleteSkillDialog(skill) }
                }
                header.addView(deleteButton)
            }
            val toggle = SwitchCompat(this).apply {
                isChecked = enabled.contains(skill.id)
                setOnCheckedChangeListener { _, isChecked ->
                    if (suppressSkillToggle) return@setOnCheckedChangeListener
                    ZtAgentAiSkillHelper.setSkillEnabled(skill.id, isChecked)
                }
            }
            header.addView(toggle)
            row.addView(header)
            val description = ZtAgentAiSkillHelper.displaySkillDescription(skill)
            if (description.isNotBlank()) {
                val desc = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(padH, (4 * dp).toInt(), padH, 0)
                    text = description
                    setTextColor(ContextCompat.getColor(this@ZtAgentAiSettingsActivity, R.color.md_grey_500))
                    textSize = 12f
                }
                row.addView(desc)
            }
            skillsListContainer.addView(row)
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
        filesystemSwitch.isChecked = ZtAgentAiConfigHelper.isFilesystemEnabled()
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
