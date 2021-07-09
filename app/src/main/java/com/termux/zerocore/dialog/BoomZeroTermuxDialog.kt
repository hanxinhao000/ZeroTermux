package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.termux.R
import com.termux.zerocore.popuwindow.NginxPopuWindow
import com.termux.zerocore.popuwindow.WebStartPopuWindow

class BoomZeroTermuxDialog : BaseDialogDown, View.OnClickListener {

    private var nginx:LinearLayout? = null
    private var web_start:LinearLayout? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        nginx = mView.findViewById(R.id.nginx)
        web_start = mView.findViewById(R.id.web_start)
        nginx!!.setOnClickListener(this)
        web_start!!.setOnClickListener(this)

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
            R.id.web_start->{


                val webStartPopuWindow = WebStartPopuWindow(mContext)
                webStartPopuWindow.showAsDropDown(web_start)



            }



        }
    }
}
