package com.termux.zerocore.config.mainmenu.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.zerocore.config.mainmenu.MainMenuPackageInfo
import com.termux.zerocore.config.mainmenu.MainMenuPackageManager

class MainMenuPackageAdapter(
    private var items: List<MainMenuPackageInfo>,
    private val listener: Listener
) : RecyclerView.Adapter<MainMenuPackageAdapter.ViewHolder>() {

    private var networkUpdating = false

    interface Listener {
        fun onNetworkUpdate()
        fun onPackageSelected(info: MainMenuPackageInfo)
        fun onInstallClick()
        fun onBackupClick(info: MainMenuPackageInfo)
        fun onDeleteClick(info: MainMenuPackageInfo)
    }

    fun updateItems(newItems: List<MainMenuPackageInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setNetworkUpdating(updating: Boolean) {
        if (networkUpdating == updating) {
            return
        }
        networkUpdating = updating
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_package, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = items[position]
        holder.title.text = info.label

        holder.normalRow.visibility = View.VISIBLE
        holder.installRow.visibility = View.GONE
        holder.updateWrap.visibility = View.GONE
        holder.delete.visibility = View.GONE
        holder.backup.visibility = View.GONE
        holder.content.setOnClickListener(null)
        holder.installRow.setOnClickListener(null)
        holder.delete.setOnClickListener(null)
        bindInstallTime(holder, info, "")

        when (info.type) {
            MainMenuPackageInfo.TYPE_PROGRAM -> {
                holder.content.setOnClickListener { listener.onPackageSelected(info) }
            }
            MainMenuPackageInfo.TYPE_DEFAULT, MainMenuPackageInfo.TYPE_NETWORK -> {
                bindInstallTime(holder, info, UUtils.getString(R.string.menu_package_install_time_prefix))
                holder.updateWrap.visibility = View.VISIBLE
                holder.backup.visibility = View.VISIBLE
                bindNetworkUpdateState(holder)
                holder.content.setOnClickListener { listener.onPackageSelected(info) }
                holder.updateWrap.setOnClickListener {
                    if (!networkUpdating) {
                        listener.onNetworkUpdate()
                    }
                }
                holder.backup.setOnClickListener { listener.onBackupClick(info) }
            }
            MainMenuPackageInfo.TYPE_INSTALL -> {
                holder.normalRow.visibility = View.GONE
                holder.installRow.visibility = View.VISIBLE
                holder.installRow.setOnClickListener { listener.onInstallClick() }
            }
            else -> {
                bindInstallTime(holder, info, UUtils.getString(R.string.menu_package_install_time_prefix))
                holder.delete.visibility = View.VISIBLE
                holder.backup.visibility = View.VISIBLE
                holder.content.setOnClickListener { listener.onPackageSelected(info) }
                holder.delete.setOnClickListener { listener.onDeleteClick(info) }
                holder.backup.setOnClickListener { listener.onBackupClick(info) }
            }
        }

        val activeColor = UUtils.getColor(R.color.color_48baf3)
        val normalColor = UUtils.getColor(R.color.color_ffffff)
        if (info.isActive) {
            holder.title.setTextColor(activeColor)
            holder.time.setTextColor(activeColor)
        } else {
            holder.title.setTextColor(normalColor)
            holder.time.setTextColor(UUtils.getColor(R.color.md_grey_500))
        }
    }

    override fun getItemCount(): Int = items.size

    private fun bindInstallTime(holder: ViewHolder, info: MainMenuPackageInfo, prefix: String) {
        val timeText = MainMenuPackageManager.formatInstallTime(info.installTime)
        if (timeText.isEmpty()) {
            holder.time.visibility = View.GONE
        } else {
            holder.time.visibility = View.VISIBLE
            holder.time.text = if (prefix.isEmpty()) timeText else "$prefix$timeText"
        }
    }

    private fun bindNetworkUpdateState(holder: ViewHolder) {
        if (networkUpdating) {
            holder.update.visibility = View.GONE
            holder.updateLoading.visibility = View.VISIBLE
            holder.updateWrap.isEnabled = false
        } else {
            holder.update.visibility = View.VISIBLE
            holder.updateLoading.visibility = View.GONE
            holder.updateWrap.isEnabled = true
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val normalRow: LinearLayout = itemView.findViewById(R.id.menu_package_normal_row)
        val installRow: FrameLayout = itemView.findViewById(R.id.menu_package_install_row)
        val installIcon: ImageView = itemView.findViewById(R.id.menu_package_install_icon)
        val content: LinearLayout = itemView.findViewById(R.id.menu_package_content)
        val title: TextView = itemView.findViewById(R.id.menu_package_title)
        val time: TextView = itemView.findViewById(R.id.menu_package_time)
        val updateWrap: FrameLayout = itemView.findViewById(R.id.menu_package_update_wrap)
        val update: TextView = itemView.findViewById(R.id.menu_package_update)
        val updateLoading: ProgressBar = itemView.findViewById(R.id.menu_package_update_loading)
        val delete: TextView = itemView.findViewById(R.id.menu_package_delete)
        val backup: TextView = itemView.findViewById(R.id.menu_package_backup)
    }
}
