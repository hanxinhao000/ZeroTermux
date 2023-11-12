package com.termux.zerocore.dialog.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.example.xh_lib.utils.UUtils.FileCallback
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.activity.ImageActivity
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.code.CodeString
import com.termux.zerocore.data.CommendShellData
import com.termux.zerocore.data.UsbFileData
import com.termux.zerocore.dialog.*
import com.termux.zerocore.dialog.CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_DATA_MSG
import com.termux.zerocore.dialog.view_holder.ItemMenuViewHolder
import com.termux.zerocore.keybord.KeyBordManage
import com.termux.zerocore.scrcpy.MainActivity
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.url.FileUrl.zeroTermuxApk
import com.termux.zerocore.url.FileUrl.zeroTermuxCommand
import com.termux.zerocore.url.FileUrl.zeroTermuxData
import com.termux.zerocore.url.FileUrl.zeroTermuxFont
import com.termux.zerocore.url.FileUrl.zeroTermuxHome
import com.termux.zerocore.url.FileUrl.zeroTermuxIso
import com.termux.zerocore.url.FileUrl.zeroTermuxMysql
import com.termux.zerocore.url.FileUrl.zeroTermuxOnlineSystem
import com.termux.zerocore.url.FileUrl.zeroTermuxQemu
import com.termux.zerocore.url.FileUrl.zeroTermuxServer
import com.termux.zerocore.url.FileUrl.zeroTermuxShare
import com.termux.zerocore.url.FileUrl.zeroTermuxSystem
import com.termux.zerocore.url.FileUrl.zeroTermuxWebConfig
import com.termux.zerocore.url.FileUrl.zeroTermuxWindows
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.PackageMsg
import com.termux.zerocore.zero.engine.ZeroCoreManage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ItemMenuAdapter :RecyclerView.Adapter<ItemMenuViewHolder> {
    private val TAG:String = "ItemMenuAdapter"
    private var mList:ArrayList<ItemMenuBean.Data>? = null
    private var mContext: Context? = null
    private var mCommonCommandsDialog:CommonCommandsDialog? = null
    private var mCommonDialogListener: CommonDialogListener? = null
    private var mVShellDialogListener: VShellDialogListener? = null
    private var mClearStyleListener: ClearStyleListener? = null
    private var mCommonCommandsDialogDismissListener: CommonCommandsDialogDismissListener? = null
    private var mKeyViewListener: KeyViewListener? = null
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val obj = msg.obj
            if (obj != null && obj is CardView) {
                val cardView = obj as CardView
                cardView.setCardBackgroundColor(cardView.context.getColor(R.color.color_55000000))
            }
        }
    }

    constructor(mList:ArrayList<ItemMenuBean.Data>?, mContext: Context, mCommonCommandsDialog:CommonCommandsDialog) : super() {
        this.mList = mList
        this.mContext = mContext
        this.mCommonCommandsDialog = mCommonCommandsDialog
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMenuViewHolder {
        return ItemMenuViewHolder(UUtils.getViewLayViewGroup(R.layout.dialog_item_menu, parent))
    }

    override fun onBindViewHolder(holder: ItemMenuViewHolder, position: Int) {
        holder.menu_image?.setImageResource(mList!![position].id)
        holder.menu_name?.text = mList!![position].title
        if (TextUtils.isEmpty(ZeroCoreManage.getVersionName()) && mList!![position].isEg) {
            holder.eg_install_tv?.visibility = View.VISIBLE
        } else {
            holder.eg_install_tv?.visibility = View.INVISIBLE
        }
        if (mList!![position].isBackAnim) {
            val message = Message()
            message.obj = holder.card_view
            holder.card_view?.setCardBackgroundColor(holder.itemView.context.getColor(R.color.color_8850b397))
            mHandler.sendMessageDelayed(message, 1000)
        }
        holder.itemView.setOnClickListener {
            LogUtils.d(TAG, "onBindViewHolder itemView click key is:${mList!![position].key}")
            clickItem(mList!![position].key, holder.itemView)
        }
    }

    override fun getItemCount(): Int {
       return mList!!.size
    }

    public fun setContext(mContext: Context) {
        this.mContext = mContext
    }

    private fun clickItem(id: Int, itemView: View) {
        when (id) {
            CommonCommandsDialog.CommonCommandsDialogConstant.VIDEO_KEY -> {
                UsbFileData.get().setImageFileCheckListener(object :UsbFileData.ImageFileCheckListener{
                    override fun imageFile(file: File) {
                        LogUtils.d(TAG, "imageFile file path is:${file.absolutePath}")
                        LogUtils.d(TAG, "imageFile mCommonDialogListener is:${mCommonDialogListener}")
                        val fileImg = File("${FileUrl.mainConfigImg}/back.jpg")
                        if(fileImg.exists()){
                            fileImg.delete()
                        }
                        FileIOUtils.setPathVideo(file)
                        mCommonDialogListener?.video(file)
                    }
                })
                val intent = Intent(mContext as Activity, ImageActivity::class.java)
                intent.action = ImageActivity.ImageActivityFlgh.VIDEO_FLGH
                mContext?.startActivity(intent)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.KEYBOARD_KEY -> {
                keyBord()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.X86_ALPINE_KEY -> {
                runQemuOs(mContext)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.CLEAR_STYLE -> {
                FileIOUtils.clearStyle()
                mClearStyleListener?.clear()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.WEB_LINUX -> {
                var replace = ""
                if (FileIOUtils.isBinFileExists("ttyd")) {
                    replace = UUtils.getString(R.string.ttyd_install_complete)
                        .replace("0.0.0.0", UUtils.getHostIP())
                    TermuxActivity.mTerminalView.sendTextToTerminal(CommendShellData.SHELL_DATA_RUN_WEB)
                } else {
                    replace = UUtils.getString(R.string.ttyd_install_msg)
                    TermuxActivity.mTerminalView.sendTextToTerminal(CommendShellData.SHELL_DATA_WEB_LINUX)
                }
                val switchDialog = SwitchDialog(mContext as Activity)
                switchDialog.createSwitchDialog(replace)
                switchDialog.ok?.setOnClickListener {
                    switchDialog.dismiss()
                    mCommonCommandsDialogDismissListener?.dismiss()
                }
                switchDialog.cancel?.setOnClickListener {
                    switchDialog.dismiss()
                    mCommonCommandsDialogDismissListener?.dismiss()
                }
                switchDialog.show()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_FILE_BROWSER -> {
                installFileBrowser()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_FTP -> {
                startFTP(itemView)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_SOFT_LINKS -> {
                commonShortcuts()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_MY_SOFT_LINKS -> {
                val yesNoDialog = YesNoDialog(mContext!!)
                yesNoDialog.createEditDialog(UUtils.getString(R.string.my_commonly_used_soft_links_path))
                yesNoDialog.inputSystemName.setHint("/xinhao/data/")
                yesNoDialog.yesTv.setOnClickListener {
                    val toString = yesNoDialog.inputSystemName.text.toString()
                    val file1 = File(FileIOUtils.getXinhaoLinkPath(UUtils.getContext()))
                    if (!file1.exists()) {
                        file1.mkdirs()
                    }
                    yesNoDialog.dismiss()
                    if (TextUtils.isEmpty(toString)) {
                        FileIOUtils.setupFileSymlinks(
                            File(FileIOUtils.getSdcardPath(), "/xinhao/data").absolutePath,
                            "${FileIOUtils.getXinhaoLinkPath(UUtils.getContext())}/xinhao_data")
                        UUtils.showMsg(UUtils.getString(R.string.成功))
                    } else {
                        val file =
                            File(FileIOUtils.getSdcardPath(), toString)
                        if (!file.exists()) {
                            UUtils.showMsg(UUtils.getString(R.string.my_commonly_used_soft_links_repeat))
                            LogUtils.d(TAG, "clickItem path is not exists")
                            return@setOnClickListener
                        }
                        FileIOUtils.setupFileSymlinks(
                            File(FileIOUtils.getSdcardPath(), toString).absolutePath,
                            "${FileIOUtils.getXinhaoLinkPath(UUtils.getContext())}/${toString.replace("/", "_").replace("\\", "_")}")
                        UUtils.showMsg(UUtils.getString(R.string.成功))
                    }
                }
                yesNoDialog.noTv.setOnClickListener {
                    yesNoDialog.dismiss()
                }
                yesNoDialog.show()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_DATA_MSG -> {
                createDataMessage()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_UNINSTALL -> {
                unInstallAll()
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_INSTALL_MODULE -> {
                val installModuleDialog = com.zp.z_file.ui.dialog.InstallModuleDialog(mContext as Activity)
                installModuleDialog.show()
                installModuleDialog.setCancelable(false)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_DEF_BASH -> {
                val switchDialog = SwitchDialog(mContext as Activity)
                switchDialog.createSwitchDialog(UUtils.getString(R.string.install_def_bash_is_writer))
                switchDialog.ok?.setOnClickListener {
                    val file = File(FileUrl.smsBashrcFile)
                    val open = UUtils.getContext().assets.open("bash.bashrc")
                    UUtils.writerFileRawInput(file, open, object : FileCallback {
                        override fun callBack(msg: String?, state: Boolean) {
                            if (state) {
                                UUtils.showMsg(UUtils.getString(R.string.install_def_bash_msg_ok))
                            } else {
                                UUtils.showMsg(UUtils.getString(R.string.install_def_bash_msg_error))
                            }
                            switchDialog.dismiss()
                        }

                    })
                }
                switchDialog.show()

            }

            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_BASH_CHANGE -> {
                val intent = Intent(mContext, EditTextActivity::class.java)
                intent.putExtra("edit_path", FileUrl.smsBashrcFile)
                mContext?.startActivity(intent)
            }

            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_START_MSG -> {
                val intent = Intent(mContext, EditTextActivity::class.java)
                intent.putExtra("edit_path", FileUrl.smsMotdFile)
                mContext?.startActivity(intent)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_DOCKER_CHECK -> {
                mCommonCommandsDialogDismissListener?.dismiss()
                UUtils.writerFile("runcommand/check-config.sh", File(FileUrl.mainHomeUrl, "/check-config.sh"))
                TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.runDocker)
            }
            CommonCommandsDialog.CommonCommandsDialogConstant.ITEM_CLICK_REMOTE_CONNECTION -> {
                val intent = Intent(mContext, MainActivity::class.java)
                mContext?.startActivity(intent)
            }
        }
    }

    public fun setCommonDialogListener(mCommonDialogListener: CommonDialogListener?) {
        this.mCommonDialogListener = mCommonDialogListener
    }

    public fun setVShellDialogListener(mVShellDialogListener: VShellDialogListener?) {
        this.mVShellDialogListener = mVShellDialogListener
    }
    public fun setClearStyleListener(mClearStyleListener: ClearStyleListener?) {
        this.mClearStyleListener = mClearStyleListener
    }

    public fun setKeyViewListener(mKeyViewListener: KeyViewListener?) {
        this.mKeyViewListener = mKeyViewListener
    }

    public fun setCommonCommandsDialogDismissListener(mCommonCommandsDialogDismissListener: CommonCommandsDialogDismissListener?) {
        this.mCommonCommandsDialogDismissListener = mCommonCommandsDialogDismissListener
    }

    public interface CommonDialogListener {
        fun video(file: File)
    }

    public interface VShellDialogListener {
        fun vShell(environment: ArrayList<String>?, processArgs: ArrayList<String>?)
        fun showDialog(boolean: Boolean)
    }

    public interface KeyViewListener {
        fun view(mView: View?)
    }

    public interface ClearStyleListener {
        fun clear()
    }

    public interface CommonCommandsDialogDismissListener {
        fun dismiss()
    }

    private fun installFileBrowser() {
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            return
        }

        val mHandler = object :Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                LogUtils.d(TAG, "handleMessage handler what: ${msg.what}")
                if (msg.what == ZeroCoreManage.INSTALL_COMPLETE) {
                    val smsBashrcFile = File(FileUrl.smsBashrcFile)
                    var fileString = UUtils.getFileString(smsBashrcFile)
                    if(!fileString.contains("filebrowser")){
                        fileString += "\n cd ~ > /dev/null && ./.filebrowser/filebrowser -a 0.0.0.0 -p 19951 -r "+FileUrl.mainFilesUrl+" & > /dev/null"
                        fileString += "\n echo '" + UUtils.getString(R.string.filebrowser已运行) + "'"
                        UUtils.setFileString(smsBashrcFile,fileString)
                    }
                    if (msg?.obj != null) {
                        TermuxActivity.mTerminalView.sendTextToTerminal(msg!!.obj as String?)
                    }
                }
            }
        }
        GlobalScope.launch {
            installFileBrowserIo(mHandler)
        }
    }

    private suspend fun installFileBrowserIo(mHandler: Handler) {
        withContext(Dispatchers.IO) {
            ZeroCoreManage.installFileBrowser(mHandler)
        }
        withContext(Dispatchers.Main) {
            mCommonCommandsDialogDismissListener?.dismiss()
        }
    }

    private fun startFTP(itemView: View) {
        val popupFtpWindows = FtpWindowsDialog(mContext!!)
        popupFtpWindows.show()
    }

    private fun runQemuOs(mContext: Context?) {
        if (mContext == null) {
            LogUtils.d(TAG, "runQemuOs mContext is null return")
            return
        }
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            return
        }
        if (FileIOUtils.isProotQemu()) {
            val switchDialog = SwitchDialog(mContext as Activity)
            switchDialog.createSwitchDialog(UUtils.getString(R.string.install_environment))
            switchDialog.ok?.text = UUtils.getString(R.string.确定)
            switchDialog.cancel?.text = UUtils.getString(R.string.取消)
            switchDialog.ok?.setOnClickListener {
                switchDialog.dismiss()
                mCommonCommandsDialog?.dismiss()
                TermuxActivity.mTerminalView.sendTextToTerminal("pkg update -y && pkg in wget proot -y && pkg install x11-repo unstable-repo -y && pkg install qemu-utils qemu-system-x86_64-headless  qemu-system-i386-headless -y &&  termux-setup-storage\n")
                Toast.makeText(
                    UUtils.getContext(),
                    UUtils.getString(R.string.请等待安装完成在进入),
                    Toast.LENGTH_SHORT
                ).show()
            }
            switchDialog.cancel?.setOnClickListener {
                switchDialog.dismiss()
            }
            switchDialog.setCancelable(false)
            switchDialog.show()
            return
        }


        val handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    ZeroCoreManage.INSTALLING -> {
                        LogUtils.d(TAG, "INSTALLING System os install....")
                        mVShellDialogListener?.showDialog(true)
                    }
                    ZeroCoreManage.INSTALL_COMPLETE -> {
                        LogUtils.d(TAG, "INSTALL_COMPLETE System os install complete.")
                        mVShellDialogListener?.showDialog(false)
                        mVShellDialogListener?.vShell(ZeroCoreManage.getEnvironment(), ZeroCoreManage.getProcessArgs())
                    }
                }
            }
        }
        ZeroCoreManage.install(handler)
    }

    private fun keyBord() {
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            return
        }
        val handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    KeyBordManage.KEY_DEF -> {
                        LogUtils.d(TAG, "handleMessage DEF:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminal(msg.obj as String?)
                        }
                    }
                    KeyBordManage.KEY_ALT -> {
                        LogUtils.d(TAG, "handleMessage ALT:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminalAlt(msg.obj as String?, true)
                        }
                    }
                    KeyBordManage.KEY_CTRL -> {
                        LogUtils.d(TAG, "handleMessage CTRL:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminalCtrl(msg.obj as String?, true)
                        }
                    }
                    KeyBordManage.KEY_OTHER -> {
                        LogUtils.d(TAG, "handleMessage OTHER:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTermuxTerminalExtraKeys.onTerminalExtraKeyButtonClick(null, msg.obj as String?, false ,false ,false , false)
                        }
                    }
                }


            }
        }
        KeyBordManage.getInstance().initKeyBord(handler)
        mKeyViewListener?.view(KeyBordManage.getInstance().keyBordView)
    }

    private fun commonShortcuts() {

        val switchDialog = SwitchDialog(mContext!!)
        switchDialog.createSwitchDialog(UUtils.getString(R.string.create_soft_links))
        switchDialog.ok?.setOnClickListener {
            switchDialog.dismiss()
            XXPermissions.with(mContext)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        val xinhaoLinkPath = FileIOUtils.getXinhaoLinkPath(UUtils.getContext())
                        val file = File(xinhaoLinkPath)
                        if (!file.exists()) {
                            if (!file.mkdirs()) {
                                UUtils.showMsg(UUtils.getString(R.string.create_soft_links_fail))
                                LogUtils.d(TAG, "commonShortcuts file mkdirs fail return")
                                return
                            }
                        }

                        val downLoadPath = FileIOUtils.getDownLoadPath()
                        val sdcardPath = FileIOUtils.getSdcardPath()
                        val qqDownloadPath = FileIOUtils.getQQAndroidDownloadPath()
                        val weiXinPath = FileIOUtils.getWeiXinPath()
                        val weiXinAndroidPath = FileIOUtils.getWeiXinAndroidPath()
                        FileIOUtils.setupFileSymlinks(downLoadPath, "$xinhaoLinkPath/download")
                        FileIOUtils.setupFileSymlinks(sdcardPath, "$xinhaoLinkPath/sdcard")
                        FileIOUtils.setupFileSymlinks(qqDownloadPath, "$xinhaoLinkPath/QQDownload")
                        FileIOUtils.setupFileSymlinks(weiXinPath, "$xinhaoLinkPath/WXDownload")
                        FileIOUtils.setupFileSymlinks(weiXinAndroidPath, "$xinhaoLinkPath/WXAndroidDownload")
                        UUtils.showMsg(UUtils.getString(R.string.成功))
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {

                    }
                })
        }
        switchDialog.show()
    }

    private fun unInstallAll() {
        PackageMsg.unInstallALLApk(mContext as Activity)
    }

    private fun createDataMessage() {
        val mingLShowDialog = MingLShowDialog(mContext!!)
        mingLShowDialog.mTitleCard.visibility = View.GONE
        mingLShowDialog.mSwitchCard.visibility = View.GONE
        mingLShowDialog.edit_text.hint = UUtils.getString(R.string.data_message_hint)
        val dataMessageFileString = FileIOUtils.getDataMessageFileString()
        if (!(dataMessageFileString.isNullOrEmpty())) {
            mingLShowDialog.edit_text.setText(dataMessageFileString)
        }
        mingLShowDialog.start.setOnClickListener {
            if (mingLShowDialog.edit_text.text.isNullOrEmpty()) {
                UUtils.showMsg(UUtils.getString(R.string.data_message_empty))
            } else {
                mingLShowDialog.dismiss()
                //在手动关闭和意外关闭都会自动保存，所以此显示信息只是增加保存按钮的一个反馈信息
                UUtils.showMsg(UUtils.getString(R.string.保存成功))
            }
        }
        mingLShowDialog.setOnDismissListener {
            val text = mingLShowDialog.edit_text.text
            //设置自动保存，关闭Dialog之后也会自动保存
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    if (!text.isNullOrEmpty()) {
                        FileIOUtils.saveDataMessageFileString(text.toString())
                    } else {
                        LogUtils.d(TAG, "data save is empty!")
                    }
                }
            }
        }
        mingLShowDialog.show()
    }
}
