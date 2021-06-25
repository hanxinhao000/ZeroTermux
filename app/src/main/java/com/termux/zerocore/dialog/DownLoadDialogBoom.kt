package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.daimajia.numberprogressbar.NumberProgressBar
import com.termux.R

class DownLoadDialogBoom : BaseDialogDown {

    private var recycler_view:RecyclerView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
    }

    override fun getContentView(): Int {

        return R.layout.dialog_text_download
    }


    //name
    class DownLoadViewHolder : RecyclerView.ViewHolder{

        public var name:TextView? = null
        public var size:TextView? = null
        public var download:ImageView? = null
        public var number_progress_bar: NumberProgressBar? = null

        constructor(itemView: View) : super(itemView){


        }
    }
}
