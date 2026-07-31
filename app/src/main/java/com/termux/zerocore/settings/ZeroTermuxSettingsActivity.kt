package com.termux.zerocore.settings

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.KeyWordFunDialog
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.guide.TermuxGuideActivity
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_CREATE_FOLDER
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_EXTRA
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_EXTRA_JUMP_OTHER

class ZeroTermuxSettingsActivity : BaseTitleActivity() {

    private val inputMethodTriggerCloseSwitch by lazy { findViewById<SwitchCompat>(R.id.input_method_trigger_close_switch) }
    private val inputMethodTriggerCloseLl by lazy { findViewById<LinearLayout>(R.id.input_method_trigger_close_ll) }

    private val styleTriggerOffSwitch by lazy { findViewById<SwitchCompat>(R.id.style_trigger_off_switch) }
    private val styleTriggerOffLl by lazy { findViewById<LinearLayout>(R.id.style_trigger_off_ll) }

    private val isToolShowSwitch by lazy { findViewById<SwitchCompat>(R.id.is_tool_show_switch) }
    private val isToolShowLl by lazy { findViewById<LinearLayout>(R.id.is_tool_show_ll) }

    private val volumeFunctionSwitch by lazy { findViewById<SwitchCompat>(R.id.volume_function_switch) }
    private val volumeFunctionLl by lazy { findViewById<LinearLayout>(R.id.volume_function_ll) }

    private val editorWordWrapSwitch by lazy { findViewById<SwitchCompat>(R.id.editor_word_wrap_switch) }
    private val editorWordWrapLl by lazy { findViewById<LinearLayout>(R.id.editor_word_wrap_ll) }

    private val mSettingsKeywordFunCardViewLayout by lazy { findViewById<CardView>(R.id.settings_keyword_fun_card) }
    private val mSettingsKeywordFunTextView by lazy { findViewById<TextView>(R.id.settings_keyword_fun_text_summary) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zero_termux_settings)
        setBaseTitle(UUtils.getString(R.string.zt_settings))
        initView()
        initStatus()
    }

    private fun initView() {
        setSwitchStatus(inputMethodTriggerCloseSwitch, inputMethodTriggerCloseLl)
        setSwitchStatus(styleTriggerOffSwitch, styleTriggerOffLl)
        setSwitchStatus(isToolShowSwitch, isToolShowLl)
        setSwitchStatus(volumeFunctionSwitch, volumeFunctionLl)
        setSwitchStatus(editorWordWrapSwitch, editorWordWrapLl)
        findViewById<CardView>(R.id.save_path).setOnClickListener {
            val intent = Intent(this, TermuxGuideActivity::class.java)
            intent.putExtra(GUIDE_EXTRA, GUIDE_CREATE_FOLDER)
            intent.putExtra(GUIDE_EXTRA_JUMP_OTHER, true)
            startActivity(intent)
        }
    }

    private fun initStatus() {
        val ztUserBean = UserSetManage.get().getZTUserBean()
        inputMethodTriggerCloseSwitch.isChecked = ztUserBean.isInputMethodTriggerClose
        styleTriggerOffSwitch.isChecked = ztUserBean.isStyleTriggerOff
        isToolShowSwitch.isChecked = ztUserBean.isToolShow
        volumeFunctionSwitch.isChecked = ztUserBean.isResetVolume
        editorWordWrapSwitch.isChecked = ztUserBean.isEditorWordWrap
        mSettingsKeywordFunTextView.text =
            "${UUtils.getString(R.string.settings_keyword_summary1)}: " +
                "${KeyWordFunDialog.getDoubleClickString(ztUserBean.doubleClickFun)}\n" +
                "${UUtils.getString(R.string.settings_keyword_summary)}"
        mSettingsKeywordFunCardViewLayout.setOnClickListener {
            val keyWordFunDialog = KeyWordFunDialog(this)
            keyWordFunDialog.show()
            keyWordFunDialog.setOnDismissListener {
                val ztUserBean1 = UserSetManage.get().getZTUserBean()
                mSettingsKeywordFunTextView.text =
                    "${UUtils.getString(R.string.settings_keyword_summary1)}: " +
                        "${KeyWordFunDialog.getDoubleClickString(ztUserBean1.doubleClickFun)}\n" +
                        "${UUtils.getString(R.string.settings_keyword_summary)}"
            }
        }
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: LinearLayout) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !(switchCompat.isChecked)
        }
        switchCompat.setOnCheckedChangeListener { _, _ ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            when (switchCompat) {
                inputMethodTriggerCloseSwitch -> {
                    ztUserBean.isInputMethodTriggerClose = switchCompat.isChecked
                }
                styleTriggerOffSwitch -> {
                    ztUserBean.isStyleTriggerOff = switchCompat.isChecked
                }
                isToolShowSwitch -> {
                    ztUserBean.isToolShow = switchCompat.isChecked
                }
                volumeFunctionSwitch -> {
                    ztUserBean.isResetVolume = switchCompat.isChecked
                }
                editorWordWrapSwitch -> {
                    ztUserBean.isEditorWordWrap = switchCompat.isChecked
                }
            }
            UserSetManage.get().setZTUserBean(ztUserBean)
        }
    }
}
