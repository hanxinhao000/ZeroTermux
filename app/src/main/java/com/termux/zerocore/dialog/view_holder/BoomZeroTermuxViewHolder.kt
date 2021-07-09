package com.termux.zerocore.dialog.view_holder

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class BoomZeroTermuxViewHolder : RecyclerView.ViewHolder {
    public var msg:TextView? = null
    public var title:TextView? = null
    public var msg_card:CardView? = null
    constructor(itemView: View) : super(itemView){

        msg = itemView.findViewById(R.id.msg)
        title = itemView.findViewById(R.id.title)
        msg_card = itemView.findViewById(R.id.msg_card)


    }
}
