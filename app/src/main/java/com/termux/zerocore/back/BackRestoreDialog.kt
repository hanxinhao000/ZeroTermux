package com.termux.zerocore.back

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.app.TermuxInstaller
import com.termux.zerocore.back.listener.BackupStoreDialogCloseListener
import com.termux.zerocore.back.listener.CreateConversationListener
import com.termux.zerocore.dialog.SwitchDialog

class BackRestoreDialog : BaseDialogDown, BackupStoreDialogCloseListener {
    private val BACKUP_SELECT = 50001
    private val RESTORE_SELECT = 50002
    private val TAG = "BackDialog"
    private var mBackup: LinearLayout? = null
    private var mRestore: LinearLayout? = null
    private var mBackLayout: RelativeLayout? = null
    private var mRestoreLayout: RelativeLayout? = null
    private var mRestoreViewModel: RestoreViewModel? = null
    private var mBackViewModel: BackViewModel? = null
    private var mCreateConversationListener: CreateConversationListener? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        mBackup = mView.findViewById(R.id.backup_ll)
        mRestore = mView.findViewById(R.id.restore_ll)
        mBackLayout = mView.findViewById(R.id.back_rl)
        mRestoreLayout = mView.findViewById(R.id.restore_rl)
    }

    public fun setCreateConversationListener(mCreateConversationListener: CreateConversationListener) {
        this.mCreateConversationListener = mCreateConversationListener
    }

    public fun initData() {
        val arrayList = ArrayList<String>()
        arrayList.add(Permission.WRITE_EXTERNAL_STORAGE)
        arrayList.add(Permission.READ_EXTERNAL_STORAGE)
        val granted = XXPermissions.isGranted(mContext, arrayList)
        if (!granted) {
            LogUtils.d(TAG, "not sd Permissions return.")
            return
        }
        mRestoreViewModel = RestoreViewModel()
        mBackViewModel = BackViewModel()
        mBackViewModel?.initView(mBackLayout, mContext, mCreateConversationListener, this)
        mRestoreViewModel?.initView(mRestoreLayout, mContext, mCreateConversationListener, this)
        clickInit()
    }

    override fun getContentView(): Int {
        return R.layout.dialog_back
    }

    private fun clickInit() {
        mBackup?.setOnClickListener {
            LogUtils.d(TAG, "backup is click")
            selectIndex(BACKUP_SELECT)
        }
        mRestore?.setOnClickListener {
            LogUtils.d(TAG, "restore is click")
            selectIndex(RESTORE_SELECT)
        }
    }

    public fun createStoragePath() {
        val arrayList = ArrayList<String>()
        arrayList.add(Permission.WRITE_EXTERNAL_STORAGE)
        arrayList.add(Permission.READ_EXTERNAL_STORAGE)
        val granted = XXPermissions.isGranted(mContext, arrayList)
        if (!granted) {
            this@BackRestoreDialog.dismiss()
            val switchDialog = SwitchDialog(mContext)
            switchDialog.createSwitchDialog(UUtils.getString(R.string.file_sd_msg))
            switchDialog.show()
            switchDialog.ok?.setOnClickListener {
                XXPermissions.with(mContext)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .request(object: OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                            switchDialog.dismiss()
                            TermuxInstaller.setupStorageSymlinks(mContext)
                            UUtils.showMsg(UUtils.getString(R.string.file_sd_success))
                        }

                        override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                            switchDialog.dismiss()
                            UUtils.showMsg(UUtils.getString(R.string.你没有SD卡权限))
                        }

                    });
            }
            switchDialog.cancel?.setOnClickListener {
                UUtils.showMsg(UUtils.getString(R.string.file_sd_fail))
                switchDialog.dismiss()
            }
        }
    }

    private fun selectIndex(index: Int) {
        mBackup?.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mRestore?.setBackgroundResource(R.drawable.shape_line_2e84e6)
        when (index) {
            BACKUP_SELECT ->{
                mBackup?.setBackgroundResource(R.drawable.shape_line_8cff5a)
                mBackLayout?.visibility = View.VISIBLE
                mRestoreLayout?.visibility = View.INVISIBLE
            }
            RESTORE_SELECT->{
                mRestore?.setBackgroundResource(R.drawable.shape_line_8cff5a)
                mBackLayout?.visibility = View.INVISIBLE
                mRestoreLayout?.visibility = View.VISIBLE
            }
        }
    }

    override fun backupStoreDismiss() {
        dismiss()
    }
}
