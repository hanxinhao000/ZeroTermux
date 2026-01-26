package com.termux.zerocore.settings

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileHttpUtils.Companion.get
import com.topjohnwu.superuser.Shell
import com.zp.z_file.util.LogUtils
import java.io.File

class ZeroTermuxSettingsActivity : AppCompatActivity() {

    private val ztDownloadServicesSwitch by lazy {findViewById<SwitchCompat>(R.id.zt_download_services_switch)}
    private val ztDownloadServicesLl by lazy {findViewById<LinearLayout>(R.id.zt_download_services_ll)}

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zero_termux_settings)
        initView()
        initStatus()
    }

    private fun initView() {
        setSwitchStatus(ztDownloadServicesSwitch, ztDownloadServicesLl)
        setSwitchStatus(inputMethodTriggerCloseSwitch, inputMethodTriggerCloseLl)
        setSwitchStatus(styleTriggerOffSwitch, styleTriggerOffLl)
        setSwitchStatus(isToolShowSwitch, isToolShowLl)
        setSwitchStatus(forceUseNumpadSwitch, forceUseNumpadLl)
        setSwitchStatus(logOutputSwitch, logOutputLl)
        setSwitchStatus(shellTermuxSwitch, shellTermuxLl)
        setSwitchStatus(volumeFunctionSwitch, volumeFunctionLl)
        setSwitchStatus(foldMenuCloseSwitch, foldMenuCloseLl)
        setSwitchStatus(mainMenuConfigCloseSwitch, mainMenuConfigCloseLl)
    }

    private fun initStatus() {
        val ztUserBean = UserSetManage.get().getZTUserBean()
        ztDownloadServicesSwitch.isChecked = ztUserBean.isOpenDownloadFileServices
        inputMethodTriggerCloseSwitch.isChecked = ztUserBean.isInputMethodTriggerClose
        styleTriggerOffSwitch.isChecked = ztUserBean.isStyleTriggerOff
        isToolShowSwitch.isChecked = ztUserBean.isToolShow
        forceUseNumpadSwitch.isChecked = ztUserBean.isForceUseNumpad
        logOutputSwitch.isChecked = ztUserBean.isOutputLOG
        shellTermuxSwitch.isChecked = isShellTermux()
        volumeFunctionSwitch.isChecked = ztUserBean.isResetVolume
        foldMenuCloseSwitch.isChecked = ztUserBean.isCloseFoldMenu
        mainMenuConfigCloseSwitch.isChecked = ztUserBean.isDisableMainConfigMenu
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
                }
                inputMethodTriggerCloseSwitch -> {
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
