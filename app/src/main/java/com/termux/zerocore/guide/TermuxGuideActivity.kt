package com.termux.zerocore.guide

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.SaveData.getStringOther
import com.example.xh_lib.utils.SaveData.saveStringOther
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.settings.BaseTitleActivity
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.url.FileUrl.MAIN_XINHAO_PATH
import com.termux.zerocore.url.FileUrl.zeroTermuxApk
import com.termux.zerocore.url.FileUrl.zeroTermuxCommand
import com.termux.zerocore.url.FileUrl.zeroTermuxData
import com.termux.zerocore.url.FileUrl.zeroTermuxFont
import com.termux.zerocore.url.FileUrl.zeroTermuxHome
import com.termux.zerocore.url.FileUrl.zeroTermuxIso
import com.termux.zerocore.url.FileUrl.zeroTermuxModule
import com.termux.zerocore.url.FileUrl.zeroTermuxMysql
import com.termux.zerocore.url.FileUrl.zeroTermuxOnlineSystem
import com.termux.zerocore.url.FileUrl.zeroTermuxQemu
import com.termux.zerocore.url.FileUrl.zeroTermuxServer
import com.termux.zerocore.url.FileUrl.zeroTermuxShare
import com.termux.zerocore.url.FileUrl.zeroTermuxSystem
import com.termux.zerocore.url.FileUrl.zeroTermuxType
import com.termux.zerocore.url.FileUrl.zeroTermuxWebConfig
import com.termux.zerocore.url.FileUrl.zeroTermuxWindows
import com.termux.zerocore.url.FileUrl.zeroTermuxWindowsConfig
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.FileIOUtils.HTML_PATH
import com.termux.zerocore.utils.FileIOUtils.HTML_ZT_LINK_PATH
import com.termux.zerocore.utils.FileIOUtils.XINHAO_PATH
import com.termux.zerocore.utils.FileIOUtils.getHomePath
import java.io.File

class TermuxGuideActivity: BaseTitleActivity() {
    companion object {
        public val TAG = TermuxGuideActivity::class.simpleName
        public const val GUIDE_EXTRA = "zt_guide"
        public const val GUIDE_EXTRA_JUMP_OTHER = "zt_guide_jump"
        public const val GUIDE_AGREEMENT = 0
        public const val GUIDE_USAGE_HABITS = 1
        public const val GUIDE_CREATE_FOLDER = 2
        public const val GUIDE_TERMUX_MAIN = 3

        public var ACTIVITYS: ArrayList<Activity>? = ArrayList<Activity>()
    }
    private var mIsFoldAndroid = false
    // 协议页面
    private var mPreviousCardView: CardView? = null
    private var mPreviousTextView: TextView? = null

    private var mNextCardView: CardView? = null
    private var mNextTextView: TextView? = null

    // 使用习惯
    private var mZeroTermuxGuideSwitch: CardView? = null
    private var mTermuxGuideSwitch: CardView? = null

    // 创建文件夹
    private var mCreateFolderSdcard: CardView? = null
    private var mCreateFolderSdcardAndroid: CardView? = null

    private val mFolders = listOf(
        zeroTermuxHome,
        zeroTermuxData,
        zeroTermuxApk,
        zeroTermuxWindows,
        zeroTermuxCommand,
        zeroTermuxFont,
        zeroTermuxIso,
        zeroTermuxMysql,
        zeroTermuxOnlineSystem,
        zeroTermuxQemu,
        zeroTermuxServer,
        zeroTermuxShare,
        zeroTermuxSystem,
        zeroTermuxWebConfig,
        zeroTermuxModule,
        zeroTermuxWindowsConfig,
        zeroTermuxType
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getGuideCode() == GUIDE_TERMUX_MAIN || UserSetManage.get().getZTUserBean().isJumpGuide && !getGuideJumpOther()) {
            setContentView(R.layout.termux_loading)
            ACTIVITYS?.let {
                it.forEach {
                    it.finish()
                }
                it.clear()
            }
            ACTIVITYS = null
            startActivity(Intent(this@TermuxGuideActivity, TermuxActivity::class.java))
            finish()
        }
        if (getGuideCode() == GUIDE_AGREEMENT) {
            // 第一次进入
            setContentView(R.layout.activity_zt_guide_main1)
            setBaseTitle(UUtils.getString(R.string.guide_zerotermux_settings_agreement))
            goneCancelButton()
        }
        if (getGuideCode() == GUIDE_USAGE_HABITS) {
            // 使用习惯
            setContentView(R.layout.activity_zt_guide_main_usage_habits)
            setBaseTitle(UUtils.getString(R.string.guide_zerotermux_usage_habits))
        }

        if (getGuideCode() == GUIDE_CREATE_FOLDER) {
            //创建文件夹
            setContentView(R.layout.activity_zt_guide_main_create_folder)
            setBaseTitle(UUtils.getString(R.string.guide_zerotermux_create_folder))
        }
        // 协议
        mPreviousCardView = findViewById<CardView>(R.id.previous)
        mPreviousTextView = findViewById<TextView>(R.id.previous_tv)

        mNextCardView = findViewById<CardView>(R.id.next)
        mNextTextView = findViewById<TextView>(R.id.next_tv)

        // 使用习惯
        mZeroTermuxGuideSwitch = findViewById(R.id.zero_termux_guide)
        mTermuxGuideSwitch = findViewById(R.id.termux_guide)

        //创建文件夹
        mCreateFolderSdcard = findViewById(R.id.sdcard)
        mCreateFolderSdcardAndroid = findViewById(R.id.sdcard_android)
        setButtonClickListener()
    }
    // 设置隐私Button点击事件
    private fun setButtonClickListener() {
        if (getGuideCode() == GUIDE_AGREEMENT) {
            mPreviousCardView?.setOnClickListener {
                System.exit(1)
            }
            mNextCardView?.setOnClickListener {
                startGuideActivity(GUIDE_USAGE_HABITS)
            }
        }
        if (getGuideCode() == GUIDE_USAGE_HABITS) {
            switchUsageHabits(true)
            mZeroTermuxGuideSwitch?.let {
                it.setOnClickListener { switchUsageHabits(true) }
            }
            mTermuxGuideSwitch?.let {
                it.setOnClickListener { switchUsageHabits(false) }
            }
            mPreviousTextView?.text = UUtils.getString(R.string.guide_zerotermux_previous)
            mNextTextView?.text = UUtils.getString(R.string.guide_zerotermux_next)
            mPreviousCardView?.setOnClickListener {
                finish()
            }
            mNextCardView?.setOnClickListener {
                startGuideActivity(GUIDE_CREATE_FOLDER)
            }
        }
        if (getGuideCode() == GUIDE_CREATE_FOLDER) {
            switchPath(UserSetManage.get().getZTUserBean().isCreateFolderForSdcardAndroid)
            LogUtils.i(TAG, "setButtonClickListener isCreateFolderForSdcardAndroid: ${UserSetManage.get().getZTUserBean().isCreateFolderForSdcardAndroid}")
            mCreateFolderSdcard?.let {
                it.setOnClickListener { switchPath(false) }
            }
            mCreateFolderSdcardAndroid?.let {
                it.setOnClickListener {
                    switchPath(true)
                    //UUtils.showMsg(UUtils.getString(R.string.guide_zerotermux_create_toast_created))
                }
            }
            if (getGuideJumpOther()) {
                mPreviousTextView?.text = UUtils.getString(R.string.取消)
                mNextTextView?.text = UUtils.getString(R.string.editor_symbol_customize_save)
            } else {
                mPreviousTextView?.text = UUtils.getString(R.string.guide_zerotermux_previous)
                mNextTextView?.text = UUtils.getString(R.string.guide_zerotermux_next)
            }

            mPreviousCardView?.setOnClickListener {
                finish()
            }
            mNextCardView?.setOnClickListener {
                val ztUserBean = UserSetManage.get().getZTUserBean()
                ztUserBean.isCreateFolderForSdcardAndroid = mIsFoldAndroid
                UserSetManage.get().setZTUserBean(ztUserBean)
                val deleteXinhaoFile = File(getHomePath(UUtils.getContext()), XINHAO_PATH)
                val deleteHtmlFile = File(getHomePath(UUtils.getContext()), HTML_PATH)
                val deleteXinhao = deleteXinhaoFile.delete()
                val deleteHtml = deleteHtmlFile.delete()
                LogUtils.i(TAG, "setButtonClickListener deleteXinhao: $deleteXinhao")
                LogUtils.i(TAG, "setButtonClickListener deleteHtml: $deleteHtml")
                LogUtils.i(TAG, "setButtonClickListener deleteXinhaoFile: ${deleteXinhaoFile.absolutePath}")
                LogUtils.i(TAG, "setButtonClickListener deleteHtmlFile: ${deleteHtmlFile.absolutePath}")
                FileIOUtils.createWebConfig()
                if (UserSetManage.get().getZTUserBean().isCreateFolderForSdcardAndroid) {
                    createSdcardAndroidFiles()
                } else {
                    createSdcardFiles()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        LogUtils.i(TAG, "setButtonClickListener finish")

    }
    // 使用习惯切换
    private fun switchUsageHabits(isZeroTermuxUsageHabits: Boolean) {
        mZeroTermuxGuideSwitch?.setCardBackgroundColor(getColor(R.color.color_55000000))
        mTermuxGuideSwitch?.setCardBackgroundColor(getColor(R.color.color_55000000))
        val ztUserBean = UserSetManage.get().getZTUserBean()
        if (isZeroTermuxUsageHabits) {
            ztUserBean.isToolShow = false
            ztUserBean.isResetVolume = false
            mZeroTermuxGuideSwitch?.setCardBackgroundColor(getColor(R.color.color_5548baf3))
        } else {
            ztUserBean.isToolShow = true
            ztUserBean.isResetVolume = true
            mTermuxGuideSwitch?.setCardBackgroundColor(getColor(R.color.color_5548baf3))
        }
        UserSetManage.get().setZTUserBean(ztUserBean)
    }

    private fun switchPath(isCreateFolderForSdcardAndroid: Boolean) {
        mCreateFolderSdcard?.setCardBackgroundColor(getColor(R.color.color_55000000))
        mCreateFolderSdcardAndroid?.setCardBackgroundColor(getColor(R.color.color_55000000))
        mIsFoldAndroid = isCreateFolderForSdcardAndroid
        if (isCreateFolderForSdcardAndroid) {
            mCreateFolderSdcardAndroid?.setCardBackgroundColor(getColor(R.color.color_5548baf3))
        } else {
            mCreateFolderSdcard?.setCardBackgroundColor(getColor(R.color.color_5548baf3))
        }
    }

    private fun startGuideActivity(code: Int) {
        if (code == GUIDE_TERMUX_MAIN) {
            val ztUserBean = UserSetManage.get().getZTUserBean();
            ztUserBean.isJumpGuide = true
            UserSetManage.get().setZTUserBean(ztUserBean)
        }
        ACTIVITYS?.add(this)
        val guideIntent = Intent()
        guideIntent.setClass(this@TermuxGuideActivity, TermuxGuideActivity::class.java)
        guideIntent.putExtra(GUIDE_EXTRA, code)
        startActivity(guideIntent)
    }

    private fun getGuideCode(): Int {
        if (intent == null) {
            return GUIDE_AGREEMENT
        }
        return intent.getIntExtra(GUIDE_EXTRA, GUIDE_AGREEMENT)
    }

    private fun getGuideJumpOther(): Boolean {
        if (intent == null) {
            return false
        }
        return intent.getBooleanExtra(GUIDE_EXTRA_JUMP_OTHER, false)
    }
    // 创建 Sdcard/Android/data/com.termux 目录
    private fun createSdcardAndroidFiles() {
        val androidDataHome = FileIOUtils.getAndroidDataHome(applicationContext)
        androidDataHome?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        listOf(
            FileUrl.MAIN_XINHAO_DATA_PATH, FileUrl.MAIN_XINHAO_APK_PATH,
            FileUrl.MAIN_XINHAO_WINDOWS_PATH, FileUrl.MAIN_XINHAO_COMMAND_PATH, FileUrl.MAIN_XINHAO_FONT_PATH,
            FileUrl.MAIN_XINHAO_ISO_PATH, FileUrl.MAIN_XINHAO_MYSQL_PATH, FileUrl.MAIN_XINHAO_ONLINE_SYSTEM_PATH,
            FileUrl.MAIN_XINHAO_QEMU_PATH, FileUrl.MAIN_XINHAO_SERVER_PATH, FileUrl.MAIN_XINHAO_SHARE_PATH,
            FileUrl.MAIN_XINHAO_SYSTEM_PATH, FileUrl.MAIN_XINHAO_WEB_CONFIG_PATH, FileUrl.MAIN_XINHAO_MODULE_PATH,
            FileUrl.MAIN_XINHAO_WINDOWS_CONFIG_PATH, FileUrl.MAIN_XINHAO_TYPE_ANDROID_PATH
        ).forEach { path ->
            FileIOUtils.getAndroidDataHomeChildPath(applicationContext, path).takeIf { !it.exists() }?.mkdirs()
        }
        UUtils.showMsg(UUtils.getString(R.string.guide_zerotermux_create_toast_ok))
        foldJumpActivity()
    }

    private fun foldJumpActivity() {
        if (getGuideJumpOther()) {
            finish()
        } else {
            startGuideActivity(GUIDE_TERMUX_MAIN)
        }
    }
    // 创建 Sdcard 目录
    private fun createSdcardFiles() {
        if (!isXinhaoFold()) {
            XXPermissions.with(this@TermuxGuideActivity)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String?>?, all: Boolean) {
                        if (all) {
                            createXinhaoFolders()
                            UUtils.showMsg(UUtils.getString(R.string.guide_zerotermux_create_toast_ok))
                        } else {
                            UUtils.showMsg(UUtils.getString(R.string.guide_zerotermux_create_toast_no))
                        }
                        foldJumpActivity()
                    }

                    override fun onDenied(permissions: MutableList<String?>?, never: Boolean) {
                        val msg = UUtils.getString(R.string.guide_zerotermux_create_toast_no)
                        if (never) {
                            UUtils.showMsg(msg)
                            XXPermissions.startPermissionActivity(
                                this@TermuxGuideActivity,
                                permissions
                            )
                        } else {
                            UUtils.showMsg(msg)
                        }
                        foldJumpActivity()
                    }
                })
        } else {
            UUtils.showMsg(UUtils.getString(R.string.guide_zerotermux_create_toast_ok))
            foldJumpActivity()
        }
    }

    private fun createXinhaoFolders() {
        mFolders.forEach { folder ->
            if (!folder.exists()) {
                folder.mkdirs()
            }
        }
    }
    private fun isXinhaoFold(): Boolean {
        return mFolders.all { it.exists() }
    }
}
