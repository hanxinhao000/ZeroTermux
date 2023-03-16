package com.termux.zerocore.dialog.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.back.bean.DataBean
import com.termux.zerocore.back.listener.RestoreFileDataListener
import com.termux.zerocore.back.listener.RestoreRefreshFileListener
import com.termux.zerocore.back.viewholder.RestoreViewHolder
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.dialog.view_holder.ItemMenuViewHolder
import com.termux.zerocore.utils.FileIOUtils

class ModuleAdapter: RecyclerView.Adapter<RestoreViewHolder> {
    private var mList: ArrayList<DataBean>? = null
    private var mContext: Context? = null
    private var mRestoreRefreshFileListener: RestoreRefreshFileListener? = null
    private var mRestoreFileDataListener: RestoreFileDataListener? = null
    constructor(mList: ArrayList<DataBean>?, mContext: Context) : super() {
        this.mList = mList
        this.mContext = mContext
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder(UUtils.getViewLayViewGroup(R.layout.item_restore, parent))
    }

    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val mFile = mList!![position].mFile
        holder.mDelete?.text = UUtils.getString(R.string.delete_module_data)
        holder.mDataName?.text = "${UUtils.getString(R.string.data_module_name)} ${mFile?.name}"
        holder.mDataSize?.text = "${UUtils.getString(R.string.data_module_size)} ${FileIOUtils.formatFileSize(mFile?.length()!!)}"
        holder.mDataPath?.text = "${UUtils.getString(R.string.data_module_path)} ${mFile?.absolutePath}"
        if (FileIOUtils.isModuleFormat(mFile!!.name)) {
            holder.mDataName?.setTextColor(UUtils.getColor(R.color.color_48baf3))
        } else {
            holder.mDataName?.setTextColor(UUtils.getColor(R.color.color_CC5A6B))
        }
        holder.mDelete?.setOnClickListener {
            val switchDialog = SwitchDialog(mContext!!)
            switchDialog.createSwitchDialog(UUtils.getString(R.string.file_module_msg))
            switchDialog.ok?.setOnClickListener {
                switchDialog.dismiss()
                if (mList!![position].mFile!!.delete()) {
                    UUtils.showMsg(UUtils.getString(R.string.删除成功))
                    mRestoreRefreshFileListener?.refresh()
                } else {
                    UUtils.showMsg(UUtils.getString(R.string.file_delete_fail))
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
}
