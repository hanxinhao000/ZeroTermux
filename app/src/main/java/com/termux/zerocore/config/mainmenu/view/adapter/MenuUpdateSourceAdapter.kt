package com.termux.zerocore.config.mainmenu.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.config.mainmenu.MenuUpdateSource
import com.termux.zerocore.config.mainmenu.MenuUpdateSourceManager

class MenuUpdateSourceAdapter(
    private var items: List<MenuUpdateSource>,
    private var selectedId: String,
    private val listener: Listener
) : RecyclerView.Adapter<MenuUpdateSourceAdapter.ViewHolder>() {

    interface Listener {
        fun onSourceSelected(source: MenuUpdateSource)
        fun onAddSourceClick()
        fun onEditSourceClick(source: MenuUpdateSource)
        fun onDeleteSourceClick(source: MenuUpdateSource)
    }

    fun updateData(newItems: List<MenuUpdateSource>, newSelectedId: String) {
        items = newItems
        selectedId = newSelectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_update_source, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val source = items[position]
        holder.icon.setImageResource(
            if (source.isAddAction) R.mipmap.add_image_back else R.mipmap.nginx
        )
        holder.title.text = MenuUpdateSourceManager.getSourceLabel(source)
        holder.edit.visibility = View.GONE
        holder.delete.visibility = View.GONE
        holder.edit.setOnClickListener(null)
        holder.delete.setOnClickListener(null)

        if (source.isAddAction) {
            holder.url.visibility = View.GONE
            holder.card.setCardBackgroundColor(UUtils.getColor(R.color.color_55000000))
            holder.itemView.setOnClickListener { listener.onAddSourceClick() }
            return
        }

        holder.url.visibility = View.VISIBLE
        holder.url.text = source.url
        val selected = MenuUpdateSourceManager.isSelected(source, selectedId)
        holder.card.setCardBackgroundColor(
            UUtils.getColor(if (selected) R.color.color_5548baf3 else R.color.color_55000000)
        )
        holder.itemView.setOnClickListener { listener.onSourceSelected(source) }

        if (MenuUpdateSourceManager.isCustomSource(source)) {
            holder.edit.visibility = View.VISIBLE
            holder.delete.visibility = View.VISIBLE
            holder.edit.setOnClickListener { listener.onEditSourceClick(source) }
            holder.delete.setOnClickListener { listener.onDeleteSourceClick(source) }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.menu_update_source_card)
        val icon: ImageView = itemView.findViewById(R.id.menu_update_source_icon)
        val title: TextView = itemView.findViewById(R.id.menu_update_source_title)
        val url: TextView = itemView.findViewById(R.id.menu_update_source_url)
        val edit: TextView = itemView.findViewById(R.id.menu_update_source_edit)
        val delete: TextView = itemView.findViewById(R.id.menu_update_source_delete)
    }
}
