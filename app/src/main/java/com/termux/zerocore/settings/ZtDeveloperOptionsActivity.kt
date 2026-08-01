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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.aidebug.ZtAiDebugManager
import com.termux.zerocore.aidebug.ZtAiDebugMatchCodeHelper
import com.termux.zerocore.aidebug.ZtAiDebugPermissionHelper
import com.termux.zerocore.crashhistory.ZtCrashHistoryActivity
import com.termux.zerocore.ftp.new_ftp.utils.NetworkEnvironmentUtil
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.utils.FileHttpUtils.Companion.get
import com.termux.zerocore.workstation.ZtWorkstationSettingsActivity
import com.zp.z_file.util.LogUtils

class ZtDeveloperOptionsActivity : BaseTitleActivity() {

    private val ztDownloadServicesSwitch by lazy { findViewById<SwitchCompat>(R.id.zt_download_services_switch) }
    private val ztDownloadServicesLl by lazy { findViewById<LinearLayout>(R.id.zt_download_services_ll) }

    private val ztWorkstationLl by lazy { findViewById<CardView>(R.id.zt_workstation_cv) }
    private val ztWorkstationStatus by lazy { findViewById<TextView>(R.id.zt_workstation_status) }

    private val logOutputSwitch by lazy { findViewById<SwitchCompat>(R.id.log_output_switch) }
    private val logOutputLl by lazy { findViewById<LinearLayout>(R.id.log_output_ll) }

    private val ztAiDebugSwitch by lazy { findViewById<SwitchCompat>(R.id.zt_ai_debug_switch) }
    private val ztAiDebugLl by lazy { findViewById<LinearLayout>(R.id.zt_ai_debug_ll) }
    private val ztAiDebugSummary by lazy { findViewById<TextView>(R.id.zt_ai_debug_summary) }
    private val ztAiDebugDetailCv by lazy { findViewById<CardView>(R.id.zt_ai_debug_detail_cv) }
    private val ztAiDebugMatchCodeValue by lazy { findViewById<TextView>(R.id.zt_ai_debug_match_code_value) }
    private val ztAiDebugMatchCodeReveal by lazy { findViewById<ImageButton>(R.id.zt_ai_debug_match_code_reveal) }
    private val ztAiDebugRootSwitch by lazy { findViewById<SwitchCompat>(R.id.zt_ai_debug_root_switch) }
    private val ztAiDebugRootLl by lazy { findViewById<LinearLayout>(R.id.zt_ai_debug_root_ll) }

    private var aiDebugSwitchUpdating = false
    private var aiDebugRootSwitchUpdating = false
    private var aiDebugMatchCodeRevealed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_developer_options)
        setBaseTitle(UUtils.getString(R.string.zt_developer_options_title))
        initView()
        initStatus()
    }

    private fun initView() {
        ztDownloadServicesLl.setOnClickListener {
            ztDownloadServicesSwitch.isChecked = !ztDownloadServicesSwitch.isChecked
        }
        ztDownloadServicesSwitch.setOnCheckedChangeListener { _, isChecked ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.isOpenDownloadFileServices = isChecked
            Thread {
                if (isChecked) {
                    if (!get().isServicesRun()) {
                        get().bootHttp()
                    }
                } else {
                    get().stopServer()
                }
            }.start()
            UserSetManage.get().setZTUserBean(ztUserBean)
        }

        ztWorkstationLl.setOnClickListener {
            startActivity(Intent(this, ZtWorkstationSettingsActivity::class.java))
        }

        logOutputLl.setOnClickListener {
            logOutputSwitch.isChecked = !logOutputSwitch.isChecked
        }
        logOutputSwitch.setOnCheckedChangeListener { _, isChecked ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.isOutputLOG = isChecked
            LogUtils.isShow = isChecked
            UserSetManage.get().setZTUserBean(ztUserBean)
        }

        findViewById<CardView>(R.id.crash_history_settings_card).setOnClickListener {
            startActivity(Intent(this, ZtCrashHistoryActivity::class.java))
        }

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
        refreshAiDebugEndpointSummary()
    }

    private fun refreshAiDebugEndpointSummary() {
        val ips = NetworkEnvironmentUtil.getLocalIpv4Addresses()
        val port = ZtAiDebugManager.PORT
        val endpoint = if (ips.isNotEmpty()) {
            ips.joinToString(" / ") { "$it:$port" }
        } else {
            "<ip>:$port"
        }
        ztAiDebugSummary.text = getString(R.string.zt_ai_debug_summary_with_endpoint, endpoint)
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
        val ztUserBean = UserSetManage.get().getZTUserBean()
        ztDownloadServicesSwitch.isChecked = ztUserBean.isOpenDownloadFileServices
        refreshWorkstationStatus()
        logOutputSwitch.isChecked = ztUserBean.isOutputLOG
        aiDebugSwitchUpdating = true
        ztAiDebugSwitch.isChecked = ztUserBean.isZtAiDebugEnabled
        aiDebugSwitchUpdating = false
        aiDebugRootSwitchUpdating = true
        ztAiDebugRootSwitch.isChecked = ztUserBean.isZtAiDebugRootEnabled
        aiDebugRootSwitchUpdating = false
        refreshAiDebugDetailVisibility()
    }
}
