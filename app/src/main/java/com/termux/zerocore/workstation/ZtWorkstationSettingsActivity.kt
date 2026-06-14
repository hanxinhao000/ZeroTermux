package com.termux.zerocore.workstation

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.settings.BaseTitleActivity

class ZtWorkstationSettingsActivity : BaseTitleActivity() {

    private enum class SwitchKind {
        MASTER, AUTO_START, TERMINAL, CAMERA, FILES, PHONE_SMS
    }

    private data class SwitchBinding(
        val kind: SwitchKind,
        val root: View,
        val contentLl: LinearLayout,
        val switch: SwitchCompat,
        val titleRes: Int,
        val riskRes: Int
    )

    private var switchUpdating = false
    private var disclaimerShownThisResume = false
    private lateinit var bindings: List<SwitchBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_workstation_settings)
        setBaseTitle(UUtils.getString(R.string.zt_workstation_settings_title))
        initBindings()
        initSwitchHandlers()
        refreshUiFromBean()
    }

    override fun onResume() {
        super.onResume()
        if (!disclaimerShownThisResume) {
            disclaimerShownThisResume = true
            showDisclaimerDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        disclaimerShownThisResume = false
    }

    private fun initBindings() {
        bindings = listOf(
            bindSwitch(R.id.ws_master_card, SwitchKind.MASTER, R.string.zt_workstation_master, R.string.zt_workstation_master_note),
            bindSwitch(R.id.ws_auto_start_card, SwitchKind.AUTO_START, R.string.zt_workstation_auto_start, R.string.zt_workstation_auto_start_risk),
            bindSwitch(R.id.ws_terminal_card, SwitchKind.TERMINAL, R.string.zt_workstation_terminal, R.string.zt_workstation_terminal_risk),
            bindSwitch(R.id.ws_camera_card, SwitchKind.CAMERA, R.string.zt_workstation_camera, R.string.zt_workstation_camera_risk),
            bindSwitch(R.id.ws_files_card, SwitchKind.FILES, R.string.zt_workstation_files, R.string.zt_workstation_files_risk),
            bindSwitch(R.id.ws_phone_sms_card, SwitchKind.PHONE_SMS, R.string.zt_workstation_phone_sms, R.string.zt_workstation_phone_sms_risk)
        )
    }

    private fun bindSwitch(
        rootId: Int,
        kind: SwitchKind,
        titleRes: Int,
        riskRes: Int
    ): SwitchBinding {
        val root = findViewById<View>(rootId)
        val title = root.findViewById<TextView>(R.id.ws_item_title)
        val subtitle = root.findViewById<TextView>(R.id.ws_item_subtitle)
        val contentLl = root.findViewById<LinearLayout>(R.id.ws_item_content_ll)
        val switch = root.findViewById<SwitchCompat>(R.id.ws_item_switch)
        title.setText(titleRes)
        subtitle.setText(riskRes)
        return SwitchBinding(kind, root, contentLl, switch, titleRes, riskRes)
    }

    private fun initSwitchHandlers() {
        bindings.forEach { binding ->
            binding.contentLl.setOnClickListener {
                if (!binding.switch.isEnabled) return@setOnClickListener
                binding.switch.isChecked = !binding.switch.isChecked
            }
            binding.switch.setOnCheckedChangeListener { _, isChecked ->
                if (switchUpdating) return@setOnCheckedChangeListener
                if (isChecked) {
                    confirmEnable(binding) {
                        applySwitchValue(binding.kind, true)
                    }
                } else {
                    applySwitchValue(binding.kind, false)
                }
            }
        }
    }

    private fun refreshUiFromBean() {
        val bean = UserSetManage.get().getZTUserBean()
        switchUpdating = true
        binding(SwitchKind.MASTER)?.switch?.isChecked = bean.isZtWorkstationEnabled
        binding(SwitchKind.AUTO_START)?.switch?.isChecked = bean.isZtWorkstationAutoStart
        binding(SwitchKind.TERMINAL)?.switch?.isChecked = bean.isZtWorkstationTerminalEnabled
        binding(SwitchKind.CAMERA)?.switch?.isChecked = bean.isZtWorkstationCameraEnabled
        binding(SwitchKind.FILES)?.switch?.isChecked = bean.isZtWorkstationFilesEnabled
        binding(SwitchKind.PHONE_SMS)?.switch?.isChecked = bean.isZtWorkstationPhoneSmsEnabled
        switchUpdating = false
        updateAutoStartEnabled(bean.isZtWorkstationEnabled)
    }

    private fun binding(kind: SwitchKind): SwitchBinding? = bindings.find { it.kind == kind }

    private fun updateAutoStartEnabled(masterOn: Boolean) {
        val auto = binding(SwitchKind.AUTO_START) ?: return
        auto.switch.isEnabled = masterOn
        auto.contentLl.isEnabled = masterOn
        auto.root.alpha = if (masterOn) 1f else 0.45f
    }

    private fun confirmEnable(binding: SwitchBinding, onConfirm: () -> Unit) {
        revertSwitch(binding, false)
        AlertDialog.Builder(this)
            .setTitle(binding.titleRes)
            .setMessage(binding.riskRes)
            .setPositiveButton(R.string.zt_workstation_confirm_enable) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel) { _, _ -> revertSwitch(binding, false) }
            .setOnCancelListener { revertSwitch(binding, false) }
            .show()
    }

    private fun showDisclaimerDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.zt_workstation_disclaimer_title)
            .setMessage(R.string.zt_workstation_disclaimer_message)
            .setCancelable(false)
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun revertSwitch(binding: SwitchBinding, checked: Boolean) {
        switchUpdating = true
        binding.switch.isChecked = checked
        switchUpdating = false
    }

    private fun applySwitchValue(kind: SwitchKind, enabled: Boolean) {
        when (kind) {
            SwitchKind.MASTER -> applyMasterSwitch(enabled)
            SwitchKind.AUTO_START -> {
                updateBean { it.isZtWorkstationAutoStart = enabled }
                refreshUiFromBean()
            }
            SwitchKind.TERMINAL -> applyFeatureSwitch(kind, enabled) {
                it.isZtWorkstationTerminalEnabled = enabled
            }
            SwitchKind.CAMERA -> applyFeatureSwitch(kind, enabled) {
                it.isZtWorkstationCameraEnabled = enabled
            }
            SwitchKind.FILES -> applyFeatureSwitch(kind, enabled) {
                it.isZtWorkstationFilesEnabled = enabled
            }
            SwitchKind.PHONE_SMS -> applyFeatureSwitch(kind, enabled) {
                it.isZtWorkstationPhoneSmsEnabled = enabled
            }
        }
    }

    private fun applyFeatureSwitch(
        kind: SwitchKind,
        enabled: Boolean,
        setter: (com.termux.zerocore.bean.ZTUserBean) -> Unit
    ) {
        if (!enabled) {
            updateBean { setter(it) }
            refreshUiFromBean()
            return
        }
        val permissions = permissionsFor(kind)
        if (permissions.isEmpty()) {
            updateBean { setter(it) }
            refreshUiFromBean()
            return
        }
        requestPermissions(permissions) { granted ->
            if (granted) {
                updateBean { setter(it) }
                refreshUiFromBean()
            } else {
                revertSwitch(binding(kind)!!, false)
                Toast.makeText(this, deniedMessageFor(kind), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyMasterSwitch(enabled: Boolean) {
        if (enabled) {
            updateBean { it.isZtWorkstationEnabled = true }
            ZtWorkstationManager.start(this)
            refreshUiFromBean()
        } else {
            updateBean {
                it.isZtWorkstationAutoStart = false
                it.isZtWorkstationEnabled = false
            }
            ZtWorkstationManager.stop(this)
            refreshUiFromBean()
        }
    }

    private fun updateBean(block: (com.termux.zerocore.bean.ZTUserBean) -> Unit) {
        val bean = UserSetManage.get().getZTUserBean()
        block(bean)
        UserSetManage.get().setZTUserBean(bean)
    }

    private fun permissionsFor(kind: SwitchKind): List<String> = when (kind) {
        SwitchKind.CAMERA -> listOf(Permission.CAMERA)
        SwitchKind.FILES -> listOf(
            Permission.READ_EXTERNAL_STORAGE,
            Permission.WRITE_EXTERNAL_STORAGE
        )
        SwitchKind.PHONE_SMS -> listOf(
            Permission.READ_SMS,
            Permission.SEND_SMS,
            Permission.READ_CONTACTS,
            Permission.READ_PHONE_STATE
        )
        else -> emptyList()
    }

    private fun deniedMessageFor(kind: SwitchKind): String = when (kind) {
        SwitchKind.CAMERA -> UUtils.getString(R.string.zt_workstation_permission_denied_camera)
        SwitchKind.FILES -> UUtils.getString(R.string.zt_workstation_permission_denied_files)
        SwitchKind.PHONE_SMS -> UUtils.getString(R.string.zt_workstation_permission_denied_phone_sms)
        else -> UUtils.getString(R.string.zt_workstation_permission_denied)
    }

    private fun requestPermissions(permissions: List<String>, onResult: (Boolean) -> Unit) {
        var request = XXPermissions.with(this)
        permissions.forEach { request = request.permission(it) }
        request.request(object : OnPermissionCallback {
            override fun onGranted(permissions: List<String>, all: Boolean) {
                onResult(all)
            }

            override fun onDenied(permissions: List<String>, never: Boolean) {
                onResult(false)
            }
        })
    }
}
