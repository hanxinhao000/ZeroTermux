package com.termux.zerocore.back.adapter

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.back.listener.BackupClickListener
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.dialog.view_holder.ItemMenuViewHolder
import com.termux.zerocore.zero.engine.ZeroCoreManage

class BackupAdapter : RecyclerView.Adapter<ItemMenuViewHolder>{
    private var mList:ArrayList<ItemMenuBean.Data>? = null
    private var mContext: Context? = null
    private var mBackupClickListener: BackupClickListener? = null
    constructor(mList:ArrayList<ItemMenuBean.Data>?, mContext: Context, mBackupClickListener: BackupClickListener?) : super() {
        this.mList = mList
        this.mContext = mContext
        this.mBackupClickListener = mBackupClickListener
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
        holder.itemView.setOnClickListener {
            mBackupClickListener?.backupClick(it, mList!![position].key)
        }
    }

    override fun getItemCount(): Int {
      return mList!!.size
    }
}
