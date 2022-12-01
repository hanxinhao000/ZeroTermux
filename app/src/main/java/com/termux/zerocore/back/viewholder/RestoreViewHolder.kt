package com.termux.zerocore.back.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class RestoreViewHolder: RecyclerView.ViewHolder {
    public var mDataName: TextView? = null
    public var mDataSize: TextView? = null
    public var mDataPath: TextView? = null
    public var mDelete: TextView? = null
    constructor(itemView: View) : super(itemView) {
        mDataName = itemView.findViewById(R.id.data_name)
        mDataSize = itemView.findViewById(R.id.data_size)
        mDataPath = itemView.findViewById(R.id.data_path)
        mDelete = itemView.findViewById(R.id.delete)
    }
}
