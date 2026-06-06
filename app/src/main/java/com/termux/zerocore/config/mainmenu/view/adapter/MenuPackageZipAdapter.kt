package com.termux.zerocore.config.mainmenu.view.adapter

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.back.viewholder.RestoreViewHolder
import com.termux.zerocore.utils.FileIOUtils
import java.io.File

class MenuPackageZipAdapter(
    private val zipFiles: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<RestoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder(UUtils.getViewLayViewGroup(R.layout.item_restore, parent))
    }

    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val file = zipFiles[position]
        holder.mDataName?.text = "${UUtils.getString(R.string.data_file_name)} ${file.name}"
        holder.mDataSize?.text = "${UUtils.getString(R.string.data_file_size)} ${FileIOUtils.formatFileSize(file.length())}"
        holder.mDataPath?.text = "${UUtils.getString(R.string.data_file_path)} ${file.absolutePath}"
        holder.mDataName?.setTextColor(UUtils.getColor(R.color.color_48baf3))
        holder.mDelete?.visibility = android.view.View.GONE
        holder.itemView.setOnClickListener { onItemClick(file) }
    }

    override fun getItemCount(): Int = zipFiles.size
}
