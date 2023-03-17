package com.termux.zerocore.back

import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.app.TermuxInstaller
import com.termux.zerocore.back.adapter.BackupAdapter
import com.termux.zerocore.back.constant.BackRestoreConstant
import com.termux.zerocore.back.listener.BackupClickListener
import com.termux.zerocore.back.listener.BackupStoreDialogCloseListener
import com.termux.zerocore.back.listener.CreateConversationListener
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.data.CommendShellData
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.WindowsUtils
import kotlinx.coroutines.*

class BackViewModel : BackupClickListener {
    private val TAG = "BackViewModel"
    private var mBackupList: RecyclerView? = null
    private var mContext: Context? = null
    private var  mBackupStoreDialogCloseListener: BackupStoreDialogCloseListener? = null
    private var  mCreateConversationListener: CreateConversationListener? = null
    public fun initView(
        mLayout: RelativeLayout?,
        mContext: Context,
        mCreateConversationListener: CreateConversationListener?,
        mBackupStoreDialogCloseListener: BackupStoreDialogCloseListener) {

        mBackupList = mLayout?.findViewById(R.id.back_list)
        this.mContext = mContext
        this.mCreateConversationListener = mCreateConversationListener
        this.mBackupStoreDialogCloseListener = mBackupStoreDialogCloseListener
        initManage()
        initAdapter()
    }
    private fun initAdapter() {
        var mList:ArrayList<ItemMenuBean.Data> = ArrayList()

        /**
         * tar.gz
         */
        var mTagGzData: ItemMenuBean.Data = ItemMenuBean.Data()
        mTagGzData.title = "tar.gz"
        mTagGzData.id = R.mipmap.back_item_ico
        mTagGzData.isEg = false
        mTagGzData.key = BackRestoreConstant.TAG_GZ
        mList.add(mTagGzData)

        /**
         * tar.bz2
         */
        var mTagBz2Data: ItemMenuBean.Data = ItemMenuBean.Data()
        mTagBz2Data.title = "tar.bz2"
        mTagBz2Data.id = R.mipmap.back_item_ico
        mTagBz2Data.isEg = false
        mTagBz2Data.key = BackRestoreConstant.TAR_BZ2
        mList.add(mTagBz2Data)

        /**
         * tar.xz
         */
        var mTarXzData: ItemMenuBean.Data = ItemMenuBean.Data()
        mTarXzData.title = "tar.xz"
        mTarXzData.id = R.mipmap.back_item_ico
        mTarXzData.isEg = false
        mTarXzData.key = BackRestoreConstant.TAR_XZ
        mList.add(mTarXzData)

        /**
         * tar.Z
         */
     /*   var mTarZData: ItemMenuBean.Data = ItemMenuBean.Data()
        mTarZData.title = "tar.Z"
        mTarZData.id = R.mipmap.back_item_ico
        mTarZData.isEg = false
        mTarZData.key = BackRestoreConstant.TAR_Z
        mList.add(mTarZData)*/


        val backupAdapter = BackupAdapter(mList, mContext!!, this)
        mBackupList?.layoutManager = GridLayoutManager(
            UUtils.getContext(),
            WindowsUtils.getGridNumber()
        )
        mBackupList?.adapter = backupAdapter
    }

    private fun initManage() {
        if (!FileIOUtils.isStoragePath(mContext!!)) {
            writeLink()
        }
    }

    override fun backupClick(mView: View, tag: Int) {
        if (!FileIOUtils.isStoragePath(mContext!!)) {
            UUtils.showMsg(UUtils.getString(R.string.storage_error))
            return
        }
        when (tag) {
            BackRestoreConstant.TAG_GZ ->{
                startBackUp(CommendShellData.SHELL_TAR_GZ, ".tar.gz")
            }
            BackRestoreConstant.TAR_BZ2 ->{
                startBackUp(CommendShellData.SHELL_TAR_BZ2, ".tar.bz2")
            }
            BackRestoreConstant.TAR_XZ ->{
                startBackUp(CommendShellData.SHELL_TAR_XZ, ".tar.xz")
            }
            BackRestoreConstant.TAR_Z ->{
                startBackUp(CommendShellData.SHELL_TAR_Z, ".tar.Z")
            }
        }
    }

    private fun startBackUp(command: String, name: String) {
        mBackupStoreDialogCloseListener?.backupStoreDismiss()
        val switchDialog = SwitchDialog(mContext!!)
        switchDialog.createSwitchDialog(UUtils.getString(R.string.backup_msg_dialog))
        switchDialog.ok?.setOnClickListener {
            switchDialog.dismiss()
            var mLoadingDialog: LoadingDialog? = null
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    mLoadingDialog = LoadingDialog(mContext!!)
                    mLoadingDialog?.msg?.text = UUtils.getString(R.string.正在载入中)
                    mLoadingDialog?.show()
                    mLoadingDialog?.setCancelable(false)
                   // mCreateConversationListener?.create()
                }
                withContext(Dispatchers.IO) {
                    delay(2000)
                }
                withContext(Dispatchers.Main) {
                    mLoadingDialog?.dismiss()
                    val replace = CommendShellData.SHELL_BACKUP.replace(
                        "systemName",
                        FileIOUtils.getTimeFileName(name)
                    ).replace("TemporaryMark", command)
                    sendTextToTerminal(replace)
                    UUtils.showMsg(UUtils.getString(R.string.开始备份))
                   // mBackupStoreDialogCloseListener?.backupStoreDismiss()
                }

            }
        }
        switchDialog.show()
    }

    private fun sendTextToTerminal(text: String) {
        LogUtils.d(TAG, "sendTextToTerminal text to:$text")
        TermuxActivity.mTerminalView.sendTextToTerminal(text)

    }

    private fun writeLink() {
        XXPermissions.with(mContext)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .request(object: OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    TermuxInstaller.setupStorageSymlinks(mContext)
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    UUtils.showMsg(UUtils.getString(R.string.没有SD卡权限))
                }

            });
    }
}
