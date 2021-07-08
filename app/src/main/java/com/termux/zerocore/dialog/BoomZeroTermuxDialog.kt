package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.termux.R
import com.termux.zerocore.popuwindow.NginxPopuWindow

class BoomZeroTermuxDialog : BaseDialogDown, View.OnClickListener {

    private var nginx:LinearLayout? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        nginx = mView.findViewById(R.id.nginx)
        nginx!!.setOnClickListener(this)

    }

    override fun getContentView(): Int {

        return R.layout.dialog_zero_termux
    }

    override fun onClick(v: View?) {
        when(v!!.id){

            R.id.nginx->{


                val nginxPopuWindow = NginxPopuWindow(mContext)
                nginxPopuWindow.showAsDropDown(nginx)


            }



        }
    }
}
