package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.UUUtils

class VNCConnectionDialog : BaseDialogCentre {

    public var title:TextView? = null
    public var ok:TextView? = null
    public var cancel:TextView? = null
    public var address:EditText? = null
    public var port:EditText? = null
    public var password:EditText? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {
        title = mView?.findViewById(R.id.title)
        ok = mView?.findViewById(R.id.ok)
        cancel = mView?.findViewById(R.id.cancel)
        title?.text = UUtils.getString(R.string.连接到自定VNC)
        address = mView?.findViewById(R.id.address)
        port = mView?.findViewById(R.id.port)
        password = mView?.findViewById(R.id.password)

        cancel?.setOnClickListener {
            dismiss()
        }
    }

    override fun getContentView(): Int {

        return R.layout.dialog_vnc_connection
    }
}
