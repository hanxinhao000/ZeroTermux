package com.termux.zerocore.dialog.view_holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class CommonCommandsHolder: RecyclerView.ViewHolder {
    public var line_tv:TextView? = null
    public var clip_name:TextView? = null
    public var clip_msg:TextView? = null
    public var clip_commend:TextView? = null
    public var delete:TextView? = null
    constructor(itemView: View) : super(itemView) {
        line_tv = itemView.findViewById<TextView>(R.id.line_tv)
        clip_name = itemView.findViewById<TextView>(R.id.clip_name)
        clip_commend = itemView.findViewById<TextView>(R.id.clip_commend)
        clip_msg = itemView.findViewById<TextView>(R.id.clip_msg)
        delete = itemView.findViewById<TextView>(R.id.delete)

    }
}
