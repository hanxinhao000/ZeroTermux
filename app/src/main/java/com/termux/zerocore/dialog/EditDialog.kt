package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.termux.R

class EditDialog : BaseDialogCentre {
    public var edit_text:EditText? = null
    public var ok:TextView? = null
    public var cancel:TextView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {
        edit_text = mView?.findViewById(R.id.edit_text)
        ok = mView?.findViewById(R.id.ok)
        cancel = mView?.findViewById(R.id.cancel)
    }

    override fun getContentView(): Int {

        return R.layout.dialog_edit
    }
}
