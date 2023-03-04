package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogCentre
import com.example.xh_lib.utils.UUtils
import com.termux.R

class SwitchDialog : BaseDialogCentre {

    public var title:TextView? = null
    public var msg:TextView? = null
    public var other:TextView? = null
    public var ok:TextView? = null
    public var cancel:TextView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        title = mView.findViewById(R.id.title)
        msg = mView.findViewById(R.id.msg)
        other = mView.findViewById(R.id.other)
        ok = mView.findViewById(R.id.ok)
        cancel = mView.findViewById(R.id.cancel)

    }

    override fun getContentView(): Int {

        return R.layout.dialog_switch

    }

    public fun createSwitchDialog(msg: String): SwitchDialog {
       // title?.visibility = View.INVISIBLE
        title?.text = UUtils.getString(R.string.提示)
        other?.visibility = View.GONE
        ok?.text = UUtils.getString(R.string.确定)
        cancel?.text = UUtils.getString(R.string.取消)
        cancel?.setOnClickListener {
            dismiss()
        }
        this.msg?.text = msg
        return this
    }

}
