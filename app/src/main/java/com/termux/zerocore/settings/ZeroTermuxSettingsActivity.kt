package com.termux.zerocore.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.KeyWordFunDialog
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.termux.zerocore.aidebug.ZtAiDebugManager
import com.termux.zerocore.aidebug.ZtAiDebugMatchCodeHelper
import com.termux.zerocore.aidebug.ZtAiDebugPermissionHelper
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileHttpUtils.Companion.get
import com.termux.zerocore.workstation.ZtWorkstationSettingsActivity
import com.topjohnwu.superuser.Shell
import com.zp.z_file.util.LogUtils
import java.io.File

class ZeroTermuxSettingsActivity : BaseTitleActivity() {

    private val ztDownloadServicesSwitch by lazy {findViewById<SwitchCompat>(R.id.zt_download_services_switch)}
    private val ztDownloadServicesLl by lazy {findViewById<LinearLayout>(R.id.zt_download_services_ll)}

    private val ztWorkstationLl by lazy { findViewById<CardView>(R.id.zt_workstation_cv) }
    private val ztWorkstationStatus by lazy { findViewById<TextView>(R.id.zt_workstation_status) }

    private val inputMethodTriggerCloseSwitch by lazy {findViewById<SwitchCompat>(R.id.input_method_trigger_close_switch)}
    private val inputMethodTriggerCloseLl by lazy {findViewById<LinearLayout>(R.id.input_method_trigger_close_ll)}

    private val styleTriggerOffSwitch by lazy {findViewById<SwitchCompat>(R.id.style_trigger_off_switch)}
    private val styleTriggerOffLl by lazy {findViewById<LinearLayout>(R.id.style_trigger_off_ll)}

    private val isToolShowSwitch by lazy {findViewById<SwitchCompat>(R.id.is_tool_show_switch)}
    private val isToolShowLl by lazy {findViewById<LinearLayout>(R.id.is_tool_show_ll)}

    private val forceUseNumpadSwitch by lazy {findViewById<SwitchCompat>(R.id.force_use_numpad_switch)}
    private val forceUseNumpadLl by lazy {findViewById<LinearLayout>(R.id.force_use_numpad_ll)}

    private val logOutputSwitch by lazy {findViewById<SwitchCompat>(R.id.log_output_switch)}
    private val logOutputLl by lazy {findViewById<LinearLayout>(R.id.log_output_ll)}

    private val shellTermuxSwitch by lazy {findViewById<SwitchCompat>(R.id.shell_termux_switch)}
    private val shellTermuxLl by lazy {findViewById<LinearLayout>(R.id.shell_termux_ll)}

    private val volumeFunctionSwitch by lazy {findViewById<SwitchCompat>(R.id.volume_function_switch)}
    private val volumeFunctionLl by lazy {findViewById<LinearLayout>(R.id.volume_function_ll)}

    private val foldMenuCloseSwitch by lazy {findViewById<SwitchCompat>(R.id.fold_menu_close_switch)}
    private val foldMenuCloseLl by lazy {findViewById<LinearLayout>(R.id.fold_menu_close_ll)}

    private val mainMenuConfigCloseSwitch by lazy {findViewById<SwitchCompat>(R.id.main_menu_config_close_switch)}
    private val mainMenuConfigCloseLl by lazy {findViewById<LinearLayout>(R.id.main_menu_config_close_ll)}

    private val editorWordWrapSwitch by lazy {findViewById<SwitchCompat>(R.id.editor_word_wrap_switch)}
    private val editorWordWrapLl by lazy {findViewById<LinearLayout>(R.id.editor_word_wrap_ll)}

    private val ztAiDebugSwitch by lazy { findViewById<SwitchCompat>(R.id.zt_ai_debug_switch) }
    private val ztAiDebugLl by lazy { findViewById<LinearLayout>(R.id.zt_ai_debug_ll) }
    private val ztAiDebugDetailCv by lazy { findViewById<CardView>(R.id.zt_ai_debug_detail_cv) }
    private val ztAiDebugMatchCodeValue by lazy { findViewById<TextView>(R.id.zt_ai_debug_match_code_value) }
    private val ztAiDebugMatchCodeReveal by lazy { findViewById<ImageButton>(R.id.zt_ai_debug_match_code_reveal) }
    private val ztAiDebugRootSwitch by lazy { findViewById<SwitchCompat>(R.id.zt_ai_debug_root_switch) }
    private val ztAiDebugRootLl by lazy { findViewById<LinearLayout>(R.id.zt_ai_debug_root_ll) }

    private var aiDebugSwitchUpdating = false
    private var aiDebugRootSwitchUpdating = false
    private var aiDebugMatchCodeRevealed = false
    private val mSettingsKeywordFunCardViewLayout by lazy {findViewById<CardView>(R.id.settings_keyword_fun_card)}
    private val mExperimentalFeature by lazy {findViewById<CardView>(R.id.experimental_feature)}
    private val mSettingsKeywordFunTextView by lazy {findViewById<TextView>(R.id.settings_keyword_fun_text_summary)}
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zero_termux_settings)
        setBaseTitle(UUtils.getString(R.string.zt_settings))
        initView()
        initStatus()
    }

    private fun initView() {
        setSwitchStatus(ztDownloadServicesSwitch, ztDownloadServicesLl)
        ztWorkstationLl.setOnClickListener {
            startActivity(Intent(this, ZtWorkstationSettingsActivity::class.java))
        }
        setSwitchStatus(inputMethodTriggerCloseSwitch, inputMethodTriggerCloseLl)
        setSwitchStatus(styleTriggerOffSwitch, styleTriggerOffLl)
        setSwitchStatus(isToolShowSwitch, isToolShowLl)
        setSwitchStatus(forceUseNumpadSwitch, forceUseNumpadLl)
        setSwitchStatus(logOutputSwitch, logOutputLl)
        setSwitchStatus(shellTermuxSwitch, shellTermuxLl)
        setSwitchStatus(volumeFunctionSwitch, volumeFunctionLl)
        setSwitchStatus(foldMenuCloseSwitch, foldMenuCloseLl)
        setSwitchStatus(mainMenuConfigCloseSwitch, mainMenuConfigCloseLl)
setSwitchStatus(editorWordWrapSwitch, editorWordWrapLl)
        initAiDebugSwitch()
    }

    private fun initAiDebugSwitch() {
        ztAiDebugLl.setOnClickListener {
            if (!aiDebugSwitchUpdating) {
                ztAiDebugSwitch.isChecked = !ztAiDebugSwitch.isChecked
            }
        }
        ztAiDebugSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (aiDebugSwitchUpdating) return@setOnCheckedChangeListener
            if (isChecked) {
                showAiDebugDisclaimer { setAiDebugEnabled(true) }
            } else {
                setAiDebugEnabled(false)
            }
        }
        ztAiDebugMatchCodeReveal.setOnClickListener {
            if (aiDebugMatchCodeRevealed) {
                aiDebugMatchCodeRevealed = false
                refreshAiDebugMatchCodeDisplay()
                return@setOnClickListener
            }
            showMatchCodeRevealDialog()
        }
        initAiDebugRootSwitch()
    }

    private fun initAiDebugRootSwitch() {
        ztAiDebugRootLl.setOnClickListener {
            if (!aiDebugRootSwitchUpdating) {
                ztAiDebugRootSwitch.isChecked = !ztAiDebugRootSwitch.isChecked
            }
        }
        ztAiDebugRootSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (aiDebugRootSwitchUpdating) return@setOnCheckedChangeListener
            if (isChecked) {
                showAiDebugRootDisclaimer { setAiDebugRootEnabled(true) }
            } else {
                setAiDebugRootEnabled(false)
            }
        }
    }

    private fun showAiDebugRootDisclaimer(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_ai_debug_root_disclaimer_title)
            .setMessage(R.string.zt_ai_debug_root_disclaimer_message)
            .setPositiveButton(R.string.zt_workstation_confirm_enable) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                aiDebugRootSwitchUpdating = true
                ztAiDebugRootSwitch.isChecked = false
                aiDebugRootSwitchUpdating = false
            }
            .setOnCancelListener {
                aiDebugRootSwitchUpdating = true
                ztAiDebugRootSwitch.isChecked = false
                aiDebugRootSwitchUpdating = false
            }
            .show()
    }

    private fun setAiDebugRootEnabled(enabled: Boolean) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.isZtAiDebugRootEnabled = enabled
        UserSetManage.get().setZTUserBean(bean)
        aiDebugRootSwitchUpdating = true
        ztAiDebugRootSwitch.isChecked = enabled
        aiDebugRootSwitchUpdating = false
    }

    private fun showMatchCodeRevealDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_ai_debug_match_code_dialog_title)
            .setMessage(R.string.zt_ai_debug_match_code_dialog_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                aiDebugMatchCodeRevealed = true
                refreshAiDebugMatchCodeDisplay()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun refreshAiDebugDetailVisibility() {
        val enabled = UserSetManage.get().getZTUserBean().isZtAiDebugEnabled
        ztAiDebugDetailCv.visibility = if (enabled) View.VISIBLE else View.GONE
        if (!enabled) {
            aiDebugMatchCodeRevealed = false
        }
        refreshAiDebugMatchCodeDisplay()
    }

    private fun refreshAiDebugMatchCodeDisplay() {
        if (!UserSetManage.get().getZTUserBean().isZtAiDebugEnabled) {
            ztAiDebugMatchCodeValue.text = getString(R.string.zt_ai_debug_match_code_hidden)
            ztAiDebugMatchCodeReveal.setImageResource(android.R.drawable.ic_menu_view)
            return
        }
        ZtAiDebugMatchCodeHelper.ensureCode()
        ztAiDebugMatchCodeValue.text = if (aiDebugMatchCodeRevealed) {
            ZtAiDebugMatchCodeHelper.getStoredCode() ?: ZtAiDebugMatchCodeHelper.MASKED_DISPLAY
        } else {
            ZtAiDebugMatchCodeHelper.MASKED_DISPLAY
        }
        ztAiDebugMatchCodeReveal.setImageResource(
            if (aiDebugMatchCodeRevealed) android.R.drawable.ic_menu_close_clear_cancel
            else android.R.drawable.ic_menu_view
        )
    }

    private fun showAiDebugDisclaimer(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_ai_debug_disclaimer_title)
            .setMessage(R.string.zt_ai_debug_disclaimer_message)
            .setPositiveButton(R.string.zt_workstation_confirm_enable) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                aiDebugSwitchUpdating = true
                ztAiDebugSwitch.isChecked = false
                aiDebugSwitchUpdating = false
            }
            .setOnCancelListener {
                aiDebugSwitchUpdating = true
                ztAiDebugSwitch.isChecked = false
                aiDebugSwitchUpdating = false
            }
            .show()
    }

    private fun setAiDebugEnabled(enabled: Boolean) {
        val bean = UserSetManage.get().getZTUserBean()
        bean.isZtAiDebugEnabled = enabled
        UserSetManage.get().setZTUserBean(bean)
        aiDebugSwitchUpdating = true
        ztAiDebugSwitch.isChecked = enabled
        aiDebugSwitchUpdating = false
        if (enabled) {
            ZtAiDebugMatchCodeHelper.rotateCode()
            aiDebugMatchCodeRevealed = false
            refreshAiDebugDetailVisibility()
            requestAiDebugPermissions {
                ZtAiDebugManager.start(this)
            }
        } else {
            ZtAiDebugMatchCodeHelper.clearCode()
            aiDebugMatchCodeRevealed = false
            setAiDebugRootEnabled(false)
            refreshAiDebugDetailVisibility()
            ZtAiDebugManager.stop(this)
        }
    }

    private fun requestAiDebugPermissions(onFinished: () -> Unit) {
        requestPostNotificationsIfNeeded()
        XXPermissions.with(this)
            .permission(*ZtAiDebugPermissionHelper.allPermissions())
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    onFinished()
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    onFinished()
                }
            })
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_AI_DEBUG_NOTIFICATION
        )
    }

    companion object {
        private const val REQ_AI_DEBUG_NOTIFICATION = 19998
    }

    override fun onResume() {
        super.onResume()
        refreshWorkstationStatus()
        refreshAiDebugDetailVisibility()
    }

    private fun refreshWorkstationStatus() {
        val enabled = UserSetManage.get().getZTUserBean().isZtWorkstationEnabled
        ztWorkstationStatus.text = if (enabled) {
            UUtils.getString(R.string.zt_workstation_status_on)
        } else {
            UUtils.getString(R.string.zt_workstation_status_off)
        }
    }

    private fun initStatus() {
        ztWorkstationLl.visibility = View.GONE
        val ztUserBean = UserSetManage.get().getZTUserBean()
        ztDownloadServicesSwitch.isChecked = ztUserBean.isOpenDownloadFileServices
        refreshWorkstationStatus()
        inputMethodTriggerCloseSwitch.isChecked = ztUserBean.isInputMethodTriggerClose
        styleTriggerOffSwitch.isChecked = ztUserBean.isStyleTriggerOff
        isToolShowSwitch.isChecked = ztUserBean.isToolShow
        forceUseNumpadSwitch.isChecked = ztUserBean.isForceUseNumpad
        logOutputSwitch.isChecked = ztUserBean.isOutputLOG
        shellTermuxSwitch.isChecked = isShellTermux()
        volumeFunctionSwitch.isChecked = ztUserBean.isResetVolume
        foldMenuCloseSwitch.isChecked = ztUserBean.isCloseFoldMenu
        mainMenuConfigCloseSwitch.isChecked = ztUserBean.isDisableMainConfigMenu
        editorWordWrapSwitch.isChecked = ztUserBean.isEditorWordWrap
        aiDebugSwitchUpdating = true
        ztAiDebugSwitch.isChecked = ztUserBean.isZtAiDebugEnabled
        aiDebugSwitchUpdating = false
        aiDebugRootSwitchUpdating = true
        ztAiDebugRootSwitch.isChecked = ztUserBean.isZtAiDebugRootEnabled
        aiDebugRootSwitchUpdating = false
        refreshAiDebugDetailVisibility()
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
        mExperimentalFeature.setOnClickListener {
            // 此入口不稳定，不要轻易在正式版本放开
            index += 1
            if (index > 20) {
                index = 0
                ztWorkstationLl.visibility = View.VISIBLE
            }
        }
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: LinearLayout) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !(switchCompat.isChecked)
        }
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            when (switchCompat) {
                ztDownloadServicesSwitch -> {
                    ztUserBean.isOpenDownloadFileServices = switchCompat.isChecked
                    Thread {
                        if (isChecked) {
                            if (!get().isServicesRun()) {
                                get().bootHttp()
                            }
                        } else {
                            get().stopServer()
                        }
                    }.start()
                    ztUserBean.isInputMethodTriggerClose = switchCompat.isChecked
                }
                styleTriggerOffSwitch -> {
                    ztUserBean.isStyleTriggerOff = switchCompat.isChecked
                }
                isToolShowSwitch -> {
                    ztUserBean.isToolShow = switchCompat.isChecked
                }
                forceUseNumpadSwitch -> {
                    ztUserBean.isForceUseNumpad = switchCompat.isChecked
                }
                logOutputSwitch -> {
                    ztUserBean.isOutputLOG = switchCompat.isChecked
                    LogUtils.isShow = switchCompat.isChecked
                }
                shellTermuxSwitch -> {
                    if (shellTermuxSwitch.isChecked) {
                        writerShellTermux()
                    } else {
                        File(FileUrl.timerShellExecFile).delete()
                    }

                }
                volumeFunctionSwitch -> {
                    ztUserBean.isResetVolume = switchCompat.isChecked
                }
                foldMenuCloseSwitch -> {
                    ztUserBean.isCloseFoldMenu = switchCompat.isChecked
                    //Toast.makeText(this, UUtils.getString(R.string.zt_command_path_ok), Toast.LENGTH_SHORT).show()
                }
                mainMenuConfigCloseSwitch -> {
                    ztUserBean.isDisableMainConfigMenu = switchCompat.isChecked
                    //Toast.makeText(this, UUtils.getString(R.string.zt_command_path_ok), Toast.LENGTH_SHORT).show()
                }
                editorWordWrapSwitch -> {
                    ztUserBean.isEditorWordWrap = switchCompat.isChecked
                }
            }
            UserSetManage.get().setZTUserBean(ztUserBean)
        }
    }

    private fun writerShellTermux() {
        val file = File(FileUrl.timerShellExecFile)
        if (!file.exists()) {
            UUtils.writerFile("runcommand/execTermuxEnv.sh", file)
            Shell.cmd("shell_chmod").exec()
        }
    }

    private fun isShellTermux(): Boolean {
        val file = File(FileUrl.timerShellExecFile)
        return file.exists()
    }

}
