package com.termux.zerocore.dialog.view_holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class ItemMenuViewHolder : RecyclerView.ViewHolder {
    public var menu_image: ImageView? = null
    public var menu_name: TextView? = null
    public var eg_install_tv: ImageView? = null
    public var card_view: CardView? = null
    constructor(itemView: View) : super(itemView) {
        menu_image = itemView.findViewById(R.id.menu_image)
        menu_name = itemView.findViewById(R.id.menu_name)
        eg_install_tv = itemView.findViewById(R.id.eg_install_tv)
        card_view = itemView.findViewById(R.id.card_view)
    }
}
