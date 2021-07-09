package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.termux.R
import com.termux.zerocore.bean.ZeroRunCommandBean
import com.termux.zerocore.data.LinuxCommandData
import com.termux.zerocore.dialog.adapter.BoomZeroTermuxAdapter
import com.termux.zerocore.popuwindow.NginxPopuWindow
import com.termux.zerocore.popuwindow.WebStartPopuWindow

class BoomZeroTermuxDialog : BaseDialogDown {

    private var recycler_view:RecyclerView? = null


    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        recycler_view = mView.findViewById(R.id.recycler_view)



        recycler_view!!.layoutManager = GridLayoutManager(mContext,4)

        var mList:ArrayList<ZeroRunCommandBean> = LinuxCommandData.getZeroRunCommandBeanData()


        val boomZeroTermuxAdapter = BoomZeroTermuxAdapter(mList, mContext as Activity)

        boomZeroTermuxAdapter.setDissListener(object : BoomZeroTermuxAdapter.DissListener{
            override fun close() {
                dismiss()
            }
        })

        recycler_view!!.adapter = boomZeroTermuxAdapter
    }

    override fun getContentView(): Int {

        return R.layout.dialog_zero_termux
    }


}
