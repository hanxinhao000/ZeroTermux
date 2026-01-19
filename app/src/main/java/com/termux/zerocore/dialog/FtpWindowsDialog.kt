package com.termux.zerocore.dialog

import android.content.Context
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.github.iielse.switchbutton.SwitchView
import com.github.iielse.switchbutton.SwitchView.OnStateChangedListener
import com.termux.R
import com.termux.zerocore.bean.SaveDataZeroEngine
import com.termux.zerocore.ftp.FsService
import com.termux.zerocore.ftp.new_ftp.Constants
import com.termux.zerocore.ftp.new_ftp.config.FTP_CONFIG
import com.termux.zerocore.ftp.new_ftp.services.FtpService
import com.termux.zerocore.url.FileUrl.mainFilesUrl
import kotlinx.coroutines.*

class FtpWindowsDialog : BaseDialogCentre {

    object FtpWindowsDialogConstant {
        public val TAG = "FtpWindowsDialog"
        public val ROOT_PATH_SDCARD = 10001
        public val ROOT_PATH_ZERO_HOME = 10002
    }
    private var mSwitchBtn: SwitchView? = null
    private var mPortEd: EditText? = null
    private var mSdcard: TextView? = null
    private var mFtpMsg: TextView? = null
    private var mTitleFtp: TextView? = null
    private var mZeroRoot: TextView? = null
    private var mPassword: EditText? = null
    private var mProgressBar: ProgressBar? = null
    private var mPopupFtpWindowsSwitchBtnListener: PopupFtpWindowsSwitchBtnListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)


    override fun initViewDialog(mView: View) {
        mSwitchBtn = mView.findViewById(R.id.switch_btn)
        mPortEd = mView.findViewById(R.id.port_ed)
        mProgressBar = mView.findViewById(R.id.pro)
        mSdcard = mView.findViewById(R.id.sdcard)
        mZeroRoot = mView.findViewById(R.id.zero_root)
        mFtpMsg = mView.findViewById(R.id.ftp_msg)
        mPassword = mView.findViewById(R.id.password)
        mTitleFtp = mView.findViewById(R.id.title_ftp)
        initViewClick()
    }

    private fun initViewClick() {
        initSwitchIndexConfig()
        writerConfig()
        initTitleText()
        mSdcard?.setOnClickListener {
            switchIndex(FtpWindowsDialogConstant.ROOT_PATH_SDCARD, true)
        }
        mFtpMsg?.setOnClickListener {
           // UUtils.startUrl("https://github.com/ppareit/swiftp")
        }
        mZeroRoot?.setOnClickListener {
            switchIndex(FtpWindowsDialogConstant.ROOT_PATH_ZERO_HOME, true)
        }
        mTitleFtp?.setOnClickListener {
            if (FsService.isRunning()) {
                UUtils.startUrl("ftp://127.0.0.1:" + SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT))
            }
        }
        mSwitchBtn?.let {
            it.isOpened = FtpService.isFTPServiceRunning()
            it.setOnStateChangedListener(object :OnStateChangedListener{
                override fun toggleToOn(view: SwitchView?) {
                    switchOn(it)
                }
                override fun toggleToOff(view: SwitchView?) {
                    switchOff(it)
                }

             })
        }
    }



    override fun getContentView(): Int {
      return R.layout.pupu_window_ftp_start
    }

    private fun switchOn(it:SwitchView) {
        /**
         * 初始化配置文件
         */
        initDefUser()
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                FtpService.stopService()
                it.visibility = View.INVISIBLE
                mProgressBar?.visibility = View.VISIBLE
            }
            withContext(Dispatchers.IO) {
                delay(1000)
            }
            withContext(Dispatchers.Main){
                FtpService.startService(UUtils.getContext())
            }
            withContext(Dispatchers.IO) {
                delay(1500)
            }
            withContext(Dispatchers.Main){
                val stringData =
                    SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_CHROOT)
                LogUtils.d(FtpWindowsDialogConstant.TAG, "initDefUser stringData:$stringData")
                it.visibility = View.VISIBLE
                mProgressBar?.visibility = View.INVISIBLE
                it.isOpened = FtpService.isFTPServiceRunning()
                mPopupFtpWindowsSwitchBtnListener?.switchBtn(true)
                initTitleText()
            }
        }
    }

    private fun switchOff(it:SwitchView) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                FtpService.stopService()
                it.visibility = View.INVISIBLE
                mProgressBar?.visibility = View.VISIBLE
            }
            withContext(Dispatchers.IO) {
                delay(2000)
            }
            withContext(Dispatchers.Main){
                it.visibility = View.VISIBLE
                mProgressBar?.visibility = View.INVISIBLE
                it.isOpened = false
                mPopupFtpWindowsSwitchBtnListener?.switchBtn(false)
                initTitleText()
            }
        }
    }

    private fun writerConfig() {
        mPortEd?.addTextChangedListener(object :TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0)) {
                    SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT, SaveDataZeroEngine.FTP_DEF_PORT)
                    return
                }
                try {
                    val parseInt = Integer.parseInt(p0!!.toString())
                    if (parseInt < SaveDataZeroEngine.FTP_MIN_PORT) {
                        mPortEd?.setTextColor(UUtils.getColor(R.color.color_CC5A6B))
                    } else if(parseInt > SaveDataZeroEngine.FTP_MAX_PORT) {
                        UUtils.showMsg(UUtils.getString(R.string.ftp_port_msg))
                        mPortEd?.setText("2121")
                    } else {
                        mPortEd?.setTextColor(UUtils.getColor(R.color.color_ffffff))
                        SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT, mPortEd!!.text.toString())
                    }
                } catch (e:Exception) {
                    UUtils.showMsg(UUtils.getString(R.string.ftp_port_msg))
                    mPortEd?.setText("2121")
                }
            }
        })
        mPassword?.addTextChangedListener(object :TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0)) {
                    SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PASS_WORD, SaveDataZeroEngine.FTP_DEF_PWD)
                    return
                }
                SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PASS_WORD, mPassword?.text.toString())
            }
        })
    }

    private fun switchIndex(index: Int, isInit: Boolean) {
        mSdcard?.setBackgroundResource(R.drawable.shape_r_3dp_161823_232635)
        mZeroRoot?.setBackgroundResource(R.drawable.shape_r_3dp_161823_232635)
        when (index) {
            FtpWindowsDialogConstant.ROOT_PATH_SDCARD -> {
                if (isInit) {
                    SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_CHROOT, SaveDataZeroEngine.FTP_SDCARD_ROOT)
                }
              //  FTP_CONFIG.PATH = Environment.getExternalStorageDirectory().absolutePath
                mSdcard?.setBackgroundResource(R.drawable.shape_line_8cff5a)
            }
            FtpWindowsDialogConstant.ROOT_PATH_ZERO_HOME -> {
                if (isInit) {
                    SaveDataZeroEngine.putStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_CHROOT, SaveDataZeroEngine.FTP_ZERO_TERMUX_FILE)
                }
                mZeroRoot?.setBackgroundResource(R.drawable.shape_line_8cff5a)
             //   FTP_CONFIG.PATH = mainFilesUrl
            }
        }
        if (isInit) {
            mSwitchBtn?.let {
                switchOn(it)
            }
        }
    }

    private fun initDefUser() {
        writerDefConfig(SaveDataZeroEngine.FTP_USER_NAME, SaveDataZeroEngine.FTP_DEF_USER)
        writerDefConfig(SaveDataZeroEngine.FTP_PASS_WORD, SaveDataZeroEngine.FTP_DEF_PWD)
        writerDefConfig(SaveDataZeroEngine.FTP_PORT, SaveDataZeroEngine.FTP_DEF_PORT)
        writerDefConfig(SaveDataZeroEngine.FTP_CHROOT, SaveDataZeroEngine.FTP_SDCARD_ROOT)
        initSwitchIndexConfig()
    }

    private fun initTitleText() {
        if (FtpService.isFTPServiceRunning()) {
            val port =
                SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT)
            val empty = SaveDataZeroEngine.isEmpty(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT)
            if (empty) {
                mTitleFtp?.setText(UUtils.getString(R.string.ftp_start_window) + "\nftp://" + UUtils.getHostIP() + ":2121")
            } else {
                mTitleFtp?.setText(UUtils.getString(R.string.ftp_start_window) + "\nftp://" + UUtils.getHostIP() + ":" + port)
            }
            mTitleFtp?.setTextColor(UUtils.getColor(R.color.color_48baf3))
        } else {
            mTitleFtp?.setText("ZeroFTP")
            mTitleFtp?.setTextColor(UUtils.getColor(R.color.color_ffffff))
        }
    }

    private fun initSwitchIndexConfig() {
        if (!SaveDataZeroEngine.isEmpty(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT)) {
            val port =
                SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PORT)
            mPortEd?.setText(port)
            try {
                Constants.PreferenceConsts.PORT_NUMBER_DEFAULT = port.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        if (!SaveDataZeroEngine.isEmpty(UUtils.getContext(), SaveDataZeroEngine.FTP_PASS_WORD)) {
            val password =
                SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_PASS_WORD)
            mPassword?.setText(password)
            FTP_CONFIG.PASSWORD = password
        }
        val stringData =
            SaveDataZeroEngine.getStringData(UUtils.getContext(), SaveDataZeroEngine.FTP_CHROOT)
        if (stringData.contains("com.termux")) {
            switchIndex(FtpWindowsDialogConstant.ROOT_PATH_ZERO_HOME, false)
        } else {
            switchIndex(FtpWindowsDialogConstant.ROOT_PATH_SDCARD, false)
        }

    }

    public fun setPopupFtpWindowsSwitchBtnListener(mPopupFtpWindowsSwitchBtnListener: PopupFtpWindowsSwitchBtnListener) {
        this.mPopupFtpWindowsSwitchBtnListener = mPopupFtpWindowsSwitchBtnListener
    }

    public interface PopupFtpWindowsSwitchBtnListener {
        fun switchBtn(switch: Boolean)
    }

    private fun writerDefConfig(key: String, value: String) {
        val empty = SaveDataZeroEngine.isEmpty(UUtils.getContext(), key)
        if (empty) {
            SaveDataZeroEngine.putStringData(UUtils.getContext(), key, value)
        }
    }


}


