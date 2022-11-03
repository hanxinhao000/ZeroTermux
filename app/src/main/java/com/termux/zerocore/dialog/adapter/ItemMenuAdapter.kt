package com.termux.zerocore.dialog.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.ImageActivity
import com.termux.zerocore.bean.Data
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.data.UsbFileData
import com.termux.zerocore.dialog.CommonCommandsDialog
import com.termux.zerocore.dialog.view_holder.ItemMenuViewHolder
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileIOUtils
import java.io.File

class ItemMenuAdapter :RecyclerView.Adapter<ItemMenuViewHolder> {
    private val TAG:String = "ItemMenuAdapter"
    private var mList:ArrayList<ItemMenuBean.Data>? = null
    private var mContext: Context? = null
    private var mCommonDialogListener: CommonDialogListener? = null
    constructor(mList:ArrayList<ItemMenuBean.Data>?, mContext: Context) : super() {
        this.mList = mList
        this.mContext = mContext
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMenuViewHolder {
        return ItemMenuViewHolder(UUtils.getViewLayViewGroup(R.layout.dialog_item_menu, parent))
    }

    override fun onBindViewHolder(holder: ItemMenuViewHolder, position: Int) {
        holder.menu_image?.setImageResource(mList!![position].id)
        holder.menu_name?.text = mList!![position].title
        holder.itemView.setOnClickListener {
            LogUtils.d(TAG, "onBindViewHolder itemView click key is:${mList!![position].key}")
            clickItem(mList!![position].key)
        }
    }

    override fun getItemCount(): Int {
       return mList!!.size
    }

    public fun setContext(mContext: Context) {
        this.mContext = mContext
    }

    private fun clickItem(id: Int) {
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

            }
        }
    }

    public fun setCommonDialogListener(mCommonDialogListener: CommonDialogListener?) {
        this.mCommonDialogListener = mCommonDialogListener
    }

    public interface CommonDialogListener {
        fun video(file: File)
    }
}
