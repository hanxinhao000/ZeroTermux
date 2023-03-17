package com.termux.zerocore.back

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.back.adapter.RestoreAdapter
import com.termux.zerocore.back.bean.DataBean
import com.termux.zerocore.back.listener.BackupStoreDialogCloseListener
import com.termux.zerocore.back.listener.CreateConversationListener
import com.termux.zerocore.back.listener.RestoreFileDataListener
import com.termux.zerocore.back.listener.RestoreRefreshFileListener
import com.termux.zerocore.data.CommendShellData
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.dialog.YesNoDialog
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.QZUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.whileSelect

class RestoreViewModel {
    private val TAG = "RestoreViewModel"
    private var mRestoreList: RecyclerView? = null
    private var mNoData: TextView? = null
    private var mContext: Context? = null
    private var  mBackupStoreDialogCloseListener: BackupStoreDialogCloseListener? = null
    private var  mCreateConversationListener: CreateConversationListener? = null
    public fun initView(
        mLayout: RelativeLayout?,
        mContext: Context,
        mCreateConversationListener: CreateConversationListener?,
        mBackupStoreDialogCloseListener: BackupStoreDialogCloseListener) {
        mRestoreList = mLayout?.findViewById(R.id.restore_list)
        mNoData = mLayout?.findViewById(R.id.not_data)
        this.mContext = mContext
        this.mCreateConversationListener = mCreateConversationListener
        this.mBackupStoreDialogCloseListener = mBackupStoreDialogCloseListener
        initAdapter()
    }

    private fun initAdapter() {
        val xinHaoDataPathFile = FileIOUtils.getXinHaoDataPathFile()
        val listFiles = xinHaoDataPathFile.listFiles()
        if (listFiles != null && listFiles.isNotEmpty()) {
            isShowData(false)
            var mArrayList: ArrayList<DataBean> = ArrayList()
            listFiles.forEach {
                var mDataBean = DataBean()
                mDataBean.mFile = it
                mArrayList.add(mDataBean)
            }
            val restoreAdapter = RestoreAdapter(mArrayList, mContext!!)
            mRestoreList?.let {
                it.layoutManager = LinearLayoutManager(mContext)
                it.adapter = restoreAdapter
            }
            restoreAdapter.setRestoreRefreshFileListener(object : RestoreRefreshFileListener{
                override fun refresh() {
                    val refreshXinHaoDataPathFile = FileIOUtils.getXinHaoDataPathFile()
                    val refreshListFiles = refreshXinHaoDataPathFile.listFiles()
                    if (refreshListFiles != null && refreshListFiles.isNotEmpty()) {
                        isShowData(false)
                        var mRefreshArrayList: ArrayList<DataBean> = ArrayList()
                        refreshListFiles.forEach {
                            var mDataBean = DataBean()
                            mDataBean.mFile = it
                            mRefreshArrayList.add(mDataBean)
                        }
                        restoreAdapter.setList(mRefreshArrayList)
                        restoreAdapter.notifyDataSetChanged()
                    } else {
                        isShowData(true)
                    }
                }

            })
            restoreAdapter.setRestoreFileDataListener(object: RestoreFileDataListener {
                override fun file(mDataBean: DataBean) {
                    val storagePath = FileIOUtils.isStoragePath(mContext!!)
                    if (!storagePath) {
                        UUtils.showMsg(UUtils.getString(R.string.storage_error))
                        return
                    }
                    if (!FileIOUtils.isPacketFormat(mDataBean.mFile!!.name)) {
                        UUtils.showMsg(UUtils.getString(R.string.unrecognized_compression_format))
                        return
                    }
                    val mSwitchDialog = YesNoDialog(mContext!!)
                    mSwitchDialog.createEditDialog(UUtils.getString(R.string.system_create_container))
                    mSwitchDialog.show()
                    mSwitchDialog.yesTv.setOnClickListener {
                        val text = mSwitchDialog.inputSystemName.text
                        if (TextUtils.isEmpty(text)) {
                            UUtils.showMsg(UUtils.getString(R.string.system_create_container_empty))
                            return@setOnClickListener
                        }
                        mSwitchDialog.dismiss()
                        val createSystem =
                            FileIOUtils.createSystem(UUtils.getContext(), text.toString())
                        if (createSystem == null || !(createSystem.exists())) {
                            LogUtils.d(TAG, "setRestoreFileDataListener -> file createSystem is fail return")
                            return@setOnClickListener
                        }
                        mBackupStoreDialogCloseListener?.backupStoreDismiss()
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
                                delay(1000)
                            }
                            withContext(Dispatchers.Main) {
                                sendTextToTerminal(CommendShellData.SHELL_WELCOME_MESSAGE)
                            }
                            withContext(Dispatchers.IO) {
                                delay(1000)
                            }
                            withContext(Dispatchers.Main) {
                                mLoadingDialog?.dismiss()
                                mDataBean.mFile?.let {
                                    LogUtils.d(TAG, "setRestoreFileDataListener -> file file name is: ${it.name}")
                                    if (it.name.endsWith("tar.gz")) {
                                        sendTextToTerminal(CommendShellData.getShellRestore(CommendShellData.SHELL_TAR_RESTORE_GZ, it, createSystem))
                                    } else if (it.name.endsWith("tar.bz2")) {
                                        sendTextToTerminal(CommendShellData.getShellRestore(CommendShellData.SHELL_TAR_RESTORE_BZ2, it, createSystem))
                                    } else if (it.name.endsWith("tar.xz")) {
                                        sendTextToTerminal(CommendShellData.getShellRestore(CommendShellData.SHELL_TAR_RESTORE_XZ, it, createSystem))
                                    } else {
                                        UUtils.showMsg(UUtils.getString(R.string.unrecognized_compression_format))
                                        LogUtils.d(TAG, "setRestoreFileDataListener -> file unrecognized compression format: ${it.name}")
                                    }
                                }
                            }

                        }
                    }
                }

            })
        } else {
            isShowData(true)
        }
    }

    private fun sendTextToTerminal(text: String) {
        LogUtils.d(TAG, "sendTextToTerminal text to:$text")
        TermuxActivity.mTerminalView.sendTextToTerminal(text)
    }

    private fun isShowData(isShow: Boolean) {
        if (isShow) {
            mNoData?.visibility = View.VISIBLE
            mRestoreList?.visibility = View.INVISIBLE
        } else {
            mNoData?.visibility = View.INVISIBLE
            mRestoreList?.visibility = View.VISIBLE
        }

    }
}
