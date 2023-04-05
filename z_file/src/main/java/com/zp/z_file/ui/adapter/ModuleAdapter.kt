package com.zp.z_file.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zp.z_file.listener.RestoreFileDataListener
import com.zp.z_file.listener.RestoreRefreshFileListener
import com.zp.z_file.R
import com.zp.z_file.bean.DataBean
import com.zp.z_file.ui.dialog.SwitchDialog
import com.zp.z_file.util.LogUtils
import com.zp.z_file.util.ZFileUUtils
import com.zp.z_file.viewholder.RestoreViewHolder
import java.text.DecimalFormat


class ModuleAdapter: RecyclerView.Adapter<RestoreViewHolder> {
    private val TAG = "ModuleAdapter"
    private var mList: ArrayList<DataBean>? = null
    private var mContext: Context? = null
    private var mRestoreRefreshFileListener: RestoreRefreshFileListener? = null
    private var mRestoreFileDataListener: RestoreFileDataListener? = null
    constructor(mList: ArrayList<DataBean>?, mContext: Context) : super() {
        this.mList = mList
        this.mContext = mContext
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder(ZFileUUtils.getViewLayViewGroup(R.layout.item_restore, parent))
    }

    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val mFile = mList!![position].mFile
        holder.mDelete?.text = ZFileUUtils.getString(R.string.delete_module_data)
        holder.mDataName?.text = "${ZFileUUtils.getString(R.string.data_module_name)} ${mFile?.name}"
        holder.mDataSize?.text = "${ZFileUUtils.getString(R.string.data_module_size)} ${formatFileSize(mFile?.length()!!)}"
        holder.mDataPath?.text = "${ZFileUUtils.getString(R.string.data_module_path)} ${mFile?.absolutePath}"
        if (isModuleFormat(mFile!!.name)) {
            holder.mDataName?.setTextColor(ZFileUUtils.getColor(R.color.color_48baf3))
        } else {
            holder.mDataName?.setTextColor(ZFileUUtils.getColor(R.color.color_CC5A6B))
        }
        holder.mDelete?.setOnClickListener {
            val switchDialog = SwitchDialog(mContext!!)
            switchDialog.createSwitchDialog(ZFileUUtils.getString(R.string.file_module_msg))
            switchDialog.ok?.setOnClickListener {
                switchDialog.dismiss()
                if (mList!![position].mFile!!.delete()) {
                    ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.删除成功))
                    mRestoreRefreshFileListener?.refresh()
                } else {
                    ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.file_delete_fail))
                }
            }
            switchDialog.show()
        }
        holder.itemView.setOnClickListener {
            mRestoreFileDataListener?.file(mList!![position])
        }
    }

    override fun getItemCount(): Int {
       return mList!!.size
    }

    public fun setList(mList: ArrayList<DataBean>?) {
        this.mList = mList
    }

    public fun setRestoreRefreshFileListener(mRestoreRefreshFileListener: RestoreRefreshFileListener?) {
        this.mRestoreRefreshFileListener = mRestoreRefreshFileListener
    }

    public fun setRestoreFileDataListener(mRestoreFileDataListener: RestoreFileDataListener) {
        this.mRestoreFileDataListener = mRestoreFileDataListener
    }

    fun formatFileSize(fileS: Long): String? {
        val df = DecimalFormat("#.00")
        var fileSizeString = ""
        val wrongSize = "0B"
        if (fileS == 0L) {
            return wrongSize
        }
        fileSizeString = if (fileS < 1024) {
            df.format(fileS.toDouble()) + "B"
        } else if (fileS < 1048576) {
            df.format(fileS.toDouble() / 1024) + "KB"
        } else if (fileS < 1073741824) {
            df.format(fileS.toDouble() / 1048576) + "MB"
        } else {
            df.format(fileS.toDouble() / 1073741824) + "GB"
        }
        LogUtils.d(TAG, "formatFileSize fileS:$fileS")
        return fileSizeString
    }

    public fun isModuleFormat(name: String): Boolean {
        return name.endsWith("7Z") || name.endsWith("7z")
    }
}
