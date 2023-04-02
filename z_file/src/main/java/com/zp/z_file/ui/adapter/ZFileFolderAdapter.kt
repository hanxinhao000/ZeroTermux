package com.zp.z_file.ui.adapter

import android.content.Context
import android.util.Log
import android.view.View
import com.zp.z_file.R
import com.zp.z_file.common.ZFileAdapter
import com.zp.z_file.common.ZFileViewHolder
import com.zp.z_file.content.*
import org.apache.commons.io.FileUtils
import java.io.File

internal class ZFileFolderAdapter(context: Context) : ZFileAdapter<ZFileBean>(context) {

    override fun getItemViewType(position: Int) = if (getItem(position).isFile) FILE else FOLDER

    override fun getLayoutID(viewType: Int) =
        when (viewType) {
            FILE -> R.layout.item_zfile_list_empty
            else -> R.layout.item_zfile_list_folder
        }

    override fun bindView(holder: ZFileViewHolder, item: ZFileBean, position: Int) {
        if (holder.itemViewType == FOLDER) {
            val hintBean = item getBadgeHintBean context
            holder.apply {
                setText(R.id.item_zfile_list_folderNameTxt, item.fileName)
                setHint(R.id.item_zfile_list_folderHintTxt, hintBean)
                setBadge(R.id.item_zfile_list_folderHintPic, hintBean)
                setImageRes(R.id.item_zfile_list_folderPic, folderRes)
                setImageRes(R.id.item_zfile_list_symlink, R.drawable.zfile_symlink)

               // setBgColor(R.id.item_zfile_list_folder_line, lineColor)
               // setVisibility(R.id.item_zfile_list_folder_line, position < itemCount - 1)
            }
            Log.e("bindView", "bindView: ${item.filePath}" )
            if(FileUtils.isSymlink(File(item.filePath))) {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.VISIBLE)
            } else {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.GONE)
            }
        }
    }

    override fun setDatas(list: MutableList<ZFileBean>?) {
        list?.add(ZFileBean(isFile = true))
        super.setDatas(list)
    }
}
