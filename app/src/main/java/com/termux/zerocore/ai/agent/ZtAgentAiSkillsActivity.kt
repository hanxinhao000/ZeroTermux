package com.termux.zerocore.ai.agent

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.settings.BaseTitleActivity

class ZtAgentAiSkillsActivity : BaseTitleActivity() {

    private val skillsPathView by lazy { findViewById<TextView>(R.id.agent_ai_skills_path) }
    private val skillsListContainer by lazy { findViewById<LinearLayout>(R.id.agent_skills_list) }
    private val skillsEmptyView by lazy { findViewById<TextView>(R.id.agent_ai_skills_empty) }
    private val skillsCreateCard by lazy { findViewById<CardView>(R.id.agent_ai_skills_create_card) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_agent_ai_skills)
        setBaseTitle(UUtils.getString(R.string.zt_agent_ai_skills_title))
        skillsPathView.text = getString(
            R.string.zt_agent_ai_skills_path_format,
            ZtAgentAiSkillHelper.skillsRootDisplayPath()
        )
        skillsEmptyView.text = getString(
            R.string.zt_agent_ai_skills_empty,
            getString(R.string.zt_agent_skill_bundled_name)
        )
        skillsCreateCard.setOnClickListener { showCreateSkillDialog() }
    }

    override fun onResume() {
        super.onResume()
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
        val editPadH = (10 * dp).toInt()
        val editPadV = (4 * dp).toInt()
        val white = ContextCompat.getColor(this, R.color.color_ffffff)
        val grey = ContextCompat.getColor(this, R.color.md_grey_500)

        skills.forEach { skill ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * dp).toInt()
                }
                radius = 6 * dp
                cardElevation = 0f
                setCardBackgroundColor(ContextCompat.getColor(this@ZtAgentAiSkillsActivity, R.color.color_55000000))
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleBlock.addView(TextView(this).apply {
                text = ZtAgentAiSkillHelper.displaySkillName(skill)
                setTextColor(white)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            val description = ZtAgentAiSkillHelper.displaySkillDescription(skill)
            if (description.isNotBlank()) {
                titleBlock.addView(TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * dp).toInt()
                    }
                    text = description
                    setTextColor(grey)
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
            }
            header.addView(titleBlock)
            header.addView(SwitchCompat(this).apply {
                isChecked = enabled.contains(skill.id)
                setOnCheckedChangeListener { _, isChecked ->
                    ZtAgentAiSkillHelper.setSkillEnabled(skill.id, isChecked)
                }
            })
            content.addView(header)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * dp).toInt()
                }
            }
            actions.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(R.drawable.shape_agent_skill_edit_button)
                setPadding(editPadH, editPadV, editPadH, editPadV)
                text = getString(R.string.zt_agent_ai_skills_edit)
                setTextColor(white)
                textSize = 12f
                setOnClickListener { openSkillEditor(skill.id) }
            })
            if (!ZtAgentAiSkillHelper.isBundledSkill(skill.id)) {
                actions.addView(TextView(this).apply {
                    setBackgroundResource(R.drawable.shape_agent_skill_delete_button)
                    setPadding(editPadH, editPadV, editPadH, editPadV)
                    text = getString(R.string.zt_agent_ai_skills_delete)
                    setTextColor(white)
                    textSize = 12f
                    setOnClickListener { showDeleteSkillDialog(skill) }
                })
            }
            content.addView(actions)
            card.addView(content)
            skillsListContainer.addView(card)
        }
    }
}
