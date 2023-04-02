package com.zp.z_file.ui.adapter

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.collection.ArrayMap
import com.zp.z_file.R
import com.zp.z_file.common.ZFileAdapter
import com.zp.z_file.common.ZFileViewHolder
import com.zp.z_file.content.*
import org.apache.commons.io.FileUtils
import java.io.File

internal class ZFileListAdapter(context: Context) : ZFileAdapter<ZFileBean>(context) {

    internal constructor(context: Context, isQW: Boolean) : this(context) {
        this.isQW = isQW
    }

    init {

        addChildClickViewIds(
            R.id.item_zfile_list_file_box1,
            R.id.item_zfile_list_file_box2,
            R.id.item_zfile_list_file_boxLayout
        )

        itemChildClick = { v, position, item ->
            when (v.id) {
                R.id.item_zfile_list_file_box1 -> {
                    boxClick(position, item)
                }
                R.id.item_zfile_list_file_box2 -> {
                    if (v is TextView) {
                        v.isSelected = !(boxMap[position] ?: false)
                    }
                    boxClick(position, item)
                }
                R.id.item_zfile_list_file_boxLayout -> {
                    boxLayoutClick(position, item)
                }
            }
        }
    }

    private var isQW = false

    private var config = getZFileConfig()

    // box选中的数据
    private val boxMap by lazy {
        ArrayMap<Int, Boolean>()
    }

    // 当前文件夹选择的数量
    private val countMap by lazy {
        ArrayMap<String, Int>()
    }

    // 已选中的数据
    var selectData = arrayListOf<ZFileBean>()

    var isManage = false
        set(value) {
            if (isQW) {
                if (value) {
                    notifyDataSetChanged()
                } else {
                    clearSelectMap()
                    clearCountMap()
                }
            } else {
                if (!value) {
                    clearSelectMap()
                    clearCountMap()
                }
            }
            field = value
        }

    /* selected or un-selected change */
    var changeListener: ((isManage: Boolean, size: Int) -> Unit)? = null

    /* qq or wechat selected or un-selected */
    var qwChangeListener: ((isManage: Boolean, item: ZFileBean, isSelect: Boolean) -> Unit)? = null

    override fun setDatas(list: MutableList<ZFileBean>?) {
        if (list.isNullOrEmpty()) {
            clear()
        } else {
            boxMap.clear()
            list.indices.forEach {
                // 判断当前List数据是否存在已经选中过的值
                if (selectData.isNullOrEmpty()) {
                    boxMap[it] = false
                } else {
                    boxMap[it] = selectData.contains(list[it])
                }
            }
            super.setDatas(list)
        }
    }

    override fun getItemViewType(position: Int) = if (getItem(position).isFile) FILE else FOLDER

    override fun getLayoutID(viewType: Int) =
        when (viewType) {
            FILE -> R.layout.item_zfile_list_file
            else -> R.layout.item_zfile_list_folder
        }

    override fun bindView(holder: ZFileViewHolder, item: ZFileBean, position: Int) {
        if (item.isFile) {
            setFileData(holder, item, position)
        } else {
            setFolderData(holder, item, position)
        }
    }

    private fun setFileData(holder: ZFileViewHolder, item: ZFileBean, position: Int) {
        holder.apply {
            setImage(R.id.item_zfile_list_file_pic, item.filePath)
            setText(R.id.item_zfile_list_file_nameTxt, item.fileName)
            setText(R.id.item_zfile_list_file_dateTxt, item.date)
            setText(R.id.item_zfile_list_file_sizeTxt, item.size)
            setChecked(R.id.item_zfile_list_file_box1, boxMap[position])
            setSelected(R.id.item_zfile_list_file_box2, boxMap[position])
            setImageRes(R.id.item_zfile_list_folderPic, folderRes)
            setImageRes(R.id.item_zfile_list_symlink, R.drawable.zfile_symlink)
            if(FileUtils.isSymlink(File(item.filePath))) {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.VISIBLE)
            } else {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.GONE)
            }
           // setBgColor(R.id.item_zfile_list_file_line, lineColor)
           // setVisibility(R.id.item_zfile_list_file_line, position < itemCount - 1)
            setVisibility(R.id.item_zfile_file_box_pic, !isManage)
            when (config.boxStyle) {
                ZFileConfiguration.STYLE1 -> setVisibility(R.id.item_zfile_list_file_box1, isManage)
                ZFileConfiguration.STYLE2 -> setVisibility(R.id.item_zfile_list_file_box2, isManage)
                else -> throwError("boxStyle")
            }
        }
    }

    fun boxLayoutClick(position: Int, item: ZFileBean) {
        if (isManage) { // 管理状态
            if (item canNotSelect config.canNotSelecteFileTypeArray) {
                context.toast(config.canNotSelecteFileTypeStr)
                return
            }
            boxClick(position, item)
            notifyItemChanged(position)
        } else { // 非管理状态
            isManage = !isManage
            if (config.needTwiceClick) { // 两次点击开启
                qwChangeListener?.invoke(isManage, item, false)
            } else { // 直接选择
                if (item canNotSelect config.canNotSelecteFileTypeArray) {
                    context.toast(config.canNotSelecteFileTypeStr)
                    return
                }
                boxClick(position, item)
                qwChangeListener?.invoke(isManage, item, true)
            }
            notifyDataSetChanged()
        }
        changeListener?.invoke(isManage, selectData.size)
    }

    private fun boxClick(position: Int, item: ZFileBean) {
        if (item canNotSelect config.canNotSelecteFileTypeArray) {
            context.toast(config.canNotSelecteFileTypeStr)
            notifyItemChanged(position)
            return
        }
        val isSelect = boxMap[position] ?: false
        if (isSelect) {
            selectData.remove(item)
            boxMap[position] = !isSelect
            resetCountMapByClick(item, true)
            changeListener?.invoke(isManage, selectData.size)
            qwChangeListener?.invoke(isManage, item, false)
        } else {
            val size = item.originaSize.toDouble() / 1048576 // byte -> MB
            if (size > config.maxSize.toDouble()) {
                context.toast(config.maxSizeStr)
                notifyItemChanged(position)
            } else {
                if (isQW) {
                    selectData.add(item)
                    boxMap[position] = !isSelect
                    changeListener?.invoke(isManage, selectData.size)
                    qwChangeListener?.invoke(isManage, item, true)
                } else {
                    if (selectData.size >= config.maxLength) {
                        context.toast(config.maxLengthStr)
                        notifyItemChanged(position)
                    } else {
                        selectData.add(item)
                        boxMap[position] = !isSelect
                        resetCountMapByClick(item, false)
                        changeListener?.invoke(isManage, selectData.size)
                        qwChangeListener?.invoke(isManage, item, true)
                    }
                }
            }
        }
    }

    private fun setFolderData(holder: ZFileViewHolder, item: ZFileBean, position: Int) {
        val hintBean = item getBadgeHintBean context
        holder.apply {
            setText(R.id.item_zfile_list_folderNameTxt, item.fileName)
            setHint(R.id.item_zfile_list_folderHintTxt, hintBean)
            setBadge(R.id.item_zfile_list_folderHintPic, hintBean)
            setImageRes(R.id.item_zfile_list_folderPic, folderRes)
            setImageRes(R.id.item_zfile_list_symlink, R.drawable.zfile_symlink)
            if(FileUtils.isSymlink(File(item.filePath))) {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.VISIBLE)
            } else {
                holder.setVisibility(R.id.item_zfile_list_symlink, View.GONE)
            }
           // setBgColor(R.id.item_zfile_list_folder_line, lineColor)
          //  setVisibility(R.id.item_zfile_list_folder_line, position < itemCount - 1)
            if (config.showSelectedCountHint) {
                val count = getCountByMap(item)
                setText(R.id.item_zfile_list_folderCountTxt, "$count")
                setVisibility(R.id.item_zfile_list_folderCountTxt, count > 0 && countMap.keys indexOf item.filePath)
            } else {
                setVisibility(R.id.item_zfile_list_folderCountTxt, false)
            }
        }
    }

    fun setQWLastState(bean: ZFileBean?) {
        var lastIndex = -1
        getDatas().indices.forEach forEach@{
            if (getItem(it) == bean) {
                lastIndex = it
                return@forEach
            }
        }
        if (lastIndex != -1) {
            selectData.remove(bean)
            boxMap[lastIndex] = false
            notifyItemChanged(lastIndex)
        }
    }

    fun reset() {
        selectData.clear()
        boxMap.clear()
        countMap.clear()
    }

    private fun clearSelectMap() {
        selectData.clear()
        for ((k, _) in boxMap) {
            boxMap[k] = false
        }
        notifyDataSetChanged()
    }

    private fun resetCountMapByClick(item: ZFileBean, remove: Boolean) {
        try {
            val value = countMap[item.parent] ?: 0
            if (remove) { // 移除
                countMap[item.parent] = value - 1
            } else { // 新增
                countMap[item.parent] = value + 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCountByMap(item: ZFileBean) : Int {
        var count = 0
        for ((k, v) in countMap) {
            if (k.indexOf(item.fileName) >= 0) {
                count += v
            }
        }
        return count
    }

    private fun clearCountMap() {
        countMap.clear()
    }
}
