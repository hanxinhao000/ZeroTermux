package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import com.termux.R

class LoadingDialog : BaseDialogCentre {
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {

    }

    override fun getContentView(): Int {

        return R.layout.dialog_loading
    }
}
